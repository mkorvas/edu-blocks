(ns edu-blocks.javascript-test
  (:require [clojure.test :refer [deftest is testing]]
            [edu-blocks.ast-helpers :as h :refer [s n v]]
            [edu-blocks.javascript :as js]))

(deftest values
  (is (= "\"hi\"" (js/emit (s "hi"))))
  (is (= "42" (js/emit (n 42))))
  (is (= "x" (js/emit (v "x"))))
  (testing "numeric $1, $2, ... index the positional parameters"
    (is (= "(__args[0] ?? \"\")" (js/emit (v "1"))))
    (is (= "(__args[2] ?? \"\")" (js/emit (v "3"))))))

(deftest echo
  (is (= "(String(\"hi\") + \"\\n\")" (js/emit (h/echo (s "hi")))))
  (is (= "String(\"hi\")" (js/emit (h/echo (s "hi") false)))))

(deftest assign
  (is (= "var x = \"hi\";" (js/emit (h/assign "x" (s "hi")))))
  (is (= "var x = \"\";" (js/emit (h/assign "x" nil)))))

(deftest commands
  (is (= "seq(Number(1), Number(5))" (js/emit (h/seq* (n 1) (n 5)))))
  (is (= "cat(\"notes.txt\")" (js/emit (h/cat* (s "notes.txt")))))
  (testing "a standalone filter command reads its file"
    (is (= "head(Number(2), cat(\"notes.txt\"))"
           (js/emit (h/head* (n 2) (s "notes.txt")))))
    (is (= "grep(\"milk\", cat(\"notes.txt\"))"
           (js/emit (h/grep* (s "milk") (s "notes.txt")))))
    (is (= "tail(Number(2), cat(\"notes.txt\"))"
           (js/emit (h/tail* (n 2) (s "notes.txt")))))
    (is (= "wcl(cat(\"notes.txt\"))" (js/emit (h/wc-l (s "notes.txt")))))
    (is (= "sort(cat(\"notes.txt\"))" (js/emit (h/sort* (s "notes.txt")))))
    (is (= "uniq(cat(\"notes.txt\"))" (js/emit (h/uniq* (s "notes.txt"))))))
  (testing "outside a pipe, an empty FILE socket reads the empty string"
    (is (= "head(Number(2), \"\")" (js/emit (h/head* (n 2) nil))))
    (is (= "sort(\"\")" (js/emit (h/sort* nil))))))

(deftest substitution
  (is (= (str "capture(function () {\n"
              "  process.stdout.write(seq(Number(1), Number(3)));\n"
              "}).split(/\\s+/).filter(Boolean)")
         (js/emit (h/subst (h/run (h/seq* (n 1) (n 3)))))))
  (is (= (str "capture(function () {\n"
              "  process.stdout.write((String(\"hi\") + \"\\n\"));\n"
              "}).replace(/\\n+$/, \"\")")
         (js/emit (h/subst-str (h/run (h/echo (s "hi"))))))))

(deftest pipelines
  (testing "a single-stage pipeline writes the command's output"
    (is (= (str "var __args = [];\n"
                "process.stdout.write((String(\"hi\") + \"\\n\"));")
           (js/emit (h/script (h/run (h/echo (s "hi"))))))))
  (testing "a multi-stage pipeline threads __stdin through the stages"
    (is (= (str "var __args = [];\n"
                "process.stdout.write((function () {\n"
                "  var __stdin = \"\";\n"
                "  __stdin = cat(\"notes.txt\");\n"
                "  __stdin = grep(\"milk\", __stdin);\n"
                "  __stdin = head(Number(1), __stdin);\n"
                "  return __stdin;\n"
                "})());")
           (js/emit (h/script (h/run (h/cat* (s "notes.txt")))
                              (h/pipe (h/grep* (s "milk") nil))
                              (h/pipe (h/head* (n 1) nil))))))))

(deftest control-flow
  (is (= "for (const i of [\"a\", \"b\"]) {\n  process.stdout.write((String(i) + \"\\n\"));\n}"
         (js/emit (h/for* "i" (h/word-list (s "a") (s "b"))
                          (h/run (h/echo (v "i")))))))
  (is (= "if (String(x) === String(\"hi\")) {\n  process.stdout.write((String(\"yes\") + \"\\n\"));\n}"
         (js/emit (h/if* (h/cmp (v "x") "EQ" (s "hi"))
                         (h/run (h/echo (s "yes")))))))
  (is (= (str "if (String(x) === String(\"hi\")) {\n"
              "  process.stdout.write((String(\"yes\") + \"\\n\"));\n"
              "} else {\n"
              "  process.stdout.write((String(\"no\") + \"\\n\"));\n"
              "}")
         (js/emit (h/if-else (h/cmp (v "x") "EQ" (s "hi"))
                             [(h/run (h/echo (s "yes")))]
                             [(h/run (h/echo (s "no")))])))))

(deftest comparisons
  (is (= "String(\"a\") !== String(\"b\")" (js/emit (h/cmp (s "a") "NEQ" (s "b")))))
  (is (= "Number(3) < Number(5)" (js/emit (h/cmp-num (n 3) "LT" (n 5))))))

(deftest arithmetic
  (is (= "(Number(1) + Number(2))" (js/emit (h/arith (n 1) "ADD" (n 2)))))
  (testing "division truncates toward zero"
    (is (= "Math.trunc(Number(7) / Number(2))"
           (js/emit (h/arith (n 7) "DIV" (n 2))))))
  (is (= "(Number(1) + Number((Number(2) * Number(3))))"
         (js/emit (h/arith (n 1) "ADD" (h/arith (n 2) "MUL" (n 3)))))))

(deftest functions
  (testing "declared args become local variables"
    (is (= (str "var greet = function (...__args) {\n"
                "  return capture(function () {\n"
                "    var who = __args[0] ?? \"\";\n"
                "    process.stdout.write((String(who) + \"\\n\"));\n"
                "  });\n"
                "};")
           (js/emit (h/func-def "greet" (h/arg-list "who")
                                (h/run (h/echo (v "who"))))))))
  (is (= "greet(...[\"World\"])"
         (js/emit (h/call "greet" (h/word-list (s "World"))))))
  (is (= "greet(...[])" (js/emit (h/call "greet" nil)))))
