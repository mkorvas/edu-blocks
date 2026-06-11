(ns edu-blocks.subs
  (:require
   [re-frame.core :as rf]
   [edu-blocks.bash :as bash]))

(rf/reg-sub
 ::bash-output
 (fn [db _]
   (some-> (get-in db [:bky :json])
           bash/bky-ws->ast
           bash/emit)))
