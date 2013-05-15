(ns clj-webjars
  (:require [ring.middleware.file-info :refer [make-http-format]]
            [clojure.java.io :as io])
  (:import [org.webjars WebJarAssetLocator]))

(set! *warn-on-reflection* true)

(def ^:dynamic ^WebJarAssetLocator *locator* (WebJarAssetLocator.))

(defn locate-asset [name]
  (.getFullPath *locator* name))

(defn list-assets
  ([] (list-assets "/"))
  ([path] (.listAssets *locator* path)))

(defn index []
  (.getFullPathIndex *locator*))

(defn- current-context-class-loader []
  (. (. Thread (currentThread)) (getContextClassLoader)))

(defn- load-resource
  ([resource] (load-resource resource (current-context-class-loader)))
  ([resource ^ClassLoader class-loader] (io/resource resource class-loader)))

(defn load-asset [name]
  (load-resource (locate-asset name)))

(defn- last-modified [^java.net.URL url]
  (->> (.getFile url)
       (re-find #"file:(.+)!")
       second
       java.io.File.
       .lastModified
       java.util.Date.))

(defn- date-as-string [^java.util.Date date]
  (.format (make-http-format) date))

(defn load-assets []
  (into {} (for [asset (list-assets)]
             [asset (let [url (load-resource asset)]
                      (with-open [^java.io.InputStream stream (io/input-stream url)]
                        {:content stream :last-modified (last-modified url)}))])))
