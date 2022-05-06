(ns notion-clone.editable
  (:require
   [reagent.core :as r]
   [react :as react]
   [notion-clone.utils :refer [set-caret-to-end get-caret-coordinates]]
   ["react-contenteditable$default" :as ContentEditable]
   [notion-clone.select-menu :refer [select-menu]]))


(defn editable-block
  [{:keys [id html tag update-page! add-block! delete-block!]}]
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
                (add-block! {:id id
                             :!ref (.-current contentEditable)}))
              "Backspace"
              (when (empty? html)
                (.preventDefault e)
                (delete-block! {:id id
                                :!ref (.-current contentEditable)}))
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
           (update-page! {:id id :html html :tag tag})))

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

(defn editable-page [initial-blocks]
  (let [*blocks (r/atom initial-blocks)

        update-page-handler
        (fn [{:keys [id] :as local-data}]
          (let [i (.indexOf (mapv :id @*blocks) id)
                block (@*blocks i)
                updated-block (merge block local-data)]
            ;; updates only if block-state differs with new data
            (when-not (= block updated-block)
              (swap! *blocks assoc i updated-block))))

        add-block-handler
        (fn [{:keys [id !ref]}]
          (let [new-block {:id (random-uuid) :html "" :tag "p"}
                i (inc (.indexOf (mapv :id @*blocks) id))]
            (swap! *blocks #(into (subvec % 0 i)
                                  (cons new-block (subvec % i))))
            (r/after-render #(.. !ref -nextElementSibling focus))))

        delete-block-handler
        (fn [{:keys [id !ref]}]
          (when-let [previous-block (.. !ref -previousElementSibling)]
            (let [i (.indexOf (mapv :id @*blocks) id)]
              (swap! *blocks #(into (subvec % 0 i)
                                    (subvec % (inc i))))
              (r/after-render #(do (set-caret-to-end previous-block)
                                   (.focus previous-block))))))]

    (fn []
      [:div.Page
       (for [{:keys [id html tag]} @*blocks]
         ^{:key id}
         [editable-block
          {:id id
           :tag tag
           :html html
           :update-page! update-page-handler
           :add-block! add-block-handler
           :delete-block! delete-block-handler}])])))

