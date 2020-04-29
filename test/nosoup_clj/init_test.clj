(ns nosoup-clj.init-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [nosoup-clj.init :as sut]
            [nosoup-clj.spec :as spec]))

(deftest read-config-test
  (is (= (sut/read-config (io/file "test/categories.edn") ::spec/categories)
         {:italian "Italian", :mexican "Mexican"})))

