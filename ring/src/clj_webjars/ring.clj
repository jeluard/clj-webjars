(ns clj-webjars.ring
  (use: [[clj-webjars.core :as wj]
         [ring.middleware.resource :as r]]))

(def ^:dynamic *asset-prefix* "")
(def resource-folder "/META-INF/resources")

(defn wrap-assets
  [handler]
  (fn [request]
    (if (.startsWith (:uri request) *asset-prefix*)
      (r/wrap-resource handler resource-folder)
      (handler request))))
