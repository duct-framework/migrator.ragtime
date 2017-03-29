(ns user
  (:require duct.database.sql
            duct.migrator.ragtime
            [integrant.core :as ig]
            [integrant.repl :refer [system prep init halt go reset set-prep!]]))

(def config
  {:duct.database/sql
   {:connection-uri "jdbc:sqlite:dev.sqlite"}

   :duct.migrator/ragtime
   {:database   (ig/ref :duct.database/sql)
    :strategy   :rebase
    :migrations [(ig/ref ::create-foo)]}

   [:duct.migrator.ragtime/sql ::create-foo]
   {:up   "CREATE TABLE foo (id int);"
    :down "DROP TABLE foo;"}})

(set-prep! (constantly config))
