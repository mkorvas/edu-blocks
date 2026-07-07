(ns edu-blocks.blocks
  (:require ["blockly" :as b]
            ["/blocks/word_list" :refer (wordListBlock)]))

(def ^:private configs
  (clj->js
   [{:type        "bash_script"
     :message0    "script %1"
     :args0       [{:type "input_statement" :name "STMTS"}]
     :colour      290}
    {:type              "bash_echo"
     :message0          "echo %1 newline %2"
     :args0             [{:type "input_value" :name "VALUE" :check "String"}
                         {:type "field_checkbox" :name "NEWLINE" :checked true}]
     :inputsInline      true
     :previousStatement nil
     :nextStatement     nil
     :colour            160}
    {:type              "bash_for"
     :message0          "for %1 in %2"
     :args0             [{:type "field_input" :name "VAR" :text "i"}
                         {:type "input_value" :name "LIST" :check "WordList"}]
     :message1          "do %1"
     :args1             [{:type "input_statement" :name "BODY"}]
     :inputsInline      true
     :previousStatement nil
     :nextStatement     nil
     :colour            120}
    {:type    "bash_string"
     :message0 "\"%1\""
     :args0   [{:type "field_input" :name "TEXT" :text "hello"}]
     :output  "String"
     :colour  160}
    {:type    "bash_var"
     :message0 "$%1"
     :args0   [{:type "field_input" :name "VAR" :text "i"}]
     :output  "String"
     :colour  330}]))

(def toolbox
  (clj->js
   {:kind     "flyoutToolbox"
    :contents [{:kind "block" :type "bash_script"}
               {:kind "block" :type "bash_echo"}
               {:kind "block" :type "bash_for"}
               {:kind "block" :type "bash_string"}
               {:kind "block" :type "bash_var"}
               {:kind "block" :type "bash_word_list"}]}))

(defn define-blocks! []
  (let [defs (b/common.createBlockDefinitionsFromJsonArray configs)]
    (set! (.-bash_word_list defs) wordListBlock)
    (b/common.defineBlocks defs)))
