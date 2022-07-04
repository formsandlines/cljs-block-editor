(ns block-editor.views.editable
  (:require
    [reagent.core :as r]
    [react :as react]
    [block-editor.utils :as utils]))


(defn editable-input
  [e !ref handler]
  (handler e (when-let [r @!ref] r)))

(defn editable-blur
  [e !ref handler]
  (handler e (when-let [r @!ref] r)))

(defn editable-keyup
  [e !ref handler]
  (handler e (when-let [r @!ref] r)))

(defn editable-keydown
  [e !ref handler]
  (handler e (when-let [r @!ref] r)))

(defn editable-el
  [{:keys [id class tag-name children disabled
           on-blur on-change on-key-down on-key-up]}]
  (let [!ref (atom nil)]
    [(or tag-name :div)
     {:class class
      :id id
      :contentEditable (not disabled)
      :suppressContentEditableWarning true
      :ref (fn [el] (reset! !ref el))
      :onInput (fn [e] (editable-input e !ref on-change))
      :onBlur (fn [e] (editable-blur e !ref on-blur))
      :onKeyUp (fn [e] (editable-keyup e !ref on-key-up))
      :onKeyDown (fn [e] (editable-keydown e !ref on-key-down))
      }
     children]))

