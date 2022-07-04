(ns block-editor.views.editable
  (:require
    [reagent.core :as r]
    [react :as react]
    [block-editor.utils :as utils]))


(defn editable-input
  [e handler]
  ;; TODO
  (handler e))

(defn editable-blur
  [e handler]
  ;; TODO
  (handler e))

(defn editable-keyup
  [e handler]
  ;; TODO
  (handler e))

(defn editable-keydown
  [e handler]
  ;; TODO
  (handler e))

(defn editable-el
  [{:keys [id classname tag-name children
           on-blur on-change on-key-down on-key-up]}]
  (let [] ;; state?
    [(or tag-name :div)
     {:class classname
      :id id
      :onInput (fn [e] (editable-input e on-change))
      :onBlur (fn [e] (editable-blur e on-blur))
      :onKeyUp (fn [e] (editable-keyup e on-key-up))
      :onKeyDown (fn [e] (editable-keydown e on-key-down))}
     children]))

