(ns clj-webjars
  (:require [clojure.java.io :as io]
            [clojure.string :refer [replace-first]]
            [ring.util.request :as request]
            [ring.util.response :as response]
            [ring.middleware.file-info :as file-info])
  (:import [org.webjars WebJarAssetLocator]))

(def ^:dynamic ^WebJarAssetLocator *locator* (WebJarAssetLocator.))
(def assets (atom {}))

;;Inspired by https://gist.github.com/cemerick/3655445

(defn locate-asset [path]
  (.getFullPath *locator* path))

(defn list-assets
  ([] (list-assets "/"))
  ([path] (.listAssets *locator* path)))

(defn- load-resource [resource class-loaders]
  (some #(io/resource resource %) class-loaders))

(defn- last-modified [^java.net.URL url]
  (->> (.getFile url)
       (re-find #"file:(.+)!")
       ^String second
       java.io.File.
       .lastModified
       java.util.Date.))

(defn- date-as-string [^java.util.Date date]
  (.format (file-info/make-http-format) date))

(defn- clone-input-stream [is]
  (let [os (java.io.ByteArrayOutputStream.)]
    (io/copy is os)
    (java.io.ByteArrayInputStream. (.toByteArray os))))

(defn load-assets [class-loaders]
  (into {} (for [asset (list-assets)]
             [asset (let [url (load-resource asset class-loaders)]
                      (with-open [^java.io.InputStream stream (io/input-stream url)]
                        {:stream (clone-input-stream stream) :last-modified (last-modified url)}))])))

(defn- current-context-class-loader []
  (. (. Thread (currentThread)) (getContextClassLoader)))

(defn refresh-assets!
  ([] (refresh-assets! [(current-context-class-loader)]))
  ([class-loaders] (reset! assets (load-assets class-loaders))))

(defn- response-not-modified []
  (-> (response/response "")
      (response/status 304)
      (response/header "Content-Length" 0)))

(defn- response-modified [stream date]
  (-> (response/response stream)
      (response/header "Last-Modified" (date-as-string date))))

(defn- response-multiple-matches [^Exception e]
  (-> (response/response (.getMessage e))
      (response/status 400)))

(defn- remove-leading-slash [^String string]
  (if (.startsWith string "/")
    (subs string 1)
    string))

(defn- remove-trailing-slash [^String string]
  (if (.endsWith string "/")
    (subs string 0 (- (.length string) 1))
    string))

(defn- extract-path [^String uri roots]
  (some #(when (.startsWith ^String (remove-leading-slash uri) %) (remove-trailing-slash (replace-first uri % ""))) (map #(remove-leading-slash %) roots)))

(defn- get-asset [uri roots]
  (if-let [path (extract-path uri roots)]
    (get @assets (locate-asset path))))

(defn asset-response [req roots]
  (try (if-let [asset (get-asset (request/path-info req) roots)]
    (let [last-modified (:last-modified asset)]
      (if (#'file-info/not-modified-since? req last-modified)
        (response-not-modified)
        (response-modified (:stream asset) last-modified))))
      (catch IllegalArgumentException e (response-multiple-matches e))))

(defn wrap-webjars
  ([handler] (wrap-webjars handler ["assets/js/" "assets/css/" "assets/img/"]))
  ([handler roots] (fn [req] (if-let [response (asset-response req roots)]
                               response
                               (handler req)))))
