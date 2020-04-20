(ns nosoup-clj.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [nosoup-clj.core :as sut]))

(def test-categories (io/resource "test-categories.edn"))
(deftest read-categories-list-adds-all-entry-test
  (testing "Entry for `:all` is added to read categories."
    (is (= "All" (:all (sut/read-categories-list test-categories))))))

(def test-restaurants (io/resource "test-restaurants.edn"))
(deftest read-restaurant-list-filters-closed-test
  (testing "Restaurants marked `:closed` are filtered."
    (let [restaurants (sut/read-restaurant-list test-restaurants)]
      (is (empty? (->> restaurants
                       (filter #(contains? (:opts %) :closed))))))))

(deftest categories->html-selected-test
  (testing "Category options list selects the correct option.")
  (is (= (sut/categories->html [[:a "A"] [:mexican "Mexican"] [:c "C"]] :mexican)
         [:nav
          [:form
           {:name "catlist", :method :get, :action "/c"}
           [:select
            {:name "cat", :size 1, :onchange "selChange();"}
            '([:option {:value "a"} "A"]
              [:option {:value "mexican", :selected "selected"} "Mexican"]
              [:option {:value "c"} "C"])]
           [:input
            {:type "submit", :name "action", :id "search", :value "Search"}]]])))

(deftest link-data->html-test
  (testing "Generate HTML output for a uri text tuple with the correct attributes.")
  (is (= (sut/link-data->html ["/abc/" "ABC"])
         "<a href=\"/abc/\">ABC</a>")))

(deftest link-data->html-with-external-link-test
  (testing "Generate HTML output for a uri text tuple with the correct attributes for external links.")
  (is (= (sut/link-data->html ["http://abc.de/" "ABC"])
         "<a href=\"http://abc.de/\" rel=\"noopener noreferrer\" target=\"_blank\">ABC</a>")))

(deftest restaurant-links-relative-link-test
  (testing "Generation of a single restaurant link to relative target - separator is not used.")
  (is (= (sut/restaurant-links " * " [["/abc/" "ABC"]])
         "<a href=\"/abc/\">ABC</a>")))

(deftest restaurant-links-relative-links-test
  (testing "Generation of restaurant links to relative targets with separator.")
  (is (= (sut/restaurant-links " + " [["/abc/" "ABC"] ["/def/" "DEF"] ["/ghi/" "GHI"]])
         "<a href=\"/abc/\">ABC</a> + <a href=\"/def/\">DEF</a> + <a href=\"/ghi/\">GHI</a>")))

(deftest restaurant-links-nil-url-dropped-test
  (testing "Generation of restaurant links with `nil` url value is dropped.")
  (is (= (sut/restaurant-links " / " [["/abc/" "ABC"] [nil "DEF"] ["/ghi/" "GHI"]])
         "<a href=\"/abc/\">ABC</a> / <a href=\"/ghi/\">GHI</a>")))

(deftest restaurant-links-nil-entry-dropped-test
  (testing "Generation of restaurant links with `nil` entry is dropped.")
  (is (= (sut/restaurant-links " & " [["/abc/" "ABC"] nil ["/ghi/" "GHI"]])
         "<a href=\"/abc/\">ABC</a> & <a href=\"/ghi/\">GHI</a>")))

(deftest restaurant-links-external-target-test
  (testing "Generation of restaurant links to external target includes `rel` attribute.")
  (is (= (sut/restaurant-links " = " [["/abc/" "ABC"] ["/def/" "DEF"] ["https://abc.de" "ABCDE"]])
         "<a href=\"/abc/\">ABC</a> = <a href=\"/def/\">DEF</a> = <a href=\"https://abc.de\" rel=\"noopener noreferrer\" target=\"_blank\">ABCDE</a>")))

(deftest restaurant-map-link-data-test
  (testing "Generate map link with form-encoded text.")
  (is (= (sut/restaurant-map-link-data {:name "A B C's" :city "Towns ville"})
         ["https://maps.apple.com/?daddr=A+B+C%27s+Towns+ville+FL" "map"])))

(deftest restaurant-map-link-data-with-coords-test
  (testing "Generate map link with `near` parameter when `:coords` present.")
  (is (= (sut/restaurant-map-link-data {:name "ABC" :city "Townsville" :coords [1.2, 2.3]})
         ["https://maps.apple.com/?daddr=ABC+Townsville+FL&near=1.2,2.3" "map"])))

(deftest twitter-link-data-test
  (testing "Generate twitter link data.")
  (is (= (sut/twitter-link-data "test")
         [(str sut/base-twitter-url "test") "@test"])))

(deftest twitter-link-data-with-nil-test
  (testing "Generate twitter link data.")
  (is (nil? (sut/twitter-link-data nil))))

(deftest restaurant-category-listing-single-all-test
  (testing "Category list with single entry, `:all` selected, creates link output with 'Under'.")
  (let [rest       {:name "ABC" :categories #{:mexican}}
        categories {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}]
    (is (= (sut/restaurant-category-listing rest categories :all)
           "Under: <a href=\"/mexican/\">Mexican</a>"))))

(deftest restaurant-category-listing-single-selected-test
  (testing "Category list with single entry, same selected, creates '<br />' for spacing.")
  (let [rest       {:name "ABC" :categories #{:mexican}}
        categories {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}]
    (is (= (sut/restaurant-category-listing rest categories :mexican)
           "<br />"))))

(deftest restaurant-category-listing-multiple-other-selected-test
  (testing "Category list with multiple entries, one selected, creates link output with 'Also under'.")
  (let [rest       {:name "ABC" :categories #{:mexican :italian}}
        categories {:a "A" :b "B" :c "C" :italian "Italian" :mexican "Mexican"}]
    (is (= (sut/restaurant-category-listing rest categories :mexican)
           "Also under: <a href=\"/italian/\">Italian</a>"))))

(deftest restaurant-by-category-single-category-test
  (testing "Restaurant list is filtered by the given category.")
  (let [restaurants [{:name "Restaurant 1" :categories #{:italian}}
                     {:name "Restaurant 2" :categories #{:mexican}}
                     {:name "Restaurant 3" :categories #{:american}}]]
    (is (= (sut/restaurant-by-category restaurants :mexican)
           [{:name "Restaurant 2" :categories #{:mexican}}]))))

(deftest restaurant-by-category-all-test
  (testing "Restaurant list is not filtered when given `:all`")
  (let [restaurants [{:name "Restaurant 1" :categories #{:italian}}
                     {:name "Restaurant 2" :categories #{:mexican}}
                     {:name "Restaurant 3" :categories #{:american}}]]
    (is (= (sut/restaurant-by-category restaurants :all)
           restaurants))))

(deftest restaurants->html-single-category-test
  (testing "Restaurant listing output with single category.")
  (let [rest-list [{:name "Rest Name" :address "B" :city "C" :zip "22222" :categories #{:mexican}}]]
    (is (= (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)
           "<li><h2>Rest Name</h2><div class=\"info\"><br /><address>B<br />C, FL 22222</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Rest+Name+C+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a></div><div class=\"cats\"><br /></div></div></li>"))))

(deftest restaurants->html-single-category-with-alias-test
  (testing "Restaurant listing output with single category and an alias.")
  (let [rest-list [{:name "Rest Name" :alias "Resty" :address "B" :city "C" :zip "22222" :categories #{:mexican}}]]
    (is (= (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)
           "<li><h2>Resty</h2><div class=\"info\"><br /><address>B<br />C, FL 22222</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Rest+Name+C+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a></div><div class=\"cats\"><br /></div></div></li>"))))

(deftest restaurants->html-single-category-with-twitter-test
  (testing "Restaurant listing output with single category and twitter link.")
  (let [rest-list [{:name "Rest Name" :address "B" :city "C" :zip "22222" :twitter "rn" :categories #{:mexican}}]]
    (is (= (sut/restaurants->html rest-list {:mexican "Mexican"} :mexican)
           "<li><h2>Rest Name</h2><div class=\"info\"><br /><address>B<br />C, FL 22222</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Rest+Name+C+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a> | <a href=\"https://twitter.com/rn\" rel=\"noopener noreferrer\" target=\"_blank\">@rn</a></div><div class=\"cats\"><br /></div></div></li>"))))

(deftest restaurants->html-single-category-under-all-test
  (testing "Restaurant listing output with single category under the `:all` listing.")
  (let [rest-list [{:name "Rest Name" :address "B" :city "C" :zip "22222" :categories #{:mexican}}]]
    (is (= (sut/restaurants->html rest-list {:mexican "Mexican"} :all)
           "<li><h2>Rest Name</h2><div class=\"info\"><br /><address>B<br />C, FL 22222</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Rest+Name+C+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a></div><div class=\"cats\">Under: <a href=\"/mexican/\">Mexican</a></div></div></li>"))))

(deftest restaurants->html-multiple-category-test
  (testing "Restaurant listing output with multiple categories.")
  (let [rest-list [{:name "Rest Name" :address "B" :city "C" :zip "22222" :categories #{:mexican :italian}}]]
    (is (= (sut/restaurants->html rest-list {:italian "Italian" :mexican "Mexican"} :mexican)
           "<li><h2>Rest Name</h2><div class=\"info\"><br /><address>B<br />C, FL 22222</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Rest+Name+C+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a></div><div class=\"cats\">Also under: <a href=\"/italian/\">Italian</a></div></div></li>"))))

(deftest generate-category-page-test
  (testing "Generating HTML from catefory + restaurant data with selected category."
    (is (= (sut/generate-category-page [:italian "Italian"]
                                       [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
                                       {:italian "Italian"}
                                       {:italian "Italian"})
           "<!DOCTYPE html>\n<html lang=\"en\"><head><title>No Soup For You - Gainesville - Italian</title><meta content=\"Ed Porras\" name=\"author\"><meta content=\"Guide of independent restaurants in Gainesville, FL.\" name=\"description\"><meta content=\"Gainesville Restaurants breakfast lunch brunch dinner\" name=\"keywords\"><meta content=\"width=device-width,initial-scale=1.0,user-scalable=no\" name=\"viewport\"><link href=\"/css/styles.css\" rel=\"stylesheet\" type=\"text/css\"><script src=\"/js/searchbar.js\" type=\"text/javascript\"></script></head><body onload=\"load();\"><header><h1><img alt=\"Dining in Gainesville\" height=\"42\" src=\"/img/logo.png\" width=\"293\"></h1><p>Locally-owned restaurants, cafes, and grocers.</p><nav><form action=\"/c\" method=\"get\" name=\"catlist\"><select name=\"cat\" onchange=\"selChange();\" size=\"1\"><option selected=\"selected\" value=\"italian\">Italian</option></select><input id=\"search\" name=\"action\" type=\"submit\" value=\"Search\"></form></nav></header><div id=\"content\"><ul id=\"listing\"><li><h2>Test 1</h2><div class=\"info\">123 456-7890<br><address>Address 1<br>City, FL 12345</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Test+1+City+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a></div><div class=\"cats\"><br /></div></div></li></ul></div><footer><p id=\"about\">This is a listing of independent businesses in Gainesville, FL. If you own or know of a business you'd like to see listed, please contact: nsfy at digressed dot net or via Twitter at <a href=\"https://twitter.com/NSFYgnv\" rel=\"noopener noreferrer\" target=\"_blank\">@NSFYgnv</a>.</p></footer></body></html>"))))

(deftest generate-category-page-for-all-test
  (testing "Generating HTML from catefory + restaurant data with category `:all`."
    (is (= (sut/generate-category-page [:all "All"]
                                       [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
                                       {:italian "Italian"}
                                       {:italian "Italian"})
           "<!DOCTYPE html>\n<html lang=\"en\"><head><title>No Soup For You - Gainesville</title><meta content=\"Ed Porras\" name=\"author\"><meta content=\"Guide of independent restaurants in Gainesville, FL.\" name=\"description\"><meta content=\"Gainesville Restaurants breakfast lunch brunch dinner\" name=\"keywords\"><meta content=\"width=device-width,initial-scale=1.0,user-scalable=no\" name=\"viewport\"><link href=\"/css/styles.css\" rel=\"stylesheet\" type=\"text/css\"><script src=\"/js/searchbar.js\" type=\"text/javascript\"></script></head><body onload=\"load();\"><header><h1><img alt=\"Dining in Gainesville\" height=\"42\" src=\"/img/logo.png\" width=\"293\"></h1><p>Locally-owned restaurants, cafes, and grocers.</p><nav><form action=\"/c\" method=\"get\" name=\"catlist\"><select name=\"cat\" onchange=\"selChange();\" size=\"1\"><option value=\"italian\">Italian</option></select><input id=\"search\" name=\"action\" type=\"submit\" value=\"Search\"></form></nav></header><div id=\"content\"><ul id=\"listing\"><li><h2>Test 1</h2><div class=\"info\">123 456-7890<br><address>Address 1<br>City, FL 12345</address><div class=\"links\"><a href=\"https://maps.apple.com/?daddr=Test+1+City+FL\" rel=\"noopener noreferrer\" target=\"_blank\">map</a></div><div class=\"cats\">Under: <a href=\"/italian/\">Italian</a></div></div></li></ul></div><footer><p id=\"about\">This is a listing of independent businesses in Gainesville, FL. If you own or know of a business you'd like to see listed, please contact: nsfy at digressed dot net or via Twitter at <a href=\"https://twitter.com/NSFYgnv\" rel=\"noopener noreferrer\" target=\"_blank\">@NSFYgnv</a>.</p></footer></body></html>"))))

(deftest generate-category-restaurant-list-test
  (testing "Generate category to restaurant list map."
    (is (= (sut/generate-category-restaurant-list {:mexican "Mexican" :italian "Italian"}
                                                  (sut/read-restaurant-list test-restaurants))
           {:italian [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
            :mexican [{:name "Test 3" :address "Address 3" :city "City" :zip "12345" :phone "123 456-7892" :categories #{:mexican}}
                      {:name "Test 4" :address "Address 4" :city "City" :zip "12345" :phone "123 456-7893" :categories #{:mexican}}]}))))

(deftest generate-category-restaurant-list-empty-test
  (testing "Generate category to restaurant list map produces empty list when none are found."
    (is (= (sut/generate-category-restaurant-list {:american "American"}
                                                  (sut/read-restaurant-list test-restaurants))
           {}))))
