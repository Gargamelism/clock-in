(ns clock-in.core
  (:require [cheshire.core :as json]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def ^:private time-watch-login-url "https://checkin.timewatch.co.il/punch/punch2.php")

(def ^:private login-map
  {:basic-auth ["user" "password" "company"]
   :headers {"Connection" "keep-alive"
             "Pragma" "no-cache"
             "Cache-Control" "no-cache"
             "Origin" "https://checkin.timewatch.co.il"
             "Upgrade-Insecure-Requests" " 1"
             "Content-Type" "application/x-www-form-urlencoded"
             "User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36"
             "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
             "Referer" "https://checkin.timewatch.co.il/punch/punch.php"
             "Accept-Encoding" "gzip, deflate, br"
             "Accept-Language" "en-US,en;q=0.9,he;q=0.8"}})

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
  (when-let [response (client/post time-watch-login-url (assoc login-map
                                                               :body (format login-query-str company-id user-num password)))]
    {:cookies (cookies (response :cookies))
     :user-id (user-id (response :body))
     :company-id company-id}))

(def ^:private post-login-headers
  {"Host" "checkin.timewatch.co.il"
   "Connection" "keep-alive"
   "Pragma" "no-cache"
   "Cache-Control" "no-cache"
   "Upgrade-Insecure-Requests" "1"
   "User-Agent" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36"
   "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
   "Referer" "http://checkin.timewatch.co.il/punch/punch2.php"
   "Accept-Encoding" "gzip, deflate"
   "Accept-Language" "en-US,en;q=0.9,he;q=0.8"
   "Cookie" ""})

(def ^:private update-days-referer-url "http://checkin.timewatch.co.il/punch/editwh2.php?ie=%s&e=%s&d=%s&jd=%s&tl=%s")
(def ^:private update-days-url "http://checkin.timewatch.co.il/punch/editwh3.php")

(def ^:private date-formatter (time-format/formatters :year-month-day))

(defn- update-day-request
  [{:keys [user-id company-id michael?]} first-month-day-str edited-day-str]
  (let [hours (if michael?
                {:emm0 "30"
                 :ehh0 "12"
                 :xmm0 "30"
                 :xhh0 "21"}
                {:emm0 "30"
                 :ehh0 "08"
                 :xmm0 "30"
                 :xhh0 "17"})]
    (merge hours
           {:e user-id
            :tl user-id
            :c company-id
            :d edited-day-str
            :jd first-month-day-str
            :atypehidden "0"
            :inclcontracts "1"
            :job "14095"
            :allowabsence "3"
            :allowremarks "0"
            :task0 "0"
            :what0 "1"
            :task1 "0"
            :what1 "1"
            :task2 "0"
            :what2 "1"
            :task3 "0"
            :what3 "1"
            :task4 "0"
            :what4 "1"
            :excuse "0"
            :atype "0"
            :B1.x "43"
            :B1.y "13"})))

(let [friday 5
      saturday 6]
  (defn- weekend?
    [date]
    (#{friday saturday} (time/day-of-week date))))

(defn- update-days
  [{:keys [company-id user-id cookies] :as request}]
  (let [start-date (time/first-day-of-the-month- (time/now))
        end-date (time/last-day-of-the-month start-date)
        first-month-day-str (time-format/unparse date-formatter start-date)]
    (loop [edited-date start-date]
      (if (weekend? edited-date)
        (recur (time/plus edited-date (time/days 1)))
        (if-not (time/after? edited-date end-date)
          (let [edited-day-str (time-format/unparse date-formatter edited-date)
                referer (format update-days-referer-url company-id user-id edited-day-str first-month-day-str user-id)]
            (when-let [response (client/post update-days-url 
                                             {:headers (assoc post-login-headers
                                                              "Cookie" cookies
                                                              "Referer" referer)
                                              :form-params (update-day-request request first-month-day-str edited-day-str)})]
              (Thread/sleep 1000)
              (println "updating:" edited-day-str)
              (recur (time/plus edited-date (time/days 1))))))))))

(defn- michael?
  [flags]
  (some #(= "--michael" %) flags))

(def cli-options
  [["-m" "--michael"
    :id :michael?]
   ["-u" "--user USER"
    :id :user]
   ["-p" "--password PASSWORD"
    :id :password]
   ["-c" "--company COMPANY"
    :id :company]
   ["-h" "--help"]])

(defn -main
  [& args]
  (if (= (count args) 0) 
    (println "usage: java -jar clock-in.jar --michael -u <USER-NUM> -p <PASSWORD> -c <COMPANY-ID>")
    (let [{:keys [michael?
                  user
                  password
                  company]} (:options (parse-opts args cli-options))]
      (update-days (assoc (login user password company) :michael? michael?))
      (println "done!"))))
