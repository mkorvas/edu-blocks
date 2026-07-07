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

(defmethod emit "bash_word_list" [{:keys [args]}]
  (str "[" (str/join ", " (map emit (:ARGS args))) "]"))

(defmethod emit "bash_subst" [{:keys [args]}]
  (if (some? (:CMD args))
    (emit (:CMD args))
    "[]"))

(defmethod emit "bash_seq" [{:keys [args]}]
  (let [{:keys [FROM TO]} args]
    (str "Array.from({length: " (inc (- TO FROM)) "}, (_, k) => k + " FROM ")")))

(defmethod emit :default [{:keys [type]}]
  (str "// unknown block: " type))
