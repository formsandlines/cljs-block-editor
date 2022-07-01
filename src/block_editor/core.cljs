(ns block-editor.core
  (:require
    [reagent.dom :as d]
    [re-frame.core :as rf]
    [block-editor.events :as events]
    [block-editor.subs :as subs]
    [block-editor.effects :as effects]
    [block-editor.views :refer [root-el]]))


(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (d/render [root-el]
            (.getElementById js/document "app")))

(defn ^:export init! []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))

