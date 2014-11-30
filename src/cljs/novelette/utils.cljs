; This file contains mostly re-used self-contained utility functions.
(ns novelette.utils)

(defn sort-z-index
  "Returns a list of sprites priority sorted by z-index"
  [sprites]
  (sort-by (comp :z-index second) #(- 0 (compare %1 %2)) sprites))
