(ns ga4gh.reference-api.main
  (:require
   [cljs.nodejs :as nodejs]
   [dmohs.requests :as r]
   [ga4gh.reference-api.tools :as tools]
   [ga4gh.reference-api.test-data :as test-data]
   [ga4gh.reference-api.testing :as testing]
   [ga4gh.reference-api.utils :as u]
   ))

(def http (nodejs/require "http"))

(nodejs/enable-util-print!)


(defn- index [ctx]
  (-> ctx
      (r/json-body
       [{:url "/tools/{id-string}" :description "Returns a tool description"}])
      r/respond))


(defn- ping [ctx]
  (r/respond ctx))


(defn- commit-suicide [ctx]
  ((or false nil) 17))


(defn- handle-request [req res]
  (let [ctx (r/create-context req res)
        url (:url (:request ctx))
        disable-logging? (contains? #{"/ping"} url)
        ctx (merge ctx (when disable-logging? {:disable-logging? true}))]
    (when-not disable-logging?
      (.log js/console
            (str "Received request: " (:id ctx) " " (:client-ip (:request ctx)) " " url)))
    (some->
     ctx
     (r/handle-url #"/" #{:get :post} index)
     (r/handle-url #"/ping" #{:get} ping)
     (r/handle-url #"/commit-suicide" #{:post} commit-suicide)
     (r/handle-url #"/api/v1/tools/([%A-Za-z0-9_/-]+)/versions/([%A-Za-z0-9_/-]+)"
                   #{:get} tools/get-tool-version)
     (r/handle-url #"/api/v1/tools/([%A-Za-z0-9_/-]+)" #{:get} tools/get-tool)
     r/respond-with-not-found)))


;; Allow the request handler to be hot reloaded (maybe http.createServer hangs on to it?)
(defonce request-handler (atom nil))
(reset! request-handler handle-request)


(defn -main [& [command]]
  (if command
    (cond
      (= command "dump-sample-config")
      (println (test-data/dump-to-yaml-string))
      (= command "run-tests")
      (testing/initialize #(cljs.test/run-tests 'ga4gh.reference-api.tools))
      :else
      (do
        (println (str "Run without arguments to start the server. Run with dump-sample-config"
                      " to dump the sample YAML configuration to stdout."))
        (.exit js/process 1)))
    (do
      (-> (.createServer http (fn [req res] (@request-handler req res)))
          (.listen 80))
      (println "Server running on port 80."))))

(set! *main-cli-fn* -main)