(ns novelette.render
  (:require-macros [schema.core :as s])
  (:require [goog.dom :as dom]
            [clojure.string]
            [novelette.schemas :as sc]
            [schema.core :as s]
            [novelette.utils :as u]))

; TODO - re-work the entire text rendering engine
; TODO - re-work the color system so we support alpha values
; TODO - properly implement ctx.save() ctx.restore()

(s/defn clear-context
  "Refresh and clear the whole canvas surface context."
  [screen :- sc/Screen]
  (let [ctx (:context screen)
        canvas (:canvas screen)
        width (.-width canvas)
        height (.-height canvas)]
    (.clearRect ctx 0 0 width height)))

(def IMAGE-MAP (atom {})) ; Global state buffer for image data

; TODO - Add support for multiple fonts
(def FONT "sans-serif")

(declare load-image)

(s/defn load-error
  [uri :- s/Str
   sym :- sc/id]
  (let [window (dom/getWindow)]
    (.setTimeout window #(load-image uri sym) 200)))

(s/defn load-image
  "Load the given image under the given keyword in the engine's buffer."
  [uri :- s/Str
   sym :- sc/id]
  (let [image (js/Image. )]
    (set! (.-onload image) #(swap! IMAGE-MAP assoc sym image))
    (set! (.-onerror image) #(load-error uri sym))
    (set! (.-src image) uri)))

(s/defn draw-image
  "Draw the image on the canvas given the coordinates and the id of the image."
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   name :- sc/id]
  (let [image (name @IMAGE-MAP)]
    (.drawImage ctx image
                (first pos)
                (second pos))))

(s/defn draw-sprite
  "Draw a given sprite on the canvas."
  [ctx :- js/CanvasRenderingContext2D
   {:keys [id position]} :- sc/Sprite]
  (draw-image ctx position id))

(s/defn fill-clear
  "Fill the whole canvas with a given color."
  [canvas :- js/HTMLCanvasElement
   ctx :- js/CanvasRenderingContext2D
   color :- s/Str]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx 0 0
             (.-width canvas)
             (.-height canvas)))

(s/defn draw-rectangle
  "Draw a rectangle shape on the canvas at the given coordinates."
  [ctx :- js/CanvasRenderingContext2D
   color :- s/Str
   pos :- sc/pos]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx (pos 0) (pos 1) (pos 2) (pos 3)))

(s/defn draw-text
  "Draw a string of text on the canvas with the given properties."
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   text :- s/Str
   attr ; TODO - Figure out the data type of this
   color :- s/Str]
  (.save ctx)
  (set! (.-textBaseline ctx) "top")
  (set! (.-font ctx) (str attr " " FONT))
  (set! (.-fillStyle ctx) color)
  (.fillText ctx text
             (first pos)
             (second pos))
  (.restore ctx))

(s/defn measure-text-length
  "Measure the length of a given string in canvas pixel units."
  [ctx :- js/CanvasRenderingContext2D
   text :- s/Str]
  (.-width (.measureText ctx text)))

(s/defn draw-text-with-cursor
  "Draw a string with the given cursor image appended at the end."
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   text :- s/Str
   attr ; TODO - Figure out the data type of this
   color :- s/Str
   cursor-id :- sc/id
   cursor-y-offset :- s/Int]
  (.save ctx)
  (set! (.-font ctx) (str attr " " FONT))
  (draw-text ctx pos text attr color)
  (let [width (measure-text-length ctx text)
        [x y] pos]
    (draw-image ctx [(+ x width 2) (+ y 14 cursor-y-offset)] cursor-id))
  (.restore ctx))

(s/defn draw-text-centered
  "Draw a string centered at the given origin."
  [ctx :- js/CanvasRenderingContext2D
   pos :- sc/pos
   text :- s/Str
   attr ; TODO - Figure out the data type of this
   color :- s/Str]
  (.save ctx)
  (set! (.-font ctx) (str attr " " FONT))
  (let [width (measure-text-length ctx text)
        newx (int (- (first pos) (/ width 2)))]
    (draw-text ctx [newx (second pos)] text attr color))
  (.restore ctx))

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
  "Draw a multiline string centered at the given origin."
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
  "Return the center position of the canvas."
  [canvas :- js/CanvasRenderingContext2D]
  (let [width (.-width canvas)
        height (.-height canvas)
        position [0 0 width height]]
    (u/get-center-coordinates position)))
