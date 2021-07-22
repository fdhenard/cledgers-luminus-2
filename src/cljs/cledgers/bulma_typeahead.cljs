(ns cledgers.bulma-typeahead
  (:require [cljs.pprint :as pp]
            [reagent.core :as reagent :refer [atom]]))

(defn query-callback [matches-atom is-loading-atom results]
  ;; (println "calling callback")
  ;; (pp/pprint results)
  (reset! matches-atom results)
  (reset! is-loading-atom false))


(defn on-typeahead-change! [new-val count-atom value-atom matches-atom is-loading-atom query-func]
  (reset! value-atom new-val)
  (swap! count-atom inc)
  (js/setTimeout
   (fn []
     (swap! count-atom dec)
     #_(println "count-atom val in timeout cb:" @count-atom)
     (when (and (<= @count-atom 0)
                (seq new-val))
       (reset! is-loading-atom true)
       #_(println "calling-exec-query! for query =" new-val)
       (query-func new-val (partial query-callback matches-atom is-loading-atom))))
   500))


(defn typeahead-textbox [value-atom matches-atom query-func]
  (let [change-count-atom (atom 0)
        is-loading-atom (atom false)]
    (fn []
      (println "is-loading:" @is-loading-atom)
      [:div {:class #{:field}}
       [:div {:class #{:control (when @is-loading-atom :is-loading)}}
        [:input {:class #{:input} :type :text :placeholder "something"
                 :value @value-atom
                 :on-change #(let [new-val (-> % .-target .-value)]
                               (on-typeahead-change!
                                new-val
                                change-count-atom
                                value-atom
                                matches-atom
                                is-loading-atom
                                query-func))}]]])))

#_(defn create-callback [response])

(defn typeahead-component [parm-map]
  (let [textbox-val-atom (atom (:value parm-map))
        matches-atom (atom #{})
        selection-val-atom (atom nil)]
    (fn [{:keys [query-func on-change item->option] :as parm-map}]
      (let [{_value :value} parm-map
            dropdown-expanded (not (= @textbox-val-atom @selection-val-atom))]
        [:div {:class #{:dropdown (when dropdown-expanded :is-active)}}
         [:div {:class #{:dropdown-trigger}}
          [typeahead-textbox textbox-val-atom matches-atom query-func]]
         [:div {:class #{:dropdown-menu} :id :dropdown-menu :role :menu}
          [:div {:class #{:dropdown-content}}
           (let [matches @matches-atom
                 #_ (pp/pprint {:matches matches})
                 textbox-val @textbox-val-atom
                 match-options (->> matches
                                    (map item->option))
                 match-texts (->> match-options
                                  (map :value)
                                  set)
                 has-exact-match (contains? match-texts textbox-val)
                 create-new {:id nil
                             :value textbox-val}
                 dropdown-vals (if has-exact-match
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
                                    (reset! textbox-val-atom text)
                                    (reset! selection-val-atom text)
                                    (on-change {:value text
                                                :is-new false
                                                :id id}))}
                    text]
                   ^{:key "new"}
                   [:a {:href "#"
                        :class #{:dropdown-item}
                        :on-click (fn [_]
                                    (reset! selection-val-atom textbox-val)
                                    (on-change {:value textbox-val
                                                :is-new true
                                                :id nil}))}
                    (str "create new \"" textbox-val "\"")]))))]]]))))
