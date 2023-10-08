(defproject nosoup-clj "0.1.2"
  :description "Site generator for nosoupforyou.com"
  :url "https://github.com/edporras/nosoup-clj/"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.219"]

                 [hiccup "1.0.5"]
                 [ring/ring-codec "1.2.0"]
                 [clojure.java-time "1.3.0"]
                 [sitemap "0.4.0"]
                 [org.clj-commons/digest "1.4.100"]

                 [com.taoensso/timbre "6.3.1"]]
  :main ^:skip-aot nosoup-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev  {:dependencies [[hickory "0.7.1"]
                                   [org.clojure/test.check "1.1.1"]
                                   [com.oscaro/tools-io "0.3.32"]]}})
