(ns com.sakekasi.linkshare.service
    (:require [io.pedestal.service.http :as bootstrap]
              [io.pedestal.service.http.route :as route]
              [io.pedestal.service.http.body-params :as body-params]
              [io.pedestal.service.http.route.definition :refer [defroutes]]
              [io.pedestal.service.log :as log]

              [ring.util.response :as ring-resp]
              [ring.middleware.params :as ring-mw-p]
              [ring.middleware.json :as ring-mw-j]

              [com.sakekasi.linkshare.url :as url]
              [com.sakekasi.linkshare.db :as db]))
; RESTful API:
; ---------------------
; /lookup?url="" (get)
; /link          (get)
; /link/:id      (get)
; /link          (post)
; /links         (get)
; /links/:lim    (get)
; /links         (post)
; /rmlink/:id    (get)
; /rmlinks       (post)
; /reset         (get)

(defn response-type
  [type]
  (comp #(ring-resp/content-type % type)
        ring-resp/response))
        

(def json-response (response-type "application/json"))
(def text-response (response-type "text/plain"))

(defn json-get ; sends the return value of f as json
  [f]
  (-> (fn [request]
        (-> (f request)
            json-response))
      ring-mw-j/wrap-json-response))

(defn json-do ; executes f, responds with success, takes json args
  [f]
  (-> (fn [request]
        (try
          (f request)
          (-> "" text-response)
          (catch Exception e#
            (-> (str (.getMessage e#) (.printStackTrace e#))
                ring-resp/response
                (ring-resp/status 500)))))
      ring-mw-j/wrap-json-params))


(def lookup-url
  "returns the title of the page at url" ; /lookup?url="" (get)
  (-> (fn [request]
        (-> (url/title (get-in request [:params :url] ""))
            text-response))
      ring-mw-p/wrap-params))


(def lookup-latest-link
  "returns the json description of the last interned link" ; /link (get)
  (json-get (fn [request] (db/get-latest-link))))

(def lookup-link
  "returns the json description of the link with id" ; /link/:id (get)
  (json-get #(db/get-link (get-in % [:path-params :id]))))

(def intern-link
  "interns link passed in json in db" ; /link (post)
  (json-do #(db/put-link (get-in % [:json-params :title])
                         (get-in % [:json-params :url]))))

(def lookup-latest-links 
  "returns a json list of the latest links page"  ; /links (get)
  (json-get (fn [request] (db/get-latest-links))))

(def lookup-links
  "returns a json list of links older than lim" ; /links/:lim (get)
  (json-get #(db/get-links (get-in % [:path-params :lim]))))

(def intern-links
  "interns links passed in json in db" ; /links (post)
  (json-do #(db/put-links (map (fn [a] (:title a)) (:json-params %))
                           (map (fn [a] (:url a)) (:json-params %)))))


(def delete-link ;;; may need to rewrite
  "removes link with id from db" ; /rmlink/:id (delete)
  (json-do #(db/remove-link (get-in % [:path-params :id]))))

(def delete-links
  "removes all links with ids from db" ; /rmlinks (post)
  (json-do #(db/remove-links (:json-params %))))

(def reset
  "removes all links from db" ; /reset (delete)
  (json-do (fn [request] (db/reinit))))


(defn home-page
  "basic home page for rest api"
  [request]
  (-> (str "<pre>Welcome to the linkshare REST API\n"
           "--------------------------------------\n"
           "/lookup?url=""   (get) make the url unquoted\n"
           "/link          (get)\n"
           "/link/:id      (get)\n"
           "/link          (post)\n"
           "/links         (get)\n"
           "/links/:lim    (get)\n"
           "/links         (post)\n"
           "/rmlink/:id    (delete)\n"
           "/rmlinks       (post) pass a json array of ints\n"
           "/reset         (delete)</pre>")
      ring-resp/response
      (ring-resp/content-type "text/html")))


(defroutes routes
  [[
    ["/" {:get home-page}
     ;; Set default interceptors for /about and any other paths under /
     ^:interceptors [(body-params/body-params) bootstrap/html-body]
     ["/lookup" 
      ^:constraints {:url #"^(?:https?://)?(?:[\w]+\.)(?:\.?[\w]{2,})+$"}
      {:get lookup-url}]
     ["/link" {:get lookup-latest-link :post intern-link}
      ["/:id" 
       ^:constraints {:id #"[0-9]+"}
       {:get lookup-link}]]
     ["/links" {:get lookup-latest-links :post intern-links}
      ["/:lim" 
       ^:constraints {:lim #"[0-9]+"}
       {:get lookup-links}]]
     ["/rmlink/:id" ;;should be a delete rather than a post/get?
      ^:constraints {:id #"[0-9]+"}
      {:delete delete-link}]
     ["/rmlinks" {:post delete-links}]
     ["/reset" {:delete reset}]]
    ]])

;; Consumed by com.sakekasi.linkshare.server/create-server
;; See bootstrap/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; :bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty or :tomcat (see comments in project.clj
              ;; to enable Tomcat)
              ;;::bootstrap/host "localhost"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
