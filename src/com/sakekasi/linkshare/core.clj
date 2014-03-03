(ns com.sakekasi.linkshare.core
  "the core namespace does routing and http request handling"
  (:require [liberator.core :refer [resource defresource]]
            [liberator.dev :refer [wrap-trace]]

            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :refer [split]]

            [ring.middleware.params :refer [wrap-params]]
            ;[ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [not-found]]
            [ring.util.codec :refer [url-decode]]

            [compojure.core :refer [defroutes ANY OPTIONS]]

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

(defn single-arg [f]
  (fn [_] (f)))

(defn non-nil [key val]
  (single-arg #(if (not (nil? val))
                 {key val})))

(defn etag [{title :title url :url id :id}] 
  (str (hash title) (hash url) id))

(defn m-etag [links]
  (hash (apply str (map etag links))))

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
  :exists? (non-nil ::val (db/get-link id))
  :handle-ok ::val
  :can-put-to-missing? nil
  :put! (comp db/update-link ::json)
  :delete! (single-arg #(db/remove-link id))
  :etag (comp etag ::val))

(defresource latest-link
  :allowed-methods [:get :post :delete]
  :available-media-types ["application/json"
                          "text/html"]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::json)
  :exists? (non-nil ::val (db/get-latest-link))
  :handle-ok ::val
  :post! (comp db/put-link ::json)
  :delete! (fn [ctx] (db/remove-link (get-in ctx [::val :id])))
  :etag (comp etag ::val))

(defresource links [lim]
  :allowed-methods [:get]
  :available-media-types ["application/json"
                         "text/html"]
  :exists? (non-nil ::val (db/get-links lim))
  :handle-ok (fn [ctx] {:links (::val ctx)
                        :next (:id (last (::val ctx)))})
  :etag (comp m-etag ::val))

  
(defresource latest-links
  :allowed-methods [:get :post]
  :available-media-types ["application/json"
                         "text/html"]
  :known-content-type? #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::json)
  :exists? (non-nil ::val (db/get-latest-links))
  :handle-ok (fn [ctx] {:links (::val ctx)
                        :next (:id (last (::val ctx)))})
  :post! (fn [ctx] (if (get-in ctx [::json :delete])
                     (db/remove-links (get-in ctx [::json :ids]))
                     (db/put-links (get-in ctx [::json :links]))))
  :etag (comp m-etag ::val))

(defresource lookup-url ;;needs to be wrapped in wrap-params
  :allowed-methods [:get]
  :available-media-types ["text/plain"
                         "text/html"]
  :malformed? #(nil? (get-in % [:request :params "url"]))
  :handle-ok (fn [ctx] (url/title (url-decode 
                                   (get-in ctx [:request :params "url"])))))

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
  (OPTIONS "*" [] "cross origin requests allowed")
  (ANY "/" [] home-page)
  (ANY "/link" [] latest-link)
  (ANY ["/link/:id", :id #"[0-9]+"] [id] (link id))
  (ANY "/links" [] latest-links)
  (ANY ["/links/:lim", :lim #"[0-9]+"] [lim] (links lim))
  (ANY "/lookup" [] lookup-url)
  (ANY "/reset" [] reset)
  (ANY "*" [] (not-found "not found"))
  )


(defn allow-cross-origin  
  "middleware function to allow cross origin"  
  [handler]  
  (fn [request]  
    (println "allowing cross origin")
    (let [response (handler request)]  
      (assoc-in response [:headers "Access-Control-Allow-Origin"]  
                "*"))))

(def app
  (-> linkshare
      (wrap-params)
      (wrap-cors :access-control-allow-origin #".*"
                 :access-control-allow-headers ["Origin" "X-Requested-With"
                                                "Content-Type" "Accept"])))

(defn init []
  (db/init)
  (println "linkshare is starting"))

(defn destroy []
  (println "linkshare is shutting down"))
