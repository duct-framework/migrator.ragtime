(defproject org.duct-framework/migrator.ragtime "0.4.0"
  :description "Integrant methods for running database migrations using Ragtime"
  :url "https://github.com/duct-framework/migrator.ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.4"]
                 [org.duct-framework/logger "0.4.0"]
                 [dev.weavejester/ragtime "0.10.1"]
                 [integrant "0.13.1"]
                 [pandect "1.0.2"]]
  :profiles
  {:dev {:dependencies [[org.duct-framework/database.sql "0.3.0"]
                        [com.github.seancorfield/next.jdbc "1.3.955"]
                        [org.xerial/sqlite-jdbc "3.47.0.0"]]}})
