(ns clock-in.core
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clj-http.client :as client]
            [clojure.core.strint :refer [<<]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [hickory.core :as html]
            [hickory.select :as select])
  (:gen-class))

(defn- remove-extension
  [^String file-name]
  (subs file-name 0 (str/index-of file-name ".")))

(defn- exit
  [status msg]
  (println msg)
  (System/exit status))

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

(defn- html->hiccup
  [^String text]
  (-> text
      (html/parse)
      (html/as-hickory)))

(defn- status-ok?
  [status]
  (when (< status 400)
    true))

(defn- checked-request!
  [fnc url headers]
  (let [{:keys [status] :as response} (fnc url headers)]
    (if (status-ok? status)
      response
      (exit (<< "ERROR! request to <~{url}> failed with error code <~{status}>") status))))

(def ^:private user-id-key "ixemplee=")
(defn- user-id
  [response-body]
  (when-let [start-idx (str/index-of response-body user-id-key)]
    (subs response-body (+ start-idx (count user-id-key))
          (str/index-of response-body ";" start-idx))))

(defn- cookies
  [cookies-map]
  (str "ie=" (get-in cookies-map ["ie" :value]) "; iee=" (get-in cookies-map ["iee" :value])
       "; lang=" (get-in cookies-map ["lang" :value])))

(def ^:private login-query-str "comp=%s&name=%s&pw=%s&B1.x=-425&B1.y=-354")
(defn- login
  [user-num password company-id]
  (when-let [response (checked-request! client/post
                                        (get-in @config [:urls :time-watch-login])
                                        (assoc (get-in @config [:headers :login-map])
                                               :body (format login-query-str company-id user-num password)))]
    {:cookies (cookies (response :cookies))
     :user-id (user-id (response :body))
     :company-id company-id}))

(def ^:private date-formatter (time-format/formatters :year-month-day))

(defn- build-hours
  [starting-hour ending-hour]
  {:ehh0 (format "%02d" (time/hour starting-hour))
   :emm0 (format "%02d" (time/minute starting-hour))
   :xhh0 (format "%02d" (time/hour ending-hour))
   :xmm0 (format "%02d" (time/minute ending-hour))})

(let [hour-format (time-format/formatter "HH:mm")
      regular-start-hour "08:30"
      michael-start-hour "12:30"]
  (defn- update-day-request
    [{:keys [user-id company-id michael?]} working-hours edited-day-str]
    (let [starting-hour (time-format/parse hour-format (if michael? michael-start-hour regular-start-hour))
          ending-hour (time/plus starting-hour (time/hours (time/hour working-hours)) (time/minutes (time/minute working-hours)))
          hours (build-hours starting-hour ending-hour)]
      (-> (merge (get-in @config [:requests :update-day])
                 hours)
          (assoc :d edited-day-str
                 :jd edited-day-str
                 :e user-id
                 :tl user-id
                 :c company-id)))))

(defn- requested-month
  [{:keys [month-override]}]
  (let [now (time/now)]
    (if-not month-override
      now
      (time/plus now (time/months month-override)))))

(let [date-format-str "dd-MM-yyyy"
      cell-date-format (time-format/formatter date-format-str)
      date-format-str-len (count date-format-str)]
  (defn- date-str->date
    [^String date]
    (time-format/parse cell-date-format (subs date 0 date-format-str-len))))

(defn- parse-working-hours
  [^String hours]
  (when hours
    (time-format/parse (time-format/formatter "hh:mm") hours)))

(defn- tr->date-map
  [row]
  (let [cells-vals (->> (:content row)
                        (map :content)
                        (flatten)
                        (map #(-> %
                                  (:content)
                                  (first))))]
    {:date (date-str->date (nth cells-vals 1))
     :working-hours (parse-working-hours (nth cells-vals 7))}))

(defn- build-month-plan
  [^String html]
  (let [rows (select/select (select/child (select/class "tr")) (html->hiccup html))]
    (->> rows
         (map tr->date-map)
         (filter :working-hours))))

(defn- month-mapping
  [{:keys [user-id company-id cookies]} start-date]
  (let [url (format (get-in @config [:urls :dates-table])
                    user-id
                    company-id
                    (time/month start-date)
                    (time/year start-date))
        response (checked-request! client/post url
                                   {:headers (assoc (get-in @config [:headers :post-login])
                                                    "Cookie" cookies
                                                    "Referer" url)})]
    (build-month-plan (:body response))))

(defn- update-days
  [{:keys [company-id user-id cookies] :as request}]
  (let [start-date (time/first-day-of-the-month- (requested-month request))
        first-month-day-str (time-format/unparse date-formatter start-date)
        month-mapping (month-mapping request start-date)]
    (doseq [{:keys [date working-hours]} month-mapping]
      (let [edited-day-str (time-format/unparse date-formatter date)
            referer (format (get-in @config [:urls :update-days-referer])
                            company-id
                            user-id
                            edited-day-str
                            first-month-day-str
                            user-id)
            response (client/post (get-in @config [:urls :update-days])
                                  {:headers (assoc (get-in @config [:headers :post-login])
                                                   "Cookie" cookies
                                                   "Referer" referer)
                                   :form-params (update-day-request request working-hours edited-day-str)})]
        (Thread/sleep 1000)
        (println "updating:" edited-day-str)))))

(defn- required-args?
  [{:keys [user password company]}]
  (and user password company))

(def cli-options
  [["-u" "--user USER" "user number"
    :id :user]
   ["-p" "--password PASSWORD" "password"
    :id :password]
   ["-c" "--company COMPANY" "company number"
    :id :company]
   ["-m" "--michael" "work 12:30-21:30"
    :id :michael?]
   ["-n" "--next-month" "fill next month"
    :id :next-month?]
   ["-q" "--previous-month" "fill previous month"
    :id :previous-month?]
   ["-h" "--help"]])

(defn- process-args
  [options]
  (let [{:keys [next-month? previous-month?]} options]
    (-> (dissoc options :next-month? :previous-month?)
        (assoc :month-override (cond
                                 next-month? 1
                                 previous-month? -1
                                 :else nil)))))

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
      (exit (if ok? 0 1) exit-message)
      (update-days (merge (login user password company)
                          options)))
    (println "done!")))
