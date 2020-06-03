(ns nosoup-clj.init-test
  (:require [clojure.test :refer [deftest is are]]
            [nosoup-clj.init :as sut]))

(deftest validate-args-test
  (let [status (sut/validate-args '("gen" "test/restaurants.edn"))]
    (is (and (= (:action status) "gen")
             (= (get-in status [:options :config]) "test/restaurants.edn")
             (not (nil? (get-in status [:options :base-output-path])))))))

(deftest validate-args-with-basepath-opt-test
  (is (= (-> (sut/validate-args '("gen" "test/restaurants.edn" "-o" "test/"))
             (get-in [:options :base-output-path]))
         "test/")))

(deftest validate-args-help-option-test
  (let [status (sut/validate-args '("gen" "-h"))]
    (is (and (= (keys status) '(:exit-message :ok?))
             (:ok? status)))))

(deftest validate-args-returns-exit-message
  (are [args] (= '(:exit-message) (keys (sut/validate-args args)))

    '("gen")
    '("gen" "blah.edn")
    '("what")))

