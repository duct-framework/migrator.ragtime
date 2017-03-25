(ns duct.migrator.ragtime
  (:require [integrant.core :as ig]
            [pandect.algo.sha1 :refer [sha1]]
            [ragtime.repl :as repl]
            [ragtime.jdbc :as jdbc]
            [ragtime.strategy :as strategy]))

(def strategies
  {:apply-new   strategy/apply-new
   :raise-error strategy/raise-error
   :rebase      strategy/rebase})

(defmethod ig/init-key :duct.migrator/ragtime
  [_ {:keys [database strategy migrations] :or {strategy :raise-error}}]
  (repl/migrate {:datastore  (jdbc/sql-database (:spec database))
                 :migrations migrations
                 :strategy   (strategies strategy)}))
