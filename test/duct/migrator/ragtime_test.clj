(ns duct.migrator.ragtime-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [duct.core :as duct]
            [duct.database.sql :as sql]
            [duct.logger :as logger]
            [duct.migrator.ragtime :as ragtime]
            [integrant.core :as ig]))

(duct/load-hierarchy)

(defrecord TestLogger [logs]
  logger/Logger
  (-log [_ level ns-str file line event data]
    (swap! logs conj [event data])))

(def logs
  (atom []))

(def db-spec
  {:connection (jdbc/get-connection {:connection-uri "jdbc:sqlite:"})})

(def config
  {:duct.database/sql db-spec

   :duct.migrator/ragtime
   {:database   (ig/ref :duct.database/sql)
    :strategy   :rebase
    :logger     (->TestLogger logs)
    :migrations [(ig/ref ::create-foo)
                 (ig/ref ::create-bar)]}

   [:duct.migrator.ragtime/sql ::create-foo]
   {:up   ["CREATE TABLE foo (id int);"]
    :down ["DROP TABLE foo;"]}

   [:duct.migrator.ragtime/sql ::create-bar]
   {:up   ["CREATE TABLE bar (id int);"]
    :down ["DROP TABLE bar;"]}})

(defn- find-tables []
  (jdbc/query db-spec ["SELECT name FROM sqlite_master WHERE type='table'"]))

(defn- drop-all-tables []
  (doseq [t (find-tables)]
    (jdbc/execute! db-spec [(str "DROP TABLE " (:name t))])))

(deftest migration-test
  (reset! logs [])
  (drop-all-tables)
  (let [system (ig/init config)]
    (is (= (find-tables)
           [{:name "ragtime_migrations"}
            {:name "foo"}
            {:name "bar"}]))
    (is (= @logs
           [[::ragtime/applying ":duct.migrator.ragtime-test/create-foo#f1480e44"]
            [::ragtime/applying ":duct.migrator.ragtime-test/create-bar#6d969ce8"]]))))

(deftest remove-and-resume-test
  (reset! logs [])
  (drop-all-tables)
  (let [system  (ig/init config)
        config' (update-in config [:duct.migrator/ragtime :migrations] pop)
        system' (ig/resume config' system)]
    (is (= (find-tables)
           [{:name "ragtime_migrations"}
            {:name "foo"}]))
    (is (= @logs
           [[::ragtime/applying ":duct.migrator.ragtime-test/create-foo#f1480e44"]
            [::ragtime/applying ":duct.migrator.ragtime-test/create-bar#6d969ce8"]
            [::ragtime/rolling-back ":duct.migrator.ragtime-test/create-bar#6d969ce8"]]))))

(deftest change-and-resume-test
  (reset! logs [])
  (drop-all-tables)
  (let [system  (ig/init config)
        config' (assoc config
                       [:duct.migrator.ragtime/sql ::create-bar]
                       {:up   ["CREATE TABLE barbaz (id int)"]
                        :down ["DROP TABLE barbaz"]})
        system' (ig/resume config' system)]
    (is (= (find-tables)
           [{:name "ragtime_migrations"}
            {:name "foo"}
            {:name "barbaz"}]))
    (is (= @logs
           [[::ragtime/applying ":duct.migrator.ragtime-test/create-foo#f1480e44"]
            [::ragtime/applying ":duct.migrator.ragtime-test/create-bar#6d969ce8"]
            [::ragtime/rolling-back ":duct.migrator.ragtime-test/create-bar#6d969ce8"]
            [::ragtime/applying ":duct.migrator.ragtime-test/create-bar#66068fd2"]]))))
