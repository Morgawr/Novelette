(ns novelette.input)

(def MOUSE-STATE (atom {:x 0
                        :y 0
                        :clicked false
                        }))

; TODO - Maybe add keyboard stuff too
; TODO - Make mouse work with relative position to top-left corner
(defn mouse-move-listener
  [event]
  (swap! MOUSE-STATE assoc
         :x (.-clientX event)
         :y (.-clientY event)))

(defn mouse-click-listener
  [event]
  (swap! MOUSE-STATE assoc :clicked true))

(defn mouse-declick
  []
  (swap! MOUSE-STATE assoc :clicked false))
