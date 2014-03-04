(defproject com.sakekasi.linkshare "0.2B"
  :description "LinkShare: an easy way to share links from phone, tablet and computer. Maintain bookmarks easily."
  :url "http://www.sakekasi.com"
  :license {:name "GNU GPLv2"
            :url "http://www.gnu.org/licenses/gpl-2.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0"]
                 [org.clojure/data.json "0.2.4"]

                 [liberator "0.11.0"]
                 [compojure "1.1.3" :exclusions [ring/ring-core]]

                 [ring/ring-core "1.2.1"]
                 [ring-server "0.3.1"]
                 [ring-cors "0.1.0"]

                 [enlive "1.1.5"]
                 [com.h2database/h2 "1.3.168"]
                 [mysql/mysql-connector-java "5.1.25"]]
  :plugins [[lein-ring "0.8.10"]]
  :dev-dependencies [[ring-server "0.3.1"]
                     [lein-ring "0.8.10"]
                     [ring/ring-devel "0.3.5"]
                     [org.clojure/java.jdbc "0.3.0"]]
  :ring {:handler com.sakekasi.linkshare.core/app
         :init com.sakekasi.linkshare.core/init
         :destroy com.sakekasi.linkshare.core/destroy
         :war-exclusions [#"com.sakekasi.linkshare.repl"]
         :servlet-name "linkshare"}
  :resource-paths ["config", "resources"])


