(defproject io.github.erdos/redis-mapper "0.1.0"
  :description "Object mapping for Redis + Clojure"
  :url "http://github.com/erdos/redis-mapper"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/carmine "2.18.0"]]
  :plugins [[quickie "0.4.2"]]
  :aot :all
  :profiles {:test {:dependencies [[com.github.kstyrc/embedded-redis "0.6"]
                                   [org.clojure/tools.nrepl "0.2.12"]]}})
