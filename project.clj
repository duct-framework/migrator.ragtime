(defproject org.duct-framework/migrator.ragtime "0.6.0"
  :description "Integrant methods for running database migrations using Ragtime"
  :url "https://github.com/duct-framework/migrator.ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [org.duct-framework/logger "0.4.0"]
                 [dev.weavejester/ragtime "0.12.1"]
                 [integrant "1.0.0"]
                 [pandect "1.0.2"]]
  :profiles
  {:dev {:dependencies [[org.duct-framework/database.sql "0.4.1"]
                        [com.github.seancorfield/next.jdbc "1.3.1070"]
                        [org.xerial/sqlite-jdbc "3.50.3.0"]]}})
