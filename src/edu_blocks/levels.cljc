(ns edu-blocks.levels
  "The toolbox, organized into categories of related blocks. Plain EDN;
   pass the result through clj->js before handing it to Blockly.")

(def ^:private categories
  [{:name "Basics" :colour 180
    :contents [{:kind "block" :type "bash_script"}
               {:kind "block" :type "bash_run"}
               {:kind "block" :type "bash_echo"}
               {:kind "block" :type "bash_string"}]}
   {:name "Variables" :colour 330
    :contents [{:kind "block" :type "bash_assign"}
               {:kind "block" :type "bash_var"}]}
   {:name "Numbers" :colour 210
    :contents [{:kind "block" :type "bash_number"}
               {:kind   "block"
                :type   "bash_arith"
                :inputs {:A {:shadow {:type "bash_number" :fields {:NUM 1}}}
                         :B {:shadow {:type "bash_number" :fields {:NUM 2}}}}}]}
   {:name "Commands" :colour 210
    :contents [{:kind   "block"
                :type   "bash_cat"
                :inputs {:FILE {:shadow {:type   "bash_string"
                                         :fields {:TEXT "/home/alice/notes.txt"}}}}}
               {:kind   "block"
                :type   "bash_head"
                :inputs {:N {:shadow {:type "bash_number" :fields {:NUM 2}}}}}
               {:kind   "block"
                :type   "bash_grep"
                :inputs {:PATTERN {:shadow {:type "bash_string" :fields {:TEXT "milk"}}}}}
               {:kind   "block"
                :type   "bash_seq"
                :inputs {:FROM {:shadow {:type "bash_number" :fields {:NUM 1}}}
                         :TO   {:shadow {:type "bash_number" :fields {:NUM 10}}}}}]}
   {:name "Pipelines" :colour 180
    :contents [{:kind "block" :type "bash_pipe"}]}
   {:name "Command output" :colour 40
    :contents [{:kind "block" :type "bash_subst"}
               {:kind "block" :type "bash_subst_str"}
               {:kind "block" :type "bash_word_list"}]}
   {:name "Loops" :colour 120
    :contents [{:kind "block" :type "bash_for"}]}
   {:name "Decisions" :colour 60
    :contents [{:kind "block" :type "bash_if"}
               {:kind "block" :type "bash_compare"}
               {:kind "block" :type "bash_compare_num"}]}
   {:name "Functions" :colour 290
    :contents [{:kind "block" :type "bash_func_def"}
               {:kind "block" :type "bash_arg_list"}
               {:kind "block" :type "bash_arg_name"}
               {:kind "block" :type "bash_func_call"}]}])

(def toolbox
  {:kind "categoryToolbox"
   :contents (mapv #(assoc % :kind "category") categories)})
