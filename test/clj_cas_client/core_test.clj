(ns clj-cas-client.core-test
  (:use clj-cas-client.core
        clojure.test)
  (:import (org.jasig.cas.client.validation Cas10TicketValidator
                                            TicketValidationException
                                            AssertionImpl)))

(deftest constants-test
  (is (= artifact-parameter-name "ticket"))
  (is (= const-cas-assertion "_const_cas_assertion_")))

(deftest validator-maker-test
  (let [v (validator-maker (fn [] "foo bar"))]
    (is (instance? Cas10TicketValidator v))))

(deftest authentication-filter-test
  (let [af (authentication-filter (fn [r] [:test-authentication-filter r]) (fn [] "cas server fn") (fn [] "service fn")
                                  (fn [r] (get-in r [:headers "ajax-request"])))]
    (is (= (af {:session {"_const_cas_assertion_" "blarg"}}) [:test-authentication-filter {:session {"_const_cas_assertion_" "blarg"}}]))
    (is (= (af {:query-params {"ticket" "the tick"}}) [:test-authentication-filter {:query-params {"ticket" "the tick"}}]))
    (is (= (af {:session {"_const_cas_assertion_" "blarg"}
                :query-params {"ticket" "the tick"}}) [:test-authentication-filter {:session {"_const_cas_assertion_" "blarg"}
                                                                              :query-params {"ticket" "the tick"}}]))
    (is (= (af {}) {:status 302 :headers {"Location" "cas server fn/login?service=service fn"}  :body ""}))
    (is (= (af {:headers {"ajax-request" true}}) {:status 403}))))

(def ^:dynamic test-validator (fn [cas-server-fn ticket service] nil))

(def test-ticket-validation-filter
  (ticket-validation-filter-maker (fn [cas-server-fn]
                                    (reify Validator
                                      (validate [v ticket service]
                                        (test-validator cas-server-fn ticket service))))))


(deftest ticket-validation-filter-test
  (let [tf (test-ticket-validation-filter (fn [r] {:res [:test-ticket-validation-filter r]}) (fn [] "cas server fn") (fn [] "service fn"))]
    (is (= (tf {:nope 42}) {:res [:test-ticket-validation-filter {:nope 42}]}))
    (binding [test-validator (fn [cas-server-fn ticket service]
                               (is (= (cas-server-fn) "cas server fn"))
                               (is (= ticket "ticket"))
                               (is (= service "service fn"))
                               :assertion)]
      (is (= (tf {:query-params {"ticket" "ticket"}})
             {:res [:test-ticket-validation-filter {:query-params {"ticket" "ticket"
                                                             "_const_cas_assertion_" :assertion}}] :session {"_const_cas_assertion_" :assertion}})))

    (binding [test-validator (fn [cas-server-fn ticket service] (throw (TicketValidationException. "blah")))]
      (is (= (tf {:query-params {"ticket" "ticket"}})
             {:status 403})))))

(deftest user-principal-filter-test
  (is (= ((user-principal-filter (fn [req] {:res [:user-principal-filter req]})) {:req 42}) {:res [:user-principal-filter {:req 42}]}))

  (let [assertion (AssertionImpl. "foobar")]
    (is (= ((user-principal-filter (fn [req] {:res [:user-principal-filter req]})) {:req 42 :query-params {"_const_cas_assertion_" assertion}})
           {:res [:user-principal-filter {:username "foobar" :req 42 :query-params {"_const_cas_assertion_" assertion}}]}))
    (is (= ((user-principal-filter (fn [req] {:res [:user-principal-filter req]})) {:req 42 :session {"_const_cas_assertion_" assertion}})
           {:res [:user-principal-filter {:username "foobar" :req 42 :session {"_const_cas_assertion_" assertion}}]}))
    ))
