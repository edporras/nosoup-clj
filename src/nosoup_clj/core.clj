(ns nosoup-clj.core
  (:require [nosoup-clj.init         :as init]
            [nosoup-clj.util         :as util]
            [nosoup-clj.spec         :as spec]
            [clojure.spec.alpha      :as s]
            [clojure.java.io         :as io]
            [clojure.string          :as str]
            [hiccup.core             :refer [html]]
            [hiccup.page             :as page]
            [ring.util.codec         :as codec :refer-only [form-encode]]
            [taoensso.timbre         :as timbre :refer [info fatal]])
  (:import [java.util Locale]
           [java.text Collator])
  (:gen-class))

(def base-title (str "No Soup For You - Gainesville"))

;; turns out this falls-back to google if the device doesn't support apple maps
(def base-mapping-url "https://maps.google.com/?daddr=")
(def base-twitter-url "https://twitter.com/")

(defn read-categories-list
  "Reads the categories config, injects a global entry used to list `:all`."
  [categories-edn]
  (->> (util/read-config categories-edn)
       (spec/check-spec ::spec/categories)
       (into {:all "All"})))

(def str-collator (Collator/getInstance (Locale. "en_US")))
(defn read-restaurant-list
  "Reads the restaurants config sorting by the name. Filters out any
  restaurants marked `:closed`."
  [restaurants-edn]
  (->> (util/read-config restaurants-edn)
       (spec/check-spec ::spec/restaurants)
       (remove #(contains? (:opts %) :closed))
       (sort-by :name str-collator)))

(defn categories->html
  "Generate the navigation pulldown selector from the category list."
  [categories selected-category]
  {:pre [(s/valid? ::spec/categories categories) keyword? selected-category]}
  [:nav [:form {:name "catlist" :method :get :action "/c"}
         [:select {:name "cat" :size 1 :onchange "selChange();"}
          (for [[k category-str] categories]
            (let [selected (if (= selected-category k)
                             {:selected "selected"}
                             {})]
              [:option (merge {:value (name k)} selected) category-str]))]
         [:input {:type "submit" :name "action" :id "search" :value "Search"}]]])

(defn link-data->html
  "Generate HTML output from a [uri text] tuple."
  [[uri text]]
  {:pre [string? uri string? text]}
  (let [opts (conj {:href uri}
                   (when (str/starts-with? uri "http")
                     {:target "_blank" :rel "noopener noreferrer"}))]
    (html [:a opts text])))

(defn restaurant-links
  "Using a collection of vectors of `[uri text]` entries, generate their
  string output separated by `separator` if there is more than one."
  [label separator link-data]
  {:pre [string? separator vector? link-data]}
  (let [filtered-link-data (remove #(or (nil? %) (nil? (first %))) link-data)]
    (when (seq filtered-link-data)
      (list label (->> (for [link filtered-link-data]
                             (link-data->html link))
                           vec
                           (str/join separator))))))

(defn restaurant-map-link-url
  "Generate the map urk for a restaurant."
  [{:keys [name address city coords]}]
  {:pre [string? name string? city (s/nilable (s/valid? :restaurant/coords coords))]}
  (str base-mapping-url
       (->> [name (when coords address) city "FL"]
              (remove nil?)
              (vec)
              (map #(codec/form-encode %))
              (str/join ","))))

(defn twitter-link-data
  "Generate the map link tuple for the twitter link, to be passed to `restaurant-links`."
  [handle]
  {:pre [(s/nilable (s/valid? :restaurant/twitter handle))]}
  (when handle
    [(str base-twitter-url handle) (str "@" handle)]))

(defn restaurant-category-listing
  "Generate the list of categories to be shown in the `Under`
  label. Show all if in the `:all` view; otherwise, show other not
  matching the selected one."
  [{:keys [categories]} full-categories-list selected-category-key]
  {:pre [set? categories (s/valid? ::spec/categories full-categories-list) keyword? selected-category-key]}
  (let [[label restaurant-categories] (if (= :all selected-category-key)
                                        ["Under: "      categories]
                                        ["Also under: " (remove #{selected-category-key} categories)])]
    (if (seq restaurant-categories)
      (->> restaurant-categories
           (mapv #(let [category-str (get full-categories-list %)]
                    (vec [(str "/" (name %) "/") category-str])))
           (sort)
           (restaurant-links label ", "))
      "&nbsp")))

(defn restaurants->html
  "Generate page output for a restaurant listing based on the selected category."
  [restaurants full-category-list selected-category-key]
  {:pre [(s/valid? ::spec/restaurants restaurants) (s/valid? ::spec/categories full-category-list) keyword? selected-category-key]}
  (html (for [{:keys [name alias address city zip phone uri twitter] :as r} restaurants]
          (let [restaurant-name (or alias name)
                map-link        [(restaurant-map-link-url r)
                                 (html address [:br] city ", FL " zip)]]
            [:li
             [:h2 restaurant-name]
             [:div {:class "info"}
              [:a {:href (str "tel:+1-" (str/replace phone #" " "-"))} phone]
              [:address (link-data->html map-link)]
              [:div {:class "links"}
               (restaurant-links "Links: "
                                 " | "
                                 [[uri "website"]             ;; restaurant's website
                                  (twitter-link-data twitter) ;; twitter link
                                  ])]]
             [:footer (restaurant-category-listing r full-category-list selected-category-key)]]))))

(defn generate-category-page-head
  "Generate the head portion of the page."
  [[category-k category-str :as c]]
  {:pre [(s/valid? ::spec/category c)]}
  [:head [:title (if (= :all category-k)
                   base-title
                   (str base-title " - " category-str))]
   [:meta {:name "author" :content "Ed Porras"}]
   [:meta {:name "description"
           :content (str "Guide of independent restaurants and grocers in Gainesville, FL"
                         (when-not (= :all category-k)
                           (str " under the " category-str " category")))}]
   [:meta {:name "keywords"
           :content (str "Gainesville Local Independently-owned Restaurants"
                         (when-not (= :all category-k)
                           (str " " category-str)))}]
   [:meta {:name "viewport" :content "width=device-width,initial-scale=1.0"}]
   (page/include-css "/css/site.css")
   (page/include-js "/js/site.js")])

(defn generate-category-page
  "Generate page output for the selected category using the filtered list of restaurants and categories."
  [[category-k :as category] filtered-restaurants filtered-category-list full-category-list]
  (page/html5 {:lang "en"}
              (generate-category-page-head category)
              [:body {:onload "load();"}
               [:header
                [:h1 [:img {:src "/img/logo.png" :alt "Dining in Gainesville" :width "293" :height "42"}]]
                [:p "Locally-owned restaurants, cafes, and grocers."]
                (categories->html filtered-category-list category-k)]
               [:div {:id "content"}
                [:ul
                 (restaurants->html filtered-restaurants full-category-list category-k)]]
               [:footer [:p
                         "This is a listing of independent businesses in Gainesville, FL. If you own or know "
                         "of a business you'd like to see listed, please contact: nsfy at digressed dot net or "
                         "via Twitter at " (link-data->html (twitter-link-data "NSFYgnv")) "."]]]))

(defn restaurant-by-category
  "Filter the list of restaurants using the given `selected-category`."
  [restaurants selected-category]
  {:pre [(s/valid? ::spec/restaurants restaurants) keyword? selected-category]}
  (if (= selected-category :all)
    restaurants ;; don't filter anything
    (->> restaurants
         (filter #(contains? (:categories %) selected-category)))))

(defn generate-category-restaurant-list
  "Create a vector of '(category filtered-restaurant-data) entries."
  [categories restaurants]
  (->> categories
       (mapv (fn [[category-k _]]
               [category-k (restaurant-by-category restaurants category-k)]))))

(defn filter-category-list-from-generated-restaurant-data
  "Removes entries from the category list that didn't generate any page output."
  [category-rest-data full-category-list]
  (->> category-rest-data
       (map (fn [[k data]] (when (seq data) k)))
       (remove nil?)
       (select-keys full-category-list)
       (into (sorted-map))))

(defn category-page-output-path
  [base-output-path category-k]
  (str base-output-path (when (not= :all category-k) (str (name category-k) "/")) "index.html"))

(defn generate-site
  [{:keys [full-restaurant-list full-category-list base-output-path]}]
  {:pre [string? base-output-path]}
  (info (str "read " (count full-category-list) " categories and " (count full-restaurant-list) " restaurants"))
  (let [category-rest-data      (generate-category-restaurant-list full-category-list full-restaurant-list)
        filtered-category-list  (filter-category-list-from-generated-restaurant-data category-rest-data full-category-list)]
    (doseq [[category-k filtered-restaurants] category-rest-data]
      (let [output-path (category-page-output-path base-output-path category-k)]
        (if (seq filtered-restaurants)
          (let [category-str (category-k full-category-list)]
            (info (str "generating output for '" category-k "' with " (count filtered-restaurants) " entries..."))
            (->> (generate-category-page [category-k category-str] filtered-restaurants filtered-category-list full-category-list)
                 (util/cleanup-markup)
                 (util/output->disk output-path)))
          (when (.exists (io/file output-path))
            (info (str "No restaurants found for category " category-k " - deleting old output at " output-path))
            (io/delete-file output-path)
            (io/delete-file (str/replace output-path #"index.html" "")))
          )))
    (util/generate-sitemap base-output-path filtered-category-list)))

(defn -main
  [& args]
  (let [{:keys [action options exit-message ok?]} (init/validate-args args)]
    (if-not exit-message
      (case action
        "gen" (let [full-categories-list {:full-category-list (read-categories-list init/categories-config)}
                    full-restaurant-list {:full-restaurant-list (read-restaurant-list (:config options))}]
                (generate-site (merge options full-restaurant-list full-categories-list))))
      (do ;; error validating args
        (fatal exit-message)
        (System/exit (if ok? 0 1))))))

(comment

  (def categories  (read-categories-list init/categories-config))
  (def restaurants (read-restaurant-list (io/file "test/restaurants.edn")))

  (generate-site {})
  )
