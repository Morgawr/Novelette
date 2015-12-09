(ns novelette.input
  (:require-macros [schema.core :as s])
  (:require [goog.events.EventType :as event-type]
            [goog.events :as events]
            [schema.core :as s]))

; TODO - How do I deal with possible events being registered by the input
; system but not processed in time by the engine so that the input state
; changes?
(def INPUT-STATE (atom {:x 0
                        :y 0
                        :clicked? [false false]
                        :down? [false false]
                        :enabled? true
                        }))

(def mouse-key-map {0 0
                    2 1})

(def BOUNDING-BOX (atom nil))

; TODO - Maybe add keyboard stuff too
(defn mouse-move-listener
  [event]
  (if (nil? @BOUNDING-BOX)
    (.log js/console "WARNING: Input not properly initialized.")
    (let [x (- (.-clientX event) (.-left @BOUNDING-BOX))
          y (- (.-clientY event) (.-top @BOUNDING-BOX))]
      (swap! INPUT-STATE assoc :x x :y y))))

(defn mouse-disable
  [event]
  (swap! INPUT-STATE assoc :enabled? false))

(defn mouse-enable
  [event]
  (swap! INPUT-STATE assoc :enabled? true))

(defn mouse-lclick-listener
  [event]
  (swap! INPUT-STATE assoc-in [:clicked? 0] true))

; This is necessary to mask the contextmenu right-click
; default event listener.
(defn mouse-rclick-listener
  [event]
  (.preventDefault event)
  (swap! INPUT-STATE assoc-in [:clicked? 1] true)
  false)

(defn mouse-down
  [event]
  (let [key (.-button event)]
    (swap! INPUT-STATE assoc-in [:down? (mouse-key-map key)] true)))

(defn mouse-up
  [event]
  (let [key (.-button event)]
    (swap! INPUT-STATE assoc-in [:down? (mouse-key-map key)] false)))

(defn mouse-declick
  []
  (swap! INPUT-STATE assoc :clicked? [false false]))

(s/defn init
  [canvas :- js/HTMLCanvasElement]
  (reset! BOUNDING-BOX (.getBoundingClientRect canvas))
  (.addEventListener canvas "contextmenu" mouse-rclick-listener)
  (events/listen canvas event-type/MOUSEMOVE mouse-move-listener)
  (events/listen canvas event-type/MOUSEOUT mouse-disable)
  (events/listen canvas event-type/MOUSEENTER mouse-enable)
  (events/listen canvas event-type/MOUSEDOWN mouse-down)
  (events/listen canvas event-type/MOUSEUP mouse-up)
  (events/listen canvas event-type/CLICK mouse-lclick-listener))
