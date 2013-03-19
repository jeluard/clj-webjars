(ns clj-webjars.core
  (:import [org.webjars WebJarAssetLocator]))

(set! *warn-on-reflection* true)

(def ^:dynamic ^WebJarAssetLocator *locator* (WebJarAssetLocator.))

(defn locate-asset
  [name]
  (.getFullPath *locator* name))

(defn list-assets
  ([] (list-assets "/"))
  ([path] (.listAssets *locator* path)))

(defn index
  []
  (.getFullPathIndex *locator*))

(defn- current-context-class-loader
  []
  (. (. Thread (currentThread)) (getContextClassLoader)))

(defn- load-resource
  ([resource] (load-resource (current-context-class-loader) resource))
  ([^ClassLoader class-loader resource] (.getResourceAsStream class-loader resource)))

(defn load-asset
  [name]
  (load-resource (locate-asset name)))
