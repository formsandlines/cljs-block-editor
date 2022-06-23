(ns notion-clone.views
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [react :as react]
    [notion-clone.utils :as utils]
    ["react-contenteditable$default" :as ContentEditable]
    [notion-clone.select-menu :refer [select-menu]]))


(defn editable-block
  [{:keys [uid]}]
  (let [*state (r/atom {:html-backup nil :html-local nil
                        :tag-backup nil :tag-local nil
                        :prev-key ""
                        :select-menu-is-open false
                        :select-menu-position {:x nil :y nil}})
        *editing? (rf/subscribe [:editing/is-editing uid]) ;; ? needed?
        update-state! #(apply swap! *state assoc %&)
        contentEditable (react/createRef)

        on-focus-handler
        (fn [_] nil)
        ; #(rf/dispatch [:editing/uid uid]) ;; triggers additional rerender!

        on-blur-handler
        (fn [_]
          (let [{:keys [html-local tag-local]} @*state]
            (rf/dispatch [:blocks/update
                          {:uid uid :html html-local :tag tag-local}])
            (rf/dispatch [:editing/uid nil])))

        on-change-handler
        (fn [e]
          (let [v (.. e -target -value)
                ; removes zero-width-char (from empty text node fix):
                html-local (utils/remove-zero-width-char v)
                ; ? how to set caret to the right position after removing zero-width-char?
                ; _ (js/console.log "before: " (utils/includes-zero-width-char v) "after: " (utils/includes-zero-width-char html-local))
                ]
            (update-state! :html-local html-local)
            (if (not= v html-local)
              ;; fixes caret position after zero-width-char removal:
              (r/after-render #(utils/set-caret-to
                                 (.-current contentEditable) :end))))) 

        on-key-down-handler
        (fn [e]
          (let [{:keys [html-local prev-key]} @*state
                k    (.-key e)
                elem (.-target e)]
            ; (js/console.log e)
            (case k
              "/" (update-state! :html-backup html-local)

              "Enter"
              (when (not (.-shiftKey e))
                ; (and (not (.-shiftKey e))
                ;          (not= prev-key "Shift"))
                (.preventDefault e)
                (let [{:keys [pos-start pos-end]} (utils/get-curr-selection)
                      ; ! correct offset to include HTML
                      html-rem (apply str (take pos-start html-local))
                      html-brk (apply str (drop pos-end html-local))]
                  ; (println "pos-start: "pos-start", pos-end: "pos-end)
                  ; (println "html local: "html-local)
                  ; (println "rem: "html-rem)
                  ; (println "brk: "html-brk)
                  (update-state! :html-local html-rem)
                  (rf/dispatch [:blocks/add
                                {:prev-uid uid
                                 :local-data {:html html-brk}
                                 :!ref (.-current contentEditable)}])))

              "Backspace"
              (when (or (== 0 (count html-local))
                        (let [{:keys [pos-start pos-end]}
                              (utils/get-curr-selection)]
                          (== 0 pos-start pos-end)))
                (.preventDefault e)
                ;; ! BUG on set-caret-to with large multi-line blocks
                ;; "DOMException: Failed to execute 'setStart' on 'Range': The offset x is larger than the node's length y"
                (rf/dispatch [:blocks/delete
                              {:uid uid
                               :!ref (.-current contentEditable)}]))

              ("ArrowUp" "ArrowDown")
              (let [{cy1 :y1 cy2 :y2
                     cx1 :x1 cx2 :x2} (utils/get-caret-bounds) 
                    cx-corr   (+ 1 cx1) ; + 1
                    edge-dist #(Math/abs
                                 (- (->> elem utils/get-elem-bounds %2)
                                    %1))
                    style     (utils/get-comp-style elem) 
                    fs        (-> (.getPropertyValue style "font-size")
                                  js/parseFloat)
                    thr (/ fs 3.5)] ; 1.99 3.5 not accurate -> needs more research!
                ; (println k)
                ; (println "bounds: " cy1 "," cy2)
                ; (println "thr: " thr)
                ; (println "dist up: " (edge-dist cy1 :y1))
                ; (println "dist down: " (edge-dist cy2 :y2))
                (case k
                  "ArrowUp" (when (< (edge-dist cy1 :y1) thr)
                              (.preventDefault e)
                              (rf/dispatch [:blocks/cross
                                            {:!ref (.-current
                                                     contentEditable)
                                             :dir :prev
                                             :caret-x cx-corr}]))
                  "ArrowDown" (when (< (edge-dist cy2 :y2) thr)
                                (.preventDefault e)
                                (rf/dispatch [:blocks/cross
                                              {:!ref (.-current
                                                       contentEditable)
                                               :dir :next
                                               :caret-x cx-corr}]))))

              nil)
            (update-state! :prev-key k)))

        close-select-menu-handler
        (fn f []
          (update-state! :html-backup nil
                         :select-menu-is-open false
                         :select-menu-position {:x nil :y nil})
          (.removeEventListener js/document "click" f)
          ;; put set-caret-to-end here instead of in tag-selection-handler
          (r/after-render #(utils/set-caret-to
                             (.. contentEditable -current) :end)))

        open-select-menu-handler
        (fn []
          (let [coords (utils/get-caret-coordinates)]
            (update-state! :select-menu-is-open true
                           :select-menu-position coords)
            (.addEventListener js/document "click"
                               close-select-menu-handler)))

        on-key-up-handler
        #(when (= "/" (.-key %)) (open-select-menu-handler))

        tag-selection-handler
        (fn [tag]
          (update-state! :tag-local tag :html-local (:html-backup @*state))
          (close-select-menu-handler))] 

    (r/create-class
      {:display-name "editable-block"

       ;; no :component-did-mount needed, see outer let binding!

       ; :component-did-update
       ; (fn []
       ;   (let [{:keys [html tag]} @*state]
       ;     (rf/dispatch [:blocks/update
       ;                   {:uid uid :html html :tag tag}])))

       :reagent-render
       (fn [{:keys [html tag]}]
         (let [_ (when (not= html (:html-backup @*state))
                   (update-state! :html-backup html :html-local html))
               _ (when (not= tag (:tag-backup @*state))
                   (update-state! :tag-backup tag :tag-local tag))
               {:keys [html-local tag-local
                       select-menu-position
                       select-menu-is-open]} @*state]
           [:<>
            (when select-menu-is-open
              [select-menu
               {:position select-menu-position
                :on-select tag-selection-handler
                :close close-select-menu-handler}])
            [:> ContentEditable
             {:id (str uid)
              :class (str "Block" (when @*editing? " editing"))
              :inner-ref contentEditable
              :html (if (zero? (count html-local))
                      ;; fixes caret position
                      utils/zero-width-char html-local)
              :tag-name tag-local
              :on-focus on-focus-handler
              :on-blur on-blur-handler
              :on-change on-change-handler
              :on-key-down on-key-down-handler
              :on-key-up on-key-up-handler}]]))})))

(defn editable-page []
  (let [*blocks (rf/subscribe [:blocks])]
    (fn []
      [:div.Page
       (for [{:keys [uid html tag]} @*blocks]
         ^{:key uid}
         [editable-block
          {:uid uid
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


