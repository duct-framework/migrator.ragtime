(ns duct.migrator.ragtime-test
  (:require [clojure.test :refer [deftest is testing]]
            [duct.logger :as logger]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]))

(defrecord TestLogger [logs]
  logger/Logger
  (-log [_ _level _ns-str _file _line _id event data]
    (swap! logs conj [event data])))

(defmethod ig/init-key ::logger [_ _]
  (->TestLogger (atom [])))

(def base-config
  {::logger {}
   :duct.database/sql {}
   :duct.migrator/ragtime
   {:logger     (ig/ref ::logger)
    :database   (ig/ref :duct.database/sql)
    :strategy   :rebase
    :migrations '[[:create-table foo (id "INTEGER")]
                  [:create-table bar (id "INTEGER")]]}})

(defn- find-tables [db]
  (jdbc/execute! db ["SELECT name FROM sqlite_master WHERE type='table'"]))

(deftest migration-test
  (let [tempfile (java.io.File/createTempFile "duct" "db")
        jdbc-url (str "jdbc:sqlite:" tempfile)
        config   (-> base-config
                     (doto (ig/load-namespaces))
                     (assoc-in [:duct.database/sql :jdbcUrl] jdbc-url))
        system   (atom (ig/init config))]

    (testing "initial migrations"
      (let [logs (-> @system ::logger :logs)
            db   (-> @system :duct.database/sql)]
        (is (= ["ragtime_migrations" "foo" "bar"]
               (map :sqlite_master/name (find-tables db))))
        (is (= [[:duct.migrator.ragtime/applying {:id "create-table-foo#52bfa531"}]
                [:duct.migrator.ragtime/applying {:id "create-table-bar#3e718b28"}]]
               @logs))))

    (testing "change migration"
      (let [config (assoc-in config [:duct.migrator/ragtime :migrations]
                             '[[:create-table foo (id "INTEGER")]
                               [:create-table baz (id "INTEGER")]])
            system (swap! system (fn [sys] (ig/suspend! sys) (ig/resume config sys)))
            logs   (-> system ::logger :logs)
            db     (-> system :duct.database/sql)]
        (is (= ["ragtime_migrations" "foo" "baz"]
               (map :sqlite_master/name (find-tables db))))
        (is (= [[:duct.migrator.ragtime/rolling-back {:id "create-table-bar#3e718b28"}]
                [:duct.migrator.ragtime/applying {:id "create-table-baz#055a605e"}]]
               @logs))))

    (testing "remove migration"
      (let [config (assoc-in config [:duct.migrator/ragtime :migrations]
                             '[[:create-table foo (id "INTEGER")]])
            system (swap! system (fn [sys] (ig/suspend! sys) (ig/resume config sys)))
            logs   (-> system ::logger :logs)
            db     (-> system :duct.database/sql)]
        (is (= ["ragtime_migrations" "foo"]
               (map :sqlite_master/name (find-tables db))))
        (is (= [[:duct.migrator.ragtime/rolling-back {:id "create-table-baz#055a605e"}]]
               @logs))))

    (testing "empty migrations"
      (let [config (update config :duct.migrator/ragtime dissoc :migrations)
            system (swap! system (fn [sys] (ig/suspend! sys) (ig/resume config sys)))
            logs   (-> system ::logger :logs)
            db     (-> system :duct.database/sql)]
        (is (= ["ragtime_migrations"]
               (map :sqlite_master/name (find-tables db))))
        (is (= [[:duct.migrator.ragtime/rolling-back {:id "create-table-foo#52bfa531"}]]
               @logs))))

    (ig/halt! @system)
    (.delete tempfile)))
