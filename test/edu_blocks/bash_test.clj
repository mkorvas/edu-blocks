(ns edu-blocks.bash-test
  (:require [clojure.test :refer [deftest is testing]]
            [edu-blocks.ast-helpers :as h :refer [s n v]]
            [edu-blocks.bash :as bash]))

(deftest values
  (is (= "\"hi\"" (bash/emit (s "hi"))))
  (is (= "42" (bash/emit (n 42))))
  (is (= "\"$x\"" (bash/emit (v "x")))))

(deftest echo
  (is (= "echo \"hi\"" (bash/emit (h/echo (s "hi")))))
  (is (= "echo -n \"hi\"" (bash/emit (h/echo (s "hi") false)))))

(deftest assign
  (is (= "x=\"hi\"" (bash/emit (h/assign "x" (s "hi")))))
  (testing "an empty socket assigns the empty string"
    (is (= "x=" (bash/emit (h/assign "x" nil))))))

(deftest commands
  (is (= "seq \"$n\"" (bash/emit (h/seq* (n 1) (v "n"))))
      "seq from 1 omits the start")
  (is (= "seq 2 5" (bash/emit (h/seq* (n 2) (n 5)))))
  (is (= "cat \"notes.txt\"" (bash/emit (h/cat* (s "notes.txt")))))
  (is (= "head -n 2 \"notes.txt\"" (bash/emit (h/head* (n 2) (s "notes.txt")))))
  (is (= "tail -n 2 \"notes.txt\"" (bash/emit (h/tail* (n 2) (s "notes.txt")))))
  (is (= "wc -l < \"notes.txt\"" (bash/emit (h/wc-l (s "notes.txt"))))
      "wc reads a redirect so the file name is not printed")
  (is (= "LC_ALL=C sort \"notes.txt\"" (bash/emit (h/sort* (s "notes.txt"))))
      "sort is pinned to byte order")
  (is (= "uniq \"notes.txt\"" (bash/emit (h/uniq* (s "notes.txt")))))
  (testing "an empty FILE socket reads stdin"
    (is (= "head -n 2" (bash/emit (h/head* (n 2) nil))))
    (is (= "grep \"milk\"" (bash/emit (h/grep* (s "milk") nil))))
    (is (= "tail -n 2" (bash/emit (h/tail* (n 2) nil))))
    (is (= "wc -l" (bash/emit (h/wc-l nil))))
    (is (= "LC_ALL=C sort" (bash/emit (h/sort* nil))))
    (is (= "uniq" (bash/emit (h/uniq* nil))))))

(deftest substitution
  (is (= "$(seq 3)" (bash/emit (h/subst (h/run (h/seq* (n 1) (n 3)))))))
  (is (= "\"$(echo \"hi\")\""
         (bash/emit (h/subst-str (h/run (h/echo (s "hi")))))))
  (testing "a pipeline inside a substitution"
    (is (= "$(cat \"notes.txt\" | grep \"milk\")"
           (bash/emit (h/subst (h/run (h/cat* (s "notes.txt")))
                               (h/pipe (h/grep* (s "milk") nil))))))))

(deftest pipelines
  (is (= "#!/bin/bash\ncat \"notes.txt\" | grep \"milk\" | head -n 1"
         (bash/emit (h/script (h/run (h/cat* (s "notes.txt")))
                              (h/pipe (h/grep* (s "milk") nil))
                              (h/pipe (h/head* (n 1) nil))))))
  (testing "consecutive runs stay separate statements"
    (is (= "#!/bin/bash\necho \"a\"\necho \"b\""
           (bash/emit (h/script (h/run (h/echo (s "a")))
                                (h/run (h/echo (s "b")))))))))

(deftest control-flow
  (is (= "for i in \"a\" \"b\"; do\n  echo \"$i\"\ndone"
         (bash/emit (h/for* "i" (h/word-list (s "a") (s "b"))
                            (h/run (h/echo (v "i")))))))
  (is (= "if [ \"$x\" = \"hi\" ]; then\n  echo \"yes\"\nfi"
         (bash/emit (h/if* (h/cmp (v "x") "EQ" (s "hi"))
                           (h/run (h/echo (s "yes")))))))
  (testing "empty bodies emit a no-op"
    (is (= "for i in \"a\"; do\n  :\ndone"
           (bash/emit (h/for* "i" (h/word-list (s "a"))))))))

(deftest comparisons
  (is (= "[ \"a\" != \"b\" ]" (bash/emit (h/cmp (s "a") "NEQ" (s "b")))))
  (is (= "[ 3 -lt 5 ]" (bash/emit (h/cmp-num (n 3) "LT" (n 5))))))

(deftest arithmetic
  (is (= "$(( 1 + 2 ))" (bash/emit (h/arith (n 1) "ADD" (n 2)))))
  (testing "nested sub-expressions get plain parens"
    (is (= "$(( 1 + (2 * 3) ))"
           (bash/emit (h/arith (n 1) "ADD" (h/arith (n 2) "MUL" (n 3))))))))

(deftest functions
  (is (= "greet() {\n  echo \"hi\"\n}"
         (bash/emit (h/func-def "greet" nil (h/run (h/echo (s "hi")))))))
  (testing "declared args become local variables"
    (is (= "greet() {\n  local who=\"$1\"\n  echo \"$who\"\n}"
           (bash/emit (h/func-def "greet" (h/arg-list "who")
                                  (h/run (h/echo (v "who"))))))))
  (is (= "greet \"World\""
         (bash/emit (h/call "greet" (h/word-list (s "World"))))))
  (is (= "greet" (bash/emit (h/call "greet" nil)))))

(deftest script
  (is (= "#!/bin/bash\nx=\"hi\"\necho \"$x\""
         (bash/emit (h/script (h/assign "x" (s "hi"))
                              (h/run (h/echo (v "x"))))))))
