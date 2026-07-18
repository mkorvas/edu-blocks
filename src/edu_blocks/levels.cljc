(ns edu-blocks.levels
  "The level progression (which blocks students have unlocked) and the
   toolbox, organized into categories of related blocks. Plain EDN;
   pass toolbox definitions through clj->js before handing them to
   Blockly.")

(def levels
  "Each level lists the block types it introduces; a student at level N
   has the union of levels 1..N available."
  [{:id 1 :label "Hello, script"
    :description "Print messages with echo."
    :blocks #{"bash_script" "bash_run" "bash_echo" "bash_string"}}
   {:id 2 :label "Variables"
    :description "Store values and read them back."
    :blocks #{"bash_assign" "bash_var"}}
   {:id 3 :label "Numbers"
    :description "Count and calculate."
    :blocks #{"bash_number" "bash_arith"}}
   {:id 4 :label "Files"
    :description "Look into files with cat, head and grep."
    :blocks #{"bash_cat" "bash_head" "bash_grep" "bash_seq"}}
   {:id 5 :label "Pipelines"
    :description "Send one command's output on to another."
    :blocks #{"bash_pipe"}}
   {:id 6 :label "Command output"
    :description "Use a command's output as a value."
    :blocks #{"bash_subst" "bash_subst_str" "bash_word_list"}}
   {:id 7 :label "Loops"
    :description "Repeat steps for every word in a list."
    :blocks #{"bash_for"}}
   {:id 8 :label "Decisions"
    :description "Run steps only when a test passes."
    :blocks #{"bash_if" "bash_compare" "bash_compare_num"}}
   {:id 9 :label "Functions"
    :description "Name a recipe and call it with arguments."
    :blocks #{"bash_func_def" "bash_func_call" "bash_arg_list" "bash_arg_name"}}])

(def max-level (:id (last levels)))

(defn level [id]
  (some #(when (= id (:id %)) %) levels))

(defn unlocked-blocks
  "The set of block types available at level-id."
  [level-id]
  (into #{} (mapcat :blocks) (filter #(<= (:id %) level-id) levels)))

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

(defn entry-block-types
  "All block types a toolbox entry mentions, including nested blocks
   and shadows."
  [entry]
  (cond
    (map? entry) (cond-> (into #{} (mapcat entry-block-types) (vals entry))
                   (:type entry) (conj (:type entry)))
    (coll? entry) (into #{} (mapcat entry-block-types) entry)
    :else #{}))

(defn toolbox-for
  "The category toolbox for level-id: only entries whose blocks (incl.
   nested ones) are all unlocked, with empty categories dropped."
  [level-id]
  (let [available (unlocked-blocks level-id)]
    {:kind "categoryToolbox"
     :contents
     (into []
           (keep (fn [category]
                   (let [entries (filterv #(every? available (entry-block-types %))
                                          (:contents category))]
                     (when (seq entries)
                       (assoc category :kind "category" :contents entries)))))
           categories)}))

(def toolbox (toolbox-for max-level))
