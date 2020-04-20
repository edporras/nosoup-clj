(ns nosoup-clj.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.set        :as set]))

;; category map
;; map of category keywords to strings
;; e.g., :american "American"
(s/def ::category (s/tuple keyword? string?))
(s/def ::categories (s/coll-of ::category :kind map?))

;; restaurant vector data
(s/def :restaurant/name string?)
(s/def :restaurant/alias string?)
(s/def :restaurant/address string?)
(s/def :restaurant/city string?)
(s/def :restaurant/zip #(and (string? %)
                             (= 5 (count %))))
(s/def :restaurant/phone #(and (string? %)
                               (re-matches #"[0-9]{3} [0-9]{3}-[0-9]{4}" %)))
(s/def :restaurant/categories set?) ;; TODO

(s/def :restaurant/uri #(and (string? %)
                             (re-matches #"^http(s)?://(.*?)" %)))
(s/def :restaurant/dine-opts #(and (set? %)
                                   (set/subset? % #{:dine-in :take-out :delivery :catering :food-truck})))
(s/def :restaurant/twitter string?)
(s/def :restaurant/opts #(and (set? %)
                              (set/subset? % #{:wifi :late-night :closed :need-verification})))
(s/def :restaurant/price keyword?)

(s/def :restaurant/coord double?)
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

(s/def ::restaurants (s/coll-of ::restaurant :kind vector?))
