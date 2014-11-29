(ns novella.core
  (:require [clojure.browser.repl :as repl]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.events.EventType :as event-type]
            novella.screens.loadingscreen
            [novella.input :as input]
            [novella.screen :as screen]))


(defn ^:export connect
  []
  (.log js/console "Starting local connection...")
  (repl/connect "http://localhost:9000/repl")
  (.log js/console "...connected"))

(defn ^:export init []
  (let [document (dom/getDocument)
        canvas (dom/getElement "surface")
        ctx (.getContext canvas "2d")
        loading (novella.screens.loadingscreen/init ctx canvas)
        state (screen/State. [loading] 0 ctx canvas)]
    (events/listen js/window event-type/MOUSEMOVE input/mouse-move-listener)
    (events/listen js/window event-type/CLICK input/mouse-click-listener)
    (screen/main-game-loop state)))
