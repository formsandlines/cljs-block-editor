(ns notion-clone.subs
  (:require
    ; [reagent.core :as r]
    [re-frame.core :as rf]))

;; --- Subscriptions ---

(rf/reg-sub
  :blocks
  (fn [db]
    (:blocks db)))

(rf/reg-sub
  :blocks/block-by-uid
  (fn [_]
    (rf/subscribe [:blocks]))
  (fn [*blocks [_ uid]]
    (filter #(= uid (% :uid)) @*blocks)))

(rf/reg-sub
  :editing/uid
  (fn [db]
    (:editing/uid db)))

(rf/reg-sub
  :editing/is-editing
  (fn [_]
    [(rf/subscribe [:editing/uid])])
  (fn [[editing-uid] [_ uid]]
    (= editing-uid uid)))

