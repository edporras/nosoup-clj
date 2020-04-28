(defproject nosoup-clj "0.1.1-SNAPSHOT"
  :description "Site generator for nosoupforyou.com"
  :url "https://github.com/edporras/nosoup-clj/"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "0.4.2"]

                 [hiccup "1.0.5"]
                 [ring/ring-codec "1.1.1"]
                 [clj-time "0.15.2"]
                 [sitemap "0.4.0"]
                 [digest "1.4.9"]

                 [com.taoensso/timbre "4.10.0"]]
  :main ^:skip-aot nosoup-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :test {:dependencies [[hickory "0.7.1"]
                                   [org.clojure/test.check "0.9.0"]
                                   [com.oscaro/tools-io "0.3.19"]]}
             :dev  {:dependencies [[hickory "0.7.1"]
                                   [org.clojure/test.check "0.9.0"]
                                   [com.oscaro/tools-io "0.3.19"]]}
             :repl {:dependencies [[hickory "0.7.1"]
                                   [org.clojure/test.check "0.9.0"]
                                   [com.oscaro/tools-io "0.3.19"]]
                                   }})
