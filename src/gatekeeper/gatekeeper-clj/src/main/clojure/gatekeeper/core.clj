(ns gatekeeper.core
  (:require [gatekeeper.authz :refer :all])
  (:require [gatekeeper.storage :refer :all])
  (:require [gatekeeper.server :refer :all]))


(def authz-duration (java.time.Duration/ofMinutes 15))

(defn authenticate-request [request] true)
