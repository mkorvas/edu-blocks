(ns edu-blocks.views
  (:require
   ["blockly" :as b]
   ["@blockly/block-dynamic-connection" :as b-dyna]
   [clojure.string :as str]
   [re-com.core :refer [button h-box horizontal-bar-tabs]]
   [re-frame.core :as rf]
   [reagent.core :as r]

   [edu-blocks.blocks :as blocks]
   [edu-blocks.events :as ev]
   [edu-blocks.subs :as subs]
   ))

(def ws-config
  (clj->js
   {:toolbox  blocks/toolbox
    :grid     {:spacing 20 :length 3 :colour "#ccc" :snap true}
    :zoom     {:controls true}
    :trashcan true
    :plugins  {:connectionPreviewer
               (b-dyna/decoratePreviewer b/InsertionMarkerPreviewer)}}))

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
      (.addChangeListener ws b-dyna/finalizeConnections)
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

(defn exec-panel []
  (let [output @(rf/subscribe [::subs/exec-output])]
    [:div {:style {:flex           "0 0 300px"
                   :height         "100%"
                   :display        "flex"
                   :flex-direction "column"
                   :background     "#000"}}
     [:div {:style {:padding "8px"}}
      [button
       :label    "Run JS"
       :class    "btn-primary"
       :on-click #(rf/dispatch [::ev/run-script])]]
     [:pre {:style {:margin      0
                    :padding     "12px"
                    :flex        "1 1 auto"
                    :overflow-y  "auto"
                    :color       "#9f9"
                    :font-family "monospace"
                    :white-space "pre-wrap"}}
      (or output "")]]))

(defn- tree-node [node]
  (if (= :dir (:type node))
    (r/with-let [open? (r/atom true)]
      [:div
       [:div {:style    {:cursor "pointer" :white-space "nowrap"}
              :on-click #(swap! open? not)}
        [:i {:class (str "zmdi " (if @open? "zmdi-chevron-down" "zmdi-chevron-right"))
             :style {:display "inline-block" :width "14px"}}]
        [:i {:class "zmdi zmdi-folder" :style {:color "#e8b339"}}]
        " " (:name node)]
       (when @open?
         [:div {:style {:margin-left "18px"}}
          (for [child (:children node)]
            ^{:key (:name child)} [tree-node child])])])
    [:div {:style {:white-space "nowrap" :margin-left "14px"}}
     [:i {:class "zmdi zmdi-file" :style {:color "#999"}}]
     " " (:name node)]))

(def ^:private max-content-bytes (* 100 1024))

(defn- read-file-entry
  "Returns a Promise of {:path ... :content ...} for a File object.
   Content is capped at max-content-bytes; a file whose text does not
   decode cleanly (binary) gets a nil content."
  [^js file]
  (-> (.text (.slice file 0 max-content-bytes))
      (.then (fn [text]
               {:path    (.-webkitRelativePath file)
                :content (when-not (str/includes? text "�") text)}))))

(defn- on-dir-upload [^js e]
  (let [files (array-seq (.. e -target -files))]
    (when (seq files)
      (-> (js/Promise.all (into-array (map read-file-entry files)))
          (.then #(rf/dispatch [::ev/fs-uploaded (vec (array-seq %))]))))))

(defn files-panel []
  (let [tree @(rf/subscribe [::subs/fs-tree])]
    [:div {:style {:flex           "0 0 250px"
                   :height         "100%"
                   :display        "flex"
                   :flex-direction "column"
                   :background     "#f7f7f7"
                   :border-left    "1px solid #ddd"}}
     [:div {:style {:padding "8px" :border-bottom "1px solid #ddd"}}
      ;; Only file names/paths are read from the picked directory,
      ;; never file contents.
      [:input {:type            "file"
               :webkitdirectory ""
               :title           "Load a folder as the file tree"
               :style           {:max-width "100%" :font-size "12px"}
               :on-change       on-dir-upload}]]
     [:div {:style {:flex        "1 1 auto"
                    :overflow    "auto"
                    :padding     "12px"
                    :font-family "monospace"
                    :font-size   "14px"
                    :line-height "1.6"}}
      [tree-node tree]]]))

(defn main-panel []
  [h-box
   :style {:height "100vh" :width "100%"}
   :children [[blockly-pane]
              [output-panel]
              [exec-panel]
              [files-panel]]])
