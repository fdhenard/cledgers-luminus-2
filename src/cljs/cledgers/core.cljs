(ns cledgers.core
  (:require
   [cljs.pprint :as pp]
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [cledgers.ajax :as ajax]
    [cledgers.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string]
    [cljs-time.core :as time]
    [cledgers.bulma-typeahead :as typeahead]

    [ajax.core :as cljs-ajax]
    [cledgers.pages.ledgers-page :as ledgers-page])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar [] 
  (r/with-let
    [expanded? (r/atom false)
     balance (rf/subscribe [:transaction/balance])]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "cledgers"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span][:span][:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/ledgers" "Ledgers" :ledgers]
       [nav-link "#/about" "About" :about]]
      [:div.navbar-end
       [:div.navbar-item
        "Balance: " @balance]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])


(def last-date-used (atom (time/today)))



(defn empty-xaction [] {:uuid (str (random-uuid))
                        :date {:month (time/month @last-date-used)
                               :day (time/day @last-date-used)
                               :year (time/year @last-date-used)}
                        :description ""
                        :amount ""
                        :add-waiting? true})

(defn xform-xaction-for-backend [xaction]
  (dissoc xaction :add-waiting?))


(defn get-payees! [q-str callback]
  (let [response->results
        (fn [{:keys [result] :as _response}]
          (callback result))]

    (cljs-ajax/GET
     "/api/payees"
     {:params {:q q-str}
      :handler response->results
      :error-handler (fn [err]
                       (pp/pprint {:error err}))})))

(defn get-ledgers! [q-str callback]
  (let [response->results
        (fn [{:keys [result] :as _response}]
          (callback result))]
    (cljs-ajax/GET
     "/api/ledgers"
     {:params {:q q-str}
      :handler response->results
      :error-handler (fn [err]
                       (pp/pprint {:error err}))})))


(defn new-xaction-row []
  (let [new-xaction (r/atom (empty-xaction))
        payee-value-state (r/atom nil)
        ledger-value-state (r/atom nil)]
    (fn []
      [:tr {:key "new-one"}
       [:td
        [:input {:type "text"
                 :size 2
                 :value (get-in @new-xaction [:date :month])
                 :on-change #(swap! new-xaction assoc-in [:date :month] (-> % .-target .-value))}]
        [:span "/"]
        [:input {:type "text"
                 :size 2
                 :value (get-in @new-xaction [:date :day])
                 :on-change #(swap! new-xaction assoc-in [:date :day] (-> % .-target .-value))}]
        [:span "/"]
        [:input {:type "text"
                 :size 4
                 :value (get-in @new-xaction [:date :year])
                 :on-change #(swap! new-xaction assoc-in [:date :year] (-> % .-target .-value))}]]
       [:td [typeahead/typeahead-component
             {:value-state payee-value-state
              :query-func get-payees!
              :on-change (fn [{:keys [value is-new id] :as _selection}]
                           (let [payee {:name value
                                        :is-new is-new
                                        :id id}]
                             (swap! new-xaction assoc :payee payee)))
              :item->option (fn [{:payee/keys [id name]}]
                              {:id id
                               :value name})}]]
       [:td [typeahead/typeahead-component
             {:value-state ledger-value-state
              :query-func get-ledgers!
              :on-change (fn [{:keys [value is-new id]}]
                           (let [ledger {:name value
                                         :is-new is-new
                                         :id id}]
                             (swap! new-xaction assoc :ledger ledger)))
              :item->option (fn [{:ledger/keys [id name]}]
                              {:id id
                               :value name})}]]
       [:td [:input {:type "text"
                     :value (:description @new-xaction)
                     :on-change #(swap! new-xaction assoc :description (-> % .-target .-value))}]]
       [:td [:input {:type "text"
                     :value (:amount @new-xaction)
                     :on-change #(swap! new-xaction assoc :amount (-> % .-target .-value))}]]
       [:td
        [:button
         {:on-click
          (fn [_evt]
            (let [_ (rf/dispatch [:transaction/add @new-xaction])
                  _ (reset! new-xaction (empty-xaction))

                  _ (reset! payee-value-state nil)
                  _ (reset! ledger-value-state nil)]))}
         "Add"]]])))

(defn home-page []
  (r/with-let
    [xactions (rf/subscribe [:transaction/all])]
    (let [#_ (pp/pprint {:xactions @xactions})]
     [:section.section>div.container>div.content
      #_(when-let [docs @(rf/subscribe [:docs])]
          [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])
      [:div.container
       #_[:div.row>div.col-sm-12
          [:div "Hello world, it is now"]
          [clock]]
       [:div.row>div.col-sm-12
        [:table.table
         [:thead
          [:tr
           [:th "date"]
           [:th "payee"]
           [:th "ledger"]
           [:th "desc"]
           [:th "amount"]
           [:th "controls"]]]
         [:tbody
          [new-xaction-row]
          (doall
           (for [[_ {:keys [add-waiting?
                            uuid
                            payee
                            ledger
                            description
                            amount]
                     {:keys [month day year]} :date
                     :as _xaction}]
                 @xactions]
             (let [class (when add-waiting?
                           "rowhighlight")
                   {payee-name :name} payee
                   {ledger-name :name} ledger]
               ^{:key uuid}
               [:tr {:key uuid
                     :class class}
                [:td (str month "/" day "/" year)]
                [:td payee-name]
                [:td ledger-name]
                [:td description]
                [:td amount]])))]]]]])))

(defn page []
  (when-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]
     ["/ledgers" {:name :ledgers
                  :view #'ledgers-page/ledgers-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rf/dispatch [:initialize])
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
