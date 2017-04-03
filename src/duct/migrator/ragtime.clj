(ns duct.migrator.ragtime
  (:require [clojure.java.io :as io]
            [duct.core :as duct]
            [integrant.core :as ig]
            [pandect.algo.sha1 :refer [sha1]]
            [ragtime.core :as ragtime]
            [ragtime.jdbc :as jdbc]
            [ragtime.reporter :as reporter]
            [ragtime.strategy :as strategy]))

(defn logger-reporter [logger]
  (fn [_ op id]
    (case op
      :up   (duct/log logger :info ::apply id)
      :down (duct/log logger :info ::rollback id))))

(def strategies
  {:apply-new   strategy/apply-new
   :raise-error strategy/raise-error
   :rebase      strategy/rebase})

(defprotocol StringSource
  (get-string [source]))

(extend-protocol StringSource
  String
  (get-string [s] s)
  java.net.URL
  (get-string [s] (slurp s)))

(defn- singularize [coll]
  (if (= (count coll) 1) (first coll) coll))

(defn- clean-key [base key]
  (if (vector? key)
    (singularize (remove #{base} key))
    key))

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

(defn- migrate [index {:keys [database migrations strategy logger]
                       :or   {strategy :raise-error}}]
  (ragtime/migrate-all (jdbc/sql-database (:spec database)) index migrations
                       {:strategy (strategies strategy)
                        :reporter (if logger
                                    (logger-reporter logger)
                                    reporter/print)})
  (ragtime/into-index index migrations))

(defmethod ig/init-key :duct.migrator/ragtime [_ options]
  (migrate {} options))

(defmethod ig/resume-key :duct.migrator/ragtime [_ options _ index]
  (migrate index options))

(defmethod ig/init-key ::sql [key {:keys [up down] :as opts}]
  (-> (jdbc/sql-migration {:id   (:id opts (clean-key ::sql key))
                           :up   (mapv get-string up)
                           :down (mapv get-string down)})
      (add-hash-to-id)))
