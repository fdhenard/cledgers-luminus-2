(ns cledgers.utils
  (:require #?(:clj [clojure.pprint :as pp]
               :cljs [cljs.pprint :as pp])))

(defn pp-str [derta]
  (with-out-str (pp/pprint derta)))
