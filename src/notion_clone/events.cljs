(ns notion-clone.events
  (:require
    [re-frame.core :as rf]))

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    {:name "here be data"}))

