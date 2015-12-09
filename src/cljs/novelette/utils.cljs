; This file contains mostly re-used self-contained utility functions.
(ns novelette.utils
  (:require-macros [schema.core :as s])
  (:require [novelette.schemas :as sc]
            [schema.core :as s]))

(defn sort-z-index
  "Returns a list of sprites priority sorted by z-index"
  [sprites] ; TODO - work on the data validation for this
  (sort-by (comp :z-index second) #(- 0 (compare %1 %2)) sprites))

(s/defn inside-bounds?
  "Check if a given point is within bounds."
  [[x1 y1] :- sc/pos
   [x2 y2 w2 h2] :- sc/pos]
  (and (< x2 x1 (+ x2 w2))
       (< y2 y1 (+ y2 h2))))

