(ns edu-blocks.fs
  (:require [clojure.string :as str]))

;; In the intermediate nested representation a directory is a map of
;; name -> child and a file is its content wrapped in a vector (so
;; that a nil content still looks different from a directory).
(defn- entries->nested [entries]
  (reduce (fn [m {:keys [path content]}]
            (assoc-in m (str/split path #"/") [content]))
          {}
          entries))

(defn- nested->node [name v]
  (if (map? v)
    {:type :dir
     :name name
     :children (->> (sort-by (fn [[child-name child]]
                               [(if (map? child) 0 1) child-name])
                             v)
                    (mapv (fn [[child-name child]]
                            (nested->node child-name child))))}
    {:type :file :name name :content (first v)}))

(defn paths->tree
  "Builds a file-tree node from entries {:path relative-path-string
   :content text-or-nil}, the paths as given by the File API's
   webkitRelativePath. Directories sort before files."
  [entries]
  (let [nested (entries->nested entries)]
    ;; webkitRelativePath prefixes every path with the picked directory,
    ;; which thus becomes the root node.
    (if (= 1 (count nested))
      (let [[name v] (first nested)]
        (nested->node name v))
      (nested->node "/" nested))))
