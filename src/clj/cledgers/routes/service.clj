(ns cledgers.routes.service
  (:require [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [java-time :as time]
            [next.jdbc :as jdbc]
            [honey.sql :as honey]
            [ring.middleware.transit :as ring-transit]
            [cledgers.db.core :as db]
            [cledgers.utils :as utils]))

(defn get-payees [q]
  (let [the-hsql {:select [:id :name]
                  :from [:payee]
                  :where [:like :name (str q "%")]
                  :limit 10}
        results (jdbc/execute!
                 db/*db*
                 (honey/format the-hsql))]
    results))

(defn service-routes []
  ["/api"
   {:middleware [ring-transit/wrap-transit-response
                 ring-transit/wrap-transit-body]}
   ["/ping"
    {:get (fn [_req]
            {:status 200
             :body {:pong true}})}]
   ["/payees"
    {:get
     (fn [req]
       (let [#_ (pp/pprint {:req _req
                            #_#_:req-keys (keys _req)})
             q-parm (get-in req [:params :q])
             result (get-payees q-parm)]
         {:status 200
          :body {:result result}}))}]
   ["/ledgers"
    {:get
     (fn [req]
       (let [q-parm (get-in req [:params :q])
             result
             (let [the-hql
                   {:select [:id :name]
                    :from [:ledger]
                    :where [:like :name (str q-parm "%")]
                    :limit 10}
                   results
                   (jdbc/execute! db/*db* (honey/format the-hql))]
               results)]
         {:status 200
          :body {:result result}}))}]
   ["/xactions"
    {:post
     (fn [req]
       (jdbc/with-transaction [tx-conn db/*db*]
         (let [#_#_user-id (get-in req [:session :identity :id])
               ;; temporarily just get frank's id until we have
               ;; login/logout setup
               user-id (:id (db/get-frank-id))
               _ (when-not user-id
                   (throw (ex-info "must have a user id" {})))
               {:keys [payee ledger date] :as xaction} (get-in req [:body :xaction])
               _ (pp/pprint {#_#_:date date
                             #_#_:xaction xaction
                             #_#_:body (:body req)
                             #_#_:req req})
               payee-id
               (if-not (:is-new payee)
                 (:id payee)
                 (let [payee (assoc payee :created-by-id user-id)
                       create-res (db/create-payee! tx-conn payee)]
                   (:id create-res)))
               ledger-id
               (if-not (:is-new ledger)
                 (:id ledger)
                 (let [ledger (assoc ledger :created-by-id user-id)
                       create-res (db/create-ledger! tx-conn ledger)]
                   (:id create-res)))
               new-date (time/local-date
                         (:year date)
                         (:month date)
                         (:day date))
               updated-xaction (-> xaction
                                   (dissoc :payee)
                                   (dissoc :ledger)
                                   (merge {:date new-date
                                           :amount (-> xaction :amount bigdec)
                                           :created-by-id user-id
                                           :payee-id payee-id
                                           :ledger-id ledger-id}))
               _ (log/debug "new-xaction:" (utils/pp-str updated-xaction))
               #_ (log/debug (str "xactions post request:\n"
                                  (utils/pp-str {:request request})))
               _ (db/create-xaction! tx-conn updated-xaction)]
           {:status 200})))}]])
