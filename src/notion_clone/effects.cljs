(ns notion-clone.effects
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [notion-clone.utils :refer [set-caret-to-end]]))


(rf/reg-fx
  :focus-next!
  (fn [!ref]
    (r/after-render #(.. !ref -nextElementSibling focus))))

(rf/reg-fx
  :set-caret-to-end!
  (fn [prev-elem]
    (r/after-render #(set-caret-to-end prev-elem))))

