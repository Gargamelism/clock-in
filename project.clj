(defproject clock-in "0.3.2"
  :description "script to automate time filling in timewatch"
  :url "https://github.com/Gargamelism/clock-in"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [cheshire "5.8.0"]
                 [clj-time "0.14.4"]
                 [clj-http "3.9.0"]
                 [hickory "0.7.1"]
                 [org.clojure/tools.cli "0.3.7"]]
  :main clock-in.core
  :aot [clock-in.core])

