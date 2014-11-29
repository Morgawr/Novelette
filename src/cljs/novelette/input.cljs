(ns novelette.input)

(def MOUSE-STATE (atom {:x 0
                        :y 0
                        :clicked false
                        }))

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
