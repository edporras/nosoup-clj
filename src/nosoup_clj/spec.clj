(ns nosoup-clj.spec
  (:require [clojure.set            :as set]
            [clojure.spec.alpha     :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string         :as str]))

(s/def ::non-empty-string (s/with-gen (s/and string? not-empty)
                            (fn [] (gen/not-empty (gen/string-alphanumeric)))))

;; category map
;; map of category keywords to strings
;; e.g., :american "American"
(s/def ::category (s/tuple keyword? ::non-empty-string))
(s/def ::categories (s/coll-of ::category :kind map?))

;; restaurant vector data
(s/def :restaurant/name ::non-empty-string)
(s/def :restaurant/alias ::non-empty-string)
(s/def :restaurant/address ::non-empty-string)
(s/def :restaurant/city ::non-empty-string)
(s/def :restaurant/categories (s/coll-of keyword? :kind set?))

(s/def :restaurant/twitter ::non-empty-string)

;; more complex ones
(s/def :restaurant/zip (s/with-gen #(and (string? %) (re-matches #"[0-9]{5}" %))
                         #(gen/fmap (fn [v] (str/join v))
                                    (gen/vector (gen/choose 0 9) 5))))
;; (gen/generate (s/gen :restaurant/zip))

(s/def :restaurant/phone (s/with-gen #(and (string? %) (re-matches #"[0-9]{3} [0-9]{3}-[0-9]{4}" %))
                           #(gen/fmap (fn [[ac pf rem]] (str (str/join ac) " " (str/join pf) "-" (str/join rem)))
                                      (gen/tuple (gen/vector (gen/choose 0 9) 3)
                                                 (gen/vector (gen/choose 0 9) 3)
                                                 (gen/vector (gen/choose 0 9) 4)))))
;; (gen/sample (s/gen :restaurant/phone) 1)
(s/def :restaurant/uri (s/with-gen #(and (string? %) (re-matches #"^http(s)?://[a-zA-Z0-9]+.[a-z]+(/)?(.*?)" %))
                         #(gen/fmap (fn [[p s e x]] (str p "://" s "." (str/lower-case (str/join e)) "/" x))
                                    (gen/tuple (gen/elements ["http" "https"])
                                               (s/gen ::non-empty-string)
                                               (gen/not-empty (gen/vector (gen/char-alpha) 2 3))
                                               (gen/string-alphanumeric)))))
;; (gen/sample (s/gen :restaurant/uri) 1)

(def nsfy-dining-opts #{:dine-in :take-out :delivery :catering :food-truck})
(s/def :restaurant/dine-opts (s/with-gen #(and (set? %) (set/subset? % nsfy-dining-opts))
                               (fn [] (gen/not-empty (gen/set (gen/elements (vec nsfy-dining-opts)))))))

(def nsfy-rest-opts #{:wifi :late-night :closed :need-verification})
(s/def :restaurant/opts (s/with-gen #(and (set? %) (set/subset? % nsfy-rest-opts))
                          (fn [] (gen/set (gen/elements (vec nsfy-rest-opts))))))

(def price-opts #{:$ :$$ :$$$ :$$$$})
(s/def :restaurant/price (s/with-gen #(contains? price-opts %)
                           #(gen/elements (vec price-opts))))

(s/def :restaurant/coord (s/with-gen double?
                           (fn [] (gen/double* {:infinite? false :NaN? false}))))
(s/def :restaurant/coords (s/coll-of :restaurant/coord :kind vector? :count 2))

(s/def ::restaurant (s/keys :req-un [:restaurant/name
                                     :restaurant/address
                                     :restaurant/city
                                     :restaurant/zip
                                     :restaurant/phone
                                     :restaurant/categories]
                            :opt-un [:restaurant/alias
                                     :restaurant/uri
                                     :restaurant/twitter
                                     :restaurant/dine-opts
                                     :restaurant/price
                                     :restaurant/opts
                                     :restaurant/coords]))

(s/def ::restaurants (s/coll-of ::restaurant))

(defn check-spec
  "Check if the data passes spec validation."
  [spec data]
  (assert (s/valid? spec data)
          (s/explain-str spec data))
  data)
