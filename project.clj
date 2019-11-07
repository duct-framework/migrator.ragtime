(defproject duct/migrator.ragtime "0.3.1"
  :description "Integrant methods for running database migrations using Ragtime"
  :url "https://github.com/duct-framework/migrator.ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [duct/core "0.7.0"]
                 [duct/logger "0.3.0"]
                 [integrant "0.7.0"]
                 [pandect "0.6.1"]
                 [ragtime "0.8.0"]]
  :profiles
  {:dev {:dependencies [[duct/database.sql "0.1.0"]
                        [org.clojure/java.jdbc "0.7.8"]
                        [org.xerial/sqlite-jdbc "3.25.2"]]}})
