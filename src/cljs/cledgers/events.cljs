(ns cledgers.events
  (:require
   [clojure.pprint :as pp]
   [cljs-time.core :as time]
    [re-frame.core :as rf]
    [ajax.core :as cljs-ajax]
    [reitit.frontend.easy :as rfe]
    [reitit.frontend.controllers :as rfc]
    [cognitect.transit :as transit]))

;;dispatchers

(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                                 (rfc/apply-controllers (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-fx
  :fetch-docs
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (cljs-ajax/raw-response-format)
                  :on-success       [:set-docs]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-event-fx
  :page/init-home
  (fn [_ _]
    {:dispatch [:fetch-docs]}))

;;subscriptions

(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))

(rf/reg-event-db
 :transaction/add-fail
 (fn [{:keys [xactions] :as db}
      [_event-id {:keys [uuid] :as other-arg}]]
   (let [_ (pp/pprint {:add-xaction-fail
                       {:other-arg other-arg
                        :uuid uuid}})
         new-xactions (dissoc xactions uuid)]
     (assoc db :xactions new-xactions))))

(rf/reg-event-fx
 :transaction/add-success
 (fn [{:keys [db] :as _cofx}
      [_event-id
       {:keys [uuid] :as xaction}]]
   (let [#_ (pp/pprint {:add-xaction-success
                       {#_#_:other-arg other-arg
                        :xaction xaction}})
         xaction-new (dissoc xaction :add-waiting?)]
     {:db (assoc-in db [:xactions uuid] xaction-new)
      :dispatch [:transaction/fetch-balance]})))


(defn xform-xaction-for-backend [xaction]
  (dissoc xaction :add-waiting?))

(rf/reg-event-fx
 :transaction/add
 (fn [{:keys [db] :as _cofx}
      [_evt-id {:keys [uuid #_amount] :as xaction}]]
   (let [#_ (pp/pprint {:transaction/add
                       {:xaction xaction
                        :event-id _evt-id}})
         xaction (assoc xaction :add-waiting? true)
         #_#_previous-balance (:balance db)
         #_#_amt-float (js/parseFloat amount)]
     {:db (-> db
              (assoc-in [:xactions uuid] xaction)
              #_(assoc :balance (+ previous-balance amt-float))
              (assoc :balance "updating"))
      :http-xhrio
      {:method :post
       :uri "/api/xactions"
       :params {:xaction (xform-xaction-for-backend xaction)}
       :format (cljs-ajax/transit-request-format)
       :response-format (cljs-ajax/raw-response-format)
       :on-success [:transaction/add-success xaction]
       :on-failure [:transaction/add-fail {:uuid uuid}]}})))

(rf/reg-sub
 :transaction/all
 (fn [{:keys [xactions] :as _db} _query-v]
   xactions))


(rf/reg-event-fx
 :initialize
 (fn [_cofx [_evt-id _something]]
   {:dispatch-n [[:transaction/fetch-balance]]}))


(rf/reg-event-fx
 :transaction/fetch-balance
 (fn [{:keys [_db] :as _cofx}
      [_evt-id _something]]
   {:http-xhrio
    {:method :get
     :uri "/api/balance"
     :response-format (cljs-ajax/transit-response-format)
     :on-success [:transaction/fetch-balance-success]
     :on-failure [:transaction/fetch-balance-fail]}}))

(rf/reg-event-db
 :transaction/fetch-balance-success
 (fn [db [_evt-id {:keys [balance] :as _response}]]
   (let [#_ (pp/pprint {#_#_:response response
                       :balance balance
                       :balance-rep (.-rep balance)
                       :is-bigdec (transit/bigdec? balance)})
         bal-as-float (-> balance
                              .-rep
                              js/parseFloat)]
    (assoc db :balance bal-as-float))))

(rf/reg-event-db
 :transaction/fetch-balance-fail
 (fn [db [_evt-id something]]
   (let [_ (pp/pprint {:fetch-balance-fail something})]
     db)))

(rf/reg-sub
 :transaction/balance
 (fn [{:keys [balance] :as _db} _query-v]
   balance))
