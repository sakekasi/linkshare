(ns com.sakekasi.linkshare.url
  (:require [net.cgrand.enlive-html :as html])
  (:import (java.net URL)))

(defn fetch-url [url]
  (html/html-resource (URL. url)))

(defn title [url]
  (let [title (html/select (fetch-url url) [:title])]
    (first (:content (first title)))))

