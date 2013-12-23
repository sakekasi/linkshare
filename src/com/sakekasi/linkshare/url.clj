(ns com.sakekasi.linkshare.url
  (:require [net.cgrand.enlive-html :as html])
  (:import (java.net URL)))

(defn fetch-url [url]
  (html/html-resource (URL. url)))

(defn title [url]
  (first (apply :content (html/select (fetch-url url) [:title]))))



