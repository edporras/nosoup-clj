(ns nosoup-clj.core-test
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.string  :as str]
            [clojure.java.io :as io]
            [hickory.core    :as html]
            [nosoup-clj.core :as sut]))

(defn- html->hiccup
  [html-str]
  (->> html-str
       (html/parse-fragment)
       (mapv html/as-hiccup)))

(def test-categories (io/file "test/categories.edn"))
(deftest read-categories-list-adds-all-entry-test
  (testing "Entry for `:all` is added to read categories."
    (is (= "All" (:all (sut/read-categories-list test-categories))))))

(def test-restaurants (io/file "test/restaurants.edn"))
(deftest read-restaurant-list-filters-closed-test
  (testing "Restaurants marked `:closed` are filtered."
    (let [restaurants (sut/read-restaurant-list test-restaurants)]
      (is (empty? (->> restaurants
                       (filter #(contains? (:opts %) :closed))))))))

(deftest categories->html-selected-test
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

(deftest link-data->html-test
  (testing "Generate HTML output for a uri text tuple with the correct attributes."
    (let [output (sut/link-data->html ["/abc/" "ABC"])]
      (is (= (html->hiccup output)
             [[:a {:href "/abc/"} "ABC"]])))))

(deftest link-data->html-with-external-link-test
  (testing "Generate HTML output for a uri text tuple with the correct attributes for external links."
    (let [output (sut/link-data->html ["http://abc.de/" "ABC"])]
      (is (= (html->hiccup output)
             [[:a
               {:href "http://abc.de/",
                :rel "noopener noreferrer",
                :target "_blank"}
               "ABC"]])))))

(deftest restaurant-links-single-entry-test
  (testing "Generation of list of links with a single entry (separator not used)."
    (let [output (sut/restaurant-links " * " [["/abc/" "ABC"]])]
      (is (= (html->hiccup output)
             [[:a {:href "/abc/"} "ABC"]])))))

(deftest restaurant-links-multiple-entries-test
  (testing "Generation of list of links with multiple entries should include separator."
    (let [output (sut/restaurant-links " + " [["/abc/" "ABC"] ["/def/" "DEF"] ["/ghi/" "GHI"]])]
      (is (= (html->hiccup output)
             [[:a {:href "/abc/"} "ABC"]
              " + "
              [:a {:href "/def/"} "DEF"]
              " + "
              [:a {:href "/ghi/"} "GHI"]])))))

(deftest restaurant-links-nil-url-dropped-test
  (testing "Generation of restaurant links with `nil` url value is dropped."
    (let [output (sut/restaurant-links " / " [["/abc/" "ABC"] [nil "DEF"] ["/ghi/" "GHI"]])]
      (is (= (html->hiccup output)
             [[:a {:href "/abc/"} "ABC"] " / " [:a {:href "/ghi/"} "GHI"]])))))

(deftest restaurant-links-nil-entry-dropped-test
  (testing "Generation of restaurant links with `nil` entry is dropped."
    (let [output (sut/restaurant-links " = " [["/abc/" "ABC"] nil ["/ghi/" "GHI"]])]
      (is (= (html->hiccup output)
             [[:a {:href "/abc/"} "ABC"] " = " [:a {:href "/ghi/"} "GHI"]])))))

(deftest restaurant-map-link-data-test
  (testing "Generate map link with form-encoded text."
    (is (= (sut/restaurant-map-link-data {:name "A B C's" :city "Towns ville"})
           ["https://maps.apple.com/?daddr=A+B+C%27s+Towns+ville+FL" "map"]))))

(deftest restaurant-map-link-data-with-coords-test
  (testing "Generate map link with `near` parameter when `:coords` present."
    (is (= (sut/restaurant-map-link-data {:name "ABC" :city "Townsville" :coords [1.2, 2.3]})
           ["https://maps.apple.com/?daddr=ABC+Townsville+FL&near=1.2,2.3" "map"]))))

(deftest twitter-link-data-test
  (testing "Generate twitter link data."
    (is (= (sut/twitter-link-data "test")
           [(str sut/base-twitter-url "test") "@test"]))))

(deftest twitter-link-data-with-nil-test
  (testing "Generate twitter link data."
    (is (nil? (sut/twitter-link-data nil)))))

(deftest restaurant-category-listing-single-all-test
  (testing "Category list with single entry, `:all` selected, creates link output with 'Under'."
    (let [rest       {:name "ABC" :address "" :city "" :zip "" :phone "" :categories #{:mexican}}
          categories {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}
          output     (sut/restaurant-category-listing rest categories :all)]
      (is (= (html->hiccup output)
             ["Under: " [:a {:href "/mexican/"} "Mexican"]])))))

(deftest restaurant-category-listing-single-selected-test
  (testing "Category list with single entry, same selected, creates '<br />' for spacing."
    (let [rest       {:name "ABC" :address "" :city "" :zip "" :phone "" :categories #{:mexican}}
          categories {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}
          output     (sut/restaurant-category-listing rest categories :mexican)]
      (is (= (html->hiccup output)
             [[:br {}]])))))

(deftest restaurant-category-listing-multiple-other-selected-test
  (testing "Category list with multiple entries, one selected, creates link output with 'Also under'."
    (let [rest       {:name "ABC" :address "" :city "" :zip "" :phone "" :categories #{:mexican :italian}}
          categories {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}
          output     (sut/restaurant-category-listing rest categories :mexican)]
      (is (= (html->hiccup output)
             ["Also under: " [:a {:href "/italian/"} "Italian"]])))))

(deftest restaurant-by-category-single-category-test
  (testing "Restaurant list is filtered by the given category."
    (let [restaurants [{:name "Restaurant 1" :address "A" :city "A" :zip "12345" :phone "123 456-7890" :categories #{:italian}}
                       {:name "Restaurant 2" :address "B" :city "B" :zip "12345" :phone "123 456-7890" :categories #{:mexican}}
                       {:name "Restaurant 3" :address "C" :city "C" :zip "12345" :phone "123 456-7890" :categories #{:american}}]]
      (is (= (sut/restaurant-by-category restaurants :mexican)
             [{:name "Restaurant 2" :address "B" :city "B" :zip "12345" :phone "123 456-7890" :categories #{:mexican}}])))))

(deftest restaurant-by-category-all-test
  (testing "Restaurant list is not filtered when given `:all`"
    (let [restaurants [{:name "Restaurant 1" :address "A" :city "A" :zip "12345" :phone "123 456-7890" :categories #{:italian}}
                       {:name "Restaurant 2" :address "B" :city "B" :zip "12345" :phone "123 456-7890" :categories #{:mexican}}
                       {:name "Restaurant 3" :address "C" :city "C" :zip "12345" :phone "123 456-7890" :categories #{:american}}]]
      (is (= (sut/restaurant-by-category restaurants :all)
             restaurants)))))

(deftest restaurants->html-single-category-test
  (testing "Restaurant listing output with single category."
    (let [rest-list [{:name "Rest Name" :address "B" :city "C" :zip "22222" :phone "123 456-7890" :categories #{:mexican}}]
          output    (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)]
      (is (= (html->hiccup output)
             [[:li
               {}
               [:h2 {} "Rest Name"]
               [:div
                {:class "info"}
                "123 456-7890" [:br {}]
                [:address {} "B" [:br {}] "C, FL 22222"]
                [:div
                 {:class "links"}
                 [:a
                  {:href "https://maps.apple.com/?daddr=Rest+Name+C+FL",
                   :rel "noopener noreferrer",
                   :target "_blank"}
                  "map"]]
                [:div {:class "cats"} [:br {}]]]]])))))

(deftest restaurants->html-single-category-with-alias-test
  (testing "Restaurant listing output with single category and an alias uses alias in heading."
    (let [rest-list [{:name "Rest Name" :alias "Resty" :address "B" :city "C" :zip "22222" :phone "123 456-7890" :categories #{:mexican}}]
          output    (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)
          h2-text   (-> (map html/as-hickory (html/parse-fragment output))
                        first
                        (get-in [:content 0 :content]))]
      (is (= h2-text
             ["Resty"])))))

(deftest restaurants->html-single-category-with-twitter-test
  (testing "Restaurant listing output with single category and twitter link."
    (let [rest-list [{:name "Rest Name" :address "B" :city "C" :zip "22222" :phone "123 456-7890" :twitter "rn" :categories #{:mexican}}]
          output    (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)
          twitter   (-> (map html/as-hickory (html/parse-fragment output))
                        first
                        (get-in [:content 1 :content 3 :content 2])
                        (dissoc :type :tag))]
      (is (= twitter
             {:attrs {:href "https://twitter.com/rn", :rel "noopener noreferrer", :target "_blank"},
              :content ["@rn"]})))))

(deftest generate-category-page-head-includes-title-test
  (testing "Generating head portion of page includes title tag."
    (let [output (sut/generate-category-page-head [:all "All"])]
      (is (= (->> output
                  (filter #(and (vector? %) (= :title (first %))))
                  count)
             1)))))

(deftest generate-category-page-head-includes-meta-author-test
  (testing "Generating head portion of page includes author meta info."
    (let [output (sut/generate-category-page-head [:all "All"])]
      (is (= (->> output
                  (filter #(and (vector? %) (= :meta (first %))))
                  (filter (fn [[_ v]] (= "author" (:name v))))
                  count)
             1)))))

(deftest generate-category-page-head-adds-category-to-keywords-test
  (testing "Generating head portion of page when the category is set appends it to keyword meta."
    (let [category [:italian "Italian"]
          output   (sut/generate-category-page-head category)
          keywords (->> output
                        (filter #(and (vector? %) (= :meta (first %))))
                        (filter (fn [[_ v]] (= "keywords" (:name v))))
                        (map (fn [[_ v]] (:content v)))
                        first)]
      (is (str/includes? keywords (last category))))))

(deftest generate-category-page-head-adds-category-to-description-test
  (testing "Generating head portion of page when the category is set appends it to description meta."
    (let [category [:italian "Italian"]
          output   (sut/generate-category-page-head category)
          keywords (->> output
                        (filter #(and (vector? %) (= :meta (first %))))
                        (filter (fn [[_ v]] (= "description" (:name v))))
                        (map (fn [[_ v]] (:content v)))
                        first)]
      (is (str/includes? keywords (last category))))))

(deftest generate-category-page-sets-lang-test
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

(deftest generate-category-page-test
  (testing "Generated HTML from category + restaurant data with selected category."
    (is (= (sut/generate-category-page [:italian "Italian"]
                                       [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
                                       {:italian "Italian"}
                                       {:italian "Italian"})
           "<!DOCTYPE html>\n<html lang=\"en\"><head><title>No Soup For You - Gainesville - Italian</title><meta content=\"Ed Porras\" name=\"author\"><meta content=\"Guide of independent restaurants and grocers in Gainesville, FL under the Italian category\" name=\"description\"><meta content=\"Gainesville Local Independently-owned Restaurants Italian\" name=\"keywords\"><meta content=\"width=device-width,initial-scale=1.0,user-scalable=no\" name=\"viewport\"><link href=\"/css/styles.css\" rel=\"stylesheet\" type=\"text/css\"><script src=\"/js/searchbar.js\" type=\"text/javascript\"></script></head><body onload=\"load();\"><header><h1><img alt=\"Dining in Gainesville\" height=\"42\" src=\"/img/logo.png\" width=\"293\"></h1><p>Locally-owned restaurants, cafes, and grocers.</p><nav><form action=\"/c\" method=\"get\" name=\"catlist\"><select name=\"cat\" onchange=\"selChange();\" size=\"1\"><option selected=\"selected\" value=\"italian\">Italian</option></select><input id=\"search\" name=\"action\" type=\"submit\" value=\"Search\"></form></nav></header><div id=\"content\"><ul id=\"listing\"><li><h2>Test 1</h2><div class=\"info\">123 456-7890<br><address>Address 1<br>City, FL 12345</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Test+1+City+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a></div><div class=\"cats\"><br /></div></div></li></ul></div><footer><p id=\"about\">This is a listing of independent businesses in Gainesville, FL. If you own or know of a business you'd like to see listed, please contact: nsfy at digressed dot net or via Twitter at <a href=\"https://twitter.com/NSFYgnv\" rel=\"noopener noreferrer\" target=\"_blank\">@NSFYgnv</a>.</p></footer></body></html>"))))

(deftest generate-category-page-for-all-test
  (testing "Generating HTML from category + restaurant data with category `:all`."
    (is (= (sut/generate-category-page [:all "All"]
                                       [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
                                       {:italian "Italian"}
                                       {:italian "Italian"})
           "<!DOCTYPE html>\n<html lang=\"en\"><head><title>No Soup For You - Gainesville</title><meta content=\"Ed Porras\" name=\"author\"><meta content=\"Guide of independent restaurants and grocers in Gainesville, FL\" name=\"description\"><meta content=\"Gainesville Local Independently-owned Restaurants\" name=\"keywords\"><meta content=\"width=device-width,initial-scale=1.0,user-scalable=no\" name=\"viewport\"><link href=\"/css/styles.css\" rel=\"stylesheet\" type=\"text/css\"><script src=\"/js/searchbar.js\" type=\"text/javascript\"></script></head><body onload=\"load();\"><header><h1><img alt=\"Dining in Gainesville\" height=\"42\" src=\"/img/logo.png\" width=\"293\"></h1><p>Locally-owned restaurants, cafes, and grocers.</p><nav><form action=\"/c\" method=\"get\" name=\"catlist\"><select name=\"cat\" onchange=\"selChange();\" size=\"1\"><option value=\"italian\">Italian</option></select><input id=\"search\" name=\"action\" type=\"submit\" value=\"Search\"></form></nav></header><div id=\"content\"><ul id=\"listing\"><li><h2>Test 1</h2><div class=\"info\">123 456-7890<br><address>Address 1<br>City, FL 12345</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Test+1+City+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a></div><div class=\"cats\">Under: <a href=\"/italian/\">Italian</a></div></div></li></ul></div><footer><p id=\"about\">This is a listing of independent businesses in Gainesville, FL. If you own or know of a business you'd like to see listed, please contact: nsfy at digressed dot net or via Twitter at <a href=\"https://twitter.com/NSFYgnv\" rel=\"noopener noreferrer\" target=\"_blank\">@NSFYgnv</a>.</p></footer></body></html>"))))

(deftest generate-category-restaurant-list-test
  (testing "Generate category to restaurant list map."
    (is (= (sut/generate-category-restaurant-list {:italian "Italian" :mexican "Mexican"}
                                                  (sut/read-restaurant-list test-restaurants))
           [[:italian '({:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}})]
            [:mexican '({:name "Test 3" :address "Address 3" :city "City" :zip "12345" :phone "123 456-7892" :categories #{:mexican}}
                        {:name "Test 4" :address "Address 4" :city "City" :zip "12345" :phone "123 456-7893" :categories #{:mexican}})]]))))

(deftest generate-category-restaurant-list-empty-test
  (testing "Generate category to restaurant list map produces entry w/ empty list when none are found."
    (is (= (sut/generate-category-restaurant-list {:american "American"}
                                                  (sut/read-restaurant-list test-restaurants))
           [[:american '()]]))))

(deftest filter-category-list-from-generated-restaurant-data-test
  (testing  "Removes entries from the category list that didn't generate any page output."
    (is (= (sut/filter-category-list-from-generated-restaurant-data [[:all '([])] [:italian '([])]] {:all "A" :italian "I" :mexican "M" :latin "L"})
           {:all "A" :italian "I"}))))

(deftest filter-category-list-from-generated-restaurant-data-returns-sorted-list-test
  (testing  "Filtered category list is sorted."
    (let [output (sut/filter-category-list-from-generated-restaurant-data [[:italian '([])] [:all '([])] [:vietnamese '([])]] {:all "A" :italian "I" :mexican "M" :latin "L" :chinese "C"})]
      (is (= output
             (into (sorted-map) output))))))

(deftest category-page-output-path-for-root-test
  (testing "Output path for main index page.")
  (is (= (sut/category-page-output-path "a-path/" :all)
         "a-path/index.html")))

(deftest category-page-output-path-for-subcategory-test
  (testing "Output path for sub-category index page includes subpath.")
  (is (= (sut/category-page-output-path "a-path/" :italian)
         "a-path/italian/index.html")))
