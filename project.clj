(defproject clj-webjars "0.9.1-SNAPSHOT"
  :description "A clojure helper library for webjars"
  :url "http://github.com/jeluard/clj-webjars"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.webjars/webjars-locator "0.17"]
                 [ring/ring-core "1.3.0"]
                 [org.webjars/rickshaw "1.1.2-1" :scope "test"]
                 [org.webjars/font-awesome "3.0.2" :scope "test"]])
