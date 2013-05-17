(ns clj-webjars-test
  (:use [clojure.test]
        [clj-webjars]))

(deftest locate-existing-asset
  (is (locate-asset "rickshaw.min.js"))
  (is (locate-asset "d3.min.js")))

(deftest not-locate-not-existing-asset
  (is (not (locate-asset "absent.js")))
  (is (not (locate-asset "zz.min.js"))))

(deftest list-existing-assets
  (is (= 7 (count (list-assets)))))

(deftest list-existing-assets-filtered
  (is (empty? (list-assets "/not-existing"))))
