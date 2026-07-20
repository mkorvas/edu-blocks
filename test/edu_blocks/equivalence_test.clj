(ns edu-blocks.equivalence-test
  "Runs the emitted bash script with real bash and the emitted
   JavaScript with node (via test/harness.js, which mirrors the in-app
   sandbox) and asserts identical output — on a fixed corpus and on
   randomly generated block stacks."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [edu-blocks.ast-helpers :as h :refer [s n v]]
            [edu-blocks.harness-util :refer [run-both]]))

(defn- equivalent?
  ([ast] (let [{:keys [bash js]} (run-both ast)]
           (is (= bash js) (pr-str ast))))
  ([ast expected]
   (let [{:keys [bash js]} (run-both ast)]
     (is (= expected bash) (pr-str ast))
     (is (= bash js) (pr-str ast)))))

;;; Fixed corpus

(deftest echo-and-variables
  (equivalent? (h/script (h/assign "x" (s "hi"))
                         (h/run (h/echo (v "x")))
                         (h/run (h/echo (s "no newline") false)))
               "hi\nno newline"))

(deftest pipeline
  (equivalent? (h/script (h/run (h/cat* (s "notes.txt")))
                         (h/pipe (h/grep* (s "a") nil))
                         (h/pipe (h/head* (n 2) nil)))
               "Remember to water the plants.\nCall grandma.\n"))

(deftest tail-of-file
  (equivalent? (h/script (h/run (h/tail* (n 2) (s "notes.txt"))))
               "Buy milk.\nCall grandma.\n"))

(deftest sort-uniq-count-pipeline
  (equivalent? (h/script (h/run (h/cat* (s "notes.txt")))
                         (h/pipe (h/sort* nil))
                         (h/pipe (h/uniq* nil))
                         (h/pipe (h/wc-l nil)))
               "3\n"))

(deftest for-over-seq-with-arith
  (equivalent? (h/script (h/assign "sum" (n 0))
                         (h/for* "i" (h/subst (h/run (h/seq* (n 1) (n 4))))
                                 (h/assign "sum" (h/arith (v "sum") "ADD" (v "i"))))
                         (h/run (h/echo (v "sum"))))
               "10\n"))

(deftest conditionals
  (equivalent? (h/script (h/assign "x" (n 7))
                         (h/if* (h/cmp-num (v "x") "GT" (n 5))
                                (h/run (h/echo (s "big"))))
                         (h/if* (h/cmp (v "x") "EQ" (s "8"))
                                (h/run (h/echo (s "eight")))))
               "big\n"))

(deftest division-truncates-toward-zero
  (equivalent? (h/script (h/run (h/echo (h/arith (n -7) "DIV" (n 2))))
                         (h/run (h/echo (h/arith (n 7) "MOD" (n 3)))))
               "-3\n1\n"))

(deftest quoted-substitution-in-comparison
  (equivalent? (h/script (h/if* (h/cmp (h/subst-str (h/run (h/echo (h/arith (n 1) "ADD" (n 2)))))
                                       "EQ" (s "3"))
                                (h/run (h/echo (s "yes")))))
               "yes\n"))

(deftest function-with-named-args-keeps-locals
  (equivalent? (h/script (h/assign "x" (s "outer"))
                         (h/func-def "shadow" (h/arg-list "x")
                                     (h/run (h/echo (v "x"))))
                         (h/run (h/call "shadow" (h/word-list (s "inner"))))
                         (h/run (h/echo (v "x"))))
               "inner\nouter\n"))

(deftest function-output-through-pipeline
  (equivalent? (h/script (h/func-def "notes" nil
                                     (h/run (h/cat* (s "notes.txt"))))
                         (h/run (h/call "notes" nil))
                         (h/pipe (h/grep* (s "milk") nil)))
               "Buy milk.\n"))

;;; Fuzzing

