(ns com.sakekasi.linkshare.core
  "the core namespace does routing and http request handling"
  (:require [liberator.core :refer [resource defresource]]
            [liberator.dev :refer [wrap-trace]]

            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :refer [split]]

            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]

            [compojure.core :refer [defroutes ANY]]

            [com.sakekasi.linkshare.url :as url]
            [com.sakekasi.linkshare.db :as db]))

; RESTful API:
; ---------------------
; /lookup?url="" (get)
; /link          (get)
; /link          (post)
; /link          (delete)
; /link/:id      (get)
; /link/:id      (put)
; /link/:id      (delete)
; /links         (get)
; /links         (post w/ delete flag)
; /links/:lim    (get)
; /reset         (delete)

;;TODO: handle 404 and give better response? json? formatted?
;;TODO: remove as much repetition as possible using higher order fns.

(defn etag [title url id] (str (hash title) (hash url) id))

(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

(defn parse-json [context key]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        (let [data (json/read-str body :key-fn keyword)]
          [false {key data}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: " (.getMessage e))}))))

(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (let [ctype (first (split (get-in ctx [:request :headers "content-type"])
                              #";"))]
      (or
       (some #{ctype}
             content-types)
       [false {:message (str "Unsupported Content-Type "
                             ctype)}]))
    true))

(defresource link [id]
  :allowed-methods [:get :put :delete]
  :available-media-types ["application/json"
                          "text/html"]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::json)
  :exists? (fn [_] (let [l (db/get-link id)]
                    (if (not (nil? l))
                      {::val l})))
  :handle-ok ::val
  :can-put-to-missing? nil
  :put! (fn [ctx] (let [title (get-in ctx [::json :title])
                        url (get-in ctx [::json :url])]
                     (db/update-link id title url)
                     nil))
  :delete! (fn [_] (db/remove-link id))
  :etag (fn [ctx] (let [title (get-in ctx [::val :title])
                        url (get-in ctx [::val :url])]
                    (etag title url id))))

(defresource latest-link
  :allowed-methods [:get :post :delete]
  :available-media-types ["application/json"
                          "text/html"]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::json)
  :exists? (fn [_] (let [l (db/get-latest-link)]
                    (if (not (nil? l))
                      {::val l})))
  :handle-ok ::val
  :post! (fn [ctx] (let [title (get-in ctx [::json :title])
                         url (get-in ctx [::json :url])]
                    (db/put-link title url)))
  :delete! (fn [ctx] (db/remove-link (get-in ctx [::val :id])))
  :etag (fn [ctx] (let [title (get-in ctx [::val :title])
                        url (get-in ctx [::val :url])
                        id (get-in ctx [::val :id])]
                    (etag title url id))))

(defresource links [lim]
  :allowed-methods [:get]
  :available-media-types ["application/json"
                         "text/html"]
  :exists? (fn [_] (let [l (db/get-links lim)]
                     (if (not (nil? l))
                       {::val l})))
  :handle-ok ::val
  :etag (fn [ctx] (hash (str (map #(etag (:title %) (:url %) (:id %))
                                  (::val ctx))))))

  
(defresource latest-links
  :allowed-methods [:get :post]
  :available-media-types ["application/json"
                         "text/html"]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::json)
  :exists? (fn [_] (let [l (db/get-latest-links)]
                     (if (not (nil? val))
                       {::val l})))
  :handle-ok ::val
  :post! (fn [ctx] (let [delete (get-in ctx [::json :delete])]
                     (if delete
                       (db/remove-links (get-in ctx [::json :ids]))
                       (db/put-links 
                        (map :title (get-in ctx [::json :links]))
                        (map :url (get-in ctx [::json :links]))))))
  :etag (fn [ctx] (hash (str (map #(etag (:title %) (:url %) (:id %))
                                  (::val ctx))))))

(defresource lookup-url ;;needs to be wrapped in wrap-params
  :allowed-methods [:get]
  :available-media-types ["text/plain"
                         "text/html"]
  :handle-ok (fn [ctx] (url/title (get-in ctx [:request :params :url] ""))))

(defresource reset 
  :allowed-methods [:delete]
  :delete! db/reinit)

(defresource home-page
  :allowed-methods [:get]
  :available-media-types ["text/plain"
                         "text/html"]
  :handle-ok (str "<pre>Welcome to the linkshare REST API\n"
                  "--------------------------------------\n"
                  "/lookup?url=""   (get) make the url unquoted\n"
                  "/link          (get)\n"
                  "/link          (post)\n"
                  "/link          (delete)\n"
                  "/link/:id      (get)\n"
                  "/link/:id      (put)\n"
                  "/link/:id      (delete)\n"
                  "/links         (get)\n"
                  "/links         (post w/ delete flag)\n"
                  "/links/:lim    (get)\n"
                  "/reset         (delete)</pre>"))

(defroutes linkshare
  (ANY "/" [] home-page)
  (ANY "/link" [] latest-link)
  (ANY ["/link/:id", :id #"[0-9]+"] [id] (link id))
  (ANY "/links" [] latest-links)
  (ANY ["/links/:lim", :lim #"[0-9]+"] [lim] (links lim))
  (ANY "/lookup" [] lookup-url)
  (ANY "/reset" [] reset))

(def handler
  (-> linkshare
      (wrap-params)
      (wrap-trace :header :ui)
      (wrap-stacktrace)))

(run-jetty #'handler {:port 10000})
