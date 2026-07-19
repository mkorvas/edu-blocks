(ns edu-blocks.levels-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [edu-blocks.ast :as ast]
            [edu-blocks.levels :as levels]))

(def ^:private all-level-blocks
  (mapcat :blocks levels/levels))

(deftest levels-are-consistent
  (testing "every level block type has an AST definition"
    (is (empty? (remove ast/block-defs all-level-blocks))))
  (testing "no block type is introduced at two levels"
    (is (apply distinct? all-level-blocks)))
  (testing "every defined block type is introduced at some level"
    (is (empty? (remove (set all-level-blocks) (keys ast/block-defs))))))

(deftest toolbox-is-consistent
  (testing "every toolbox block (incl. nested and shadows) is unlocked at the max level"
    (is (set/subset? (levels/block-types levels/toolbox)
                     (levels/unlocked-blocks levels/max-level))))
  (testing "every defined block type appears in the toolbox"
    (is (empty? (remove (levels/block-types levels/toolbox)
                        (keys ast/block-defs))))))

(deftest each-level-enlarges-the-toolbox
  (let [entry-counts (map (fn [{:keys [id]}]
                            (->> (levels/toolbox-for id)
                                 :contents
                                 (mapcat :contents)
                                 count))
                          levels/levels)]
    (is (apply < 0 entry-counts))))
