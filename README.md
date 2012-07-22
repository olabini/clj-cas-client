# clj-cas-client

A simple CAS client for Clojure, for use as a middleware with Ring.

## Usage

To install, add this to your project.clj:

```clojure
  :dependencies [[clj-cas-client "0.0.5"]]
```

To wrap a handler with cas:

```clojure
(use 'clj-cas-client.core)

(defn cas-server []
  "https://example.org/cas")

(defn service-name []
  "http://my-current-server.example.org")

;; ... routes ...

(def app (-> routes
             handler/site
             (cas cas-server service-name)))
```

This will redirect all requests to the cas server for login, validate the tickets from the cas server, and make sure to add a :username key to the request map.

## License

Copyright (C) 2012 Ola Bini

Distributed under the Eclipse Public License, the same as Clojure.
