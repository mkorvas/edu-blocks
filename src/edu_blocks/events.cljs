(ns edu-blocks.events
  (:require
    [re-frame.core :as rf]

    [edu-blocks.ast :as ast]
    [edu-blocks.db :as db]
    [edu-blocks.fs :as fs]
    [edu-blocks.javascript :as javascript]
    [edu-blocks.levels :as levels]
))

(rf/reg-event-fx
 ::initialize-db
 (fn [_ _]
   ;; ?unlock=all in the URL opens every level (for demonstrations).
   (let [unlock-all? (= "all" (.get (js/URLSearchParams. js/window.location.search)
                                    "unlock"))]
     {:db (cond-> db/default-db
            unlock-all? (assoc :unlocked levels/max-level))})))

(rf/reg-fx
 ::update-toolbox
 (fn [{:keys [ws level]}]
   (when ws
     (.updateToolbox ^js ws (clj->js (levels/toolbox-for level))))))

(rf/reg-event-fx
 ::ws-created
 (fn [{:keys [db]} [_ ws observer]]
   {:db (update db :bky assoc :ws ws :observer observer)
    :fx [[::update-toolbox {:ws ws :level (:level db)}]]}))

(rf/reg-event-fx
 ::set-level
 (fn [{:keys [db]} [_ level]]
   {:db (assoc db :level level :challenge-result nil)
    :fx [[::update-toolbox {:ws (get-in db [:bky :ws]) :level level}]]}))

(rf/reg-event-db
 ::ws-modified
 (fn [db [_ json]]
   (assoc-in db [:bky :json] json)))

(rf/reg-event-db
 ::ws-disposed
 (fn [db _]
   (update db :bky dissoc :ws :observer)))

(rf/reg-event-db
 ::set-lang
 (fn [db [_ lang]]
   (assoc db :lang lang)))

(rf/reg-event-db
 ::fs-uploaded
 (fn [db [_ paths]]
   (assoc db :fs-tree (fs/paths->tree paths))))

(defn- file-text
  "The content of the file at path in the tree; throws a bash-styled
   error when it cannot be read."
  [tree cmd path]
  (let [node (fs/resolve-path tree path)]
    (cond
      (nil? node)
      (throw (js/Error. (str cmd ": " path ": No such file or directory")))
      (= :dir (:type node))
      (throw (js/Error. (str cmd ": " path ": Is a directory")))
      (nil? (:content node))
      (throw (js/Error. (str cmd ": " path ": cannot read a binary file")))
      :else (:content node))))

(defn eval-js
  "Evaluates JS code with console.log and process.stdout.write redirected
   into a buffer stack (so nested captures for $(...) get their own),
   and with command helpers (cat, head, grep, seq) backed by the virtual
   file tree; returns the outer buffer once execution ends."
  [code fs-tree]
  (let [bufs    (atom [""])
        append! (fn [s] (swap! bufs update (dec (count @bufs)) str s))
        console #js {:log (fn [x] (append! (str x "\n")))}
        process #js {:stdout #js {:write (fn [x] (append! x))}}
        cat     (fn [path] (file-text fs-tree "cat" path))
        head    (fn [n text] (fs/head-text n text))
        grep    (fn [pattern text] (fs/grep-text pattern text))
        seq*    (fn [from to] (fs/seq-text from to))
        capture (fn [f]
                  (swap! bufs conj "")
                  (try
                    (f)
                    (peek @bufs)
                    (finally
                      (swap! bufs pop))))]
    (try
      ((js/Function. "console" "process" "cat" "head" "grep" "seq" "capture" code)
       console process cat head grep seq* capture)
      (catch :default e
        (append! (str "Error: " (ex-message e)))))
    (first @bufs)))

(rf/reg-fx
 ::eval-js
 (fn [{:keys [code fs-tree]}]
   (rf/dispatch [::script-ran (eval-js code fs-tree)])))

(rf/reg-event-fx
 ::run-script
 (fn [{:keys [db]} _]
   (when-let [code (some-> (get-in db [:bky :json])
                           ast/bky-ws->ast
                           javascript/emit)]
     {:fx [[::eval-js {:code code :fs-tree (:fs-tree db)}]]})))

(rf/reg-event-db
 ::script-ran
 (fn [db [_ output]]
   (assoc db :exec-output output)))

(rf/reg-event-db
 ::check-challenge
 (fn [db _]
   ;; Runs against the built-in sample tree (not an uploaded one), so
   ;; file-based challenges stay deterministic.
   (let [level     (:level db)
         challenge (:challenge (levels/level level))
         ast       (some-> (get-in db [:bky :json]) ast/bky-ws->ast)
         output    (some-> ast javascript/emit (eval-js db/sample-fs-tree))
         status    (if (nil? ast)
                     :no-script
                     (levels/check-challenge challenge ast output))]
     (cond-> (assoc db
                    :challenge-result {:level level :status status}
                    :exec-output output)
       (= :passed status)
       (update :unlocked max (min levels/max-level (inc level)))))))

(defn bky-js->clj [bky]
  (cond
    (array? bky)
    (mapv bky-js->clj bky)
    (string? bky)
    bky
    (nil? bky)
    bky
    (seq (js/Object.keys bky))
    (into {} (map (fn [[k v]] [(keyword k) (bky-js->clj v)])
                  (js/Object.entries bky)))
    :else bky))
