(ns nosoup-clj.util
  (:require [nosoup-clj.spec         :as spec]
            [clj-time.local          :as l]
            [clojure.spec.alpha      :as s]
            [clojure.java.io         :as io]
            [digest                  :refer [sha-256]]
            [sitemap.core            :as sitemap :refer-only [generate-sitemap]]
            [taoensso.timbre         :as timbre :refer [info]])
  (:import  [java.util Date])
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

(defn to-disk
  [out-file output]
  (with-open [w (io/writer out-file)]
    (binding [*print-length* false
              *out* w]
      (print output)
      true)))

(defn output->disk
  "Writes output to disk unless existing matching output already exists."
  [output-path page-output]
  (if-not (.exists (io/file output-path))
    (do
      (io/make-parents output-path)
      (to-disk output-path page-output))
    (when (not= (sha-256 page-output) (sha-256 (slurp output-path)))
      (info "  writing" output-path)
      (to-disk output-path page-output))))

(comment

  (-> (Date. (.lastModified (File. "resources/site/html/pizza/index.html")))
       (l/format-local-time :year-month-day))

)
