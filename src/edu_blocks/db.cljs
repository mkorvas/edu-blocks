(ns edu-blocks.db)

(def default-db

  {:lang :bash ; the output language, :bash or :js
   :exec-output nil ; captured output of the last script run
   :bky {
        ; :ws -- Blockly Workspace object
        ; :json -- serialized repr of the Workspace as keywordized cljs data, may be nil
        ; :observer -- ResizeObserver for the workspace container
        }
  })
