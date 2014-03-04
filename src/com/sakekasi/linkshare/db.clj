(ns com.sakekasi.linkshare.db
  "the db namespace is in charge of transactions with our database"
  (:require [clojure.java.jdbc :as jdbc]
            [com.sakekasi.linkshare.config :as config]))

(def db 
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname config/dbpath
   :user     "linkshare"
   :password "linkshare"})
;; we may want different users to login, implementing multi user.
;; different db tables for different users

(def create-table
  "ddl command to create the appropriate table"
  (str "CREATE TABLE IF NOT EXISTS links "
       "(id INTEGER AUTO_INCREMENT PRIMARY KEY, "
       "title TEXT, "
       "url VARCHAR(65000) );"))

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
  [links]
  (apply (partial jdbc/insert! db
                  :links)
         links))

(defn put-link
  "inserts a single * pair into the database"
  [link]
  (jdbc/insert! db :links link))

(defn update-link
  "updates a single link in database"
  [link]
  (jdbc/update! db :links 
                (reduce (fn [m [k v]] (assoc m k v)) {}
                        (filter (fn [[k v]] (and (not (nil? v))
                                                 (not (= k :id)))) 
                                link))
                ["id = ?" (:id link)]))

(defn get-link
  "gets the link with id from the table"
  [id]
  (first (jdbc/query db ["SELECT * FROM links WHERE id=? LIMIT 1" id])))

(defn get-latest-link
  "gets the latest link from the table"
  []
  (first (jdbc/query db ["SELECT * from links ORDER BY id DESC LIMIT 1"])))

(defn get-links
  "gets page-size links older than lim"
  [lim]
  (doall config/page-size
         (jdbc/query db 
                     ["SELECT * from links WHERE id<? ORDER BY id DESC LIMIT ?" 
                      lim config/page-size])))

(defn get-latest-links
  "gets the page-size newest links"
  []
  (doall
   (jdbc/query db ["SELECT * from links ORDER BY id DESC LIMIT ?" 
                   config/page-size])))

(defn remove-links
  "removes links with ids from db"
  [ids]
  (doall (map (partial jdbc/db-do-prepared db "DELETE FROM links WHERE id=?")
              (map vector ids))))

(defn remove-link
  "removes link with id from db"
  [id]
  (remove-links (vector id)))
