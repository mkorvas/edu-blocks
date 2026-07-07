(ns edu-blocks.db)

; A node of the virtual file tree is either
;   {:type :file :name "..."} or
;   {:type :dir  :name "..." :children [node ...]}.
(def sample-fs-tree
  {:type :dir :name "/"
   :children
   [{:type :dir :name "home"
     :children
     [{:type :dir :name "alice"
       :children
       [{:type :dir :name "photos"
         :children [{:type :file :name "cat.jpg"}
                    {:type :file :name "dog.jpg"}]}
        {:type :file :name "notes.txt"}
        {:type :file :name "todo.md"}]}
      {:type :dir :name "bob"
       :children [{:type :file :name "script.sh"}]}]}
    {:type :dir :name "tmp"
     :children [{:type :file :name "scratch.txt"}]}]})

(def default-db

  {:lang :bash ; the output language, :bash or :js
   :exec-output nil ; captured output of the last script run
   :fs-tree sample-fs-tree ; virtual file tree shown in the files panel
   :bky {
        ; :ws -- Blockly Workspace object
        ; :json -- serialized repr of the Workspace as keywordized cljs data, may be nil
        ; :observer -- ResizeObserver for the workspace container
        }
  })
