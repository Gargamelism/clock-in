(ns clock-in.time-set
  (:require [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clj-http.client :as client]
            [clock-in.utils :as utils]
            [clojure.string :as str]
            [hickory.core :as html]
            [hickory.select :as select]))

(defn- html->hiccup
  [^String text]
  (-> text
      (html/parse)
      (html/as-hickory)))

(def ^:private user-id-key "ixemplee=")
(defn- user-id
  [response-body]
  (when-let [start-idx (str/index-of response-body user-id-key)]
    (subs response-body (+ start-idx (count user-id-key))
          (str/index-of response-body ";" start-idx))))

(defn- cookies
  [cookies-map]
  (when (and (cookies-map "ie")
             (cookies-map "iee")
             (cookies-map "lang"))
    (str "ie=" (get-in cookies-map ["ie" :value]) "; iee=" (get-in cookies-map ["iee" :value])
         "; lang=" (get-in cookies-map ["lang" :value]))))

(def ^:private login-query-str "comp=%s&name=%s&pw=%s&B1.x=-425&B1.y=-354")
(defn login
  [user-num password company-id]
  (let [response (utils/checked-request! client/post
                                         (get-in @utils/config [:urls :time-watch-login])
                                         (assoc (get-in @utils/config [:headers :login-map])
                                                :body (format login-query-str company-id user-num password)))
        cookies (cookies (response :cookies))
        user-id (user-id (response :body))]
    (if-not (and cookies user-id)
      (utils/exit 1 "ERROR! cookies or user-id could not be retrieved")
      {:cookies cookies
       :user-id user-id
       :company-id company-id})))

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
      (-> (merge (get-in @utils/config [:requests :update-day])
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
  (let [url (format (get-in @utils/config [:urls :dates-table])
                    user-id
                    company-id
                    (time/month start-date)
                    (time/year start-date))
        response (utils/checked-request! client/post url
                                         {:headers (assoc (get-in @utils/config [:headers :post-login])
                                                          "Cookie" cookies
                                                          "Referer" url)})]
    (build-month-plan (:body response))))

(defn update-days
  [{:keys [company-id user-id cookies] :as request}]
  (let [start-date (time/first-day-of-the-month- (requested-month request))
        first-month-day-str (time-format/unparse date-formatter start-date)
        month-mapping (month-mapping request start-date)]
    (doseq [{:keys [date working-hours]} month-mapping]
      (let [edited-day-str (time-format/unparse date-formatter date)
            referer (format (get-in @utils/config [:urls :update-days-referer])
                            company-id
                            user-id
                            edited-day-str
                            first-month-day-str
                            user-id)
            response (client/post (get-in @utils/config [:urls :update-days])
                                  {:headers (assoc (get-in @utils/config [:headers :post-login])
                                                   "Cookie" cookies
                                                   "Referer" referer)
                                   :form-params (update-day-request request working-hours edited-day-str)})]
        (Thread/sleep 1000)
        (println "updating:" edited-day-str)))))