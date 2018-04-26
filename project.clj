(defproject duct/migrator.ragtime "0.2.1"
  :description "Integrant methods for running database migrations using Ragtime"
  :url "https://github.com/duct-framework/migrator.ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [duct/core "0.6.2"]
                 [duct/logger "0.2.1"]
                 [integrant "0.6.3"]
                 [pandect "0.6.1"]
                 [ragtime "0.7.2"]]
  :profiles
  {:dev {:dependencies [[duct/database.sql "0.1.0"]
                        [org.clojure/java.jdbc "0.7.6"]
                        [org.xerial/sqlite-jdbc "3.21.0.1"]]}})
