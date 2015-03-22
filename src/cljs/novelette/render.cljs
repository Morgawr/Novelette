(ns novelette.render
  (:require-macros [schema.core :as s])
  (:require [goog.dom :as dom]
            [clojure.string]
            [novelette.schemas :as sc]
            [schema.core :as s]))

(s/defn clear-context
  [screen :- sc/Screen]
  (let [ctx (:context screen)
        canvas (:canvas screen)
        width (.-width canvas)
        height (.-height canvas)]
    (.clearRect ctx 0 0 width height)))

(def IMAGE-MAP (atom {}))

; TODO - Add support for multiple fonts
(def FONT "sans-serif")

(declare load-image)

(s/defn load-error
  [uri :- s/Str
   sym :- s/Keyword]
  (let [window (dom/getWindow)]
    (.setTimeout window #(load-image uri sym) 200)))

(s/defn load-image
  [uri :- s/Str
   sym :- s/Keyword]
  (let [image (js/Image. )]
    (set! (.-onload image) #(swap! IMAGE-MAP assoc sym image))
    (set! (.-onerror image) #(load-error uri sym))
    (set! (.-src image) uri)))

(s/defn draw-image
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   name :- s/Keyword]
  (let [image (name @IMAGE-MAP)]
    (.drawImage ctx image
                (first pos)
                (second pos))))

(s/defn draw-sprite
  [ctx :- js/CanvasRenderingContext2D
   {:keys [id position]} :- sc/Sprite] 
  (draw-image ctx position id))

(s/defn fill-clear
  [canvas :- js/HTMLCanvasElement
   ctx :- js/CanvasRenderingContext2D
   color :- s/Str]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx 0 0
             (.-width canvas)
             (.-height canvas)))

(s/defn draw-text
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   text :- s/Str
   attr ; TODO - Figure out the data type of this
   color :- s/Str]
  (set! (.-textBaseline ctx) "top")
  (set! (.-font ctx) (str attr " " FONT))
  (set! (.-fillStyle ctx) color)
  (.fillText ctx text
             (first pos)
             (second pos)))

(s/defn measure-text-length
  [ctx :- js/CanvasRenderingContext2D
   text :- s/Str]
  (.-width (.measureText ctx text)))

(s/defn draw-text-with-cursor
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   text :- s/Str
   attr ; TODO - Figure out the data type of this
   color :- s/Str
   cursor-id :- s/Keyword
   cursor-y-offset :- s/Int]
  (draw-text ctx pos text attr color)
  (let [width (measure-text-length ctx text)
        [x y] pos]
    (draw-image ctx [(+ x width 2) (+ y 14 cursor-y-offset)] cursor-id)))

(s/defn draw-text-centered
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   text :- s/Str
   attr ; TODO - Figure out the data type of this
   color :- s/Str]
  (set! (.-font ctx) (str attr " " FONT))
  (let [width (measure-text-length ctx text)
        newx (int (- (first pos) (/ width 2)))]
    (draw-text ctx [newx (second pos)] text attr color)))

(s/defn split-index
  [msg :- s/Str
   length :- s/Int]
  (loop [idx 0 prevs 0]
    (cond
     (< (count msg) length)
       [msg ""]
     (= (str (nth msg idx)) " ")
       (recur (inc idx) idx)
     (>= idx length)
       [(take prevs msg) (drop prevs msg)]
     :else
       (recur (inc idx) prevs))))

(s/defn draw-multiline-center-text
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   msg :- s/Str
   attr ; TODO - Figure out the data type of this
   color :- s/Str
   maxlength :- s/Int
   spacing :- s/Int]
  (loop [msgs msg 
         [_ y] pos]
    (let [res (split-index msgs maxlength)
          first-msg (clojure.string/join (first res))
          rest-msg (clojure.string/join (second res))]
      (if (empty? rest-msg)
        (draw-text-centered ctx [(first pos) y] first-msg attr color)
        (do
          (draw-text-centered ctx [(first pos) y] first-msg attr color)
          (recur rest-msg (+ spacing y)))))))

(s/defn get-center
  [canvas :- js/CanvasRenderingContext2D]
  (let [width (.-width canvas)
        height (.-height canvas)]
    [(int (/ width 2)) (int (/ height 2))]))
