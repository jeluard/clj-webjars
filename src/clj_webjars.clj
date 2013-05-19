(ns clj-webjars
  (:require [clojure.java.io :as io]
            [clojure.string :refer [replace-first]]
            [ring.util.mime-type :refer [ext-mime-type]]
            [ring.util.response :as response]
            [ring.middleware.file-info :as file-info])
  (:import [org.webjars WebJarAssetLocator]))

(def loaded-assets (atom {}))

(defn- asset-locator [pattern class-loaders]
  (WebJarAssetLocator. (WebJarAssetLocator/getFullPathIndex pattern (into-array ClassLoader class-loaders))))

(defn all-assets [pattern class-loaders]
  "List all assets available."
  (set (.values ^java.util.Map (.getFullPathIndex ^WebJarAssetLocator (asset-locator pattern class-loaders)))))

(defn- load-resource [resource class-loaders]
  (some #(io/resource resource %) class-loaders))

;;Inspired by https://gist.github.com/cemerick/3655445

(defn- last-modified [^java.net.URL url]
  (->> (.getFile url)
       (re-find #"file:(.+)!")
       ^String second
       java.io.File.
       .lastModified
       java.util.Date.))

(defn- date-as-string [^java.util.Date date]
  (.format (file-info/make-http-format) date))

(defn- input-stream-to-array [is]
  (let [os (java.io.ByteArrayOutputStream.)]
    (io/copy is os)
    (.toByteArray os)))

(defn load-assets [pattern class-loaders]
  "Create a map of all assets mapped to their :stream content and :last-modified date."
  (into {} (for [asset (all-assets pattern class-loaders)]
             [(replace-first asset "META-INF/resources/webjars/" "")
              (let [url (load-resource asset class-loaders)]
                (with-open [^java.io.InputStream stream (io/input-stream url)]
                  {:content (input-stream-to-array stream) :last-modified (last-modified url)}))])))

(defn current-context-class-loader []
  "Current context ClassLoader."
  (. (. Thread (currentThread)) (getContextClassLoader)))

(defn refresh-assets!
  "Reset loaded assets to the result of loaded-assets call."
  ([] (refresh-assets! #".*" [(current-context-class-loader)]))
  ([pattern class-loaders] (reset! loaded-assets (load-assets pattern class-loaders))))

(defn- response-not-modified []
  (-> (response/response "")
      (response/status 304)
      (response/header "Content-Length" 0)))

(defn- response-modified [uri stream date]
  (-> (response/response stream)
      (response/header "Last-Modified" (date-as-string date))
      (response/content-type (or (ext-mime-type uri) "application/octet-stream"))))

(defn- response-multiple-matches [uri assets]
  (-> (response/response (format "Found several matching assets for %s: %s " uri assets))
      (response/status 400)))

(defn- normalize-leading-slash [^String string]
  (str "/" (second (re-find #"[/]*(.*)" string))))

(defn- normalize-trailing-slash [^String string]
  (clojure.string/replace string #"[/]*$" ""))

(def normalize-slashes (comp normalize-leading-slash normalize-trailing-slash))

(defn subpath [uri roots]
  "Return trailing part of uri compared to first matching root; nil if none matches.
  e.g. (subpath '/a/b' ['/a']) => /b
       (subpath '/a/b' ['/c']) => nil"
  (let [normalized-uri (normalize-slashes uri)
        normalized-roots (map #(normalize-slashes %) roots)]
    (some #(when (.startsWith ^String normalized-uri %) (replace-first normalized-uri % "")) normalized-roots)))

(defn assets-for [^String path]
  "All assets whose name ends with `path`."
  (filter #(.endsWith ^String (key %) path) @loaded-assets))

(defn matching-assets [uri roots]
  "All assets whose `uri` matches one of `roots`."
  (if-let [path (subpath uri roots)]
    (assets-for path)))

(defn- asset-response [req uri asset]
  (let [last-modified (:last-modified asset)]
    (if (#'file-info/not-modified-since? req last-modified)
      (response-not-modified)
      (response-modified uri (io/input-stream (:content asset)) last-modified))))

(defn wrap-webjars
  "Ring wrapper serving webjars assets. Intercepts uri matching [assets/js, assets/css, assets/img] by default."
  ([handler] (wrap-webjars handler ["assets/js" "assets/css" "assets/img"]))
  ([handler roots] (fn [req] (let [uri (:uri req)
                                   assets (matching-assets uri roots)]
                               (case (count assets)
                                 0 (handler req)
                                 1 (asset-response req uri (val (first assets)))
                                 (response-multiple-matches uri  (keys assets))))))) ;; provided path matched multiple webjars assets, assuming this is a user error
