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

(defn resolve-path
  "Returns the node at path in the tree, or nil. Tolerates a leading
   slash and the root directory's name as the first path segment."
  [tree path]
  (let [segs (remove str/blank? (str/split path #"/"))
        segs (if (= (first segs) (:name tree)) (rest segs) segs)]
    (reduce (fn [node seg]
              (some #(when (= seg (:name %)) %) (:children node)))
            tree
            segs)))

(defn- lines->text [lines]
  (if (seq lines)
    (str (str/join "\n" lines) "\n")
    ""))

(defn head-text
  "The first n lines of text, as text."
  [n text]
  (if (= text "")
    ""
    (lines->text (take n (str/split-lines text)))))

(defn grep-text
  "The lines of text matching the regex pattern, as text."
  [pattern text]
  (lines->text (filter #(re-find (re-pattern pattern) %)
                       (str/split-lines text))))

(defn tail-text
  "The last n lines of text, as text."
  [n text]
  (if (= text "")
    ""
    (lines->text (take-last n (str/split-lines text)))))

(defn wc-l-text
  "What wc -l prints for text: the number of newline characters."
  [text]
  (str (count (re-seq #"\n" text)) "\n"))

(defn sort-text
  "The lines of text sorted (byte order, like LC_ALL=C sort), as text."
  [text]
  (if (= text "")
    ""
    (lines->text (sort (str/split-lines text)))))

(defn uniq-text
  "Text with repeated adjacent lines collapsed, like uniq."
  [text]
  (if (= text "")
    ""
    (lines->text (dedupe (str/split-lines text)))))

(defn seq-text
  "What seq(1) prints for the inclusive range from..to."
  [from to]
  (lines->text (range from (inc to))))

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
