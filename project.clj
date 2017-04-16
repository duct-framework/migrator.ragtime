(defproject duct/migrator.ragtime "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [duct/logger "0.1.1"]
                 [integrant "0.3.3"]
                 [pandect "0.6.1"]
                 [ragtime "0.7.0"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[duct/database.sql "0.1.0"]
                        [integrant/repl "0.2.0"]
                        [org.clojure/java.jdbc "0.7.0-alpha3"]
                        [org.xerial/sqlite-jdbc "3.16.1"]]}})
