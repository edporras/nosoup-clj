(ns nosoup-clj.core
  (:require [nosoup-clj.init         :as init]
            [nosoup-clj.spec         :as spec]
            [clojure.java.io         :as io]
            [clojure.string          :as str]
            [hiccup.core             :refer [html]]
            [hiccup.page             :as page]
            [ring.util.codec         :as codec :refer-only [form-encode]]
            [taoensso.timbre         :as timbre :refer [info warn fatal]])
  (:import [java.util Locale]
           [java.text Collator])
  (:gen-class))

(def categories-config  (io/resource "categories.edn"))

(def site-city "Gainesville")
(def site-state "FL")
(def site-city-state (str site-city ", " site-state))
(def base-title (str "No Soup For You - " site-city))

;; turns out this falls-back to google if the device doesn't support apple maps
(def base-mapping-url "https://maps.apple.com/?daddr=")
(def base-twitter-url "https://twitter.com/")

(defn read-categories-list
  "Reads the categories config, injects a global entry used to list `:all`."
  [categories-edn]
  (->> (init/read-config categories-edn ::spec/categories)
       (into {:all "All"})))

(def str-collator (Collator/getInstance (Locale. "en_US")))
(defn read-restaurant-list
  "Reads the restaurants config sorting by the name. Filters out any
  restaurants marked `:closed`."
  [restaurants-edn]
  (->> (init/read-config restaurants-edn ::spec/restaurants)
       (remove #(contains? (:opts %) :closed))
       (sort-by :name str-collator)))

(defn categories->html
  "Generate the navigation pulldown selector from the category list."
  [categories selected-category]
  {:pre [keyword? selected-category]}
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
  [separator link-data]
  {:pre [string? separator vector? link-data]}
  (let [filtered-link-data (remove #(or (nil? %) (nil? (first %))) link-data)]
    (->> (for [link filtered-link-data]
           (link-data->html link))
         vec
         (str/join separator))))

(defn restaurant-map-link-data
  "Generate the map link tuple for the restaurant's address to be passed to `restaurant-links`."
  [{:keys [name city coords]}]
  {:pre [string? name string? city]}
  [(str base-mapping-url
         (str #_"\"" (codec/form-encode name) #_"\""
              (codec/form-encode (str " " city " " site-state))
              (when coords
                (str "&near=" (first coords) "," (last coords))))
         #_(codec/form-encode (str "\""(:name restaurant) "\" " (:city restaurant) " " site-state))
         #_(codec/form-encode (:name restaurant))
         #_(if-let [coords (:coords restaurant)]
             (str "&near=" (first coords) "," (last coords))
             (codec/form-encode (str " " (:city restaurant) " " site-state)))) "map"])

(defn twitter-link-data
  "Generate the map link tuple for the twitter link, to be passed to `restaurant-links`."
  [handle]
  (when handle
    [(str base-twitter-url handle) (str "@" handle)]))

(defn restaurant-category-listing
  "Generate the list of categories to be shown in the `Under`
  label. Show all if in the `:all` view; otherwise, show other not
  matching the selected one."
  [{:keys [categories] :as restaurant} full-categories-list selected-category-key]
  {:pre [set? categories vector? full-categories-list keyword? selected-category-key]}
  (let [[label restaurant-categories] (if (= :all selected-category-key)
                                        ["Under: "      categories]
                                        ["Also under: " (remove #{selected-category-key} categories)])]
    (if (seq restaurant-categories)
      (let [links (->> restaurant-categories
                       (mapv #(let [category-str (get full-categories-list %)]
                                ;; TODO: move to spec?
                                (assert (string? category-str)
                                        (str "Restaurant '" (or (:alias restaurant) (:name restaurant)) "' has unknown category '" % "' in :categories."))
                                (vec [(str "/" (name %) "/") category-str])))
                       (sort)
                       (restaurant-links ", "))]
        (html label links))
      (html [:br]))))

(defn restaurants->html
  "Generate page output for a restaurant listing based on the selected category."
  [restaurants full-category-list selected-category-key]
  (html (for [{:keys [name alias address city zip phone uri twitter] :as r} restaurants]
          (let [restaurant-name (or alias name)]
            [:li
             [:h2 restaurant-name]
             [:div {:class "info"} phone [:br]
              [:address address [:br] city ", " site-state " " zip]
              [:div {:class "links"}
               (->> [
                     [uri "website"]                                       ;; restaurant's website
                     (restaurant-map-link-data r)                          ;; map link
                     (twitter-link-data twitter)                           ;; twitter link
                     ]
                    (restaurant-links " | "))]
              [:div {:class "cats"} (restaurant-category-listing r full-category-list selected-category-key)]]]))))

(defn generate-category-page
  "Generate page output for the selected category using the filtered list of restaurants and categories."
  [[category-k category-str] filtered-restaurants filtered-category-list full-category-list]
  (page/html5 {:lang "en"}
              [:head
               [:title (if (= :all category-k)
                         base-title
                         (str base-title " - " category-str))]
               [:meta {:name "author" :content "Ed Porras"}]
               [:meta {:name "description"
                       :content (str "Guide of independent restaurants and grocers in " site-city-state (when-not (= :all category-k) 
                                                                                                          (str " under the " category-str " category")))}]
               [:meta {:name "keywords"
                       :content (str site-city " Local Independently-owned Restaurants" (when-not (= :all category-k)
                                                                                          (str " " category-str)))}]
               [:meta {:name "viewport" :content "width=device-width,initial-scale=1.0,user-scalable=no"}]
               (page/include-css "/css/styles.css")
               (page/include-js "/js/searchbar.js")]
              [:body {:onload "load();"}
               [:header
                [:h1 [:img {:src "/img/logo.png" :alt (str "Dining in " site-city) :width "293" :height "42"}]]
                [:p "Locally-owned restaurants, cafes, and grocers."]
                (categories->html filtered-category-list category-k)]
               [:div {:id "content"}
                [:ul {:id "listing"}
                 (restaurants->html filtered-restaurants full-category-list category-k)]]
               [:footer [:p {:id "about"}
                         "This is a listing of independent businesses in " site-city-state ". If you own or know "
                         "of a business you'd like to see listed, please contact: nsfy at digressed dot net or "
                         "via Twitter at " (link-data->html (twitter-link-data "NSFYgnv")) "."]]]))

(defn restaurant-by-category
  "Filter the list of restaurants using the given `selected-category`."
  [restaurants selected-category]
  {:pre [vector? restaurants keyword? selected-category]}
  (if (= selected-category :all)
    restaurants ;; don't filter anything
    (->> restaurants
         (filter #(contains? (:categories %) selected-category)))))

(defn generate-category-restaurant-list
  "Create a map of {category filtered-restaurant-data}."
  [categories restaurants]
  (->> categories
       (map (fn [[category-k category-str]]
              (let [filtered-restaurants (restaurant-by-category restaurants category-k)]
                (if (empty? filtered-restaurants)
                  (warn "No restaurants in listing" category-str)
                  {category-k filtered-restaurants}))))
       (into {})))

(defn generate-site
  [{:keys [full-restaurant-list full-category-list base-output-path]}]
  {:pre [string? base-output-path]}
  (info (str "read " (count full-category-list) " categories and " (count full-restaurant-list) " restaurants"))
  (let [category-rest-data      (generate-category-restaurant-list full-category-list full-restaurant-list)
        filtered-category-list  (sort (select-keys full-category-list (keys category-rest-data)))]
    (doseq [[category-k filtered-restaurants] category-rest-data]
      (let [category-str (category-k full-category-list)
            output-path (str base-output-path
                             (when (not= :all category-k)
                               (str (name category-k) "/"))
                             "index.html")]
        (info (str "generating output for '" category-k "' with " (count filtered-restaurants) " entries, saved to '" output-path "'"))
        (io/make-parents output-path)
        (with-open [w (io/writer output-path)]
          (binding [*print-length* false
                    *out* w]
            (print (generate-category-page [category-k category-str] filtered-restaurants filtered-category-list full-category-list))))))))

(defn -main
  [& args]
  (let [{:keys [action options exit-message ok?]} (init/validate-args args)]
    (if-not exit-message
      (case action
        "gen" (let [full-restaurant-list {:full-restaurant-list (read-restaurant-list (:config options))}
                    full-categories-list {:full-category-list (read-categories-list categories-config)}]
                (generate-site (merge options full-restaurant-list full-categories-list))))
      (do ;; error validating args
        (fatal exit-message)
        (System/exit (if ok? 0 1))))))

(comment

  (def categories  (read-categories-list categories-config))
  (def restaurants (read-restaurant-list (io/resource "test-restaurants.edn")))

  (let [category-rest-data   (generate-category-restaurant-list categories restaurants)
        final-category-list  (sort (select-keys categories (keys category-rest-data)))
        [_ filtered-restaurants :as c] (first category-rest-data)
        ]
    (generate-category-page c filtered-restaurants final-category-list categories))

  (generate-site {})
)
