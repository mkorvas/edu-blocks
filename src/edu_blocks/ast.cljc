(ns edu-blocks.ast)

;; Describes the structure of each block type: which keys are fields
;; (plain values), inputs (single connected block), stmts (ordered
;; sequence of connected blocks via the :next chain), or varargs
;; (all dynamic ADD<n> inputs collected into one ordered vector).
(def block-defs
  {"bash_script"    {:stmts  [:STMTS]}
   "bash_echo"      {:fields [:NEWLINE] :inputs [:VALUE]}
   "bash_for"       {:fields [:VAR] :inputs [:LIST] :stmts [:BODY]}
   "bash_word_list" {:varargs [:ARGS]}
   "bash_string"    {:fields [:TEXT]}
   "bash_var"       {:fields [:VAR]}
   "bash_assign"    {:fields [:VAR] :inputs [:VALUE]}
   "bash_subst"     {:stmts [:CMD]}
   "bash_subst_str" {:stmts [:CMD]}
   "bash_seq"       {:inputs [:FROM :TO]}
   "bash_cat"       {:inputs [:FILE]}
   "bash_head"      {:inputs [:N :FILE]}
   "bash_tail"      {:inputs [:N :FILE]}
   "bash_grep"      {:inputs [:PATTERN :FILE]}
   "bash_wc_l"      {:inputs [:FILE]}
   "bash_sort"      {:inputs [:FILE]}
   "bash_uniq"      {:inputs [:FILE]}
   "bash_run"       {:inputs [:CMD]}
   "bash_pipe"      {:inputs [:CMD]}
   "bash_if"          {:inputs [:COND] :stmts [:THEN]}
   "bash_compare"     {:fields [:OP] :inputs [:A :B]}
   "bash_number"      {:fields [:NUM]}
   "bash_compare_num" {:fields [:OP] :inputs [:A :B]}
   "bash_arith"       {:fields [:OP] :inputs [:A :B]}
   "bash_func_def"    {:fields [:NAME] :inputs [:ARGS] :stmts [:BODY]}
   "bash_func_call"   {:fields [:NAME] :inputs [:ARGS]}
   "bash_arg_list"    {:varargs [:ARGS]}
   "bash_arg_name"    {:fields [:NAME]}})

(declare bky-block->ast)

(defn- bky-field [block k]
  (get-in block [:fields k]))

;; A real block connected over a shadow takes precedence over it.
(defn- input-block [input]
  (or (:block input) (:shadow input)))

(defn- bky-input [block k]
  (bky-block->ast (input-block (get-in block [:inputs k]))))

(defn- bky-varargs [block]
  (->> (:inputs block)
       (keep (fn [[k v]]
               (when-let [[_ n] (re-matches #"ADD(\d+)" (name k))]
                 [(parse-long n) (bky-block->ast (input-block v))])))
       (sort-by first)
       (mapv second)))

(defn- bky-stmts [block k]
  (loop [b   (get-in block [:inputs k :block])
         acc []]
    (if (nil? b)
      acc
      (recur (get-in b [:next :block])
             (conj acc (bky-block->ast b))))))

(defn bky-block->ast [block]
  (when (some? block)
    (when-let [def (get block-defs (:type block))]
      {:type (:type block)
       :args (-> {}
                 (into (map (fn [f] [f (bky-field block f)]) (:fields def [])))
                 (into (map (fn [i] [i (bky-input block i)]) (:inputs def [])))
                 (into (map (fn [s] [s (bky-stmts block s)]) (:stmts def [])))
                 (into (map (fn [va] [va (bky-varargs block)]) (:varargs def []))))})))

(defn bky-ws->ast
  "Returns the AST for the first bash_script block in the workspace JSON,
   or nil if none is present."
  [ws-json]
  (->> (get-in ws-json [:blocks :blocks] [])
       (filter #(= "bash_script" (:type %)))
       first
       bky-block->ast))
