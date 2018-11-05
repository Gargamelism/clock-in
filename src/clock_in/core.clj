(ns clock-in.core
  (:require [clock-in.time-set :as time-set]
            [clock-in.utils :as utils]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn- required-args?
  [{:keys [user password company]}]
  (boolean (and user password company)))

(def cli-options
  [["-u" "--user USER" "user number"
    :id :user]
   ["-p" "--password PASSWORD" "password"
    :id :password]
   ["-c" "--company COMPANY" "company number"
    :id :company]
   ["-t" "--start-time TIME" "work from time <TIME>. Time examples '08:15', '21:15'"
    :id :start-time]
   ["-m" "--michael" "work 12:30-21:30"
    :id :michael?]
   ["-n" "--next-month" "fill next month"
    :id :next-month?]
   ["-q" "--previous-month" "fill previous month"
    :id :previous-month?]
   ["-o" "--overwrite-existing" "overwrite dates already filled"
    :id :overwrite?]
   ["-h" "--help"]])

(defn- process-args
  [options]
  (let [{:keys [next-month? previous-month? start-time michael?]} options]
    (-> (dissoc options :next-month? :previous-month?)
        (assoc :month-override (cond next-month? 1
                                     previous-month? -1
                                     :else nil))
        (assoc :start-time (cond
                             start-time start-time
                             michael? "12:30"
                             :else "08:30")))))

(defn- validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) {:exit-message summary :ok? true}
      errors {:exit-message (str/join "\n" (concat ["ERROR!"] errors))}
      (required-args? options) (process-args options)
      :else {:exit-message summary})))

(defn -main
  [& args]
  (let [{:keys [user
                password
                company
                exit-message
                ok?] :as options} (validate-args args)]
    (if exit-message
      (utils/exit (if ok? 0 1) exit-message)
      (time-set/update-days (merge (time-set/login user password company)
                                   options)))
    (println "done!")))
