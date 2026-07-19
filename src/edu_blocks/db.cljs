(ns edu-blocks.db)

; A node of the virtual file tree is either
;   {:type :file :name "..." :content text-or-nil} or
;   {:type :dir  :name "..." :children [node ...]}.
; A nil :content means the content is not available (binary file).
(def sample-fs-tree
  {:type :dir :name "/"
   :children
   [{:type :dir :name "home"
     :children
     [{:type :dir :name "alice"
       :children
       [{:type :dir :name "photos"
         :children [{:type :file :name "cat.jpg" :content nil}
                    {:type :file :name "dog.jpg" :content nil}]}
        {:type :file :name "notes.txt"
         :content "Remember to water the plants.\nBuy milk.\nCall grandma.\n"}
        {:type :file :name "todo.md"
         :content "# TODO\n- learn bash\n- sort the photos\n"}]}
      {:type :dir :name "bob"
       :children [{:type :file :name "script.sh"
                   :content "#!/bin/bash\necho \"Hello from Bob\"\n"}]}]}
    {:type :dir :name "tmp"
     :children [{:type :file :name "scratch.txt"
                 :content "just scribbles\n"}]}]})

(def default-db

  {:lang :bash ; the output language, :bash or :js
   :level 1 ; current level, gating which blocks the toolbox offers
   :unlocked 1 ; highest level reached by passing challenges
   :challenge-result nil ; {:level n :status :passed/:wrong-output/...}
   :exec-output nil ; captured output of the last script run
   :fs-tree sample-fs-tree ; virtual file tree shown in the files panel
   :bky {
        ; :ws -- Blockly Workspace object
        ; :json -- serialized repr of the Workspace as keywordized cljs data, may be nil
        ; :observer -- ResizeObserver for the workspace container
        }
  })
