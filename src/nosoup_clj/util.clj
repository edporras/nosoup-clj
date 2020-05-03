(ns nosoup-clj.util
  (:require [clj-time.local          :as l]
            [clojure.edn             :as edn]
            [clojure.java.io         :as io]
            [digest                  :refer [sha-256]]
            [sitemap.core            :as sitemap :refer-only [generate-sitemap]]
            [taoensso.timbre         :as timbre :refer [info]])
  (:import  [java.util Date])
  (:gen-class))

(defn read-config
  "Reads the contents of the EDN configuration FILE."
  [file]
  (with-open [r (io/reader file)]
    (edn/read (java.io.PushbackReader. r))))

(defn file-mdate
  "Get the modified date from a file."
  [file]
  (-> (Date. (.lastModified (io/file file)))
      (l/format-local-time :year-month-day)))

(defn categories->sitemap
  "Generate the site's sitemap from the given final category list. Reads
  the modification time stamps from the output files found under the
  `base-output-path`."
  [base-output-path filtered-categories]
  {:pre [string? base-output-path]}
  (->> filtered-categories
       (remove #(= :all (first %)))
       (mapv (fn [[cat-k _]]
               (let [cat-str (str (name cat-k) "/")]
                 {:loc (str "https://nosoupforyou.com/" cat-str)
                  :lastmod (file-mdate (str base-output-path cat-str "index.html"))
                  :changefreq "monthly"})))
       (sitemap/generate-sitemap)))

(defn generate-sitemap
  [base-output-path site-categories]
  (->> site-categories
       (categories->sitemap base-output-path)
       (sitemap/save-sitemap (io/file (str base-output-path "sitemap.xml")))))

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
  (l/format-local-time (l/local-now) :year-month-day)
)
