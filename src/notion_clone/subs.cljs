(ns notion-clone.subs
  (:require
    ; [reagent.core :as r]
    [re-frame.core :as rf]))

;; --- Subscriptions ---

(rf/reg-sub
  :blocks
  (fn [db]
    (:blocks db)))
