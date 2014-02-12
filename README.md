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
(require '[ring.middleware.session :refer [wrap-session]])

(defn cas-server []
  "https://example.org/cas")

(defn service-name []
  "http://my-current-server.example.org")

;; ... routes ...

(def app (-> routes
             handler/site
             (cas cas-server service-name)
             wrap-session))
```

Note that clj-cas-client depends on the session middleware, so the handler returned by `cas` must be wrapped with `wrap-session`.

This will redirect all requests to the cas server for login, validate the tickets from the cas server, and make sure to add a :username key to the request map.

## AJAX Requests

By default `clj-cas-client` redirects unauthenticated users to the CAS server for authentication. This works fine with regular page loads, but causes problems with AJAX requests because `XMLHttpRequest` automatically follows all redirects, thus trying to retrieve the CAS server's login page as the response to the request (and typically failing due to the same-origin policy).

To solve this problem, you can configure `clj-cas-client` to return `403 Forbidden` instead of `302 Found` in response to requests that match a given predicate. Just pass the predicate to the `cas` function as the `:no-redirect?` option. Then, in your JavaScript, check the response code of each AJAX response and reload the page on a `403`.

If there's no other way to detect AJAX requests (e.g. by their URL), you can add a custom HTTP header to them and check for it in your predicate:

```javascript
$.ajax({
  url: "/my/api",
  headers: {"my-ajax-request-header": true},
  statusCode: {
    403: function(){
      location.reload(true);
    }
  },
  success: function(data){
    // do something with data
  }
})
```

```clojure
(defn ajax-request? [request]
  (get-in request [:headers "my-ajax-request-header"]))

(def app (-> routes
             handler/site
             (cas cas-server service-name :no-redirect? ajax-request?)
             wrap-session))
```

## Single Sign Out

For single sign out, use the [cas-single-sign-out](https://github.com/solita/cas-single-sign-out) middleware.

## License

Copyright (C) 2012 Ola Bini

Distributed under the Eclipse Public License, the same as Clojure.
