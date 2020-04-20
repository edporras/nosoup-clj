(ns nosoup-clj.spec-test
  (:require [clojure.test       :refer [deftest is]]
            [clojure.spec.alpha :as s]
            [nosoup-clj.spec]))

(deftest zip-spec-not-string-test
  (is (not= :s/invalid (s/conform :restaurant/zip 32321))))

(deftest phone-spec-not-string-test
  (is (not= :s/invalid (s/conform :restaurant/phone 3521234567))))

(deftest uri-spec-not-string-test
  (is (not= :s/invalid (s/conform :restaurant/uri :temp))))

(deftest uri-spec-matches-format-test
  (is (not= :s/invalid (s/conform :restaurant/uri "http://abc.de"))))

(deftest dine-opts-spec-not-set-test
  (is (not= :s/invalid (s/conform :restaurant/dine-opts [:delivery]))))

(deftest dine-opts-spec-unknown-test
  (is (not= :s/invalid (s/conform :restaurant/dine-opts #{:what-is-this}))))

(deftest opts-spec-not-set-test
  (is (not= :s/invalid (s/conform :restaurant/opts [:wifi]))))
