(ns novelette.render
  (:require [goog.dom :as dom]))

(defn clear-context
  [screen]
  (let [ctx (:context screen)
        canvas (:canvas screen)
        width (.-width canvas)
        height (.-height canvas)]
    (.clearRect ctx 0 0 width height)))

(def IMAGE-MAP (atom {}))

; TODO - Add support for multiple fonts
(def FONT "Arial")

(declare load-image)

(defn load-error
  [uri sym]
  (let [window (dom/getWindow)]
    (. window setTimeout #(load-image uri sym) 200)))

(defn load-image
  [uri sym]
  (let [image (js/Image. )]
    (set! (. image -onload) #(swap! IMAGE-MAP assoc sym image))
    (set! (. image -onerror) #(load-error uri sym))
    (set! (. image -src) uri)))

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
  (set! (. ctx -fillStyle) color)
  (.fillRect ctx 0 0
             (.-width canvas)
             (.-height canvas)))

(defn draw-text
  [ctx pos text attr color]
  (set! (. ctx -textBaseline) "top")
  (set! (. ctx -font) (str attr " " FONT))
  (set! (. ctx -fillStyle) color)
  (.fillText ctx text
             (first pos)
             (second pos)))

(defn draw-text-centered
  [ctx pos text attr color]
  (set! (. ctx -font) (str attr " " FONT))
  (let [width (.-width (.measureText ctx text))
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
          first-msg (apply str (first res))
          rest-msg (apply str (second res))]
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
