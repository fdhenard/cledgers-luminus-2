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

    [ajax.core :as cljs-ajax])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
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
                 [nav-link "#/about" "About" :about]]]]))

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
                        :add-waiting true})

(def xactions (r/atom {}))

(defn xform-xaction-for-backend [xaction]
  (dissoc xaction :add-waiting))


(defn get-payees! [q-str callback]
  #_(println "callback:" callback)
  (let [response->results
        (fn [response]
          (let [payees (-> response :result)
                #_ (println "something:")
                #_ (pp/pprint something)]
            (callback payees)))]

    (cljs-ajax/GET
     "/api/payees"
     {:params {:q q-str}
      :handler response->results
      :error-handler (fn [err]
                       (pp/pprint {:error err})
                       #_(.log js/console "error: " (utils/pp err)))})))


(defn get-ledgers! [q-str callback]
  (let [response->results
        (fn [response]
          (let [ledgers (-> response :result)]
            (callback ledgers)))]
    (cljs-ajax/GET
     "/api/ledgers"
     {:params {:q q-str}
      :handler response->results
      :error-handler (fn [err]
                       (pp/pprint {:error err})
                       #_(.log js/console "error: " (utils/pp err)))})))


(defn new-xaction-row []
  (let [new-xaction (r/atom (empty-xaction))]
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
             {:value (get-in @new-xaction [:payee :name])
              :query-func get-payees!
              :on-change (fn [selection]
                           (let [#_ (pp/pprint {:payee-change--selection
                                               selection})
                                 payee {:name (:value selection)
                                        :is-new (:is-new selection)
                                        :id (:id selection)}]
                             (swap! new-xaction assoc :payee payee)))
              :item->text (fn [item]
                            ;; (println "calling item->text")
                            ;; (pp/pprint item)
                            (:name item))}]]
       [:td [typeahead/typeahead-component
             {:value (get-in @new-xaction [:ledger :name])
              :query-func get-ledgers!
              :on-change (fn [selection]
                           (let [ledger {:name (:value selection)
                                         :is-new (:is-new selection)
                                         :id (:id selection)}]
                             (swap! new-xaction assoc :ledger ledger)))
              :item->text (fn [item]
                            (:name item))}]]
       [:td [:input {:type "text"
                     :value (:description @new-xaction)
                     :on-change #(swap! new-xaction assoc :description (-> % .-target .-value))}]]
       [:td [:input {:type "text"
                     :value (:amount @new-xaction)
                     :on-change #(swap! new-xaction assoc :amount (-> % .-target .-value))}]]
       [:td [:button
             {:on-click
              (fn [_evt]
                (let [xaction-to-add @new-xaction
                      #_ (pp/pprint {:xaction-to-add xaction-to-add})]
                  (reset! last-date-used (time/local-date
                                          (js/parseInt (get-in @new-xaction [:date :year]))
                                          (js/parseInt (get-in @new-xaction [:date :month]))
                                          (js/parseInt (get-in @new-xaction [:date :day]))))
                  (swap! xactions assoc (:uuid xaction-to-add) xaction-to-add)
                  (reset! new-xaction (empty-xaction))
                  ;; (.log js/console "new-xaction: " (utils/pp xaction-to-add))
                  (cljs-ajax/POST
                   "/api/xactions"
                   {:params {:xaction (xform-xaction-for-backend xaction-to-add)}
                    :error-handler
                    (fn [err]
                      (pp/pprint {:error err})
                      #_(.log js/console "error: " (utils/pp err))
                      ;; (.log js/console "xactions before dissoc: " (utils/pp @xactions))
                      (swap! xactions dissoc (:uuid xaction-to-add))
                      ;; (.log js/console "xactions after dissoc: " (utils/pp @xactions))
                      )
                    :handler
                    (fn [response]
                      (let [added-xaction (get @xactions (:uuid xaction-to-add))
                            added-xaction (dissoc added-xaction :add-waiting)]
                        (swap! xactions assoc (:uuid xaction-to-add) added-xaction)
                        (.log js/console "success adding xaction")
                        (pp/pprint response)))})))
              }
             "Add"]]])))

(defn home-page []
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
      (for [[_ xaction] @xactions]
        (let [#_ (.log js/console "xaction: " (utils/pp xaction))
              class (when (:add-waiting xaction)
                      "rowhighlight")]
          [:tr {:key (:uuid xaction)
                :class class}
           [:td (let [date (:date xaction)]
                  (str (:month date) "/" (:day date) "/" (:year date)))]
           [:td (get-in xaction [:payee :name])]
           [:td (get-in xaction [:ledger :name])]
           [:td (:description xaction)]
           [:td (:amount xaction)]]))]]]]])

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
                :view #'about-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
