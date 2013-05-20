# clj-webjars [![Travis CI status](https://secure.travis-ci.org/jeluard/clj-webjars.png)](http://travis-ci.org/#!/jeluard/clj-webjars/builds)

A [ring](https://github.com/ring-clojure/ring) wrapper to easily serve [WebJars.org](webjars.org) assets.

## Installation

Add ```[clj-webjars "0.9.0"]``` to your leiningen dependencies.

## Usage

Define `wrap-webjars` as part of your handler stack to serve webjars. Then call `refresh-assets!` to trigger discovery/loading of all webjars that will be available through the wrapper.
`refresh-assets!` accepts filters/class-loaders as argument to control which assets will be exposed.

By default all requests whose URI match `assets/js`, `assets/css` or `assets/img` will be served (alternatively provide custom root URIs using `(wrap-webjars ["assets/js"])`). Extra part of the URI will be used to match the asset among your declared webjars.

Assets are served from memory and honor caching semantics (i.e. return status code 304 when needed).

### Example

Following is a complete example showing how to serve [bootstrap](http://twitter.github.io/bootstrap/) webjar.

```clojure
;; define your main ring handler
(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World"})

;; trigger assets refresh
(refresh-assets!)

;; use wrap-webjars
(defn -main[]
  (run-jetty (-> handler (wrap-webjars)) {:port 3000}))
```

Add bootstrap webjar to your leinigen dependencies: `[org.webjars/bootstrap "2.3.1"]`.

Now you can declare bootstrap css in your html file:

```html
<link rel="stylesheet" href="assets/css/bootstrap.min.css">
```

Or just check using curl:

```
curl http://localhost:3000/assets/css/bootstrap.min.css
```

## License

Copyright © 2013 Julien Eluard.

Distributed under the Eclipse Public License, the same as Clojure.
