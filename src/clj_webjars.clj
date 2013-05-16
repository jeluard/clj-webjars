(ns clj-webjars
  (:require [clojure.java.io :as io]
            [clojure.string :refer [replace-first]]
            [ring.util.request :as request]
            [ring.util.response :as response]
            [ring.middleware.file-info :as file-info])
  (:import [org.webjars WebJarAssetLocator]))

(def ^:dynamic ^WebJarAssetLocator *locator* (WebJarAssetLocator.))

;;Inspired by https://gist.github.com/cemerick/3655445

(defn locate-asset [path]
  (.getFullPath *locator* path))

(defn list-assets
  ([] (list-assets "/"))
  ([path] (.listAssets *locator* path)))

(defn- current-context-class-loader []
  (. (. Thread (currentThread)) (getContextClassLoader)))

(defn- load-resource
  ([resource] (load-resource resource (current-context-class-loader)))
  ([resource ^ClassLoader class-loader] (io/resource resource class-loader)))

(defn- last-modified [^java.net.URL url]
  (->> (.getFile url)
       (re-find #"file:(.+)!")
       second
       java.io.File.
       .lastModified
       java.util.Date.))

(defn- date-as-string [^java.util.Date date]
  (.format (file-info/make-http-format) date))

(defn- clone-input-stream [is]
  (let [os (java.io.ByteArrayOutputStream.)]
    (io/copy is os)
    (java.io.ByteArrayInputStream. (.toByteArray os))))

(defn load-assets []
  (into {} (for [asset (list-assets)]
             [asset (let [url (load-resource asset)]
                      (with-open [^java.io.InputStream stream (io/input-stream url)]
                        {:stream (clone-input-stream stream) :last-modified (last-modified url)}))])))

(def assets (atom {}))

(defn refresh-assets! []
  (reset! assets (load-assets)))

(refresh-assets!)

(defn- response-not-modified []
  (-> (response/response "")
      (response/status 304)
      (response/header "Content-Length" 0)))

(defn- response-modified [stream date]
  (-> (response/response stream)
      (response/header "Last-Modified" (date-as-string date))))

(defn response-multiple-matches [e]
  (-> (response/response (.getMessage e))
      (response/status 400)))

(defn remove-leading-slash [^String string]
  (if (.startsWith string "/")
    (subs string 1)
    string))

(defn remove-trailing-slash [^String string]
  (if (.endsWith string "/")
    (subs string 0 (- (.length string) 1))
    string))

(defn extract-path [^String uri roots]
  (some #(when (.startsWith (remove-leading-slash uri) %) (remove-trailing-slash (replace-first uri % ""))) roots))

(defn get-asset [uri roots]
  (if-let [path (extract-path uri (map #(remove-leading-slash %) roots))]
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
  ([handler roots] (fn [req] (let [response (asset-response req roots)] (if (nil? response) (handler req) response)))))
