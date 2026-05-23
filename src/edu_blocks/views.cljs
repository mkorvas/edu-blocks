(ns edu-blocks.views
  (:require
   [re-com.core :refer [box gap h-box v-box input-text]]
   ))

(defn main-panel []
  [h-box
   :style {:height "100%"
           :width "100%"}
   :children [
              [:div "Hello"]
              [:div "World"]
              ]]
  )
