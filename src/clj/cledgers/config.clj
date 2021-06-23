(ns cledgers.config
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [cprop.core :as cprop]
   [cprop.source]
   [mount.core :as mount]))


(def dev-local-config-file-path (str (System/getProperty "user.home")
                                     "/Dropbox/dev/dev-local-config/cledgers-luminus-2-config.edn"))
(def dev-local-config-file (io/as-file dev-local-config-file-path))
(def dev-local-config (if-not (.exists dev-local-config-file)
                        {}
                        (-> dev-local-config-file slurp edn/read-string)))

(mount/defstate env
  :start
  (cprop/load-config
   :resource "config-cprop.edn"
   :merge
   [dev-local-config
    (mount/args)
    (cprop.source/from-system-props)
    (cprop.source/from-env)]))
