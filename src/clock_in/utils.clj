(ns clock-in.utils
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hickory.core :as html]))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn remove-extension
  [^String file-name]
  (if-let [dot-idx (str/index-of file-name ".")]
    (subs file-name 0 dot-idx)
    file-name))

(defn status-ok?
  [status]
  (boolean (< status 400)))

(defn checked-request!
  [fnc url headers]
  (let [{:keys [status] :as response} (fnc url headers)]
    (if (status-ok? status)
      response
      (exit status (format "ERROR! request to <%s> failed with error code <%d>" url status)))))

(def config (atom {}))
(def load-config!
  (doseq [resource-name ["urls.edn" "headers.edn" "requests.edn"]]
    (try
      (with-open [resource (io/reader (io/resource resource-name))]
        (swap! config assoc
               (keyword (remove-extension resource-name))
               (edn/read (java.io.PushbackReader. resource))))
      (catch java.io.IOException e
        (exit 1 (format "Couldn't open '%s': %s\n" resource-name (.getMessage e))))
      (catch RuntimeException e
        (exit 1 (format "Error parsing edn file '%s': %s\n" resource-name (.getMessage e)))))))

(defn clean-string
  [^String string]
  (if (string? string)
    (-> string
        (str/replace #"[^\x00-\x7F]" "")
        (str/replace #"[\p{Cntrl}&&[^\r\n\t]]" "")
        (str/replace #"\p{C}" "")
        (str/trim))
    string))

(defn html->hiccup
  [^String text]
  (-> text
      (html/parse)
      (html/as-hickory)))