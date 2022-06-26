(ns notion-clone.effects
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [notion-clone.utils :as utils]))


(rf/reg-fx
  :focus-next!
  (fn [!ref]
    (r/after-render #(.. !ref -nextElementSibling focus))))

(rf/reg-fx
  :set-caret-to!
  (fn [[elem pos]]
    (let [pos (if (zero? (count (utils/remove-zero-width-char
                                  (.. elem -innerText))))
                :start ; if elem.childNodes[0] is null (no text)
                pos)]
      (js/setTimeout #(utils/set-caret-to elem pos) 100))))

(rf/reg-fx
  :set-caret-from-coords!
  (fn [[x y]]
    (js/setTimeout #(utils/set-caret-from-coords x y) 50)))

