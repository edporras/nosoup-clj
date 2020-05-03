(ns nosoup-clj.init
  (:require [clojure.java.io         :as io]
            [clojure.string          :as str]
            [clojure.tools.cli       :refer [parse-opts]])
  (:gen-class))

(def default-base-output-dir "resources/site/html/")

(def categories-config  (io/file "resources/categories.edn"))

(defn- usage [options-summary]
  (->> ["Nosoup Site Generator"
        ""
        "Usage: nosoup-clj ACTION"
        ""
        "Actions:"
        "  gen PATH-TO-RESTAURANTS-EDN   Generate site pages from the specified restaurant EDN configuration."
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(def cli-options
  [["-o" "--base-output-path OUTPUT-PATH" "Specify the output directory where the files will be written to."
    :validate [#(if (.exists (io/file %))
                  true
                  (io/make-parents (str % "/dummy"))) "Error creating output path."]
    :required true
    :default default-base-output-dir]
   ["-h" "--help"]])

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= (count arguments) 2) (#{"gen"} (first arguments)))
      (let [config (second arguments)]
        (if (.exists (io/file config))
          {:action (first arguments)
           :options (assoc options :config config)}
          {:exit-message (error-msg [(str "Error reading input file '" config "'.")])}))
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))
