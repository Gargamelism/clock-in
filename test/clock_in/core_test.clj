(ns clock-in.core-test
  (:require [clojure.test :refer :all]
            [clock-in.core :as clock-in]))

(def ^:private scenarios [{:name "required args true"
                           :fn  #'clock-in/required-args?
                           :args [{:user "jorge" :password "secret" :company "the world"}]
                           :expected true}
                          {:name "required args false"
                           :fn  #'clock-in/required-args?
                           :args [{:user "jorge" :password nil :company "the world"}]
                           :expected false}
                          {:name "process-args next month"
                           :fn  #'clock-in/process-args
                           :args [{:next-month? true}]
                           :expected {:month-override 1}}
                          {:name "process-args previous month"
                           :fn  #'clock-in/process-args
                           :args [{:previous-month? true}]
                           :expected {:month-override -1}}])

(deftest core-unit-tests
  (doseq [{:keys [name fn args expected]} scenarios]
    (testing name
      (let [res (apply fn args)]
        (is (= res expected))))))
