(ns block-editor.views.block-page
  (:require
    [re-frame.core :as rf]
    [block-editor.views.block :refer [block-el]]))


(defn block-page-el []
  (let [*blocks (rf/subscribe [:blocks])]
    (fn []
      [:div.Page
       (for [{:keys [uid html tag]} @*blocks]
         ^{:key uid}
         [block-el
          {:uid uid
           :tag tag
           :html html}])])))
