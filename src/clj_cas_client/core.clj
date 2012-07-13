
(ns clj-cas-client.core
  (:use ring.util.response)
  (:require [clojure.tools.logging :as log])
  (:import (org.jasig.cas.client.validation Cas10TicketValidator
                                            TicketValidationException)))

(def ^{:private true} artifact-parameter-name "ticket")
(def ^{:private true} const-cas-assertion "_const_cas_assertion_")

(defn- authentication-filter
  [handler cas-server-fn service-fn]
  (fn [request]
    (if (or (get-in request [:session const-cas-assertion])
            (get-in request [:params artifact-parameter-name]))
      (handler request)
      (redirect (str (cas-server-fn) "/login?service=" (service-fn))))))

(defn- ticket-validation-filter
  [handler cas-server-fn service-fn]
  (let [ticket-validator (Cas10TicketValidator. (cas-server-fn))]
    (fn [request]
      (if-let [ticket (get-in request [:params artifact-parameter-name])]
        (try
          (let [assertion (.validate ticket-validator ticket (service-fn))]
            (handler (update-in (update-in request [:params] assoc const-cas-assertion assertion) [:session] assoc const-cas-assertion assertion)))
          (catch TicketValidationException e
            (log/error "Ticket validation exception " e)
            {:status 403}))
        (handler request)))))

(defn cas
  [handler cas-server-fn service-fn]
  (-> handler
      (authentication-filter cas-server-fn service-fn)
      (ticket-validation-filter cas-server-fn service-fn)
      ;; maybe adding username etc to request here
      ))
