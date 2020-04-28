(ns nosoup-clj.util
  (:require [nosoup-clj.spec         :as spec]
            [clj-time.local          :as l]
            [clojure.spec.alpha      :as s]
            [clojure.java.io         :as io]
            [sitemap.core            :as sitemap :refer-only [generate-sitemap]])
  (:import [java.io   File]
           [java.util Date])
  (:gen-class))

(defn categories->sitemap
  "Generate the site's sitemap from the givene final category list."
  [lastmod-date filtered-categories]
  {:pre [string? lastmod-date (s/valid? ::spec/categories filtered-categories)]}
  (->> filtered-categories
       (remove #(= :all (first %)))
       (mapv (fn [[cat-k _]]
               {:loc (str "https://nosoupforyou.com/"(name cat-k) "/")
                :lastmod lastmod-date
                :changefreq "monthly"}))
       (sitemap/generate-sitemap)))

(defn generate-sitemap
  [out-file site-categories]
  (->> site-categories
       (categories->sitemap (l/format-local-time (l/local-now) :year-month-day))
       (sitemap/save-sitemap out-file)))

(defn html->disk
  [output-path page-output]
  (when-not (.isDirectory (io/file output-path))
    (io/make-parents output-path))
  (with-open [w (io/writer output-path)]
    (binding [*print-length* false
              *out* w]
      (print page-output))))

(comment

  (-> (Date. (.lastModified (File. "resources/site/html/pizza/index.html")))
       (l/format-local-time :year-month-day))

)
