(ns novelette.input
  (:require [goog.events.EventType :as event-type]
            [goog.events :as events]))

(def MOUSE-STATE (atom {:x 0
                        :y 0
                        :clicked? false
                        }))

(def BOUNDING-BOX (atom nil))

; TODO - Maybe add keyboard stuff too
(defn mouse-move-listener
  [event]
  (if (nil? @BOUNDING-BOX)
    (.log js/console "WARNING: Input not properly initialized.")
    (let [x (- (.-clientX event) (.-left @BOUNDING-BOX))
          y (- (.-clientY event) (.-top @BOUNDING-BOX))]
      (swap! MOUSE-STATE assoc :x x :y y))))

(defn mouse-click-listener
  [event]
  (swap! MOUSE-STATE assoc :clicked? true))

(defn mouse-declick
  []
  (swap! MOUSE-STATE assoc :clicked? false))

(defn init
  [canvas]
  (reset! BOUNDING-BOX (.getBoundingClientRect canvas))
  (events/listen canvas event-type/MOUSEMOVE mouse-move-listener)
  (events/listen canvas event-type/CLICK mouse-click-listener))
