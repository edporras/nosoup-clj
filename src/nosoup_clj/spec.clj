(ns nosoup-clj.spec
  (:require
   [clojure.set            :as set]
   [clojure.spec.alpha     :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.string         :as str]
   [nosoup-clj.init        :as init]
   [nosoup-clj.util        :as util]))

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
(s/def :restaurant/twitter ::non-empty-string)
(s/def :restaurant/instagram ::non-empty-string)

;; more complex ones
(s/def :restaurant/categories (let [category-keys (set (keys (util/read-config init/categories-config)))]  ;; TODO: avoid reading file again
                                (s/with-gen #(and (set? %) (set/subset? % category-keys) (not= % :all))
                                  (fn [] (gen/not-empty
                                         (gen/set (gen/elements (vec category-keys)) {:max-elements 3}))))))

(s/def :restaurant/zip (s/with-gen #(and (string? %) (re-matches #"[0-9]{5}" %))
                         #(gen/fmap (fn [v] (str/join v))
                                    (gen/vector (gen/choose 0 9) 5))))

(s/def ::phone (s/with-gen #(and (string? %) (re-matches #"[0-9]{3} [0-9]{3}-[0-9]{4}" %))
                 #(gen/fmap (fn [[ac pf rem]] (str (str/join ac) " " (str/join pf) "-" (str/join rem)))
                            (gen/tuple (gen/vector (gen/choose 0 9) 3)
                                       (gen/vector (gen/choose 0 9) 3)
                                       (gen/vector (gen/choose 0 9) 4)))))
(s/def ::not-listed #{:not-listed})
(s/def :restaurant/phone (s/or :ph ::phone
                               :nl ::not-listed))
(s/def :restaurant/uri (s/with-gen #(and (string? %) (re-matches #"^http(s)?://[a-zA-Z0-9]+.[a-z]+(/)?(.*?)" %))
                         #(gen/fmap (fn [[p s e x]] (str p "://" s "." (str/lower-case (str/join e)) "/" x))
                                    (gen/tuple (gen/elements ["http" "https"])
                                               (s/gen ::non-empty-string)
                                               (gen/not-empty (gen/vector (gen/char-alpha) 2 3))
                                               (gen/string-alphanumeric)))))

(s/def :restaurant/dine-opts (let [nsfy-dining-opts #{:dine-in :take-out :delivery :catering :food-truck}]
                               (s/with-gen #(and (set? %) (set/subset? % nsfy-dining-opts))
                                 (fn [] (gen/not-empty (gen/set (gen/elements (vec nsfy-dining-opts))))))))

(s/def :restaurant/opts (let [nsfy-rest-opts #{:wifi :late-night :closed :need-verification}]
                          (s/with-gen #(and (set? %) (set/subset? % nsfy-rest-opts))
                            (fn [] (gen/set (gen/elements (vec nsfy-rest-opts)))))))

(s/def :restaurant/price #{:$ :$$ :$$$ :$$$$})

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
                                     :restaurant/instagram
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

(comment
(gen/generate (s/gen ::restaurant))
;; (gen/generate (s/gen :restaurant/zip))
;; (gen/sample (s/gen :restaurant/phone) 1)
;; (gen/sample (s/gen :restaurant/uri) 1)
)
