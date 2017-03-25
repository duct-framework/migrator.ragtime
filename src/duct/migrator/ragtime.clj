(ns duct.migrator.ragtime
  (:require [integrant.core :as ig]
            [pandect.algo.sha1 :refer [sha1]]
            [ragtime.repl :as repl]
            [ragtime.jdbc :as jdbc]
            [ragtime.strategy :as strategy]))

(def strategies
  {:apply-new   strategy/apply-new
   :raise-error strategy/raise-error
   :rebase      strategy/rebase})

(def ^:private colon (.getBytes ":" "US-ASCII"))
(def ^:private comma (.getBytes "," "US-ASCII"))

(defn- netstring [bs]
  (let [size (.getBytes (str (count bs)) "US-ASCII")]
    (byte-array (concat size colon bs comma))))

(defn- hash-migration [{:keys [up down]}]
  (sha1 (byte-array (concat (netstring (.getBytes up))
                            (netstring (.getBytes down))))))

(defn- generate-id [key opts]
  (str (:id opts key) ":" (subs (hash-migration opts) 0 8)))

(defmethod ig/init-key :duct.migrator/ragtime
  [_ {:keys [database strategy migrations] :or {strategy :raise-error}}]
  (repl/migrate {:datastore  (jdbc/sql-database (:spec database))
                 :migrations migrations
                 :strategy   (strategies strategy)}))

(defmethod ig/init-key ::sql [key opts]
  (jdbc/sql-migration (assoc opts :id (generate-id key opts))))
