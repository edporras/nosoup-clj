(ns nosoup-clj.core-test
  (:require [clojure.java.io        :as io]
            [clojure.string         :as str]
            [clojure.spec.alpha     :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test           :refer [deftest is are testing]]
            [ring.util.codec        :as codec :refer-only [form-encode]]
            [hickory.core           :as html]
            [nosoup-clj.core        :as sut]
            [nosoup-clj.spec        :as spec]))

(defn- html->hiccup
  [html-str]
  (->> html-str
       (html/parse-fragment)
       (mapv html/as-hiccup)))

(def test-categories (io/file "test/categories.edn"))
(deftest read-categories-list-adds-all-entry
  (testing "Entry for `:all` is added to read categories."
    (is (= "All" (:all (sut/read-categories-list test-categories))))))

(def test-restaurants (io/file "test/restaurants.edn"))
(deftest read-restaurant-list-filters-closed
  (testing "Restaurants marked `:closed` are filtered."
    (let [restaurants (sut/read-restaurant-list test-restaurants)]
      (is (empty? (->> restaurants
                       (filter #(contains? (:opts %) :closed))))))))

(deftest categories->html-selected
  (testing "Category options list selects the correct option."
    (is (= (sut/categories->html {:a "A" :mexican "Mexican" :c "C"} :mexican)
           [:nav
            [:form
             {:name "catlist", :method :get, :action "/c"}
             [:select
              {:name "cat", :size 1, :onchange "selChange();"}
              '([:option {:value "a"} "A"]
                [:option {:value "mexican", :selected "selected"} "Mexican"]
                [:option {:value "c"} "C"])]
             [:input
              {:type "submit", :name "action", :id "search", :value "Search"}]]]))))

(deftest link-data->html
  (testing "Generate HTML output for a uri text tuple with the correct attributes."
    (are [link-data expected] (= expected (html->hiccup (sut/link-data->html link-data)))

      ["/abc/" "ABC"]          [[:a {:href "/abc/"} "ABC"]]
      ["http://abc.de/" "ABC"] [[:a {:href "http://abc.de/",
                                     :rel "noopener noreferrer",
                                     :target "_blank"}
                                 "ABC"]])))

(deftest restaurant-links
  (testing "Generation of list of links."
    (are [label sep link-data expected] (= expected (sut/restaurant-links label sep link-data))

      "Test: " " * " [["/abc/" "ABC"]]
      (list "Test: " "<a href=\"/abc/\">ABC</a>")

      "Test: " " + " [["/abc/" "ABC"] ["/def/" "DEF"] ["/ghi/" "GHI"]]
      (list "Test: " "<a href=\"/abc/\">ABC</a> + <a href=\"/def/\">DEF</a> + <a href=\"/ghi/\">GHI</a>")

      "Test: " " / " [["/abc/" "ABC"] [nil "DEF"] ["/ghi/" "GHI"]]
      (list "Test: " "<a href=\"/abc/\">ABC</a> / <a href=\"/ghi/\">GHI</a>")

      "Test: " " = " [["/abc/" "ABC"] nil ["/ghi/" "GHI"]]
      (list "Test: " "<a href=\"/abc/\">ABC</a> = <a href=\"/ghi/\">GHI</a>"))))

(deftest restaurant-map-link-url
  (testing "Generate map link with form-encoded text."
    (is (= (sut/restaurant-map-link-url {:name "A B C's" :address "123 Main Rd." :city "Towns ville"})
           (str sut/base-mapping-url "A+B+C%27s,Towns+ville,FL")))))

(deftest restaurant-map-link-url-with-coords
  (testing "Generate map link including address when `:coords` present."
    (let [rest   (assoc (gen/generate (s/gen ::spec/restaurant)) :coords (gen/generate (s/gen :restaurant/coords)))
          markup (sut/restaurant-map-link-url rest)]
      (is (str/includes? markup (codec/form-encode (:address rest)))))))

(deftest twitter-link-data
  (testing "Generate twitter link data."
    (are [handle expected] (= expected (sut/twitter-link-data handle))

      "test" [(str sut/base-twitter-url "test") "@test"]
      nil    nil)))

(deftest restaurant-category-listing
  (testing "Restaurant category list with label and link."
    (are [rest cats cuisine expected] (= expected (sut/restaurant-category-listing rest cats cuisine))

      {:name "ABC" :address "" :city "" :zip "" :phone "" :categories #{:mexican}}
      {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}
      :all
      (list "Under: " "<a href=\"/mexican/\">Mexican</a>")

      {:name "ABC" :address "" :city "" :zip "" :phone "" :categories #{:mexican}}
      {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}
      :mexican
      "&nbsp"

      {:name "ABC" :address "" :city "" :zip "" :phone "" :categories #{:mexican :italian}}
      {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}
      :mexican
      (list "Also under: " "<a href=\"/italian/\">Italian</a>"))))


(deftest restaurant-by-category-single-category
  (testing "Restaurant list is filtered by the given category."
    (let [restaurants [{:name "Restaurant 1" :address "A" :city "A" :zip "12345" :phone "123 456-7890" :categories #{:italian}}
                       {:name "Restaurant 2" :address "B" :city "B" :zip "12345" :phone "123 456-7890" :categories #{:mexican}}
                       {:name "Restaurant 3" :address "C" :city "C" :zip "12345" :phone "123 456-7890" :categories #{:american}}]]
      (is (= (sut/restaurant-by-category restaurants :mexican)
             [{:name "Restaurant 2" :address "B" :city "B" :zip "12345" :phone "123 456-7890" :categories #{:mexican}}])))))

(deftest restaurant-by-category-all
  (testing "Restaurant list is not filtered when given `:all`"
    (let [restaurants (gen/generate (s/gen ::spec/restaurants))]
      (is (= (sut/restaurant-by-category restaurants :all)
             restaurants)))))

(deftest restaurants->html-single-category
  (testing "Restaurant listing output with single category."
    (let [rest-list [{:name "Rest Name" :address "B" :city "C" :zip "22222" :phone "123 456-7890" :categories #{:mexican}}]
          output    (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)]
      (is (= (html->hiccup output)
             [[:li
               {}
               [:h2 {} "Rest Name"]
               [:div
                {:class "info"}
                [:a {:href "tel:+1-123-456-7890"} "123 456-7890"]
                [:address {} 
                 [:a {:href (str sut/base-mapping-url "Rest+Name,C,FL")
                      :rel "noopener noreferrer"
                      :target "_blank"}
                  "B" [:br {}] "C, FL 22222"]]
                [:div {:class "links"}]]
               [:footer {} " "]]])))))

(deftest restaurants->html-single-category-with-alias
  (testing "Restaurant listing output with single category and an alias uses alias in heading."
    (let [rest-list [{:name "Rest Name" :alias "Resty" :address "B" :city "C" :zip "22222" :phone "123 456-7890" :categories #{:mexican}}]
          output    (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)
          h2-text   (-> (map html/as-hickory (html/parse-fragment output))
                        first
                        (get-in [:content 0 :content]))]
      (is (= h2-text
             ["Resty"])))))

(deftest restaurants->html-single-category-with-twitter
  (testing "Restaurant listing output with single category and twitter link."
    (let [rest-list [{:name "Rest Name" :address "B" :city "C" :zip "22222" :phone "123 456-7890" :twitter "rn" :categories #{:mexican}}]
          output    (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)
          twitter   (-> (map html/as-hickory (html/parse-fragment output))
                        first
                        (get-in [:content 1 :content 2 :content 1])
                        (dissoc :type :tag))]
      (is (= twitter
             {:attrs {:href (str sut/base-twitter-url "rn"), :rel "noopener noreferrer", :target "_blank"},
              :content ["@rn"]})))))

(deftest generate-category-page-head-includes-title
  (testing "Generating head portion of page includes title tag."
    (let [output (sut/generate-category-page-head [:all "All"])]
      (is (= (->> output
                  (filter #(and (vector? %) (= :title (first %))))
                  count)
             1)))))

(deftest generate-category-page-head-includes-meta-author
  (testing "Generating head portion of page includes author meta info."
    (let [output (sut/generate-category-page-head [:all "All"])]
      (is (= (->> output
                  (filter #(and (vector? %) (= :meta (first %))))
                  (filter (fn [[_ v]] (= "author" (:name v))))
                  count)
             1)))))

(deftest generate-category-page-head-adds-category-to-keywords
  (testing "Generating head portion of page when the category is set appends it to keyword meta."
    (let [category [:italian "Italian"]
          output   (sut/generate-category-page-head category)
          keywords (->> output
                        (filter #(and (vector? %) (= :meta (first %))))
                        (filter (fn [[_ v]] (= "keywords" (:name v))))
                        (map (fn [[_ v]] (:content v)))
                        first)]
      (is (str/includes? keywords (last category))))))

(deftest generate-category-page-head-adds-category-to-description
  (testing "Generating head portion of page when the category is set appends it to description meta."
    (let [category [:italian "Italian"]
          output   (sut/generate-category-page-head category)
          keywords (->> output
                        (filter #(and (vector? %) (= :meta (first %))))
                        (filter (fn [[_ v]] (= "description" (:name v))))
                        (map (fn [[_ v]] (:content v)))
                        first)]
      (is (str/includes? keywords (last category))))))

(deftest generate-category-page-sets-lang
  (testing "Generated HTML for a page has language set to 'en'."
    (let [output  (sut/generate-category-page [:italian "Italian"]
                                              [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
                                              {:italian "Italian"}
                                              {:italian "Italian"})
          content (->> (:content (html/as-hickory (html/parse output)))
                       (filter #(= :html (:tag %)))
                       first)]
      (is (= (get-in content [:attrs :lang])
             "en")))))

(deftest generate-category-page
  (testing "Generated HTML from category + restaurant data."
    (let [filtered-restaurants   [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
          cats                   {:italian "Italian"}]
      (are [selected-category expected] (= expected (sut/generate-category-page selected-category filtered-restaurants cats cats))

        [:italian "Italian"]
        "<!DOCTYPE html>\n<html lang=\"en\"><head><title>No Soup For You - Gainesville - Italian</title><meta content=\"Ed Porras\" name=\"author\"><meta content=\"Guide of independent restaurants and grocers in Gainesville, FL under the Italian category\" name=\"description\"><meta content=\"Gainesville Local Independently-owned Restaurants Italian\" name=\"keywords\"><meta content=\"width=device-width,initial-scale=1.0\" name=\"viewport\"><link href=\"/css/site.css\" rel=\"stylesheet\" type=\"text/css\"><script src=\"/js/site.js\" type=\"text/javascript\"></script></head><body onload=\"load();\"><header><h1><img alt=\"Dining in Gainesville\" height=\"42\" src=\"/img/logo.png\" width=\"293\"></h1><p>Locally-owned restaurants, cafes, and grocers.</p><nav><form action=\"/c\" method=\"get\" name=\"catlist\"><select name=\"cat\" onchange=\"selChange();\" size=\"1\"><option selected=\"selected\" value=\"italian\">Italian</option></select><input id=\"search\" name=\"action\" type=\"submit\" value=\"Search\"></form></nav></header><div id=\"content\"><ul><li><h2>Test 1</h2><div class=\"info\"><a href=\"tel:+1-123-456-7890\">123 456-7890</a><address><a href=\"https://maps.google.com/?daddr=Test+1,City,FL\" rel=\"noopener noreferrer\" target=\"_blank\">Address 1<br />City, FL 12345</a></address><div class=\"links\"></div></div><footer>&nbsp</footer></li></ul></div><footer><p>This is a listing of independent businesses in Gainesville, FL. If you own or know of a business you'd like to see listed, please contact: nsfy at digressed dot net or via Twitter at <a href=\"https://twitter.com/NSFYgnv\" rel=\"noopener noreferrer\" target=\"_blank\">@NSFYgnv</a>.</p></footer></body></html>"

        [:all "All"]
        "<!DOCTYPE html>\n<html lang=\"en\"><head><title>No Soup For You - Gainesville</title><meta content=\"Ed Porras\" name=\"author\"><meta content=\"Guide of independent restaurants and grocers in Gainesville, FL\" name=\"description\"><meta content=\"Gainesville Local Independently-owned Restaurants\" name=\"keywords\"><meta content=\"width=device-width,initial-scale=1.0\" name=\"viewport\"><link href=\"/css/site.css\" rel=\"stylesheet\" type=\"text/css\"><script src=\"/js/site.js\" type=\"text/javascript\"></script></head><body onload=\"load();\"><header><h1><img alt=\"Dining in Gainesville\" height=\"42\" src=\"/img/logo.png\" width=\"293\"></h1><p>Locally-owned restaurants, cafes, and grocers.</p><nav><form action=\"/c\" method=\"get\" name=\"catlist\"><select name=\"cat\" onchange=\"selChange();\" size=\"1\"><option value=\"italian\">Italian</option></select><input id=\"search\" name=\"action\" type=\"submit\" value=\"Search\"></form></nav></header><div id=\"content\"><ul><li><h2>Test 1</h2><div class=\"info\"><a href=\"tel:+1-123-456-7890\">123 456-7890</a><address><a href=\"https://maps.google.com/?daddr=Test+1,City,FL\" rel=\"noopener noreferrer\" target=\"_blank\">Address 1<br />City, FL 12345</a></address><div class=\"links\"></div></div><footer>Under: <a href=\"/italian/\">Italian</a></footer></li></ul></div><footer><p>This is a listing of independent businesses in Gainesville, FL. If you own or know of a business you'd like to see listed, please contact: nsfy at digressed dot net or via Twitter at <a href=\"https://twitter.com/NSFYgnv\" rel=\"noopener noreferrer\" target=\"_blank\">@NSFYgnv</a>.</p></footer></body></html>"))))

(deftest generate-category-restaurant-list
  (testing "Generate category to restaurant list map."
    (let [rest-list (sut/read-restaurant-list test-restaurants)]
      (are [categories expected] (= expected (sut/generate-category-restaurant-list categories rest-list))

        {:italian "Italian" :mexican "Mexican"}
        [[:italian '({:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}})]
         [:mexican '({:name "Test 3" :address "Address 3" :city "City" :zip "12345" :phone "123 456-7892" :categories #{:mexican}}
                     {:name "Test 4" :address "Address 4" :city "City" :zip "12345" :phone "123 456-7893" :categories #{:mexican}})]]

        {:american "American"}
        [[:american '()]]))))

(deftest filter-category-list-from-generated-restaurant-data
  (testing  "Removes entries from the category list that didn't generate any page output."
    (are [cat-rest-data cat-list expected] (= expected (sut/filter-category-list-from-generated-restaurant-data cat-rest-data cat-list))

      [[:all '([])] [:italian '([])]] {:all "A" :italian "I" :mexican "M" :latin "L"}
      {:all "A" :italian "I"}

      [[:italian '([])] [:all '([])] [:chinese '([])]] {:all "A" :italian "I" :mexican "M" :latin "L" :chinese "C"}
      (into (sorted-map) {:all "A" :italian "I" :chinese "C"}))))

(deftest category-page-output-path
  (testing "Output path for top-level and sub index pages.")
  (are [expected-path subpath category] (= expected-path (sut/category-page-output-path subpath category))

    "a-path/index.html" "a-path/" :all
    "a-path/italian/index.html" "a-path/" :italian))
