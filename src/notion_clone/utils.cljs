(ns notion-clone.utils)

(defn set-caret-to-end [el]
  (let [rng (.createRange js/document)
        sel (.getSelection js/window)]
    (.selectNodeContents rng el)
    (.collapse rng false)
    (.removeAllRanges sel)
    (.addRange sel rng)
    (.focus el)))

(defn get-caret-coordinates []
  (let [sel (.getSelection js/window)]
    (when-not (== 0 (.. sel -rangeCount))
      (let [rng (.. sel (getRangeAt 0) cloneRange)
            _   (.collapse rng false)
            rect (aget (.getClientRects rng) 0)]
        (when (some? rect)
          {:x (.. rect -left) :y (.. rect -top)})))))
