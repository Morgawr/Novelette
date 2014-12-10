(ns novelette.syntax
  (:require-macros [novelette.syntax :as stx]))

(defn option
  [id jump & args]
  (loop [opt args res {:jump jump}]
    (cond
     (empty? opt) {:options {id res}}
     (= :pre (ffirst opt))
       (recur (rest opt) (assoc res :pre (second (first opt))))
     (= :post (ffirst opt))
       (recur (rest opt) (assoc res :post (second (first opt))))
     :else ; ignore it
       (recur (rest opt) res))))

(defn choice
  [& args]
  (if (string? (first args))
    (stx/choice-explicit* (first args) (rest args))
    (stx/choice-implicit* args)))