(def ^:private gen-word
  (gen/fmap #(apply str %) (gen/vector gen/char-alphanumeric 1 5)))

(def ^:private gen-str-node (gen/fmap s gen-word))
(def ^:private gen-num-node (gen/fmap n (gen/choose -9 9)))
(def ^:private gen-pos-num (gen/fmap n (gen/choose 1 5)))
(def ^:private gen-nonzero-num
  (gen/fmap n (gen/such-that (complement zero?) (gen/choose -9 9))))

(defn- gen-arith [depth]
  (gen/let [op (gen/elements ["ADD" "SUB" "MUL" "DIV" "MOD"])
            a (if (pos? depth)
                (gen/one-of [gen-num-node (gen-arith (dec depth))])
                gen-num-node)
            ;; A zero divisor would error in bash but not in JS.
            b (cond
                (#{"DIV" "MOD"} op) gen-nonzero-num
                (pos? depth) (gen/one-of [gen-num-node (gen-arith (dec depth))])
                :else gen-num-node)]
    (h/arith a op b)))

(defn- gen-value [env]
  (gen/one-of
   (cond-> [gen-str-node
            gen-num-node
            (gen-arith 2)
            (gen/fmap #(h/subst-str (h/run (h/echo %)))
                      (gen/one-of [gen-str-node gen-num-node]))]
     (seq env) (conj (gen/fmap v (gen/elements (vec env)))))))

(defn- gen-pipeline
  "A run block plus 0–2 pipe stages, as a vector of statements."
  [env]
  (gen/let [source (gen/one-of
                    [(gen/fmap #(h/run (h/echo %)) (gen-value env))
                     (gen/return (h/run (h/cat* (s "notes.txt"))))
                     (gen/fmap (fn [[a b]] (h/run (h/seq* a b)))
                               (gen/tuple gen-pos-num gen-pos-num))])
            stages (gen/vector
                    (gen/one-of
                     [(gen/fmap #(h/pipe (h/head* % nil)) gen-pos-num)
                      (gen/fmap #(h/pipe (h/tail* % nil)) gen-pos-num)
                      (gen/fmap #(h/pipe (h/grep* (s %) nil)) gen-word)
                      (gen/return (h/pipe (h/wc-l nil)))
                      (gen/return (h/pipe (h/sort* nil)))
                      (gen/return (h/pipe (h/uniq* nil)))])
                    0 2)]
    (into [source] stages)))

(def ^:private gen-condition
  (gen/one-of
   [(gen/fmap (fn [[a op b]] (h/cmp a op b))
              (gen/tuple gen-str-node (gen/elements ["EQ" "NEQ"]) gen-str-node))
    (gen/fmap (fn [[a op b]] (h/cmp-num a op b))
              (gen/tuple (gen-arith 1)
                         (gen/elements ["EQ" "NEQ" "LT" "LE" "GT" "GE"])
                         gen-num-node))]))

(defn- gen-stmts
  "A vector of up to n statement groups; env is the set of assigned
   variable names in scope."
  [env depth n]
  (if (zero? n)
    (gen/return [])
    (gen/let [kind (gen/elements (cond-> [:pipeline :assign :if]
                                   (pos? depth) (conj :for)))]
      (case kind
        :pipeline
        (gen/let [p (gen-pipeline env)
                  more (gen-stmts env depth (dec n))]
          (into p more))

        :assign
        (gen/let [name (gen/elements ["a" "b" "c"])
                  value (gen-value env)
                  more (gen-stmts (conj env name) depth (dec n))]
          (into [(h/assign name value)] more))

        :if
        (gen/let [cond gen-condition
                  then (gen-stmts env (dec depth) 2)
                  more (gen-stmts env depth (dec n))]
          (into [(apply h/if* cond then)] more))

        :for
        (gen/let [var (gen/elements ["i" "j"])
                  list (gen/one-of
                        [(gen/fmap #(apply h/word-list %)
                                   (gen/vector (gen/one-of [gen-str-node gen-num-node]) 1 3))
                         (gen/fmap (fn [[a b]] (h/subst (h/run (h/seq* a b))))
                                   (gen/tuple gen-pos-num gen-pos-num))])
                  body (gen-stmts (conj env var) (dec depth) 2)
                  more (gen-stmts env depth (dec n))]
          (into [(apply h/for* var list body)] more))))))

(def ^:private gen-script
  (gen/fmap #(apply h/script %) (gen-stmts #{} 2 4)))

(defspec bash-and-js-agree 40
  (prop/for-all [ast gen-script]
    (let [{:keys [bash js]} (run-both ast)]
      (= bash js))))
