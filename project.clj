(defproject erdos.redis-mapper "0.1.0-SNAPSHOT"
  :description "Object mapping for Redis + Clojure"
  :url "http://github.com/erdos/erdos.redis-mapper"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/carmine "2.18.0"]]
  :plugins [[quickie "0.4.2"]]
  :profiles {:test {:dependencies [[com.github.kstyrc/embedded-redis "0.6"]
                                   [org.clojure/tools.nrepl "0.2.12"]]}})
