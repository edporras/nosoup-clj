(ns nosoup-clj.util-test
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [java-time.api :as t]
   [nosoup-clj.core :as nosoup]
   [nosoup-clj.util :as sut]))

(def test-site-baseroot-path "test/site/")
(def test-site-sitemap-xml "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>https://nosoupforyou.com/italian/</loc><lastmod>2020-04-14</lastmod><changefreq>monthly</changefreq></url><url><loc>https://nosoupforyou.com/mexican/</loc><lastmod>2020-04-29</lastmod><changefreq>monthly</changefreq></url></urlset>")

(defn timestamp-test-files
  [all-tests]
  (doseq [[f stamp] [["test/site/italian/index.html" (t/local-date "2020-04-14")]
                     ["test/site/mexican/index.html" (t/local-date "2020-04-29")]]]
    (let [file     (io/file f)
          mod-time (-> (.lastModified file)
                       (t/instant)
                       (t/local-date (t/zone-id)))]
      (when-not (= mod-time stamp)
        (.setLastModified file (-> stamp
                                   (t/zoned-date-time (t/zone-id))
                                   (t/java-date)
                                   (t/to-millis-from-epoch))))))
  (all-tests))

(use-fixtures :once timestamp-test-files)

(deftest file-mdate-test
  (testing "Getting a file's mod time."
    (is (= (sut/file-mdate "test/site/italian/index.html")
           "2020-04-14"))))

(deftest categories->sitemap-test
  (testing "Sitemap generation from a filtered and sorted category list."
    (are [cat-list] (= test-site-sitemap-xml (sut/categories->sitemap test-site-baseroot-path cat-list))

      {:italian "Italian" :mexican "Mexican"}
      {:all "All" :italian "Italian" :mexican "Mexican"})))

(deftest generate-sitemap-test
  (testing "Sitemap is written to disk at given path."
    (let [sitemap (io/file (str test-site-baseroot-path "sitemap.xml"))]
      (sut/generate-sitemap test-site-baseroot-path {:all "All" :italian "Italian" :mexican "Mexican"})
      (.deleteOnExit sitemap)
      (is (fs/exists? sitemap)))))

(deftest resource-outdated?-test
  (testing "Returns `true` if destination does not exist."
    (fs/with-temp-dir [tmp]
      (is (sut/resource-outdated? (str tmp fs/file-separator "t.tmp") "1"))))

  (testing "Returns `false` if destination exists and matches output."
    (let [path "test/site/italian/index.html"
          data (slurp path)]
      (is (not (sut/resource-outdated? path data)))))

  (testing "Returns `true` if destination exists but output differs."
    (let [path "test/site/italian/index.html"
          data (str (slurp path) "blah")]
      (is (sut/resource-outdated? path data)))))

(deftest cleanup-markup-test
  (testing "Removes type='javascript' attribute not valid in html5."
    (is (= (sut/cleanup-markup "aa<script src=\"/js/searchbar.js\" type=\"text/javascript\"></script>aaa")
           "aa<script src=\"/js/searchbar.js\"></script>aaa"))))

(deftest to-disk-test
  (testing "Writes file to disk and returns `true`."
    (fs/with-temp-dir [tmp]
      (let [tmp-file (io/file (str tmp fs/file-separator "t.tmp"))
            status   (sut/to-disk tmp-file "[1 2 3 4]")]
        (is (and (= status true) (fs/exists? tmp-file)))))))

(deftest output->disk-test
  (testing "Writes file to disk (creating parent dirs), returning `true`."
    (fs/with-temp-dir [tmp]
      (let [tmp-file (io/file (str tmp fs/file-separator "t.tmp"))
            status   (sut/output->disk (.getAbsolutePath tmp-file) "[1 2 3 4]")]
        (is (and (= status true) (fs/exists? tmp-file))))))

  (testing "Does not overwrite file to disk when matching file exists; returns `nil`."
    (fs/with-temp-dir [tmp]
      (let [output "[1 2 3 4]"
            out-file (io/file (str tmp fs/file-separator "t.tmp"))]
        (sut/to-disk out-file output) ; pre-write file to disk
        (let [mod-date-before (.lastModified out-file)
              status (sut/output->disk out-file output)]
          (is (and (nil? status) (= mod-date-before (.lastModified out-file))))))))

  (testing "Writes file with correct whitespaces."
    (let [category [:italian "Italian"]
          restaurants [{:name "Test 1" :address "Address 1" :city "City" :zip "12345" :phone "123 456-7890" :categories #{:italian}}]
          categories {:italian "Italian"}]
      (fs/with-temp-dir [tmp]
        (let [filename (io/file (str tmp fs/file-separator "t.tmp"))]
          (->> (nosoup/generate-category-page category restaurants categories categories)
               (sut/output->disk filename))
          (is (not (str/includes? filename "\\n"))))))))
