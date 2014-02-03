
(ns clj-cas-client.core
  (:use ring.util.response)
  (:require [clojure.tools.logging :as log]
            [ring.middleware.params :refer [wrap-params]])
  (:import (org.jasig.cas.client.validation Cas10TicketValidator
                                            TicketValidationException)))
(def artifact-parameter-name "ticket")
(def const-cas-assertion     "_const_cas_assertion_")

(defprotocol Validator
  (validate [v ticket service]))

(extend-type Cas10TicketValidator
  Validator
  (validate [v ticket service] (.validate v ticket service)))

(defn validator-maker [cas-server-fn]
  (Cas10TicketValidator. (cas-server-fn)))

(defn- valid? [request]
  (or (get-in request [:session const-cas-assertion])
      (get-in request [:params artifact-parameter-name])))

(defn authentication-filter
  [handler cas-server-fn service-fn]
  (fn [request]
    (if (valid? request)
      (handler request)
      (redirect (str (cas-server-fn) "/login?service=" (service-fn))))))

(defn session-assertion [res assertion]
  (assoc-in res [:session const-cas-assertion] assertion))

(defn request-assertion [req assertion]
  (update-in req [:params] assoc const-cas-assertion assertion))

(defn ticket [r] (get-in r [:params artifact-parameter-name]))

(defn ticket-validation-filter-maker [validator-maker]
  (fn [handler cas-server-fn service-fn]
    (let [ticket-validator (validator-maker cas-server-fn)]
      (fn [request]
        (if-let [t (ticket request)]
          (try
            (let [assertion (validate ticket-validator t (service-fn))]
              (session-assertion (handler (request-assertion request assertion)) assertion))
            (catch TicketValidationException e
              (log/error "Ticket validation exception " e)
              {:status 403}))
          (handler request))))))

(def ticket-validation-filter (ticket-validation-filter-maker validator-maker))

(defn user-principal-filter [handler]
  (fn [request]
    (if-let [assertion (or (get-in request [:params const-cas-assertion])
                           (get-in request [:session const-cas-assertion]))]
      (handler (assoc request :username (.getName (.getPrincipal assertion))))
      (handler request))))

(defn cas
  [handler cas-server-fn service-fn & options]
  (let [opts (apply hash-map options)]
    (if (get opts :enabled true)
      (-> handler
          user-principal-filter
          (authentication-filter cas-server-fn service-fn)
          (ticket-validation-filter cas-server-fn service-fn)
          wrap-params)
      handler)))
