(defproject duct/migrator.ragtime "0.1.2"
  :description "Integrant methods for running database migrations using Ragtime"
  :url "https://github.com/duct-framework/migrator.ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [duct/core "0.6.0"]
                 [duct/logger "0.1.1"]
                 [integrant "0.6.1"]
                 [pandect "0.6.1"]
                 [ragtime "0.7.1"]]
  :profiles
  {:dev {:dependencies [[duct/database.sql "0.1.0"]
                        [org.clojure/java.jdbc "0.7.0"]
                        [org.xerial/sqlite-jdbc "3.20.0"]]}})
