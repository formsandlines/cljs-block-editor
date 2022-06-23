(ns notion-clone.events
  (:require
    ; [reagent.core :as r]
    [re-frame.core :as rf]
    [notion-clone.utils :as utils]))

;; ---- Interceptors --------------------------------------------

(def get-prev-elem!
  (rf/->interceptor
    :id     :get-prev-elem!
    :before (fn [context]
              (let [{:keys [event]} (:coeffects context)
                    !ref (-> event second :!ref)]
                (assoc-in context [:coeffects :prev-elem]
                          (.. !ref -previousElementSibling))))))

(def get-adjacent-elem!
  (rf/->interceptor
    :id     :get-adjacent-elem!
    :before (fn [context]
              (let [{:keys [event]} (:coeffects context)
                    !ref (-> event second :!ref)
                    dir  (-> event second :dir)]
                (assoc-in context [:coeffects :adjacent-elem]
                          (case dir
                            :prev (.. !ref -previousElementSibling)
                            :next (.. !ref -nextElementSibling)))))))

;; ---- Event handler -------------------------------------------

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    {:blocks [{ :uid (random-uuid) :html "This is an h3 block" :tag "h3" }
              { :uid (random-uuid) :html "This is a p block" :tag "p" }]
     :editing/uid nil}))

(rf/reg-event-db
  :blocks/update
  (fn [db [_ {:keys [uid] :as local-data}]]
    (let [blocks (:blocks db)
          i (.indexOf (mapv :uid blocks) uid)
          block (blocks i)
          updated-block (merge block local-data)]
      ;; updates only if block-state differs with new data
      (when-not (= block updated-block)
        (assoc-in db [:blocks i] updated-block)))))

(rf/reg-event-fx
  :blocks/add
  (fn [{:keys [db]} [_ {:keys [prev-uid local-data !ref]}]]
    (let [blocks (:blocks db)
          new-block (merge {:uid (random-uuid) :html "" :tag "p"}
                           local-data)
          i (inc (.indexOf (mapv :uid blocks) prev-uid))]
      {:db (update db :blocks #(into (subvec % 0 i)
                                     (cons new-block (subvec % i))))
       :focus-next! !ref})))

(rf/reg-event-fx
  :blocks/delete
  [get-prev-elem!]
  (fn [{:keys [db prev-elem]} [_ {:keys [uid !ref]}]]
    (let [blocks (:blocks db)]
      (when prev-elem
        (let [i (.indexOf (mapv :uid blocks) uid)
              prev-uid  (uuid (.. prev-elem -id))
              prev-html (utils/remove-zero-width-char
                          (.. prev-elem -innerHTML))
              prev-html-merged (str prev-html
                                    (utils/remove-zero-width-char
                                      (.. !ref -innerHTML)))]
          ; (js/console.log (str "prev: '" (-> prev-html utils/html->text) "'"))
          {:db (update db :blocks #(into (subvec % 0 i)
                                         (subvec % (inc i))))
           :fx [[:dispatch [:blocks/update
                            {:uid prev-uid :html prev-html-merged}]]
                [:set-caret-to! [prev-elem (-> prev-html
                                               utils/html->text
                                               count)]]]})))))

(rf/reg-event-fx
  :blocks/cross
  [get-adjacent-elem!]
  (fn [{:keys [adjacent-elem]} [_ {:keys [dir caret-x]}]]
    (when adjacent-elem
      (when-let [{:keys [y1 y2]} (utils/get-elem-bounds adjacent-elem)]
        {:fx [[:set-caret-from-coords!
               [caret-x (case dir
                          :prev (- y2 4) ; 10 10, 4 4
                          :next (+ y1 4))]]]}))))

(rf/reg-event-db
  :editing/uid
  (fn [db [_ uid]]
    (assoc db :editing/uid uid)))

