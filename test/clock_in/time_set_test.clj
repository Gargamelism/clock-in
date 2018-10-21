(ns clock-in.time-set-test
  (:require [clj-time.core :as time]
            [clj-time.coerce :as time-convert]
            [clojure.test :refer :all]
            [clock-in.time-set :as time-set]
            [clock-in.utils :as utils]
            [hickory.core :as html]))

(def ^:private string-html-row  "<tr class=\"tr\" onmouseover=\"h(this)\" onmouseout=\"n(this)\" style=\"cursor: pointer;\"  onClick=\"javascript:openInnewWindow('editwh2.php?ie=3042&e=211185&d=2018-10-02&jd=2018-10-01&tl=211185',550,650,1,'upd')\">
  <td nowrap bgcolor=#e6e6e6><font size=2 face=Arial>02-10-2018 ג</font></td>
  <td nowrap bgcolor=#e6e6e6><font size=2 face=Arial>יום ג</font></td>
  <td nowrap bgcolor=#e6e6e6><font size=2 face=Arial>עובדים גלובליים</font></td>
  <td nowrap bgcolor=#e6e6e6><font size=2 face=Arial>9:00</font></td>
  <td bgcolor=#e6e6e6><font size=2 face=Arial color=black>08:30<img src=/images/oved4.png border=0 title=\"109.64.233.99\"></font></td><td bgcolor=#e6e6e6><font size=2 face=Arial color=black>13:00<img src=/images/oved4.png border=0 title=\"109.64.233.99\"></font></td><td bgcolor=#e6e6e6><font size=2 face=Arial color=#000080>&nbsp;</font></td><td bgcolor=#e6e6e6><font size=2 face=Arial color=#000080>&nbsp;</font></td><td bgcolor=#e6e6e6><font size=2 face=Arial color=#800080>&nbsp;</font></td><td bgcolor=#e6e6e6><font size=2 face=Arial color=#800080>&nbsp;</font></td><td nowrap bgcolor=#e6e6e6><font size=2 face=Arial>
  חצי יום חופש 17:30-13:00 <img src=\"/images/oved4.png\" border=0 title=\"109.64.233.99\"></font></td>
  <td bgcolor=#e6e6e6 style=\"width: 110pt\"><font size=2 face=Arial>
  &nbsp;</font></td>
  <td bgcolor=#e6e6e6><font size=2 face=Arial>&nbsp;4:30</font></td>
  <td bgcolor=#e6e6e6 align=center><font size=2 face=Arial>
  <img src='/images/pen.png' border=0>
  </td>
  </tr>")

(def ^:private scenarios [{:name "user-id user"
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
                          {:name "row->row-map"
                           :fn #'time-set/row->row-map
                           :args [(utils/html->hiccup string-html-row)]
                           :expected {:date (time-convert/from-string "2018-10-02T00:00:00.000Z"), :day-type "", :clock-in-2 "", :edit "", :absence-reason "17:30-13:00", :actual-hours "4:30", :clock-in-1 "08:30", :clock-in-3 "", :day-name "", :notes "", :required-hours (time-convert/from-string "1970-01-01T09:00:00.000Z"), :clock-out-3 "", :clock-out-2 "", :clock-out-1 "13:00"}}
                          {:name "update-date? required-hours"
                           :fn #'time-set/update-date?
                           :args [{:required-hours true} false]
                           :expected true}
                          {:name "update-date? overwrite"
                           :fn #'time-set/update-date?
                           :args [{:clock-in-1 true
                                   :required-hours true} true]
                           :expected true}
                          {:name "update-date? no overwrite"
                           :fn #'time-set/update-date?
                           :args [{:clock-in-1 true} false]
                           :expected nil}])

(deftest time-set-unit-tests
  (doseq [{:keys [name fn args post-process expected]} scenarios]
    (testing name
      (let [res (if post-process
                  (post-process (apply fn args))
                  (apply fn args))]
        (is (= res expected))))))