(ns edu-blocks.views
  (:require
   ["blockly" :as b]
   [re-com.core :refer [h-box horizontal-bar-tabs]]
   [re-frame.core :as rf]

   [edu-blocks.blocks :as blocks]
   [edu-blocks.events :as ev]
   [edu-blocks.subs :as subs]
   ))

(def ws-config
  (clj->js
   {:toolbox  blocks/toolbox
    :grid     {:spacing 20 :length 3 :colour "#ccc" :snap true}
    :zoom     {:controls true}
    :trashcan true}))

(defn bky-js->clj [bky]
  (cond
    (array? bky)
      (mapv bky-js->clj bky)
    (string? bky)
      bky
    (nil? bky)
      bky
    (seq (js/Object.keys bky))
      (into {} (map (fn [[k v]] [(keyword k) (bky-js->clj v)])
                    (js/Object.entries bky)))
    :else bky))

(defn- on-ws-change [^js ws]
  (fn [^js e]
    (when-not (or ^boolean (.-isUiEvent e)
                  (.isDragging ws))
      (rf/dispatch [::ev/ws-modified
                    (bky-js->clj (.save b/serialization.workspaces ws))]))))

(defn- inject-blockly!
  "React 19 ref callback: injects Blockly into the container element and
   returns a cleanup fn disposing of the workspace."
  [el]
  (when el
    (let [ws (b/inject el ws-config)
          observer (js/ResizeObserver. (fn [_] (b/svgResize ws)))]
      (.observe observer el)
      (.addChangeListener ws (on-ws-change ws))
      (rf/dispatch [::ev/ws-created ws observer])
      (fn []
        (rf/dispatch [::ev/ws-disposed])
        (.disconnect observer)
        (.dispose ws)))))

(defn blockly-pane []
  [:div {:style {:flex "1 1 auto" :height "100%" :min-width 0}
         :ref inject-blockly!}])

(def langs
  [{:id :bash :label "Bash"}
   {:id :js   :label "JavaScript"}])

(defn output-panel []
  (let [lang @(rf/subscribe [::subs/lang])
        code @(rf/subscribe [::subs/code-output])]
    [:div {:style {:flex           "0 0 300px"
                   :height         "100%"
                   :display        "flex"
                   :flex-direction "column"
                   :background     "#1e1e1e"}}
     [:div {:style {:padding "8px"}}
      [horizontal-bar-tabs
       :model     lang
       :tabs      langs
       :on-change #(rf/dispatch [::ev/set-lang %])]]
     [:pre {:style {:margin      0
                    :padding     "12px"
                    :flex        "1 1 auto"
                    :overflow-y  "auto"
                    :color       "#d4d4d4"
                    :font-family "monospace"
                    :white-space "pre"}}
      (or code (case lang
                 :js "// Drop a script block\n// to get started"
                 "# Drop a script block\n# to get started"))]]))

(defn main-panel []
  [h-box
   :style {:height "100vh" :width "100%"}
   :children [[blockly-pane]
              [output-panel]]])
