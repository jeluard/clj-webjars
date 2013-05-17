(defproject clj-webjars "0.9.0-SNAPSHOT"
  :description "A clojure helper library for webjars"
  :url "http://github.com/jeluard/clj-webjars"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.webjars/webjars-locator "0.4"]
                 [ring/ring-core "1.2.0-beta2"]
                 [org.webjars/rickshaw "1.1.2-1" :scope "test"]])
