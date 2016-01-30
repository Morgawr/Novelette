; This file contains mostly re-used self-contained utility functions.
(ns novelette.utils
  (:require-macros [schema.core :as s])
  (:require [novelette-sprite.schemas :as scs]
            [schema.core :as s]))

(s/defn sort-z-index
  "Returns a list of sprites priority sorted by z-index"
  [sprites :- [scs/Sprite]]
  (reverse (sort-by :z-index sprites)))

(s/defn inside-bounds?
  "Check if a given point is within bounds."
  [[x1 y1] :- scs/pos
   [x2 y2 w2 h2] :- scs/pos]
  (and (< x2 x1 (+ x2 w2))
       (< y2 y1 (+ y2 h2))))

(s/defn get-center-coordinates
  "Given an x,y,w,h area return the center coordinates."
  [bounds :- scs/pos]
  [(+ (bounds 0) (/ (bounds 2) 2)) ; X coordinate of the center
   (+ (bounds 1) (/ (bounds 3) 2))]) ; Y coordinate of the center
