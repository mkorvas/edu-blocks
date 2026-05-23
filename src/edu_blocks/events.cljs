(ns edu-blocks.events
  (:require
    [re-frame.core :as rf]

    [edu-blocks.db :as db]
))

(rf/reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db
    }))
