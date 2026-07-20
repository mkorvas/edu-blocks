(ns edu-blocks.javascript
  (:require [clojure.string :as str]))

(defmulti emit :type)

(defmethod emit nil [_] "")

(defn- group-pipes [stmts]
  (reduce (fn [acc s]
            (case (:type s)
              "bash_run"  (conj acc [:pipe [s]])
              "bash_pipe" (if (= :pipe (first (peek acc)))
                            (update-in acc [(dec (count acc)) 1] conj s)
                            (conj acc [:stmt s]))
              (conj acc [:stmt s])))
          []
          stmts))

(declare emit-stage)
(declare indent)

(defn- emit-pipeline [stages]
  (if (= 1 (count stages))
    (str "process.stdout.write(" (emit-stage (:CMD (:args (first stages))) nil) ");")
    (str "process.stdout.write((function () {\n"
         (indent
          (str "var __stdin = \"\";\n"
               (str/join
                "\n"
                (map (fn [stage]
                       (str "__stdin = " (emit-stage (:CMD (:args stage)) "__stdin") ";"))
                     stages))
               "\nreturn __stdin;"))
         "\n})());")))

(defn- emit-group [[kind v]]
  (case kind
    :stmt (emit v)
    :pipe (emit-pipeline v)))

(defn- emit-stmts [stmts]
  (str/join "\n" (map emit-group (group-pipes stmts))))

(defmethod emit "bash_script" [{:keys [args]}]
  ;; __args holds a bash function's positional parameters; the script-
  ;; level empty default makes $1, $2, ... expand to "" outside a call.
  (str "var __args = [];\n" (emit-stmts (:STMTS args))))

(defmethod emit "bash_echo" [{:keys [args]}]
  ;; As a Command, echo evaluates to the text it would have printed
  ;; (with a trailing newline unless "newline" is off).
  (let [v (emit (:VALUE args))]
    (if (false? (:NEWLINE args))
      (str "String(" v ")")
      (str "(String(" v ") + \"\\n\")"))))

