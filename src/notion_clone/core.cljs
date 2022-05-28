(ns notion-clone.core
  (:require
    [reagent.dom :as d]
    [re-frame.core :as rf]
    [notion-clone.events :as events]
    [notion-clone.subs :as subs]
    [notion-clone.editable :refer [editable-page]]))


(defonce initial-blocks [{ :id (random-uuid) :html "" :tag "p" }])

(defn rf-test []
  (let [name (rf/subscribe [::subs/name])]
    [:div @name]))

(defn root []
  [:<>
   [:h1.Logo
    "notion.clone in Reagent & re-frame"]
   [:p.Intro
    "Hello "
    [:span.Emoji {:role "img" :aria-label "greetings"} "👋"]
    " You can add content below. Type "
    [:span.Code "/"]
    " to see available elements."]
   [rf-test]
   [editable-page initial-blocks]])

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (d/render [root initial-blocks]
            (.getElementById js/document "app")))

(defn ^:export init! []
  (rf/dispatch-sync [::events/initialize-db])
  (mount-root))


(comment
  
  ,)
