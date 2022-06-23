(ns notion-clone.utils
  (:require [clojure.string :as string]))


(defn- text-node?
  [node]
  (== 3 (.. node -nodeType)))

(defn html->text
  [html]
  (string/replace html #"\&nbsp\;" " "))

; (defn text->html
;   [text]
;   nil)

(defn set-caret-to 
  "Sets the caret to `pos` in `el` (assumed to be a contentEditable). 
  - `pos` can be :start/:end (first/last position in `el`) or an integer
    (the total char count across all childNodes that are text nodes)
  - assumes visible char count, consistent with the selection api
    (use utils/html->text to convert from html for counting)
  - <br> is not counted as a char"
  [el pos]
  (let [rng (.createRange js/document)
        sel (.getSelection js/window)]
    (if (keyword? pos)
      (do (.selectNodeContents rng el)
          (.collapse rng (= pos :start)))
      (let [[nth-node nth-pos]
            (loop [nodes (.. el -childNodes)
                   char-sum 0]
              (let [[node r] (vector (first nodes) (rest nodes))]
                (if (text-node? node)
                  (let [node-len (.. node -length)
                        ;; add 4 because of <br>
                        next-sum (+ char-sum node-len)]
                    (cond
                      (>= next-sum pos) [node (- node-len (- next-sum pos))]
                      (empty? r) (throw (ex-info
                                          "Caret pos exceeds char sum."
                                          {:pos pos
                                           :next-sum next-sum}))
                      :else (recur r (+ next-sum 4))))
                  (recur r char-sum))))]
        ; (js/console.log el)
        ; (println "pos: " pos "nth-pos: " nth-pos
        ;          ", Text node: " (.-textContent nth-node))
        (.setStart rng nth-node nth-pos)
        (.setEnd   rng nth-node nth-pos)))
    (.removeAllRanges sel)
    (.addRange sel rng)))

(defn set-caret-from-coords [x y]
  ; ! caretRangeFromPoint is non-std, might need a polyfill
  (when-let [rng (.caretRangeFromPoint js/document x y)]
    (let [sel (.getSelection js/window)]
      (.removeAllRanges sel)
      (.addRange sel rng))))

(defn get-caret-coordinates []
  (let [sel (.getSelection js/window)]
    (when-not (== 0 (.. sel -rangeCount))
      (let [rng (.. sel (getRangeAt 0) cloneRange)
            _   (.collapse rng false)
            rect (aget (.getClientRects rng) 0)]
        (when (some? rect)
          {:x (.. rect -left) :y (.. rect -top)})))))


(defn- calc-line-offset
  ([line-nodes target-node]
   (calc-line-offset line-nodes target-node 0))
  ([line-nodes target-node rest-offset]
   (loop [[node & r] line-nodes
          offset     rest-offset]
     (if (= (.. node -nodeValue) (.. target-node -nodeValue))
       offset
       (let [char-count (case (.. node -nodeType)
                          3 (.. node -length) ; text node
                          1 4 ; <br>
                          0)]
         ; (println "chars "char-count)
         ; (println (.. node -nodeType))
         (recur r (+ offset char-count)))))))

(defn- correct-sel-offset
  "If a space is at the end or adjacent to another in a contentEditable,
  it appears as '&nbsp;' (code 160) instead of ' ' (code 32) in the HTML.
  The former is 1 char in selection offset, but must become 6 chars
  for the actual HTML position, which is corrected here."
  [node offset]
  (let [js-str (case (.. node -nodeType)
                 3 (.. node -nodeValue) ; text node
                 1 (.. node -innerText) ; <br>
                 (throw (js/Error. "Invalid node type")))
        i      (dec (.. js-str -length))
        fqs    (->> js-str
                    (take offset)
                    (map #(.. % charCodeAt))
                    frequencies)
        corr   (* (fqs 160) 5)]
    (+ offset corr)))

(defn get-curr-selection []
  (let [sel (.getSelection js/window)
        anchorNode (.. sel -anchorNode)
        focusNode  (.. sel -focusNode)
        ; _ (js/console.log anchorNode)
        ; _ (js/console.log focusNode)
        sel-start  (correct-sel-offset anchorNode (.. sel -anchorOffset))
        sel-end    (correct-sel-offset focusNode (.. sel -focusOffset))
        ; _ (println sel-start "-" sel-end)
        line-nodes (.. sel -baseNode -parentNode -childNodes)
        pos-start  (if (text-node? anchorNode)
                     (calc-line-offset line-nodes anchorNode sel-start)
                     sel-start)
        pos-end    (if (text-node? focusNode)
                     (calc-line-offset line-nodes focusNode sel-end)
                     sel-end)]
    ; (js/console.log line-nodes)
    {:sel-start sel-start
     :sel-end   sel-end
     :pos-start pos-start
     :pos-end   pos-end}))


(defn get-caret-bounds []
  (let [sel (.getSelection js/window)]
    (when-not (== 0 (.. sel -rangeCount))
      (let [rng (.. sel (getRangeAt 0) cloneRange)
            _ (.collapse rng false)
            rect (aget (.getClientRects rng) 0)]
        (when (some? rect)
          {:x1 (.. rect -left)  :y1 (.. rect -top)
           :x2 (.. rect -right) :y2 (.. rect -bottom)})))))

(defn get-elem-bounds [el]
  (let [rects (.. el getClientRects)]
    (when (some? rects)
      (let [rect (aget rects 0)]
        {:x1 (.. rect -left)  :y1 (.. rect -top)
         :x2 (.. rect -right) :y2 (.. rect -bottom)}))))

(defn get-comp-style [el]
  (when-let [comp-style (.. js/window (getComputedStyle el))]
    comp-style))


;; Use this to fix selection in empty contentEditable elements
;; See: https://stackoverflow.com/a/18813725/1204047
(def zero-width-char "&#8203;")

(defn includes-zero-width-char [js-str]
  (.. js-str (includes "​")))

(defn remove-zero-width-char [js-str]
  (if (includes-zero-width-char js-str)
    (.. js-str (split "​") (join ""))
    js-str))

(comment
  
  )
