(defproject com.sakekasi.linkshare "0.0.1-SNAPSHOT"
  :description "LinkShare: an easy way to share links from phone, tablet and computer. Maintain bookmarks easily."
  :url "http://www.sakekasi.com"
  :license {:name "GNU GPLv2"
            :url "http://www.gnu.org/licenses/gpl-2.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0"]
                 [org.clojure/data.json "0.2.4"]

                 [liberator "0.10.0"]
                 [compojure "1.1.3"]

                 [ring "1.2.1"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring/ring-json "0.2.0"]

                 [enlive "1.0.0"]
                 [com.h2database/h2 "1.3.168"]]
  :dev-dependencies [[org.clojure/java.jdbc "0.3.0"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :main ^{:skip-aot true} com.sakekasi.linkshare.core)
