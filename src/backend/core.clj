;; Based on https://ericnormand.me/guide/clojure-web-tutorial.
(ns backend.core
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.tools.logging :as log]
   [clojure.walk :refer [postwalk]]

   [compojure.core :refer [context routes GET OPTIONS POST PUT]]
   [compojure.route :as route]
   [ring.adapter.jetty :as jetty]
   [ring.logger :as ring-logger]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.nested-params :refer [wrap-nested-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.io :refer [last-modified-date]]
   [ring.util.response :refer [content-type header response]]
   [ring.util.time :refer [format-date]]

   [nrepl.server :as nrepl-server]
   [cider.nrepl :refer [cider-nrepl-handler]]

   [config.core :refer [env]]
   [jsonista.core :as j]

   [clojure.string :as s]
   )
  (:use [com.rpl.specter])
  (:import [java.io File])
)

(defonce server (atom nil))

;; DEBUG
(defn echo [req]
  {:status 200
   :body (with-out-str (pprint req))
   :headers {"Content-Type" "text/plain"}
   })

(def local-api-headers
  {
    "Content-Type" "application/json"
  })

(defn- as-response
  [data]
  {:status 200
   :body (j/write-value-as-bytes data)
   :headers local-api-headers})

(defn- vecs-to-sets [data]
  (postwalk #(if (or (map-entry? %) (not (vector? %))) % (set %))
            data))

(defn modified-resource [path type mod]
  (let [body (mod (slurp path))
        num-utf8-bytes (count (.getBytes ^String body "UTF-8"))]
    (-> (response body)
        (header "Last-Modified"
                (format-date (last-modified-date (File. path))))
        (header "Content-Length" num-utf8-bytes)
        (content-type type))))

(defn prefix-index [root]
  (modified-resource "resources/public/index.html"
                     "text/html; charset=utf-8"
                     (if (s/blank? root)
                       identity
                       #(s/replace % #"\"/" (str "\"/" root "/")))))

(defn prefix-css [root css-path]
  (modified-resource css-path
                     "text/css"
                     (if (s/blank? root)
                       identity
                       #(s/replace % #"url\(/" (str "url(/" root "/")))))

(defn prefix-app-js [root]
  (modified-resource "resources/public/js/compiled/app.js"
                     "text/javascript"
                     (if (s/blank? root)
                       identity
                       #(s/replace % #"/assets/media"
                                   (str "/" root "/assets/media")))))

(defn snoqb-routes
  ([] (snoqb-routes ""))
  ([root]
   (context (if (s/blank? root) "" (str "/" root)) []
     (routes
       (GET "/echo" req (echo req))

       (GET "/index.html" _ (prefix-index root))
       (GET "/" _ (prefix-index root))

       (GET "*.css" {{file-path :*} :route-params}
         (prefix-css root (str "resources/public" file-path ".css")))

       (GET "/js/compiled/app.js" _
         (prefix-app-js root))

       (route/files "/" {:root "resources/public"})
       (route/not-found {:status 404
                         :body "Not found."
                         :headers {"Content-Type" "text/plain"}})))))

(def app (-> (snoqb-routes (:route-root env))
             (wrap-session {:cookie-attrs {:http-only false
                                           :max-age (* 8 3600)}})
             wrap-nested-params
             wrap-keyword-params
             wrap-params
             ))

(defn stop-server! []
  (when-some [s @server]
    (.stop s)
    (reset! server nil)))

(defn start-server! []
  (reset! server
          ;; For the wrap-reload and use of #', see
          ;; https://tonitalksdev.com/clojure-ring-hot-reload-server-on-code-changes.
          (jetty/run-jetty (wrap-reload #'app)
                           {:port 3001
                            :join? false}))
)

(defn -main [& args]
  (log/log-capture! *ns*)
  (nrepl-server/start-server :port 7889 :handler cider-nrepl-handler)
  (start-server!))

(comment

 (stop-server!)
 )
