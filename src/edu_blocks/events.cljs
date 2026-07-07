(ns edu-blocks.events
  (:require
    [re-frame.core :as rf]

    [edu-blocks.ast :as ast]
    [edu-blocks.db :as db]
    [edu-blocks.fs :as fs]
    [edu-blocks.javascript :as javascript]
))

(rf/reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db
    }))

(rf/reg-event-db
 ::ws-created
 (fn [db [_ ws observer]]
   (update db :bky assoc :ws ws :observer observer)))

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
   into a buffer, and with command helpers (cat, head, grep, seq) backed
   by the virtual file tree; returns the captured output."
  [code fs-tree]
  (let [buf     (atom "")
        console #js {:log (fn [x] (swap! buf str x "\n"))}
        process #js {:stdout #js {:write (fn [x] (swap! buf str x))}}
        cat     (fn [path] (file-text fs-tree "cat" path))
        head    (fn [n path] (fs/head-text n (file-text fs-tree "head" path)))
        grep    (fn [pattern path]
                  (fs/grep-text pattern (file-text fs-tree "grep" path)))
        seq*    (fn [from to] (fs/seq-text from to))]
    (try
      ((js/Function. "console" "process" "cat" "head" "grep" "seq" code)
       console process cat head grep seq*)
      (catch :default e
        (swap! buf str "Error: " (ex-message e))))
    @buf))

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
