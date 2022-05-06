(ns notion-clone.core
  (:require
   [reagent.dom :as d]
   [notion-clone.editable :refer [editable-page]]))


(defonce initial-blocks [{ :id (random-uuid) :html "" :tag "p" }])

(defn root []
  [:<>
   [:h1.Logo
    "notion.clone in Reagent"]
   [:p.Intro
    "Helloo "
    [:span.Emoji {:role "img" :aria-label "greetings"} "👋"]
    " You can add content below. Type "
    [:span.Code "/"]
    " to see available elements."]
   [editable-page initial-blocks]])

(defn mount-root []
  (d/render [root initial-blocks]
            (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))

