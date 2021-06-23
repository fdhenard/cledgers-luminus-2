(ns cledgers.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [cledgers.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[cledgers started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[cledgers has shut down successfully]=-"))
   :middleware wrap-dev})
