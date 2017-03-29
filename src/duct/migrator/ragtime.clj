(ns duct.migrator.ragtime
  (:require [clojure.java.io :as io]
            [integrant.core :as ig]
            [pandect.algo.sha1 :refer [sha1]]
            [ragtime.core :as ragtime]
            [ragtime.jdbc :as jdbc]
            [ragtime.reporter :as reporter]
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

(defprotocol ByteSource
  (get-bytes [source]))

(extend-protocol ByteSource
  String
  (get-bytes [s]
    (.getBytes s "UTF-8"))
  java.net.URL
  (get-bytes [url]
    (let [out (java.io.ByteArrayOutputStream.)]
      (io/copy (io/input-stream url) out)
      (.toByteArray out))))

(defn- hash-migration [{:keys [up down]}]
  (sha1 (byte-array (concat (netstring (get-bytes up))
                            (netstring (get-bytes down))))))

(defn- singularize [coll]
  (if (= (count coll) 1) (first coll) coll))

(defn- clean-key [base key]
  (if (vector? key)
    (singularize (remove #{base} key))
    key))

(defn- generate-id [base key opts]
  (str (:id opts (clean-key base key)) "#" (subs (hash-migration opts) 0 8)))

(defn- migrate [index {:keys [database migrations strategy] :or {strategy :raise-error}}]
  (ragtime/migrate-all (jdbc/sql-database (:spec database)) index migrations
                       {:strategy (strategies strategy)
                        :reporter reporter/print})
  (ragtime/into-index index migrations))

(defmethod ig/init-key :duct.migrator/ragtime [_ options]
  (migrate {} options))

(defmethod ig/resume-key :duct.migrator/ragtime [_ options _ index]
  (migrate index options))

(defmethod ig/init-key ::sql [key {:keys [up down] :as opts}]
  (jdbc/sql-migration {:id   (generate-id ::sql key opts)
                       :up   [up]
                       :down [down]}))
