(ns edu-blocks.javascript
  (:require [clojure.string :as str]))

(defmulti emit :type)

(defmethod emit nil [_] "")

(defmethod emit "bash_script" [{:keys [args]}]
  (str/join "\n" (map emit (:STMTS args))))

(defmethod emit "bash_echo" [{:keys [args]}]
  (if (false? (:NEWLINE args))
    (str "process.stdout.write(" (emit (:VALUE args)) ");")
    (str "console.log(" (emit (:VALUE args)) ");")))

(defn- indent [s]
  (->> (str/split-lines s)
       (map #(str "  " %))
       (str/join "\n")))

(defmethod emit "bash_for" [{:keys [args]}]
  (let [list (if (some? (:LIST args)) (emit (:LIST args)) "[]")
        body (str/join "\n" (map emit (:BODY args)))]
    (str "for (const " (:VAR args) " of " list ") {\n"
         (indent body)
         "\n}")))

(defmethod emit "bash_string" [{:keys [args]}]
  (pr-str (:TEXT args)))

(defmethod emit "bash_var" [{:keys [args]}]
  (:VAR args))

(defmethod emit "bash_assign" [{:keys [args]}]
  ;; var, not let: repeated set-blocks for the same name would make
  ;; let throw, and var's function scoping matches Bash variables.
  ;; An empty socket means the empty string, as in Bash.
  (let [value (if (some? (:VALUE args)) (emit (:VALUE args)) "\"\"")]
    (str "var " (:VAR args) " = " value ";")))

(defmethod emit "bash_word_list" [{:keys [args]}]
  (str "[" (str/join ", " (map emit (:ARGS args))) "]"))

(defmethod emit "bash_subst" [{:keys [args]}]
  (if (some? (:CMD args))
    (emit (:CMD args))
    "[]"))

(defmethod emit "bash_seq" [{:keys [args]}]
  (let [{:keys [FROM TO]} args]
    (str "Array.from({length: " (inc (- TO FROM)) "}, (_, k) => k + " FROM ")")))

(defmethod emit "bash_if" [{:keys [args]}]
  (let [cond (if (some? (:COND args)) (emit (:COND args)) "false")
        then (str/join "\n" (map emit (:THEN args)))]
    (str "if (" cond ") {\n"
         (indent then)
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

(defmethod emit :default [{:keys [type]}]
  (str "// unknown block: " type))
