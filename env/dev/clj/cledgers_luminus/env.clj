(ns cledgers-luminus.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [cledgers-luminus.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[cledgers-luminus started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[cledgers-luminus has shut down successfully]=-"))
   :middleware wrap-dev})
