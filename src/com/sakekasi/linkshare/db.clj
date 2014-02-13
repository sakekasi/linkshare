(ns com.sakekasi.linkshare.db
    "the db namespace is in charge of transactions with our database"
    (:require [clojure.java.jdbc :as jdbc]))

;;TODO: remove as much repetition as possible using higher order fns.
;;TODO: add config file for dbpath etc.

;(def dbpath "db") ; this path is wrong. fix by looking at h2 source
(def dbpath "db")
(def page-size 20) ; number of links in a db page

(let [db-protocol "file"
      db-host dbpath
      db-name "links"]
  (def db 
    {:classname   "org.h2.Driver" ; must be in classpath
     :subprotocol "h2"
     :subname db-host
     ; Any additional keys are passed to the driver
     ; as driver-specific properties.
     :user     "default"
     :password ""}))
;; we may want different users to login, implementing multi user.
;; different db tables for different users

(def create-table
  "ddl command to create the appropriate table"
  (str "CREATE TABLE IF NOT EXISTS links (id INT IDENTITY PRIMARY KEY, "
       "title VARCHAR, "
       "url VARCHAR(65535))"))

(defn init
  "creates the appropriate table if it does not exist"
  []
  (jdbc/db-do-commands db create-table))

(defn reinit
  "reinitializes table"
  []
  (jdbc/db-do-commands db
    (jdbc/drop-table-ddl :links)
    create-table))

(defn put-links
  "inserts multiple * pairs into the database"
  [titles urls]
  (apply (partial jdbc/insert! db
          :links
          [:title :url])
         (map vector titles urls)))

(defn put-link
  "inserts a single * pair into the database"
  [title url]
  (put-links (vector title) (vector url)))

(defn update-link
  "updates a single link in database"
  [id title url]
  (jdbc/update! db :links (-> (if (not (nil? title))
                                 (assoc {} :title title)
                                 {})
                              (#(if (not (nil? url))
                                  (assoc % :url url)
                                  {})))
                ["id = ?" id]))

(defn get-link
  "gets the link with id from the table"
  [id]
  (first
   (jdbc/query db 
               ["SELECT * from links where id=(?)" id])))

(defn get-latest-link
  "gets the latest link from the table"
  []
  (first 
   (jdbc/query db
               ["SELECT * from links ORDER BY id DESC LIMIT 1"])))

(defn get-links
  "gets page-size links older than lim"
  [lim]
  (doall page-size
   (jdbc/query db
               ["SELECT * from links WHERE id<(?) ORDER BY id DESC" lim])))

(defn get-latest-links
  "gets the page-size newest links"
  []
  (doall
   (jdbc/query db
               ["SELECT * from links ORDER BY id DESC LIMIT (?)" 
                page-size])))
           
(defn remove-links
  "removes links with ids from db"
  [ids]
  (doall (map (partial jdbc/db-do-prepared db 
                       "DELETE FROM links WHERE id=(?)")
              (map vector ids))))
;(partial vector "DELETE FROM links WHERE id=(?)")

(defn remove-link
  "removes link with id from db"
  [id]
  (remove-links (vector id)))
