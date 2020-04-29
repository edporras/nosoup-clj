(ns nosoup-clj.init-test
  (:require [clojure.test :refer [deftest is]]
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

(deftest validate-args-no-file-error-test
  (is (= (keys (sut/validate-args '("gen")))
         '(:exit-message))))

(deftest validate-args-help-option-test
  (let [status (sut/validate-args '("gen" "-h"))]
    (is (and (= (keys status) '(:exit-message :ok?))
             (:ok? status)))))

(deftest validate-args-file-error-test
  (is (= (keys (sut/validate-args '("gen" "blah.edn")))
         '(:exit-message))))

(deftest validate-args-unknown-command-test
  (is (= (keys (sut/validate-args '("what")))
         '(:exit-message))))
