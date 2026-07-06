(ns edu-blocks.ast)

;; Describes the structure of each block type: which keys are fields
;; (plain values), inputs (single connected block), or stmts (ordered
;; sequence of connected blocks via the :next chain).
(def block-defs
  {"bash_script" {:stmts  [:STMTS]}
   "bash_echo"   {:fields [:NEWLINE] :inputs [:VALUE]}
   "bash_for"    {:fields [:VAR] :inputs [:LIST] :stmts [:BODY]}
   "bash_words"  {:fields [:WORDS]}
   "bash_string" {:fields [:TEXT]}})

(declare bky-block->ast)

(defn- bky-field [block k]
  (get-in block [:fields k]))

(defn- bky-input [block k]
  (bky-block->ast (get-in block [:inputs k :block])))

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
                 (into (map (fn [s] [s (bky-stmts block s)]) (:stmts def []))))})))

(defn bky-ws->ast
  "Returns the AST for the first bash_script block in the workspace JSON,
   or nil if none is present."
  [ws-json]
  (->> (get-in ws-json [:blocks :blocks] [])
       (filter #(= "bash_script" (:type %)))
       first
       bky-block->ast))
