(ns block-editor.core
  (:require
    [reagent.dom :as d]
    [re-frame.core :as rf]
    [block-editor.events :as events]
    [block-editor.subs :as subs]
    [block-editor.effects :as effects]
    [block-editor.views.block-page :refer [block-page-el]]))

(defn root-el []
  [:<>
   [:h1.Logo
    "Block-based RTE"]
   [:p.Intro
    [:span.Code "/"]
    " to see available elements."]
   [block-page-el]])

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (d/render [root-el]
            (.getElementById js/document "app")))

(defn ^:export init! []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))

