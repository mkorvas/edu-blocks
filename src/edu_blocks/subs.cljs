(ns edu-blocks.subs
  (:require
   [re-frame.core :as rf]
   [edu-blocks.ast :as ast]
   [edu-blocks.bash :as bash]))

(rf/reg-sub
 ::bash-output
 (fn [db _]
   (some-> (get-in db [:bky :json])
           ast/bky-ws->ast
           bash/emit)))
