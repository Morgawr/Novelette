(ns novelette.render
  (:require [goog.dom :as dom]
            [clojure.string]))

(defn clear-context
  [screen]
  (let [ctx (:context screen)
        canvas (:canvas screen)
        width (.-width canvas)
        height (.-height canvas)]
    (.clearRect ctx 0 0 width height)))

(def IMAGE-MAP (atom {}))

; TODO - Add support for multiple fonts
(def FONT "sans-serif")

(declare load-image)

(defn load-error
  [uri sym]
  (let [window (dom/getWindow)]
    (.setTimeout window #(load-image uri sym) 200)))

(defn load-image
  [uri sym]
  (let [image (js/Image. )]
    (set! (.-onload image) #(swap! IMAGE-MAP assoc sym image))
    (set! (.-onerror image) #(load-error uri sym))
    (set! (.-src image) uri)))

(defn draw-image
  [ctx pos name]
  (let [image (name @IMAGE-MAP)]
    (.drawImage ctx image
                (first pos)
                (second pos))))

(defn draw-sprite
  [ctx {:keys [id position]}]
  (draw-image ctx position id))

(defn fill-clear
  [canvas ctx color]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx 0 0
             (.-width canvas)
             (.-height canvas)))

(defn draw-text
  [ctx pos text attr color]
  (set! (.-textBaseline ctx) "top")
  (set! (.-font ctx) (str attr " " FONT))
  (set! (.-fillStyle ctx) color)
  (.fillText ctx text
             (first pos)
             (second pos)))

(defn measure-text-length
  [ctx text]
  (.-width (.measureText ctx text)))

(defn draw-text-with-cursor
  [ctx pos text attr color cursor-id cursor-y-offset]
  (draw-text ctx pos text attr color)
  (let [width (measure-text-length ctx text)
        [x y] pos]
    (draw-image ctx [(+ x width 2) (+ y 14 cursor-y-offset)] cursor-id)))

(defn draw-text-centered
  [ctx pos text attr color]
  (set! (.-font ctx) (str attr " " FONT))
  (let [width (measure-text-length ctx text)
        newx (- (first pos) (/ width 2))]
    (draw-text ctx [newx (second pos)] text attr color)))

(defn split-index
  [msg length]
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

(defn draw-multiline-center-text
  [ctx pos msg attr color maxlength spacing]
  (loop [msgs msg y (second pos)]
    (let [res (split-index msgs maxlength)
          first-msg (clojure.string/join (first res))
          rest-msg (clojure.string/join (second res))]
      (if (empty? rest-msg)
        (draw-text-centered ctx [(first pos) y] first-msg attr color)
        (do
          (draw-text-centered ctx [(first pos) y] first-msg attr color)
          (recur rest-msg (+ spacing y)))))))


(defn get-center
  [canvas]
  (let [width (.-width canvas)
        height (.-height canvas)]
    [(int (/ width 2)) (int (/ height 2))]))
