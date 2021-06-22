(ns cledgers-luminus.app
  (:require [cledgers-luminus.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
