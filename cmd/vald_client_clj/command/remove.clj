(ns vald-client-clj.command.remove
  (:require
   [clojure.tools.cli :as cli]
   [vald-client-clj.core :as vald]))

(def cli-options
  [["-h" "--help" :id :help?]])

(defn run [client args]
  (let [parsed-result (cli/parse-opts args cli-options)
        {:keys [options summary arguments]} parsed-result
        {:keys [help?]} options
        id (first arguments)]
    (if (or help? (nil? id))
      (println summary)
      (do
        (vald/remove-id client id)
        (println "removed.")))))
