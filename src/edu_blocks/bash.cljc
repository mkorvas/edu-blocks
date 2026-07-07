(ns edu-blocks.bash
  (:require [clojure.string :as str]))

(defmulti emit :type)

(defmethod emit nil [_] "")

(defmethod emit "bash_script" [{:keys [args]}]
  (str "#!/bin/bash\n"
       (str/join "\n" (map emit (:STMTS args)))))

(defmethod emit "bash_echo" [{:keys [args]}]
  (str "echo "
       (when (false? (:NEWLINE args)) "-n ")
       (emit (:VALUE args))))

(defn- indent [s]
  (->> (str/split-lines s)
       (map #(str "  " %))
       (str/join "\n")))

(defmethod emit "bash_for" [{:keys [args]}]
  (let [body (if (seq (:BODY args))
               (str/join "\n" (map emit (:BODY args)))
               ":")]
    (str "for " (:VAR args) " in " (emit (:LIST args)) "; do\n"
         (indent body)
         "\ndone")))

(defmethod emit "bash_string" [{:keys [args]}]
  (str "\"" (:TEXT args) "\""))

(defmethod emit "bash_var" [{:keys [args]}]
  (str "\"$" (:VAR args) "\""))

(defmethod emit "bash_word_list" [{:keys [args]}]
  (str/join " " (map emit (:ARGS args))))

(defmethod emit "bash_subst" [{:keys [args]}]
  (str "$(" (emit (:CMD args)) ")"))

(defmethod emit "bash_seq" [{:keys [args]}]
  (let [{:keys [FROM TO]} args]
    (if (= 1 FROM)
      (str "seq " TO)
      (str "seq " FROM " " TO))))

(defmethod emit "bash_if" [{:keys [args]}]
  (let [cond (if (some? (:COND args)) (emit (:COND args)) "false")
        then (if (seq (:THEN args))
               (str/join "\n" (map emit (:THEN args)))
               ":")]
    (str "if " cond "; then\n"
         (indent then)
         "\nfi")))

(defmethod emit "bash_compare" [{:keys [args]}]
  (let [op (case (:OP args) "NEQ" "!=" "=")]
    (str "[ " (emit (:A args)) " " op " " (emit (:B args)) " ]")))

(defmethod emit :default [{:keys [type]}]
  (str "# unknown block: " type))
