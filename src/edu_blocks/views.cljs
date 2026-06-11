(ns edu-blocks.views
  (:require
   ["blockly" :as b]
   [re-com.core :refer [box gap h-box v-box input-text]]
   ))

(def toolbox
  ;; Placeholder built-in blocks, to be replaced by custom Bash blocks.
  (clj->js
   {:kind "flyoutToolbox"
    :contents [{:kind "block" :type "controls_if"}
               {:kind "block" :type "controls_repeat_ext"}
               {:kind "block" :type "text"}]}))

(def ws-config
  (clj->js
   {:toolbox toolbox
    :grid {:spacing 20 :length 3 :colour "#ccc" :snap true}
    :zoom {:controls true}
    :trashcan true}))

(defn- inject-blockly!
  "React 19 ref callback: injects Blockly into the container element and
   returns a cleanup fn disposing of the workspace."
  [el]
  (when el
    (let [ws (b/inject el ws-config)]
      (fn [] (.dispose ws)))))

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
