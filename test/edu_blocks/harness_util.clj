(ns edu-blocks.harness-util
  "Runs an AST's emitted bash script with real bash and its emitted
   JavaScript with node (via test/harness.js, which mirrors the in-app
   sandbox), in a temp dir containing notes.txt."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [edu-blocks.bash :as bash]
            [edu-blocks.javascript :as js]))

(def notes-txt
  "Remember to water the plants.\nBuy milk.\nCall grandma.\n")

(def ^:private harness
  (.getPath (io/resource "harness.js")))

(defn run-both
  "Returns {:bash out :js out} on success, throws on execution errors."
  [ast]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "edu-blocks-test"
                      (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (spit (io/file dir "notes.txt") notes-txt)
      (spit (io/file dir "script.sh") (bash/emit ast))
      (spit (io/file dir "script.js") (js/emit ast))
      (let [b (sh "bash" "script.sh" :dir dir)
            j (sh "node" harness "script.js" :dir dir)]
        ;; A grep with no matches legitimately makes the script exit
        ;; non-zero, so judge bash by its stderr, not its exit code.
        (when (seq (:err b))
          (throw (ex-info (str "bash failed: " (:err b))
                          {:ast ast :result b})))
        (when-not (zero? (:exit j))
          (throw (ex-info (str "node failed: " (:err j))
                          {:ast ast :result j})))
        {:bash (:out b) :js (:out j)})
      (finally
        (doseq [f (reverse (file-seq dir))]
          (.delete f))))))
