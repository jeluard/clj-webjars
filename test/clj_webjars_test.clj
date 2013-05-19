(ns clj-webjars-test
  (:use [clojure.test]
        [clj-webjars]))

(deftest should-all-assets-be-listable
  (is (= 159 (count (all-assets #".*" [(current-context-class-loader)])))))

(deftest should-all-filtered-assets-be-listable
  (is (= 15 (count (all-assets #".*.js" [(current-context-class-loader)])))))

(deftest should-all-assets-be-loadable
  (is (= 159 (count (load-assets #".*" [(current-context-class-loader)])))))

(deftest should-valid-path-be-extractable
  (is (= "/c" (subpath "/a/b/c" ["/a/b"])))
  (is (= "/c" (subpath "/a/b/c/" ["/a/b"])))
  (is (= "/c" (subpath "/a/b/c" ["a/b"])))
  (is (= "/c" (subpath "/a/b/c/" ["a/b"])))
  (is (= "/c" (subpath "/a/b/c" ["a/b/"])))
  (is (= "/c" (subpath "/a/b/c/" ["a/b/"])))
  (is (= "/c" (subpath "/a/b/c" ["/a/b/"])))
  (is (= "/c" (subpath "/a/b/c/" ["/a/b/"])))
  (is (= "/c" (subpath "/a/b/c/" ["/c" "/a/b/"])))
  (is (= "/c" (subpath "a/b/c/" ["/a/b/"])))
  (is (= "/c" (subpath "//a/b/c/" ["/a/b/"])))
  (is (= "/jquery.min.js" (subpath "/assets/js/jquery.min.js" ["assets/js"]))))

(deftest should-invalid-subpath-be-rejected
  (is (nil? (subpath "/a/b/c" [])))
  (is (nil? (subpath "/a/b/c" ["/b" "/b/c"])))
  (is (nil? (subpath "/a/b/c" ["/a/bb"]))))

(deftest should-assets-be-found
  (refresh-assets!)
  (is (= 1 (count (assets-for "rickshaw.min.js"))))
  (is (= 1 (count (assets-for "d3.min.js"))))
  (is (= 2 (count (assets-for "font-awesome.min.css")))))

(deftest should-inexistent-assets-not-be-found
  (refresh-assets!)
  (is (empty? (assets-for "absent.js")))
  (is (empty? (assets-for "zz.min.js"))))

(deftest should-assets-not-be-found-after-empty-refresh
  (refresh-assets! #"no-match" [(current-context-class-loader)])
  (is (empty? (assets-for "d3.min.js"))))

(deftest should-matching-assets-be-identifiable
  (is (matching-assets "/assets/js/jquery.min.js" ["assets/js"]))
  (is (not (matching-assets "/assets/js/jquery.min.js" []))))
