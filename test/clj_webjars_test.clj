(ns clj-webjars-test
  (:use [clojure.test]
        [clj-webjars]))

(deftest locate-existing-asset
  (is (locate-asset "rickshaw.min.js"))
  (is (locate-asset "d3.min.js")))

(deftest not-locate-not-existing-asset
  (is (not (locate-asset "absent.js")))
  (is (not (locate-asset "zz.min.js"))))

(deftest locate-multiple.asset
  (is (thrown? IllegalArgumentException (locate-asset "font-awesome.min.css"))))

(deftest list-existing-assets
  (is (= 159 (count (list-assets)))))

(deftest list-existing-assets-filtered
  (is (empty? (list-assets "/not-existing"))))
