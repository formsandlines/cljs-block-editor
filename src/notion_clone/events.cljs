(ns notion-clone.events
  (:require
    ; [reagent.core :as r]
    [re-frame.core :as rf]))

;; ---- Interceptors --------------------------------------------

(def get-prev-elem!
  (rf/->interceptor
    :id     :get-prev-elem!
    :before (fn [{:keys [coeffects] :as context}]
              (let [!ref (-> coeffects :event second :!ref)]
                (assoc-in context [:coeffects :prev-elem]
                          (.. !ref -previousElementSibling))))))

;; ---- Event handler -------------------------------------------

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    {:blocks [{ :id (random-uuid) :html "This is an h3 block" :tag "h3" }
              { :id (random-uuid) :html "This is a p block" :tag "p" }]}))

(rf/reg-event-db
  :blocks/update
  (fn [db [_ {:keys [id] :as local-data}]]
    (let [blocks (:blocks db)
          i (.indexOf (mapv :id blocks) id)
          block (blocks i)
          updated-block (merge block local-data)]
      ;; updates only if block-state differs with new data
      (when-not (= block updated-block)
        (assoc-in db [:blocks i] updated-block)))))

(rf/reg-event-fx
  :blocks/add
  (fn [{:keys [db]} [_ {:keys [id !ref]}]]
    (let [blocks (:blocks db)
          new-block {:id (random-uuid) :html "" :tag "p"}
          i (inc (.indexOf (mapv :id blocks) id))]
      {:db (update db :blocks #(into (subvec % 0 i)
                                     (cons new-block (subvec % i))))
       :focus-next! !ref})))

(rf/reg-event-fx
  :blocks/delete
  [get-prev-elem!]
  (fn [{:keys [db prev-elem]} [_ {:keys [id]}]]
    (let [blocks (:blocks db)]
      (when [prev-elem]
        (let [i (.indexOf (mapv :id blocks) id)]
          {:db (update db :blocks #(into (subvec % 0 i)
                                         (subvec % (inc i))))
           :set-caret-to-end! prev-elem})))))

