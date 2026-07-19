(ns edu-blocks.challenges-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [edu-blocks.ast-helpers :as h :refer [s n v]]
            [edu-blocks.harness-util :as hu]
            [edu-blocks.levels :as levels]))

;; Sample solutions use the relative path "notes.txt" (the harness runs
;; bash in a temp dir containing it), while students use the virtual
;; tree's /home/alice/notes.txt — same content, per hu/notes-txt.
(def ^:private solutions
  {1 (h/script (h/run (h/echo (s "Hello, world!"))))
   2 (h/script (h/assign "name" (s "Bob"))
               (h/run (h/echo (v "name"))))
   3 (h/script (h/run (h/echo (h/arith (n 6) "MUL" (n 7)))))
   4 (h/script (h/run (h/head* (n 2) (s "notes.txt"))))
   5 (h/script (h/run (h/cat* (s "notes.txt")))
               (h/pipe (h/grep* (s "milk") nil)))
   6 (h/script (h/run (h/echo (h/subst-str (h/run (h/seq* (n 1) (n 3)))))))
   7 (h/script (h/for* "i" (h/subst (h/run (h/seq* (n 1) (n 5))))
                       (h/run (h/echo (v "i")))))
   8 (h/script (h/for* "i" (h/subst (h/run (h/seq* (n 1) (n 5))))
                       (h/if* (h/cmp-num (v "i") "GT" (n 3))
                              (h/run (h/echo (v "i"))))))
   9 (h/script (h/func-def "twice" (h/arg-list "x")
                           (h/run (h/echo (v "x")))
                           (h/run (h/echo (v "x"))))
               (h/run (h/call "twice" (h/word-list (s "Alice")))))})

(deftest every-level-has-a-solvable-challenge
  (doseq [{:keys [id label challenge]} levels/levels]
    (testing (str "level " id " — " label)
      (let [solution (solutions id)]
        (is (some? challenge) "has a challenge")
        (is (some? solution) "has a sample solution")
        (testing "the solution uses only blocks unlocked at its level"
          (is (set/subset? (levels/block-types solution)
                           (levels/unlocked-blocks id))))
        (testing "the solution uses the required blocks"
          (is (set/subset? (:requires challenge)
                           (levels/block-types solution))))
        (testing "the solution prints the expected output in both languages"
          (let [{:keys [bash js]} (hu/run-both solution)]
            (is (= (:expected challenge) bash))
            (is (= bash js))
            (is (= :passed (levels/check-challenge challenge solution js)))))))))

(deftest check-challenge-verdicts
  (let [challenge (:challenge (levels/level 2))]
    (testing "wrong output loses regardless of blocks used"
      (is (= :wrong-output
             (levels/check-challenge challenge (solutions 2) "nope"))))
    (testing "right output without the level's blocks is called out"
      (is (= :missing-blocks
             (levels/check-challenge challenge
                                     (h/script (h/run (h/echo (s "Bob"))))
                                     "Bob\n"))))))
