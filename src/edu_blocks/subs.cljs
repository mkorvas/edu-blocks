(ns edu-blocks.subs
  (:require
   [re-frame.core :as rf]
   [edu-blocks.ast :as ast]
   [edu-blocks.bash :as bash]
   [edu-blocks.javascript :as javascript]))

(def emitters
  {:bash bash/emit
   :js   javascript/emit})

(rf/reg-sub
 ::lang
 (fn [db _]
   (:lang db)))

(rf/reg-sub
 ::exec-output
 (fn [db _]
   (:exec-output db)))

(rf/reg-sub
 ::fs-tree
 (fn [db _]
   (:fs-tree db)))

(rf/reg-sub
 ::code-output
 (fn [db _]
   (let [emit (emitters (:lang db) bash/emit)]
     (some-> (get-in db [:bky :json])
             ast/bky-ws->ast
             emit))))
