(ns edu-blocks.views
  (:require
   ["blockly" :as b]
   [re-com.core :refer [h-box]]
   [re-frame.core :as rf]

   [edu-blocks.blocks :as blocks]
   [edu-blocks.events :as ev]
   ))

(def ws-config
  (clj->js
   {:toolbox  blocks/toolbox
    :grid     {:spacing 20 :length 3 :colour "#ccc" :snap true}
    :zoom     {:controls true}
    :trashcan true}))

(defn- on-ws-change [^js ws]
  (fn [^js e]
    (when-not (or ^boolean (.-isUiEvent e)
                  (.isDragging ws))
      (rf/dispatch [::ev/ws-modified
                    (js->clj (.save b/serialization.workspaces ws)
                             :keywordize-keys true)]))))

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
  [:div {:style {:flex "1 1 auto" :height "100%"}
         :ref inject-blockly!}])

(defn main-panel []
  [h-box
   :style {:height "100vh"
           :width "100%"}
   :children [
              [blockly-pane]
              ]]
  )
