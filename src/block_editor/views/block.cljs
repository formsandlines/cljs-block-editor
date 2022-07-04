(ns block-editor.views.block
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [react :as react]
    [block-editor.utils :as utils]
    [block-editor.views.editable :refer [editable-el]]
    ; ["react-contenteditable$default" :as ContentEditable]
    [block-editor.views.select-menu :refer [select-menu-el]]))


(defn block-blur
  [_ uid *state _]
  (let [{:keys [html-local tag-local]} @*state]
    (rf/dispatch [:blocks/update
                  {:uid uid :html html-local :tag tag-local}])
    (rf/dispatch [:editing/uid nil])))

(defn block-change
  [e _ *state elem]
  ; (js/console.log (.. e -target))
  (let [v (.. e -target -innerHTML)
        ; removes zero-width-char (from empty text node fix):
        html-local (utils/remove-zero-width-char v)]
    (swap! *state assoc :html-local html-local)
    (when (not= v html-local)
      ;; fixes caret position after zero-width-char removal:
      (r/after-render #(utils/set-caret-to elem :end)))
    ))

(defn block-keydown
  [e uid *state el]
  (let [{:keys [html-local]} @*state
        k    (.-key e)
        elem el ; ? remove  elem (.-target e)
        ]
    (case k
      "/" (swap! *state assoc :html-backup html-local)

      "Enter"
      (when (not (.-shiftKey e))
        (.preventDefault e)
        (let [{:keys [pos-start pos-end]} (utils/get-curr-selection)
              html-rem (apply str (take pos-start html-local))
              html-brk (apply str (drop pos-end html-local))]
          (swap! *state assoc :html-local html-rem)
          (rf/dispatch [:blocks/add
                        {:prev-uid uid
                         :local-data {:html html-brk}
                         :elem el}])))

      "Backspace"
      (when (or (== 0 (count html-local))
              (let [{:keys [pos-start pos-end]}
                    (utils/get-curr-selection)]
                (== 0 pos-start pos-end)))
        (.preventDefault e)
        (rf/dispatch [:blocks/delete
                      {:uid uid
                       :elem el}]))

      ("ArrowUp" "ArrowDown")
      (let [{cy1 :y1 cy2 :y2
             cx1 :x1} (utils/get-caret-bounds)
            cx-corr   (+ 1 cx1)
            edge-dist #(Math/abs
                         (- (->> elem utils/get-elem-bounds %2)
                            %1))
            style     (utils/get-comp-style elem)
            fs        (-> (.getPropertyValue style "font-size")
                          js/parseFloat)
            thr (/ fs 3.5)] ; arbitrary value -> needs more research!
        (case k
          "ArrowUp" (when (< (edge-dist cy1 :y1) thr)
                      (.preventDefault e)
                      (rf/dispatch [:blocks/cross
                                    {:elem el
                                     :dir :prev
                                     :caret-x cx-corr}]))
          "ArrowDown" (when (< (edge-dist cy2 :y2) thr)
                        (.preventDefault e)
                        (rf/dispatch [:blocks/cross
                                      {:elem el
                                       :dir :next
                                       :caret-x cx-corr}]))))

      nil) ;; default case
    (swap! *state assoc :prev-key k)))

(defn block-key-up
  [e _ *state _ open-select-menu]
  (when (= "/" (.-key e)) (open-select-menu)))

(defn block-el
  [{:keys [uid]}]
  (let [*state (r/atom {:html-backup nil :html-local nil
                        :tag-backup nil :tag-local nil
                        :prev-key ""
                        :select-menu-is-open false
                        :select-menu-position {:x nil :y nil}})
        *editing? (rf/subscribe [:editing/is-editing uid]) ;; ? needed?

        ;; ! select-menu triggers some weird editing behaviour after close
        close-select-menu
        (fn f []
          (swap! *state assoc
            :html-backup nil
            :select-menu-is-open false
            :select-menu-position {:x nil :y nil})
          (.removeEventListener js/document "click" f)
          (r/after-render #(utils/set-caret-to
                             nil ; ! how to get: (.. !ref -current)
                             :end)))

        open-select-menu
        (fn []
          (let [coords (utils/get-caret-coordinates)]
            (swap! *state assoc
              :select-menu-is-open true
              :select-menu-position coords)
            (.addEventListener js/document "click" close-select-menu)))

        tag-selection
        (fn [tag]
          (swap! *state assoc
            :tag-local tag
            :html-local (:html-backup @*state))
          (close-select-menu))]

    (fn [{:keys [html tag]}]
      (let [_ (when (not= html (:html-backup @*state))
                (swap! *state assoc :html-backup html :html-local html))
            _ (when (not= tag (:tag-backup @*state))
                (swap! *state assoc :tag-backup tag :tag-local tag))
            {:keys [html-local tag-local
                    select-menu-position
                    select-menu-is-open]} @*state]
        [:<>
         (when select-menu-is-open
           [select-menu-el
            {:position select-menu-position
             :on-select tag-selection
             :close close-select-menu}])
         [editable-el
          {:id (str uid)
           :class (str "Block" (when @*editing? " editing"))
           :tag-name tag-local
           :children (if (zero? (count html-local))
                       utils/zero-width-char html-local)
           :on-blur (fn [e elem] (block-blur e uid *state elem))
           :on-change (fn [e elem] (block-change e uid *state elem))
           :on-key-down (fn [e elem] (block-keydown e uid *state elem))
           :on-key-up (fn [e elem] (block-key-up e uid *state elem
                                     open-select-menu))}]]))))

