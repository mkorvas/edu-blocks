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
