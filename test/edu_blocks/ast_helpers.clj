(ns edu-blocks.ast-helpers
  "Concise constructors for the block-AST maps consumed by the
   bash and javascript emitters.")

(defn s [text] {:type "bash_string" :args {:TEXT text}})
(defn n [num] {:type "bash_number" :args {:NUM num}})
(defn v [var] {:type "bash_var" :args {:VAR var}})

(defn echo
  ([value] (echo value true))
  ([value newline?] {:type "bash_echo" :args {:VALUE value :NEWLINE newline?}}))

(defn run [cmd] {:type "bash_run" :args {:CMD cmd}})
(defn pipe [cmd] {:type "bash_pipe" :args {:CMD cmd}})

(defn assign [var value] {:type "bash_assign" :args {:VAR var :VALUE value}})
(defn word-list [& items] {:type "bash_word_list" :args {:ARGS (vec items)}})

(defn subst [& stmts] {:type "bash_subst" :args {:CMD (vec stmts)}})
(defn subst-str [& stmts] {:type "bash_subst_str" :args {:CMD (vec stmts)}})

(defn seq* [from to] {:type "bash_seq" :args {:FROM from :TO to}})
(defn cat* [file] {:type "bash_cat" :args {:FILE file}})
(defn head* [n file] {:type "bash_head" :args {:N n :FILE file}})
(defn grep* [pattern file] {:type "bash_grep" :args {:PATTERN pattern :FILE file}})

(defn for* [var list & body]
  {:type "bash_for" :args {:VAR var :LIST list :BODY (vec body)}})
(defn if* [cond & then]
  {:type "bash_if" :args {:COND cond :THEN (vec then)}})

(defn cmp [a op b] {:type "bash_compare" :args {:A a :OP op :B b}})
(defn cmp-num [a op b] {:type "bash_compare_num" :args {:A a :OP op :B b}})
(defn arith [a op b] {:type "bash_arith" :args {:A a :OP op :B b}})

(defn arg-name [name] {:type "bash_arg_name" :args {:NAME name}})
(defn arg-list [& names]
  {:type "bash_arg_list" :args {:ARGS (mapv arg-name names)}})
(defn func-def [name args & body]
  {:type "bash_func_def" :args {:NAME name :ARGS args :BODY (vec body)}})
(defn call [name args] {:type "bash_func_call" :args {:NAME name :ARGS args}})

(defn script [& stmts] {:type "bash_script" :args {:STMTS (vec stmts)}})
