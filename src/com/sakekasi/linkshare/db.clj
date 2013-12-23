(ns com.sakekasi.linkshare.db
    "the db namespace is in charge of transactions with our database"
    (:require [clojure.java.jdbc :as jdbc]
              [clojure.java.jdbc.sql :as sql]))

(def dbpath "/var/www/db/") ; this path is wrong. fix by looking at h2 source
(def page-size 50) ; number of links in a db page

(let [db-protocol "file"
      db-host dbpath
      db-name "links"]
  (def db 
    {:classname   "org.h2.Driver" ; must be in classpath
     :subprotocol "h2"
     :subname (str db-protocol "://" db-host "/" db-name)
     ; Any additional keys are passed to the driver
     ; as driver-specific properties.
     :user     "default" ; probably want to change this
     :password ""}))

(defn put-link 
  "inserts a single title, url pair into the database"
  [title url]
  (sql/with-connection db
    (sql/insert-record
     :links
     {:title title :url url})))

(defn put-links
  "inserts multiple title, url pairs into the database"
  [links]
  (sql/with-connection db
    (sql/insert-records
     :links
     links)))

(defn get-link
  "gets the latest link from the table"
  []
  (sql/with-connection db
    (sql/with-query-results rs 
      ["SELECT row from links ORDER BY id DESC LIMIT 1"]
      (first rs))))

(defn get-links
  "gets page-size links older than id"
  [lim]
  (sql/with-connection db
    (sql/with-query-results rs
      [(str "SELECT rows from links WHERE id<" lim)]
      (doall page-size rs))))

(defn get-latest-links
  "gets the page-size newest links"
  []
  (sql/with-connection db
    (sql/with-query-results rs
      [(str "SELECT row from links ORDER BY id DESC LIMIT " page-size)]
      (doall rs))))


