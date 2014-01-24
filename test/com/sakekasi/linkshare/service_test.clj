(ns com.sakekasi.linkshare.service-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [io.pedestal.service.test :refer :all]
            [io.pedestal.service.http :as bootstrap]
            [com.sakekasi.linkshare.service :as service]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(defn link-eq [map link]
  (and
   (= (:title map) (:title link))
   (= (:url map) (:url link))))

(defn json-resp [method url]
(json/read-str (:body (response-for service method url)) :key-fn keyword))

(deftest single-put-test
  (is (=
       (:status (response-for service :post "/link" 
                              :body 
                              (json/write-str {:title "Google"
                                               :url "http://www.google.com"}
                                              :escape-slash false)
                              :headers
                              {"Content-Type" "application/json"}))
       200))
  (let [resp (json-resp :get "/link")]
    (is (link-eq {:title "Google" :url "http://www.google.com"}
                 resp))
    (is (=
         (:status (response-for service :delete (str "/rmlink/"
                                                     (:id resp))))
         200))))

(deftest multi-put-test
  (let [initial (json-resp :get "/links")]
    (is (=
         (:status (response-for service :post "/links"
                                :body
                                (json/write-str 
                                 [{:title "Google" 
                                   :url "http://www.google.com"},
                                  {:title "Reddit" 
                                   :url "http://www.reddit.com"},
                                  {:title "Yahoo" 
                                   :url "http://www.yahoo.com"}])
                                :headers
                                {"Content-Type" "application/json"}))
         200))
    (let [resp (json-resp :get "/links")]
      (is (link-eq {:title "Google" :url "http://www.google.com"}
                   (nth resp 2 {})))
      (is (link-eq {:title "Reddit" :url "http://www.reddit.com"}
                   (nth resp 1 {})))
      (is (link-eq {:title "Yahoo" :url "http://www.yahoo.com"}
                   (nth resp 0 {})))
      (is (=
           (:status (response-for service :post "/rmlinks"
                                  :body
                                  (json/write-str (take 3 (map #(:id %) resp)))
                                  :headers
                                  {"Content-Type" "application/json"}))
           200)))
    (is (=
         initial
         (json-resp :get "/links")))))

  

        


