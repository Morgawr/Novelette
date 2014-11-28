(ns novella.core
  (:require [clojure.browser.repl :as repl]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.events.EventType :as event-type]))


(defn ^:export connect
  []
  (.log js/console "Starting local connection...")
  (repl/connect "http://localhost:9000/repl")
  (.log js/console "...connected"))

(defn ^:export init []
  (let [document (dom/getDocument)
        canvas (dom/getElement "surface")
        ctx (.getContext canvas "2d")]
    (.log js/console "Init called")))
