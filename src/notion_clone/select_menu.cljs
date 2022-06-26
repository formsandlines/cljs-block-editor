(ns notion-clone.select-menu
  (:require 
    [reagent.core :as r]
    ["match-sorter" :refer (matchSorter)]))


(defonce menu-height 150)

(def allowed-tags [{:id "page-title"
                    :tag "h1"
                    :label "Page Title"}
                   {:id "heading"
                    :tag "h2"
                    :label "Heading"}
                   {:id "subheading"
                    :tag "h3"
                    :label "Subheading"}
                   {:id "paragraph"
                    :tag "p"
                    :label "Paragraph"}])

(defn select-menu-key-down
  [e *state on-select close]
  (let [{:keys [items selected-item command]} @*state
        k (.-key e)]
    (case k
      "Escape" (close)
      "Enter"
      (do (.preventDefault e)
          (on-select (-> items (aget selected-item) .-tag)))
      "Backspace"
      (do (when (empty? command) (close))
          (swap! *state assoc
            :command (subs command 0 (-> command count dec))))
      "ArrowUp"
      (do (.preventDefault e)
          (let [prev-selected (if (== 0 selected-item)
                                (-> items count dec)
                                (-> selected-item dec))]
            (swap! *state assoc :selected-item prev-selected)))
      ("ArrowDown" "Tab")
      (do (.preventDefault e)
          (let [next-selected (if (== (-> items count dec)
                                      selected-item)
                                0
                                (-> selected-item inc))]
            (swap! *state assoc :selected-item next-selected)))
      (swap! *state assoc :command (str command k)))

    ;; update items here instead of in component-did-update
    (r/after-render
      #(let [{new-command :command} @*state]
         (when-not (= command new-command)
           (swap! *state assoc
             :items (matchSorter (clj->js allowed-tags)
                      new-command
                      (js-obj "keys" (array "tag")))))))))

(defn select-menu-el [{:keys [position on-select close]}]
  (let [*state (r/atom {:command ""
                        :items (clj->js allowed-tags) ; managed by matchSorter
                        :selected-item 0})
        position-attributes {:top  (- (:y position) menu-height)
                             :left (:x position)}
        key-down-handler (fn [e] (select-menu-key-down
                                   e *state on-select close))]

    (r/create-class
      {:display-name "SelectMenu"

       :component-did-mount
       #(.addEventListener js/document "keydown" key-down-handler)

       :component-will-unmount
       #(.removeEventListener js/document "keydown" key-down-handler)

       :reagent-render
       (fn []
         [:div.SelectMenu
          {:style position-attributes}

          (let [{:keys [items selected-item]} @*state
                items-clj (js->clj items :keywordize-keys true)]
            (into [:div.Items]
              (map (fn [{:keys [id tag label] :as item}]
                     (let [is-selected (== (.indexOf items-clj item)
                                           selected-item)]
                       [:div
                        {:class (str "Item" (when is-selected " Selected"))
                         :id id
                         :role "button"
                         :tab-index "0"
                         :on-click #(on-select tag)}
                        label]))
                items-clj)))])})))

