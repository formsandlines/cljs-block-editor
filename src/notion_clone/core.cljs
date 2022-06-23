(ns notion-clone.core
  (:require
    [reagent.dom :as d]
    [re-frame.core :as rf]
    [notion-clone.events :as events]
    [notion-clone.subs :as subs]
    [notion-clone.effects :as effects]
    [notion-clone.views :as views]))


(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (d/render [views/root]
            (.getElementById js/document "app")))

(defn ^:export init! []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))


(comment

 
  ,)
