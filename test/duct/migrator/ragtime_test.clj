(ns duct.migrator.ragtime-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [duct.core :as duct]
            [duct.database.sql :as sql]
            [duct.logger :as logger]
            [duct.migrator.ragtime :as ragtime]
            [integrant.core :as ig]
            [clojure.java.io :as io]))

(duct/load-hierarchy)

(defrecord TestLogger [logs]
  logger/Logger
  (-log [_ level ns-str file line id event data]
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

(def config-resources
  {:duct.database/sql db-spec

   :duct.migrator/ragtime
   {:database   (ig/ref :duct.database/sql)
    :strategy   :rebase
    :logger     (->TestLogger logs)
    :migrations (ig/ref :duct.migrator.ragtime/resources)}

   :duct.migrator.ragtime/resources
   {:path "duct/migrator/migrations"}})

(def config-directory
  {:duct.database/sql db-spec

   :duct.migrator/ragtime
   {:database   (ig/ref :duct.database/sql)
    :strategy   :rebase
    :logger     (->TestLogger logs)
    :migrations (ig/ref :duct.migrator.ragtime/directory)}

   :duct.migrator.ragtime/directory
   {:path "test/duct/migrator/migrations"}})

(def config-mixed
  {:duct.database/sql db-spec

   :duct.migrator/ragtime
   {:database   (ig/ref :duct.database/sql)
    :strategy   :rebase
    :logger     (->TestLogger logs)
    :migrations [(ig/ref :duct.migrator.ragtime/resources)
                 (ig/ref ::create-baz)]}

   :duct.migrator.ragtime/resources
   {:path "duct/migrator/migrations"}

   [:duct.migrator.ragtime/sql ::create-baz]
   {:up   ["CREATE TABLE baz (id int);"]
    :down ["DROP TABLE baz;"]}})

(defn- find-tables []
  (jdbc/query db-spec ["SELECT name FROM sqlite_master WHERE type='table'"]))

(defn- drop-all-tables []
  (doseq [t (find-tables)]
    (jdbc/execute! db-spec [(str "DROP TABLE " (:name t))])))

(defn reset-test-state []
  (reset! logs [])
  (drop-all-tables))

(deftest migration-test
  (testing "default migrations table name"
    (reset-test-state)
    (let [system (ig/init config)]
      (is (= (find-tables)
             [{:name "ragtime_migrations"} {:name "foo"} {:name "bar"}]))
      (is (= @logs
             [[::ragtime/applying ":duct.migrator.ragtime-test/create-foo#f1480e44"]
              [::ragtime/applying ":duct.migrator.ragtime-test/create-bar#6d969ce8"]]))))

  (testing "custom migrations table name"
    (reset-test-state)
    (let [migrations-table "custom_migrations"
          system (-> config
                     (assoc-in [:duct.migrator/ragtime :migrations-table] migrations-table)
                     ig/init)]
      (is (= (find-tables)
             [{:name migrations-table} {:name "foo"} {:name "bar"}]))
      (is (= @logs
             [[::ragtime/applying ":duct.migrator.ragtime-test/create-foo#f1480e44"]
              [::ragtime/applying ":duct.migrator.ragtime-test/create-bar#6d969ce8"]]))))

  (testing "migrations from resource directory"
    (reset-test-state)
    (let [system (ig/init config-resources)]
      (is (= (find-tables)
             [{:name "ragtime_migrations"} {:name "foo"} {:name "bar"}]))
      (is (= @logs
             [[::ragtime/applying "01-create-foo#f1480e44"]
              [::ragtime/applying "02-create-bar#6d969ce8"]]))))

  (testing "migrations from filesystem directory"
    (reset-test-state)
    (let [system (ig/init config-directory)]
      (is (= (find-tables)
             [{:name "ragtime_migrations"} {:name "foo"} {:name "bar"}]))
      (is (= @logs
             [[::ragtime/applying "01-create-foo#f1480e44"]
              [::ragtime/applying "02-create-bar#6d969ce8"]]))))

  (testing "migrations from multiple sources"
    (reset-test-state)
    (let [system (ig/init config-mixed)]
      (is (= (find-tables)
             [{:name "ragtime_migrations"} {:name "foo"} {:name "bar"} {:name "baz"}]))
      (is (= @logs
             [[::ragtime/applying "01-create-foo#f1480e44"]
              [::ragtime/applying "02-create-bar#6d969ce8"]
              [::ragtime/applying ":duct.migrator.ragtime-test/create-baz#01f7277d"]])))))

(deftest remove-and-resume-test
  (reset-test-state)
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
  (reset-test-state)
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

(deftest string-source-test
  (is (= "foo\n" (ragtime/get-string "foo\n")))
  (is (= "foo\n" (ragtime/get-string (io/resource "duct/migrator/example.txt"))))
  (is (= "foo\n" (ragtime/get-string (duct/resource "duct/migrator/example.txt")))))
