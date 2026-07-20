(ns edu-blocks.bash
  (:require [clojure.string :as str]))

(defmulti emit :type)

(defmethod emit nil [_] "")

(defn- group-pipes
  "Groups a bash_run followed by zero or more bash_pipe blocks into
   a pipeline. Returns a seq of [:pipe [run pipe ...]] or [:stmt b]."
  [stmts]
  (reduce (fn [acc s]
            (case (:type s)
              "bash_run"  (conj acc [:pipe [s]])
              "bash_pipe" (if (= :pipe (first (peek acc)))
                            (update-in acc [(dec (count acc)) 1] conj s)
                            (conj acc [:stmt s]))
              (conj acc [:stmt s])))
          []
          stmts))

(declare emit-stmts)

(defn- emit-group [[kind v]]
  (case kind
    :stmt (emit v)
    :pipe (str/join " | " (map #(emit (:CMD (:args %))) v))))

(defn- emit-stmts [stmts]
  (str/join "\n" (map emit-group (group-pipes stmts))))

(defmethod emit "bash_script" [{:keys [args]}]
  (str "#!/bin/bash\n" (emit-stmts (:STMTS args))))

(defmethod emit "bash_echo" [{:keys [args]}]
  (str "echo "
       (when (false? (:NEWLINE args)) "-n ")
       (emit (:VALUE args))))

(defn- indent [s]
  (->> (str/split-lines s)
       (map #(str "  " %))
       (str/join "\n")))

(defmethod emit "bash_for" [{:keys [args]}]
  (let [body (if (seq (:BODY args)) (emit-stmts (:BODY args)) ":")]
    (str "for " (:VAR args) " in " (emit (:LIST args)) "; do\n"
         (indent body)
         "\ndone")))

(defmethod emit "bash_string" [{:keys [args]}]
  (str "\"" (:TEXT args) "\""))

(defmethod emit "bash_var" [{:keys [args]}]
  (str "\"$" (:VAR args) "\""))

(defmethod emit "bash_assign" [{:keys [args]}]
  (str (:VAR args) "=" (emit (:VALUE args))))

(defmethod emit "bash_word_list" [{:keys [args]}]
  (str/join " " (map emit (:ARGS args))))

(defmethod emit "bash_subst" [{:keys [args]}]
  (str "$(" (emit-stmts (:CMD args)) ")"))

(defmethod emit "bash_subst_str" [{:keys [args]}]
  (str "\"$(" (emit-stmts (:CMD args)) ")\""))

(defmethod emit "bash_seq" [{:keys [args]}]
  (let [{:keys [FROM TO]} args
        from-one? (and (= "bash_number" (:type FROM))
                       (= 1 (get-in FROM [:args :NUM])))]
    (if from-one?
      (str "seq " (emit TO))
      (str "seq " (emit FROM) " " (emit TO)))))

(defmethod emit "bash_cat" [{:keys [args]}]
  (str "cat " (emit (:FILE args))))

(defmethod emit "bash_head" [{:keys [args]}]
  (str "head -n " (emit (:N args))
       (when (some? (:FILE args)) (str " " (emit (:FILE args))))))

(defmethod emit "bash_grep" [{:keys [args]}]
  (str "grep " (emit (:PATTERN args))
       (when (some? (:FILE args)) (str " " (emit (:FILE args))))))

(defmethod emit "bash_tail" [{:keys [args]}]
  (str "tail -n " (emit (:N args))
       (when (some? (:FILE args)) (str " " (emit (:FILE args))))))

;; Redirect the file into wc so it does not print the file name
;; alongside the count.
(defmethod emit "bash_wc_l" [{:keys [args]}]
  (str "wc -l"
       (when (some? (:FILE args)) (str " < " (emit (:FILE args))))))

;; Pin the sort order to plain byte order; the default is
;; locale-dependent and would disagree with the JavaScript translation.
(defmethod emit "bash_sort" [{:keys [args]}]
  (str "LC_ALL=C sort"
       (when (some? (:FILE args)) (str " " (emit (:FILE args))))))

(defmethod emit "bash_uniq" [{:keys [args]}]
  (str "uniq"
       (when (some? (:FILE args)) (str " " (emit (:FILE args))))))

(defmethod emit "bash_run" [{:keys [args]}]
  (emit (:CMD args)))

;; A stray bash_pipe outside a stack of pipes still runs its command.
(defmethod emit "bash_pipe" [{:keys [args]}]
  (emit (:CMD args)))

(defmethod emit "bash_if" [{:keys [args]}]
  (let [cond (if (some? (:COND args)) (emit (:COND args)) "false")
        then (if (seq (:THEN args)) (emit-stmts (:THEN args)) ":")]
    (str "if " cond "; then\n"
         (indent then)
         "\nfi")))

(defmethod emit "bash_compare" [{:keys [args]}]
  (let [op (case (:OP args) "NEQ" "!=" "=")]
    (str "[ " (emit (:A args)) " " op " " (emit (:B args)) " ]")))

(defmethod emit "bash_number" [{:keys [args]}]
  (str (:NUM args)))

(defmethod emit "bash_compare_num" [{:keys [args]}]
  (let [op ({"EQ" "-eq" "NEQ" "-ne"
             "LT" "-lt" "LE" "-le"
             "GT" "-gt" "GE" "-ge"} (:OP args))]
    (str "[ " (emit (:A args)) " " op " " (emit (:B args)) " ]")))

(defn- arith-op [op]
  ({"ADD" "+" "SUB" "-" "MUL" "*" "DIV" "/" "MOD" "%"} op))

(declare arith-inner)

(defn- arith-operand [x]
  ;; A nested arithmetic sub-expression is bracketed with plain parens
  ;; to preserve the user's precedence, without a redundant $((..)).
  (if (= "bash_arith" (:type x))
    (str "(" (arith-inner x) ")")
    (emit x)))

(defn- arith-inner [{:keys [args]}]
  (str (arith-operand (:A args))
       " " (arith-op (:OP args)) " "
       (arith-operand (:B args))))

(defmethod emit "bash_arith" [expr]
  (str "$(( " (arith-inner expr) " ))"))

(defn- arg-names [func-def-args]
  (->> (some-> func-def-args :ARGS :args :ARGS)
       (keep #(some-> % :args :NAME))
       (remove str/blank?)))

(defmethod emit "bash_func_def" [{:keys [args]}]
  (let [preamble (->> (arg-names args)
                      (map-indexed (fn [i n] (str "local " n "=\"$" (inc i) "\"")))
                      (str/join "\n"))
        body     (if (seq (:BODY args)) (emit-stmts (:BODY args)) ":")
        body     (if (seq preamble) (str preamble "\n" body) body)]
    (str (:NAME args) "() {\n" (indent body) "\n}")))

(defmethod emit "bash_func_call" [{:keys [args]}]
  (let [a (when (some? (:ARGS args)) (emit (:ARGS args)))]
    (if (seq a) (str (:NAME args) " " a) (:NAME args))))

(defmethod emit :default [{:keys [type]}]
  (str "# unknown block: " type))
