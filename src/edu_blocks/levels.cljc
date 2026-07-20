(ns edu-blocks.levels
  "The level progression (which blocks students have unlocked) and the
   toolbox, organized into categories of related blocks. Plain EDN;
   pass toolbox definitions through clj->js before handing them to
   Blockly.")

(def levels
  "Each level lists the block types it introduces; a student at level N
   has the union of levels 1..N available. Its :challenge gates the
   next level: the script must print :expected and use the :requires
   block types."
  [{:id 1 :label "Hello, script"
    :description "Print messages with echo."
    :blocks #{"bash_script" "bash_run" "bash_echo" "bash_string"}
    :challenge {:task     "Print Hello, world!"
                :expected "Hello, world!\n"
                :requires #{"bash_echo"}}}
   {:id 2 :label "Variables"
    :description "Store values and read them back."
    :blocks #{"bash_assign" "bash_var"}
    :challenge {:task     "Store Bob in a variable, then print the variable."
                :expected "Bob\n"
                :requires #{"bash_assign" "bash_var"}}}
   {:id 3 :label "Numbers"
    :description "Count and calculate."
    :blocks #{"bash_number" "bash_arith"}
    :challenge {:task     "Print the result of 6 × 7."
                :expected "42\n"
                :requires #{"bash_arith"}}}
   {:id 4 :label "Files"
    :description "Look into files with cat, head, tail, grep and wc."
    :blocks #{"bash_cat" "bash_head" "bash_tail" "bash_grep" "bash_wc_l"
              "bash_seq"}
    :challenge {:task     "Print the first 2 lines of /home/alice/notes.txt."
                :expected "Remember to water the plants.\nBuy milk.\n"
                :requires #{"bash_head"}}}
   {:id 5 :label "Pipelines"
    :description "Send one command's output on to another; tidy lines with sort and uniq."
    :blocks #{"bash_pipe" "bash_sort" "bash_uniq"}
    :challenge {:task     "Using a pipeline, print the lines of /home/alice/notes.txt in alphabetical order."
                :expected "Buy milk.\nCall grandma.\nRemember to water the plants.\n"
                :requires #{"bash_pipe" "bash_sort"}}}
   {:id 6 :label "Command output"
    :description "Use a command's output as a value."
    :blocks #{"bash_subst" "bash_subst_str" "bash_word_list"}
    :challenge {:task     "Print the output of seq 1 .. 3 (three lines) with a single echo."
                :expected "1\n2\n3\n"
                :requires #{"bash_subst_str"}}}
   {:id 7 :label "Loops"
    :description "Repeat steps for every word in a list."
    :blocks #{"bash_for"}
    :challenge {:task     "Using a loop, print the numbers 1 to 5, one per line."
                :expected "1\n2\n3\n4\n5\n"
                :requires #{"bash_for"}}}
   {:id 8 :label "Decisions"
    :description "Run steps only when a test passes."
    :blocks #{"bash_if" "bash_if_else" "bash_compare" "bash_compare_num"}
    :challenge {:task     "Print only the numbers from 1 to 5 that are greater than 3."
                :expected "4\n5\n"
                :requires #{"bash_if" "bash_compare_num"}}}
   {:id 9 :label "Functions"
    :description "Name a recipe and call it with arguments."
    :blocks #{"bash_func_def" "bash_func_call" "bash_arg_list" "bash_arg_name"}
    :challenge {:task     "Define a function that prints its argument twice, and call it with Alice."
                :expected "Alice\nAlice\n"
                :requires #{"bash_func_def" "bash_func_call"}}}])

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
               {:kind "block" :type "bash_string"}
               ;; run + echo, ready to print
               {:kind   "block"
                :type   "bash_run"
                :inputs {:CMD {:block {:type   "bash_echo"
                                       :inputs {:VALUE {:shadow {:type   "bash_string"
                                                                 :fields {:TEXT "Hello, world!"}}}}}}}}]}
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
                :type   "bash_tail"
                :inputs {:N {:shadow {:type "bash_number" :fields {:NUM 2}}}}}
               {:kind   "block"
                :type   "bash_grep"
                :inputs {:PATTERN {:shadow {:type "bash_string" :fields {:TEXT "milk"}}}}}
               {:kind "block" :type "bash_wc_l"}
               {:kind "block" :type "bash_sort"}
               {:kind "block" :type "bash_uniq"}
               {:kind   "block"
                :type   "bash_seq"
                :inputs {:FROM {:shadow {:type "bash_number" :fields {:NUM 1}}}
                         :TO   {:shadow {:type "bash_number" :fields {:NUM 10}}}}}]}
   {:name "Pipelines" :colour 180
    :contents [{:kind "block" :type "bash_pipe"}
               ;; a two-stage pipeline: cat a file, grep its lines
               {:kind   "block"
                :type   "bash_run"
                :inputs {:CMD {:block {:type   "bash_cat"
                                       :inputs {:FILE {:shadow {:type   "bash_string"
                                                                :fields {:TEXT "/home/alice/notes.txt"}}}}}}}
                :next   {:block {:type   "bash_pipe"
                                 :inputs {:CMD {:block {:type   "bash_grep"
                                                        :inputs {:PATTERN {:shadow {:type   "bash_string"
                                                                                    :fields {:TEXT "milk"}}}}}}}}}}]}
   {:name "Command output" :colour 40
    :contents [{:kind "block" :type "bash_subst"}
               {:kind "block" :type "bash_subst_str"}
               {:kind "block" :type "bash_word_list"}]}
   {:name "Loops" :colour 120
    :contents [{:kind "block" :type "bash_for"}
               ;; for i in $(seq 1..10), echoing $i
               {:kind   "block"
                :type   "bash_for"
                :inputs {:LIST {:block {:type   "bash_subst"
                                        :inputs {:CMD {:block {:type   "bash_run"
                                                               :inputs {:CMD {:block {:type   "bash_seq"
                                                                                      :inputs {:FROM {:shadow {:type "bash_number" :fields {:NUM 1}}}
                                                                                               :TO   {:shadow {:type "bash_number" :fields {:NUM 10}}}}}}}}}}}}
                         :BODY {:block {:type   "bash_run"
                                        :inputs {:CMD {:block {:type   "bash_echo"
                                                               :inputs {:VALUE {:block {:type   "bash_var"
                                                                                        :fields {:VAR "i"}}}}}}}}}}}]}
   {:name "Decisions" :colour 60
    :contents [{:kind "block" :type "bash_if"}
               {:kind "block" :type "bash_if_else"}
               {:kind "block" :type "bash_compare"}
               {:kind "block" :type "bash_compare_num"}]}
   {:name "Functions" :colour 290
    :contents [{:kind "block" :type "bash_func_def"}
               {:kind "block" :type "bash_arg_list"}
               {:kind "block" :type "bash_arg_name"}
               {:kind "block" :type "bash_func_call"}
               ;; a greet(who) function echoing its argument
               {:kind   "block"
                :type   "bash_func_def"
                :fields {:NAME "greet"}
                :inputs {:ARGS {:block {:type   "bash_arg_list"
                                        :inputs {:ADD0 {:block {:type   "bash_arg_name"
                                                                :fields {:NAME "who"}}}}}}
                         :BODY {:block {:type   "bash_run"
                                        :inputs {:CMD {:block {:type   "bash_echo"
                                                               :inputs {:VALUE {:block {:type   "bash_var"
                                                                                        :fields {:VAR "who"}}}}}}}}}}}
               ;; a matching call, ready to run
               {:kind   "block"
                :type   "bash_run"
                :inputs {:CMD {:block {:type   "bash_func_call"
                                       :fields {:NAME "greet"}
                                       :inputs {:ARGS {:block {:type   "bash_word_list"
                                                               :inputs {:ADD0 {:block {:type   "bash_string"
                                                                                       :fields {:TEXT "World"}}}}}}}}}}}]}])

(defn block-types
  "All block types a nested structure mentions — works on toolbox
   entries (incl. nested blocks and shadows) and on emitter ASTs."
  [x]
  (cond
    (map? x) (cond-> (into #{} (mapcat block-types) (vals x))
               (:type x) (conj (:type x)))
    (coll? x) (into #{} (mapcat block-types) x)
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
                   (let [entries (filterv #(every? available (block-types %))
                                          (:contents category))]
                     (when (seq entries)
                       (assoc category :kind "category" :contents entries)))))
           categories)}))

(def toolbox (toolbox-for max-level))

(defn check-challenge
  "Judges a script AST and its output against a challenge. Returns
   :passed, :wrong-output, or :missing-blocks (right output without
   using this level's new blocks)."
  [{:keys [expected requires]} ast output]
  (cond
    (not= expected output)                        :wrong-output
    (not (every? (block-types ast) requires))     :missing-blocks
    :else                                         :passed))
