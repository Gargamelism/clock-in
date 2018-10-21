(ns clock-in.utils-test
  (:require [clojure.test :refer :all]
            [clock-in.utils :as utils]))

(def ^:private scenarios [{:name "remove-extension remove"
                           :fn utils/remove-extension
                           :args ["best_file.ever"]
                           :expected "best_file"}
                          {:name "remove-extension do not remove"
                           :fn utils/remove-extension
                           :args ["best_file_ever"]
                           :expected "best_file_ever"}
                          {:name "status-ok? ok"
                           :fn utils/status-ok?
                           :args [399]
                           :expected true}
                          {:name "status-ok? not ok"
                           :fn utils/status-ok?
                           :args [400]
                           :expected false}
                          {:name "clean-string change"
                           :fn utils/clean-string
                           :args [" 1 23 \n"]
                           :expected "1 23"}
                          {:name "clean-string ignore"
                           :fn utils/clean-string
                           :args [3]
                           :expected 3}
                          {:name "clean-string don't change"
                           :fn utils/clean-string
                           :args ["2 2 2"]
                           :expected "2 2 2"}])

(deftest utils-unit-tests
  (doseq [{:keys [name fn args expected]} scenarios]
    (testing name
      (let [res (apply fn args)]
        (is (= res expected))))))
