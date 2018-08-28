(ns clock-in.time-set-test
  (:require [clj-time.core :as time]
            [clojure.test :refer :all]
            [clock-in.time-set :as time-set]))

(def ^:private scenarios [{:name "html->hiccup"
                           :fn #'time-set/html->hiccup
                           :args ["<br>"]
                           :expected {:type :document, :content [{:type :element, :attrs nil, :tag :html, :content [{:type :element, :attrs nil, :tag :head, :content nil} {:type :element, :attrs nil, :tag :body, :content [{:type :element, :attrs nil, :tag :br, :content nil}]}]}]}}
                          {:name "user-id user"
                           :fn #'time-set/user-id
                           :args ["lksjdfajixemplee=batman;fasdlfj"]
                           :expected "batman"}
                          {:name "cookies"
                           :fn #'time-set/cookies
                           :args [{"ie" {:value "eg"} "iee" {:value "egg"} "lang" {:value "heb"}}]
                           :expected "ie=eg; iee=egg; lang=heb"}
                          {:name "cookies false"
                           :fn #'time-set/cookies
                           :args [{}]
                           :expected nil}
                          {:name "requested-month next"
                           :fn #'time-set/requested-month
                           :args [{:month-override 1}]
                           :post-process #(time/month %)
                           :expected (time/month (time/plus (time/now) (time/months 1)))}
                          {:name "requested-month"
                           :fn #'time-set/requested-month
                           :args [{}]
                           :post-process #(time/month %)
                           :expected (time/month (time/now))}
                          {:name "tr->date-map"
                           :fn #'time-set/tr->date-map
                           :args [{:type :element, :attrs {:class "tr", :onmouseover "h(this)", :onmouseout "n(this)", :style "cursor: pointer;", :onclick "javascript:openInnewWindow('editwh2.php?ie=3042&e=211185&d=2018-09-30&jd=2018-09-01&tl=211185',550,650,1,'upd')"}, :tag :tr, :content ["\r\n" {:type :element, :attrs {:nowrap "", :bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial"}, :tag :font, :content ["30-09-2018 א"]}]} "\r\n" {:type :element, :attrs {:nowrap "", :bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial"}, :tag :font, :content ["ערב חג"]}]} "\r\n" {:type :element, :attrs {:nowrap "", :bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial"}, :tag :font, :content ["ערב חג"]}]} "\r\n" {:type :element, :attrs {:nowrap "", :bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial"}, :tag :font, :content ["5:00"]}]} "\r\n" {:type :element, :attrs {:bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial", :color "black"}, :tag :font, :content ["12:30" {:type :element, :attrs {:src "/images/oved4.png", :border "0", :title "79.179.229.74"}, :tag :img, :content nil}]}]} {:type :element, :attrs {:bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial", :color "black"}, :tag :font, :content ["17:30" {:type :element, :attrs {:src "/images/oved4.png", :border "0", :title "79.179.229.74"}, :tag :img, :content nil}]}]} {:type :element, :attrs {:bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial", :color "#000080"}, :tag :font, :content [" "]}]} {:type :element, :attrs {:bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial", :color "#000080"}, :tag :font, :content [" "]}]} {:type :element, :attrs {:bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial", :color "#800080"}, :tag :font, :content [" "]}]} {:type :element, :attrs {:bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial", :color "#800080"}, :tag :font, :content [" "]}]} {:type :element, :attrs {:nowrap "", :bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial"}, :tag :font, :content ["\r\n "]}]} "\r\n" {:type :element, :attrs {:bgcolor "#e6e6e6", :style "width: 110pt"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial"}, :tag :font, :content ["\r\n "]}]} "\r\n" {:type :element, :attrs {:bgcolor "#e6e6e6"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial"}, :tag :font, :content [" 5:00"]}]} "\r\n" {:type :element, :attrs {:bgcolor "#e6e6e6", :align "center"}, :tag :td, :content [{:type :element, :attrs {:size "2", :face "Arial"}, :tag :font, :content ["\r\n" {:type :element, :attrs {:src "/images/pen.png", :border "0"}, :tag :img, :content nil} "\r\n"]}]} "\r\n"]}]
                           :expected {:date (time/date-time 2018 9 30)
                                      :working-hours (time/date-time 1970 01 01 05)}}])

(deftest time-set-unit-tests
  (doseq [{:keys [name fn args post-process expected]} scenarios]
    (testing name
      (let [res (if post-process
                  (post-process (apply fn args))
                  (apply fn args))]
        (is (= res expected))))))