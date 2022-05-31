(ns notion-clone.views
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [react :as react]
    [notion-clone.utils :refer [set-caret-to-end get-caret-coordinates]]
    ["react-contenteditable$default" :as ContentEditable]
    [notion-clone.select-menu :refer [select-menu]]))


(defn editable-block
  [{:keys [id html tag]}]
  (let [*state (r/atom {:html-backup nil :html html :tag tag
                        :prev-key ""
                        :select-menu-is-open false
                        :select-menu-position {:x nil :y nil}})
        update-state! #(apply swap! *state assoc %&)
        contentEditable (react/createRef)

        on-change-handler
        #(update-state! :html (.. % -target -value))

        on-key-down-handler
        (fn [e]
          (let [{:keys [html prev-key]} @*state
                k (.-key e)]
            (case k
              "/" (update-state! :html-backup html)
              "Enter"
              (when (not= prev-key "Shift")
                (.preventDefault e)
                (rf/dispatch [:blocks/add
                              {:id id
                               :!ref (.-current contentEditable)}]))
              "Backspace"
              (when (empty? html)
                (.preventDefault e)
                (rf/dispatch [:blocks/delete
                              {:id id
                               :!ref (.-current contentEditable)}]))
              nil)
            (update-state! :prev-key k)))

        close-select-menu-handler
        (fn f []
          (update-state! :html-backup nil
                         :select-menu-is-open false
                         :select-menu-position {:x nil :y nil})
          (.removeEventListener js/document "click" f)
          ;; put set-caret-to-end here instead of in tag-selection-handler
          (r/after-render #(set-caret-to-end (.. contentEditable -current))))

        open-select-menu-handler
        (fn []
          (let [coords (get-caret-coordinates)]
            (update-state! :select-menu-is-open true
                           :select-menu-position coords)
            (.addEventListener js/document "click"
                               close-select-menu-handler)))

        on-key-up-handler
        #(when (= "/" (.-key %)) (open-select-menu-handler))

        tag-selection-handler
        (fn [tag]
          (update-state! :tag tag :html (:html-backup @*state))
          (close-select-menu-handler))] 

    (r/create-class
      {:display-name "editable-block"

       ;; no :component-did-mount needed, see outer let binding!

       :component-did-update
       (fn []
         (let [{:keys [html tag]} @*state]
           (rf/dispatch [:blocks/update
                         {:id id :html html :tag tag}])))

       :reagent-render
       (fn []
         (let [{:keys [html tag
                       select-menu-position
                       select-menu-is-open]} @*state]
           [:<>
            (when select-menu-is-open
              [select-menu
               {:position select-menu-position
                :on-select tag-selection-handler
                :close close-select-menu-handler}])
            [:> ContentEditable
             {:class "Block"
              :inner-ref contentEditable
              :html html
              :tag-name tag
              :on-change on-change-handler
              :on-key-down on-key-down-handler
              :on-key-up on-key-up-handler}]]))})))


(defn editable-page []
  (let [*blocks (rf/subscribe [:blocks])]
    (fn []
      [:div.Page
       (for [{:keys [id html tag]} @*blocks]
         ^{:key id}
         [editable-block
          {:id id
           :tag tag
           :html html}])])))


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
   [editable-page]])


