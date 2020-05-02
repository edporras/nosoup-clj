(ns nosoup-clj.util-test
  (:require [clj-time.coerce    :as c]
            [clj-time.local     :as l]
            [clojure.java.io    :as io]
            [clojure.test       :refer [deftest is testing use-fixtures]]
            [nosoup-clj.core    :as nosoup]
            [nosoup-clj.util    :as sut]
            [tools.io           :refer [with-tempfile]])
  (:import  [java.util Date]))

(def test-site-baseroot-path "test/site/")
(def test-site-sitemap-xml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>https://nosoupforyou.com/italian/</loc><lastmod>2020-04-14</lastmod><changefreq>monthly</changefreq></url><url><loc>https://nosoupforyou.com/mexican/</loc><lastmod>2020-04-29</lastmod><changefreq>monthly</changefreq></url></urlset>")

(defn timestamp-test-files
  [all-tests]
  (doseq [[f stamp] [["test/site/italian/index.html" "2020-04-14"]
                     ["test/site/mexican/index.html" "2020-04-29"]]]
    (let [file     (io/file f)
          mod-time (-> (.lastModified file)
                       (l/format-local-time :year-month-day))]
      (when-not (= mod-time stamp)
        (.setLastModified file (c/to-long stamp)))))
  (all-tests))

(use-fixtures :once timestamp-test-files)

(deftest file-mdate-test
  (testing "Getting a file's mod time."
    (is (= (sut/file-mdate "test/site/italian/index.html")
           "2020-04-14"))))

(deftest categories->sitemap-test
  (testing "Sitemap generation from a filtered and sorted category list."
    (is (= (sut/categories->sitemap test-site-baseroot-path {:italian "Italian" :mexican "Mexican"})
           test-site-sitemap-xml))))

(deftest categories->sitemap-omits-all-test
  (testing "Sitemap generation from a filtered and sorted category list omits `:all` entry."
    (is (= (sut/categories->sitemap test-site-baseroot-path {:all "All" :italian "Italian" :mexican "Mexican"})
           test-site-sitemap-xml))))

(deftest generate-sitemap-test
  (testing "Sitemap is written to disk at given path."
    (let [sitemap (io/file (str test-site-baseroot-path "sitemap.xml"))]
      (sut/generate-sitemap test-site-baseroot-path {:all "All" :italian "Italian" :mexican "Mexican"})
      (.deleteOnExit sitemap)
      (is (.exists sitemap)))))

(deftest to-disk-test
  (testing "Writes file to disk and returns `true`."
    (with-tempfile [tmp]
      (let [tmp-file (io/file tmp)
            status   (sut/to-disk tmp-file "[1 2 3 4]")]
        (is (and (= status true) (.exists tmp-file)))))))

(deftest output->disk-test
  (testing "Writes file to disk (creating parent dirs), returning `true`."
    (with-tempfile [tmp]
      (let [tmp-file (io/file tmp)
            status   (sut/output->disk (.getAbsolutePath tmp-file) "[1 2 3 4]")]
        (is (and (= status true) (.exists tmp-file)))))))

(deftest output->disk-no-write-test
  (testing "Does not overwrite file to disk when matching file exists; returns `nil`."
    (with-tempfile [tmp]
      (let [output   "[1 2 3 4]"
            out-file (io/file tmp)]
        (sut/to-disk out-file output) ;; pre-write file to disk
        (let [mod-date-before (Date. (.lastModified out-file))
              status          (sut/output->disk out-file output)]
          (is (and (nil? status) (= mod-date-before (Date. (.lastModified out-file))))))))))

(deftest output->disk-uses-correct-print-test
  (testing "Writes file with correct whitespaces."
    (let [category    [:italian "Italian"]
          restaurants [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
          categories  {:italian "Italian"}]
      (with-tempfile [tmp]
        (->> (nosoup/generate-category-page category restaurants categories categories)
             (sut/output->disk tmp))
        (is (= (slurp tmp)
               (slurp "test/site/italian/index.html")))))))
