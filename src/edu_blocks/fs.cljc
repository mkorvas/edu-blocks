(ns edu-blocks.fs
  (:require [clojure.string :as str]))

(defn- paths->nested [paths]
  (reduce (fn [m segs] (assoc-in m segs ::file))
          {}
          (map #(str/split % #"/") paths)))

(defn- nested->node [name v]
  (if (map? v)
    {:type :dir
     :name name
     :children (->> (sort-by (fn [[child-name child]]
                               [(if (map? child) 0 1) child-name])
                             v)
                    (mapv (fn [[child-name child]]
                            (nested->node child-name child))))}
    {:type :file :name name}))

(defn paths->tree
  "Builds a file-tree node from relative path strings, as given by
   the File API's webkitRelativePath. Directories sort before files."
  [paths]
  (let [nested (paths->nested paths)]
    ;; webkitRelativePath prefixes every path with the picked directory,
    ;; which thus becomes the root node.
    (if (= 1 (count nested))
      (let [[name v] (first nested)]
        (nested->node name v))
      (nested->node "/" nested))))