(defn- indent [s]
  (->> (str/split-lines s)
       (map #(str "  " %))
       (str/join "\n")))

(defmethod emit "bash_for" [{:keys [args]}]
  (let [list (if (some? (:LIST args)) (emit (:LIST args)) "[]")
        body (emit-stmts (:BODY args))]
    (str "for (const " (:VAR args) " of " list ") {\n"
         (indent body)
         "\n}")))

(defmethod emit "bash_string" [{:keys [args]}]
  (pr-str (:TEXT args)))

(defmethod emit "bash_var" [{:keys [args]}]
  ;; Numeric $1, $2, ... refer to a function's positional parameters
  ;; via a rest-param shadowing the top-level __args.
  (let [v (:VAR args)]
    (if (re-matches #"\d+" v)
      (str "(__args[" (dec (parse-long v)) "] ?? \"\")")
      v)))

(defmethod emit "bash_assign" [{:keys [args]}]
  ;; var, not let: repeated set-blocks for the same name would make
  ;; let throw, and var's function scoping matches Bash variables.
  ;; An empty socket means the empty string, as in Bash.
  (let [value (if (some? (:VALUE args)) (emit (:VALUE args)) "\"\"")]
    (str "var " (:VAR args) " = " value ";")))

(defmethod emit "bash_word_list" [{:keys [args]}]
  (str "[" (str/join ", " (map emit (:ARGS args))) "]"))

;; Commands translate to sandbox-provided helper functions returning
;; their stdout as text, like the Bash commands they mirror.

(defn- capture-body [stmts]
  ;; The sandbox helper "capture" runs its function argument with the
  ;; script's stdout redirected to a fresh buffer, then returns that
  ;; buffer's contents.
  (str "capture(function () {\n" (indent (emit-stmts stmts)) "\n})"))

(defmethod emit "bash_subst" [{:keys [args]}]
  ;; Unquoted $(...) in Bash splits the output into words.
  (str (capture-body (:CMD args)) ".split(/\\s+/).filter(Boolean)"))

(defmethod emit "bash_subst_str" [{:keys [args]}]
  ;; "$(...)" in Bash yields the output verbatim, minus trailing newlines.
  (str (capture-body (:CMD args)) ".replace(/\\n+$/, \"\")"))

(defmethod emit "bash_seq" [{:keys [args]}]
  (str "seq(Number(" (emit (:FROM args)) "), Number(" (emit (:TO args)) "))"))

;; The filter commands take text (not a path); cat wraps a file path
;; into text, and inside a pipe an empty FILE reads from the previous
;; stage's stdout.
(defn- file-source [file stdin]
  (cond
    (some? file) (str "cat(" (emit file) ")")
    (some? stdin) stdin
    :else "\"\""))

(defmethod emit "bash_cat" [{:keys [args]}]
  (str "cat(" (emit (:FILE args)) ")"))

(defmethod emit "bash_head" [{:keys [args]}]
  (emit-stage {:type "bash_head" :args args} nil))

(defmethod emit "bash_tail" [{:keys [args]}]
  (emit-stage {:type "bash_tail" :args args} nil))

(defmethod emit "bash_grep" [{:keys [args]}]
  (emit-stage {:type "bash_grep" :args args} nil))

(defmethod emit "bash_wc_l" [{:keys [args]}]
  (emit-stage {:type "bash_wc_l" :args args} nil))

(defmethod emit "bash_sort" [{:keys [args]}]
  (emit-stage {:type "bash_sort" :args args} nil))

(defmethod emit "bash_uniq" [{:keys [args]}]
  (emit-stage {:type "bash_uniq" :args args} nil))

(defn- emit-stage
  "Emits a Command in a (possibly pipe-) context; when stdin is a JS
   expression (variable name), the filter commands read from it if
   FILE is empty."
  [cmd stdin]
  (let [source (file-source (:FILE (:args cmd)) stdin)]
    (case (:type cmd)
      "bash_head"
      (str "head(Number(" (emit (:N (:args cmd))) "), " source ")")
      "bash_tail"
      (str "tail(Number(" (emit (:N (:args cmd))) "), " source ")")
      "bash_grep"
      (str "grep(" (emit (:PATTERN (:args cmd))) ", " source ")")
      "bash_wc_l"
      (str "wcl(" source ")")
      "bash_sort"
      (str "sort(" source ")")
      "bash_uniq"
      (str "uniq(" source ")")
      (emit cmd))))

(defmethod emit "bash_run" [{:keys [args]}]
  (if (some? (:CMD args))
    (str "process.stdout.write(" (emit (:CMD args)) ");")
    ""))

;; A stray bash_pipe outside a stack of pipes still runs its command.
(defmethod emit "bash_pipe" [{:keys [args]}]
  (if (some? (:CMD args))
    (str "process.stdout.write(" (emit (:CMD args)) ");")
    ""))

(defmethod emit "bash_if" [{:keys [args]}]
  (let [cond (if (some? (:COND args)) (emit (:COND args)) "false")
        then (emit-stmts (:THEN args))]
    (str "if (" cond ") {\n"
         (indent then)
         "\n}")))

(defmethod emit "bash_if_else" [{:keys [args]}]
  (let [cond (if (some? (:COND args)) (emit (:COND args)) "false")]
    (str "if (" cond ") {\n"
         (indent (emit-stmts (:THEN args)))
         "\n} else {\n"
         (indent (emit-stmts (:ELSE args)))
         "\n}")))

(defmethod emit "bash_compare" [{:keys [args]}]
  ;; Bash [ = ] compares strings, so coerce both sides to keep the
  ;; two languages in agreement (e.g. a numeric loop var vs "3").
  (let [op (case (:OP args) "NEQ" "!==" "===")]
    (str "String(" (emit (:A args)) ") " op " String(" (emit (:B args)) ")")))

(defmethod emit "bash_number" [{:keys [args]}]
  (str (:NUM args)))

(defmethod emit "bash_compare_num" [{:keys [args]}]
  ;; Bash [ -eq ] etc. compare numerically, so coerce both sides
  ;; (operands may be string-typed, e.g. a $var).
  (let [op ({"EQ" "===" "NEQ" "!=="
             "LT" "<" "LE" "<="
             "GT" ">" "GE" ">="} (:OP args))]
    (str "Number(" (emit (:A args)) ") " op " Number(" (emit (:B args)) ")")))

(defmethod emit "bash_arith" [{:keys [args]}]
  ;; Bash arithmetic is integer-only, so / truncates toward zero.
  (let [a (str "Number(" (emit (:A args)) ")")
        b (str "Number(" (emit (:B args)) ")")
        op (:OP args)]
    (if (= "DIV" op)
      (str "Math.trunc(" a " / " b ")")
      (str "(" a " " ({"ADD" "+" "SUB" "-" "MUL" "*" "MOD" "%"} op) " " b ")"))))

(defn- arg-names [func-def-args]
  (->> (some-> func-def-args :ARGS :args :ARGS)
       (keep #(some-> % :args :NAME))
       (remove str/blank?)))

(defmethod emit "bash_func_def" [{:keys [args]}]
  ;; A function returns its captured stdout as text, matching how bash
  ;; functions are consumed by $() and pipelines.
  (let [preamble (->> (arg-names args)
                      (map-indexed (fn [i n] (str "var " n " = __args[" i "] ?? \"\";")))
                      (str/join "\n"))
        body     (emit-stmts (:BODY args))
        body     (if (seq preamble) (str preamble "\n" body) body)]
    (str "var " (:NAME args) " = function (...__args) {\n"
         (indent (str "return capture(function () {\n"
                      (indent body)
                      "\n});"))
         "\n};")))

(defmethod emit "bash_func_call" [{:keys [args]}]
  (let [a (if (some? (:ARGS args)) (emit (:ARGS args)) "[]")]
    (str (:NAME args) "(..." a ")")))

(defmethod emit :default [{:keys [type]}]
  (str "// unknown block: " type))
