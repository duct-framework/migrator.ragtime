(ns duct.migrator.ragtime
  (:require [duct.logger :as logger]
            [integrant.core :as ig]
            [pandect.algo.sha1 :refer [sha1]]
            [ragtime.core :as ragtime]
            [ragtime.sql :as sql]
            [ragtime.next-jdbc :as jdbc]
            [ragtime.reporter :as reporter]
            [ragtime.strategy :as strategy]))

(defn logger-reporter [logger]
  (fn [_ op id]
    (case op
      :up   (logger/report logger ::applying {:id id})
      :down (logger/report logger ::rolling-back {:id id}))))

(def strategies
  {:apply-new     strategy/apply-new
   :raise-error   strategy/raise-error
   :rebase        strategy/rebase
   :ignore-future strategy/ignore-future})

(def ^:private colon (.getBytes ":"  "US-ASCII"))
(def ^:private comma (.getBytes ","  "US-ASCII"))
(def ^:private u=    (.getBytes "u=" "US-ASCII"))
(def ^:private d=    (.getBytes "d=" "US-ASCII"))

(defn- netstring [bs]
  (let [size (.getBytes (str (count bs)) "US-ASCII")]
    (byte-array (concat size colon bs comma))))

(defn- get-bytes [s]
  (.getBytes s "UTF-8"))

(defn- coll->netstring [coll]
  (netstring (mapcat (comp netstring get-bytes) coll)))

(defn- hash-migration [{:keys [up down]}]
  (sha1 (byte-array (concat u= (coll->netstring up)
                            d= (coll->netstring down)))))

(defn- add-hash-to-id [migration]
  (update migration :id str "#" (subs (hash-migration migration) 0 8)))

(defn- get-migrations [{:keys [migrations]}]
  (->> (sql/->migrations migrations)
       (map (comp jdbc/sql-migration add-hash-to-id))))

(defn- get-database [{:keys [database] :as opts}]
  (jdbc/sql-database database (select-keys opts [:migrations-table])))

(defn- get-strategy [{:keys [strategy] :or {strategy :raise-error}}]
  (strategies strategy))

(defn- get-reporter [{:keys [logger]}]
  (if logger (logger-reporter logger) reporter/print))

(defn- migrate [index options]
  (let [db    (get-database options)
        strat (get-strategy options)
        rep   (get-reporter options)
        migs  (get-migrations options)]
    (ragtime/migrate-all db index migs {:reporter rep, :strategy strat})
    (ragtime/into-index index migs)))

(defmethod ig/init-key :duct.migrator/ragtime [_ options]
  (migrate {} options))

(defmethod ig/resume-key :duct.migrator/ragtime [_ options _ index]
  (migrate index options))
