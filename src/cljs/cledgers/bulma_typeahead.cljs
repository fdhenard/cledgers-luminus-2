(ns cledgers.bulma-typeahead
  (:require [cljs.pprint :as pp]
            [reagent.core :as reagent :refer [atom]]))

(defn query-callback [matches-atom is-loading-atom results]
  (reset! matches-atom results)
  (reset! is-loading-atom false))


(defn on-typeahead-change! [new-val count-atom value-state matches-atom is-loading-atom query-func]
  (let [_ (swap! value-state (fn [state-in]
                               (if-not state-in
                                 {:textbox-val new-val}
                                 (assoc state-in :textbox-val new-val))))
        _ (swap! count-atom inc)
        _ (js/setTimeout
           (fn []
             (let [_ (swap! count-atom dec)
                   _ (when (and (<= @count-atom 0)
                                (seq new-val))
                       (reset! is-loading-atom true)
                       (query-func new-val (partial query-callback matches-atom is-loading-atom)))]))
           500)]))


(defn typeahead-textbox [value-state matches-atom query-func]
  (let [change-count-atom (atom 0)
        is-loading-atom (atom false)]
    (fn []
      (let [curr-val-state @value-state
            textbox-val (when curr-val-state
                          (:textbox-val curr-val-state))]
       (println "is-loading:" @is-loading-atom)
       [:div {:class #{:field}}
        [:div {:class #{:control (when @is-loading-atom :is-loading)}}
         [:input {:class #{:input} :type :text :placeholder "something"
                  :value textbox-val
                  :on-change #(let [new-val (-> % .-target .-value)]
                                (on-typeahead-change!
                                 new-val
                                 change-count-atom
                                 value-state
                                 matches-atom
                                 is-loading-atom
                                 query-func))}]]]))))

(defn typeahead-component [_parm-map]
  (let [matches-atom (atom #{})]
    (fn [{:keys [query-func on-change item->option value-state] :as _parm-map}]
      (let [{:keys [selection-val textbox-val]} @value-state
            dropdown-expanded (not (= textbox-val selection-val))]
        [:div {:class #{:dropdown (when dropdown-expanded :is-active)}}
         [:div {:class #{:dropdown-trigger}}
          [typeahead-textbox value-state matches-atom query-func]]
         [:div {:class #{:dropdown-menu} :id :dropdown-menu :role :menu}
          [:div {:class #{:dropdown-content}}
           (let [match-options (->> @matches-atom
                                    (map item->option))
                 match-texts (->> match-options
                                  (map :value)
                                  set)
                 has-exact-match? (contains? match-texts textbox-val)
                 create-new {:id nil
                             :value textbox-val}
                 dropdown-vals (if has-exact-match?
                                 match-options
                                 (conj match-options create-new))
                 #_ (pp/pprint {:dropdown-vals dropdown-vals})]
             (for [{:keys [id] :as item} dropdown-vals]
               (let [{text :value} item]
                 (if id
                   ^{:key id}
                   [:a {:href "#"
                        :class #{:dropdown-item}
                        :on-click (fn [_]
                                    (reset! value-state
                                            {:textbox-val text
                                             :selection-val text})
                                    (on-change {:value text
                                                :is-new false
                                                :id id}))}
                    text]
                   ^{:key "new"}
                   [:a {:href "#"
                        :class #{:dropdown-item}
                        :on-click (fn [_]
                                    (swap! value-state assoc :selection-val textbox-val)
                                    (on-change {:value textbox-val
                                                :is-new true
                                                :id nil}))}
                    (str "create new \"" textbox-val "\"")]))))]]]))))
