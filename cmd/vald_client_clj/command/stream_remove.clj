(ns vald-client-clj.command.stream-remove
  (:require
   [clojure.tools.cli :as cli]
   [clojure.string :as string]
   [clojure.edn :as edn]
   [vald-client-clj.core :as vald]
   [vald-client-clj.util :as util]))

(def cli-options
  [["-h" "--help" :id :help?]
   ["-j" "--json" "read as json"
    :id :json?]
   [nil "--skip-strict-exist-check" "skip strict exist check"
    :id :skip-strict-exist-check?]
   [nil "--elapsed-time" "show elapsed time the request took"
    :id :elapsed-time?]
   ["-t" "--threads THREADS" "Number of threads"
    :id :threads
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [pos? "Must be positive number"]]])

(defn usage [summary]
  (->> ["Usage: valdcli [OPTIONS] stream-remove [SUBOPTIONS] IDs"
        ""
        "Remove multiple IDs."
        ""
        "Sub Options:"
        summary
        ""]
       (string/join "\n")))

(defn run [client args]
  (let [parsed-result (cli/parse-opts args cli-options)
        {:keys [options summary arguments]} parsed-result
        {:keys [help? json? skip-strict-exist-check? elapsed-time? threads]} options
        read-string (if json?
                      util/read-json
                      edn/read-string)
        writer (if json?
                 (comp println util/->json)
                 (comp println util/->edn))
        config {:skip-strict-exist-check skip-strict-exist-check?}]
    (if help?
      (-> summary
          (usage)
          (println))
      (let [ids (-> (or (first arguments)
                        (util/read-from-stdin))
                    (read-string))
            idss (partition-all (/ (count ids) threads) ids)
            f (fn [ids]
                (-> client
                    (vald/stream-remove writer config ids)
                    (deref)))
            res (->> (if elapsed-time?
                       (time (doall (pmap f idss)))
                       (doall (pmap f idss)))
                     (apply merge-with (fn [x y]
                                         (if (and (number? x) (number? y))
                                           (+ x y)
                                           x))) )]
        (if (:error res)
          (throw (:error res))
          (->> res
               (:count)
               (str "removed: ")
               (println)))))))
