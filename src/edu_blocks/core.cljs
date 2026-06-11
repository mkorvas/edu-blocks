(ns edu-blocks.core
  (:require
   ;["blockly" :as b]
   ["react" :refer [StrictMode]]
   ["react-dom/client" :rename {createRoot create-root}]
   [reagent.core :as r]
   [re-frame.core :as rf]

   [edu-blocks.blocks :as blocks]
   [edu-blocks.config :as config]
   [edu-blocks.events :as ev]
   [edu-blocks.views :as views]
))


(defn dev-setup []
  (when config/debug?
    (js/console.log "debug mode")
    (println "dev mode")))

(defonce root (create-root (.getElementById js/document "edu-blocks")))
(def mounted (atom false))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (when (compare-and-set! mounted true true)
    (.unmount root))
  (js/console.log {:debug? config/debug? :strict? config/strict?})
  (.render root (r/as-element
                 (if (and config/debug? config/strict?)
                   [:> StrictMode [views/main-panel]]
                   [views/main-panel]))))

(defn init! []
  (rf/dispatch-sync [::ev/initialize-db])
  (dev-setup)
  (blocks/define-blocks!)
  (mount-root))
