(ns nosoup-clj.util-test
  (:require [clojure.test    :refer [deftest is testing]]
            [clojure.java.io :as io]
            [nosoup-clj.util :as sut])
  (:import  [java.util Date]))

(deftest categories->sitemap-test
  (testing "Sitemap generation from a filtered and sorted category list."
    (is (= (sut/categories->sitemap "2020-04-22" {:italian "Italian" :mexican "Mexican"})
           "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>https://nosoupforyou.com/italian/</loc><lastmod>2020-04-22</lastmod><changefreq>monthly</changefreq></url><url><loc>https://nosoupforyou.com/mexican/</loc><lastmod>2020-04-22</lastmod><changefreq>monthly</changefreq></url></urlset>"))))

(deftest categories->sitemap-omits-all-test
  (testing "Sitemap generation from a filtered and sorted category list omits `:all` entry."
    (is (= (sut/categories->sitemap "2020-04-22" {:all "All" :italian "Italian" :mexican "Mexican"})
           "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>https://nosoupforyou.com/italian/</loc><lastmod>2020-04-22</lastmod><changefreq>monthly</changefreq></url><url><loc>https://nosoupforyou.com/mexican/</loc><lastmod>2020-04-22</lastmod><changefreq>monthly</changefreq></url></urlset>"))))

(deftest generate-sitemap-test
  (testing "Sitemap is written to disk at given path."
    (let [tmp-file (java.io.File/createTempFile "sitemap" ".xml")]
      (sut/generate-sitemap tmp-file {:all "All" :italian "Italian" :mexican "Mexican"})
      (.deleteOnExit tmp-file)
      (is (.exists (io/file tmp-file))))))

(deftest to-disk-test
  (testing "Writes file to disk and returns `true`."
    (let [tmp-file (java.io.File/createTempFile "out" ".txt")
          status   (sut/to-disk tmp-file (pr-str [1 2 3 4]))]
      (.deleteOnExit tmp-file)
      (is (and (= status true) (.exists (io/file tmp-file)))))))

(deftest output->disk-test
  (testing "Writes file to disk (creating parent dirs) and returns `true`."
    (let [tmp-file (java.io.File/createTempFile "index" ".html")
          status   (sut/output->disk (.getAbsolutePath tmp-file) [1 2 3 4])]
      (.deleteOnExit tmp-file)
      (is (and (= status true) (.exists tmp-file))))))

(deftest output->disk-no-write-test
  (testing "Does not overwrite file to disk when matching file exists; returns `nil`"
    (let [tmp-file (java.io.File/createTempFile "index" ".html")
          output   [1 2 3 4]]
      (.deleteOnExit tmp-file)
      (sut/to-disk tmp-file (pr-str output)) ;; pre-write file to disk
      (let [mod-date-before (Date. (.lastModified tmp-file))
            status          (sut/output->disk (.getAbsolutePath tmp-file) output)]
        (is (and (nil? status) (= mod-date-before (Date. (.lastModified tmp-file)))))))))
