(ns edu-blocks.blocks
  (:require ["blockly" :as b]))

(def ^:private configs
  (clj->js
   [{:type        "bash_script"
     :message0    "script %1"
     :args0       [{:type "input_statement" :name "STMTS"}]
     :colour      290}
    {:type              "bash_echo"
     :message0          "echo %1"
     :args0             [{:type "input_value" :name "VALUE" :check "String"}]
     :previousStatement nil
     :nextStatement     nil
     :colour            160}
    {:type    "bash_string"
     :message0 "\"%1\""
     :args0   [{:type "field_input" :name "TEXT" :text "hello"}]
     :output  "String"
     :colour  160}]))

(def toolbox
  (clj->js
   {:kind     "flyoutToolbox"
    :contents [{:kind "block" :type "bash_script"}
               {:kind "block" :type "bash_echo"}
               {:kind "block" :type "bash_string"}]}))

(defn define-blocks! []
  (-> configs
      b/common.createBlockDefinitionsFromJsonArray
      b/common.defineBlocks))
