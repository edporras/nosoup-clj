(ns nosoup-clj.spec-test
  (:require [clojure.test                  :refer [deftest is]]
            [clojure.spec.alpha            :as s]
            [clojure.test.check.generators :as gen]
            [nosoup-clj.spec               :as sut]))

(deftest check-spec-returns-data-test
  (let [categories {:italian "Italian" :chinese "Chinese"}]
    (is (= (sut/check-spec ::sut/categories categories)
           categories))))

(deftest zip-spec-not-string-test
  (is (not= :s/invalid (s/conform :restaurant/zip 32321))))

(deftest phone-spec-not-string-test
  (is (not= :s/invalid (s/conform :restaurant/phone 3521234567))))

(deftest phone-spec-not-formatted-test
  (is (not= :s/invalid (s/conform :restaurant/phone "352-123-4567"))))

(deftest uri-spec-not-string-test
  (is (not= :s/invalid (s/conform :restaurant/uri :temp))))

(deftest dine-opts-spec-not-set-test
  (is (not= :s/invalid (s/conform :restaurant/dine-opts [:delivery]))))

(deftest dine-opts-spec-unknown-test
  (is (not= :s/invalid (s/conform :restaurant/dine-opts #{:what-is-this}))))

(deftest opts-spec-not-set-test
  (is (not= :s/invalid (s/conform :restaurant/opts [:wifi]))))

(deftest restaurant-uri-gen-test
  (is (s/valid? :restaurant/uri (gen/generate (s/gen :restaurant/uri)))))

(deftest restaurant-dine-opts-gen-test
  (is (s/valid? :restaurant/dine-opts (gen/generate (s/gen :restaurant/dine-opts)))))

(deftest restaurant-price-gen-test
  (is (s/valid? :restaurant/price (gen/generate (s/gen :restaurant/price)))))

(deftest restaurant-coords-gen-test
  (is (s/valid? :restaurant/coords (gen/generate (s/gen :restaurant/coords)))))

(deftest restaurant-gen-test
  (is (s/valid? ::sut/restaurant (gen/generate (s/gen ::sut/restaurant)))))
