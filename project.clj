(defproject com.sakekasi.linkshare "0.0.1-SNAPSHOT"
  :description "LinkShare: an easy way to share links from phone, tablet and computer. Maintain bookmarks easily."
  :url "http://www.sakekasi.com"
  :license {:name "GNU GPLv2"
            :url "http://www.gnu.org/licenses/gpl-2.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0"]
                 [org.clojure/data.json "0.2.4"]

                 [io.pedestal/pedestal.service "0.2.2"]
                 [io.pedestal/pedestal.service-tools "0.2.2"]
                 ;; Remove this line and uncomment the next line to
                 ;; use Tomcat instead of Jetty:
                 [io.pedestal/pedestal.jetty "0.2.2"]
                 ;; [io.pedestal/pedestal.tomcat "0.2.2"]

                 [ring/ring-json "0.2.0"]
                 [ring "1.2.1"]
                 [enlive "1.0.0"]]
  :dev-dependencies [[org.clojure/java.jdbc "0.3.0"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources", "lib/*"]
  :aliases {"run-dev" ["trampoline" "run" "-m" "com.sakekasi.linkshare.server/run-dev"]}
  :repl-options  {:init-ns user
                  :init (try
                          (use 'io.pedestal.service-tools.dev)
                          (require 'com.sakekasi.linkshare.service)
                          ;; Nasty trick to get around being unable to reference non-clojure.core symbols in :init
                          (eval '(init com.sakekasi.linkshare.service/service #'com.sakekasi.linkshare.service/routes))
                          (catch Throwable t
                            (println "ERROR: There was a problem loading io.pedestal.service-tools.dev")
                            (clojure.stacktrace/print-stack-trace t)
                            (println)))
                  :welcome (println "Welcome to pedestal-service! Run (tools-help) to see a list of useful functions.")}
  :main ^{:skip-aot true} com.sakekasi.linkshare.server)
