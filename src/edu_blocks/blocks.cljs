(ns edu-blocks.blocks
  (:require ["blockly" :as b]
            ["/blocks/arg_list" :refer (argListBlock)]
            ["/blocks/word_list" :refer (wordListBlock)]))

(def ^:private configs
  (clj->js
   [{:type        "bash_script"
     :message0    "script %1"
     :args0       [{:type "input_statement" :name "STMTS" :check "Stmt"}]
     :colour      290}
    {:type         "bash_echo"
     :message0     "echo %1 newline %2"
     :args0        [{:type "input_value" :name "VALUE" :check "String"}
                    {:type "field_checkbox" :name "NEWLINE" :checked true}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Prints a value; wrap in \"run\" to print it as a statement."
     :colour       160}
    {:type              "bash_for"
     :message0          "for %1 in %2"
     :args0             [{:type "field_input" :name "VAR" :text "i"}
                         {:type "input_value" :name "LIST" :check "WordList"}]
     :message1          "do %1"
     :args1             [{:type "input_statement" :name "BODY" :check "Stmt"}]
     :inputsInline      true
     :previousStatement "Stmt"
     :nextStatement     "Stmt"
     :colour            120}
    {:type    "bash_string"
     :message0 "\"%1\""
     :args0   [{:type "field_input" :name "TEXT" :text "hello"}]
     :output  "String"
     :colour  0}
    {:type    "bash_var"
     :message0 "$%1"
     :args0   [{:type "field_input" :name "VAR" :text "i"}]
     :output  "String"
     :colour  330}
    {:type              "bash_assign"
     :message0          "set %1 to %2"
     :args0             [{:type "field_input" :name "VAR" :text "x"}
                         {:type "input_value" :name "VALUE" :check ["String" "Number"]}]
     :inputsInline      true
     :previousStatement "Stmt"
     :nextStatement     "Stmt"
     :colour            330}
    {:type     "bash_subst"
     :message0 "$( %1 )"
     :args0    [{:type "input_statement" :name "CMD" :check "Stmt"}]
     :output   "WordList"
     :tooltip  "The output of a command or pipeline, split into words."
     :colour   40}
    {:type     "bash_subst_str"
     :message0 "\"$( %1 )\""
     :args0    [{:type "input_statement" :name "CMD" :check "Stmt"}]
     :output   "String"
     :tooltip  "The output of a command or pipeline, as one text value."
     :colour   40}
    {:type         "bash_seq"
     :message0     "seq %1 .. %2"
     :args0        [{:type "input_value" :name "FROM" :check ["Number" "String"]}
                    {:type "input_value" :name "TO" :check ["Number" "String"]}]
     :inputsInline true
     :output       "Command"
     :colour       210}
    {:type         "bash_cat"
     :message0     "cat %1"
     :args0        [{:type "input_value" :name "FILE" :check "String"}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Prints the contents of a file."
     :colour       210}
    {:type         "bash_head"
     :message0     "head -n %1 %2"
     :args0        [{:type "input_value" :name "N" :check ["Number" "String"]}
                    {:type "input_value" :name "FILE" :check "String"}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Prints the first N lines of a file."
     :colour       210}
    {:type         "bash_grep"
     :message0     "grep %1 %2"
     :args0        [{:type "input_value" :name "PATTERN" :check "String"}
                    {:type "input_value" :name "FILE" :check "String"}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Prints the lines of a file matching a pattern."
     :colour       210}
    {:type         "bash_tail"
     :message0     "tail -n %1 %2"
     :args0        [{:type "input_value" :name "N" :check ["Number" "String"]}
                    {:type "input_value" :name "FILE" :check "String"}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Prints the last N lines of a file."
     :colour       210}
    {:type         "bash_wc_l"
     :message0     "wc -l %1"
     :args0        [{:type "input_value" :name "FILE" :check "String"}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Counts the lines of a file."
     :colour       210}
    {:type         "bash_sort"
     :message0     "sort %1"
     :args0        [{:type "input_value" :name "FILE" :check "String"}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Prints the lines of a file in order."
     :colour       210}
    {:type         "bash_uniq"
     :message0     "uniq %1"
     :args0        [{:type "input_value" :name "FILE" :check "String"}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Removes repeated neighboring lines; sort first to remove all duplicates."
     :colour       210}
    {:type              "bash_run"
     :message0          "run %1"
     :args0             [{:type "input_value" :name "CMD" :check "Command"}]
     :inputsInline      true
     :previousStatement "Stmt"
     :nextStatement     ["Stmt" "PipeStage"]
     :tooltip           "Runs a command; add \"outputting to\" blocks below to pipe its output onward."
     :colour            180}
    {:type              "bash_pipe"
     :message0          "outputting to %1"
     :args0             [{:type "input_value" :name "CMD" :check "Command"}]
     :inputsInline      true
     :previousStatement "PipeStage"
     :nextStatement     ["Stmt" "PipeStage"]
     :tooltip           "Continues a pipeline; only attaches below a \"run\" or another \"outputting to\" block."
     :colour            180}
    {:type              "bash_if"
     :message0          "if %1"
     :args0             [{:type "input_value" :name "COND" :check "Condition"}]
     :message1          "then %1"
     :args1             [{:type "input_statement" :name "THEN" :check "Stmt"}]
     :previousStatement "Stmt"
     :nextStatement     "Stmt"
     :colour            60}
    {:type         "bash_compare"
     :message0     "%1 %2 %3 %4"
     :args0        [{:type "input_value" :name "A" :check "String"}
                    {:type    "field_dropdown"
                     :name    "OP"
                     :options [["=" "EQ"] ["≠" "NEQ"]]}
                    {:type "field_label" :text "abc" :class "eduSuperLabel"}
                    {:type "input_value" :name "B" :check "String"}]
     :inputsInline true
     :output       "Condition"
     :tooltip      "Compares the two values as text."
     :colour       30}
    {:type    "bash_number"
     :message0 "%1"
     :args0   [{:type "field_number" :name "NUM" :value 0}]
     :output  ["Number" "String"]
     :colour  210}
    {:type         "bash_arith"
     :message0     "%1 %2 %3"
     :args0        [{:type "input_value" :name "A" :check ["Number" "String"]}
                    {:type    "field_dropdown"
                     :name    "OP"
                     :options [["+" "ADD"] ["−" "SUB"] ["×" "MUL"]
                               ["/" "DIV"] ["mod" "MOD"]]}
                    {:type "input_value" :name "B" :check ["Number" "String"]}]
     :inputsInline true
     :output       ["Number" "String"]
     :tooltip      "Arithmetic on integers; division truncates toward zero."
     :colour       210}
    {:type              "bash_func_def"
     :message0          "function %1 %2"
     :args0             [{:type "field_input" :name "NAME" :text "greet"}
                         {:type "input_value" :name "ARGS" :check "ArgList"}]
     :message1          "%1"
     :args1             [{:type "input_statement" :name "BODY" :check "Stmt"}]
     :inputsInline      true
     :previousStatement "Stmt"
     :nextStatement     "Stmt"
     :tooltip           "Defines a function; declared args become local variables in the body, alongside $1, $2, ..."
     :colour            290}
    {:type    "bash_arg_name"
     :message0 "%1"
     :args0   [{:type "field_input" :name "NAME" :text "x"}]
     :output  "ArgName"
     :tooltip "Names a positional argument; becomes a local variable in the function body."
     :colour  290}
    {:type         "bash_func_call"
     :message0     "call %1 with %2"
     :args0        [{:type "field_input" :name "NAME" :text "greet"}
                    {:type "input_value" :name "ARGS" :check "WordList"}]
     :inputsInline true
     :output       "Command"
     :tooltip      "Calls a function with the given argument words."
     :colour       290}
    {:type         "bash_compare_num"
     :message0     "%1 %2 %3 %4"
     :args0        [{:type "input_value" :name "A" :check ["Number" "String"]}
                    {:type    "field_dropdown"
                     :name    "OP"
                     :options [["=" "EQ"] ["≠" "NEQ"]
                               ["<" "LT"] ["≤" "LE"]
                               [">" "GT"] ["≥" "GE"]]}
                    {:type "field_label" :text "123" :class "eduSuperLabel"}
                    {:type "input_value" :name "B" :check ["Number" "String"]}]
     :inputsInline true
     :output       "Condition"
     :tooltip      "Compares the two values as numbers."
     :colour       230}]))

(defn define-blocks! []
  (let [defs (b/common.createBlockDefinitionsFromJsonArray configs)]
    (set! (.-bash_word_list defs) wordListBlock)
    (set! (.-bash_arg_list defs) argListBlock)
    (b/common.defineBlocks defs)))
