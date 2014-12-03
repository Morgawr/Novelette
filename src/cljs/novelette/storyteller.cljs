; Storyteller is the underlying engine used for parsing, macroexpnding and
; setting up the storytelling engine with event hooks and storytelling
; routines.
(ns novelette.storyteller
  (:require-macros [novelette.macros :as macros]))

(defrecord StoryTeller [machine ; State-machine of the entire game.
                        runtime-hooks ; Map of runtime hooks to in-text macros
                        ])

;(defmacro speak*
;  [name color text] ; TODO - add support for font
;  (let [result  { :name name :color color }
;        msgs (clojure.string/split text #"[{}]")]
;    (loop [messages '() actions '() todo msgs]
;      (let [curr (first todo) next (rest todo)]
;        (cond
;          (nil? curr)
;            (assoc result
;              :messages (reverse messages)
;              :actions (reverse actions))
;          (= (first curr) \()
;            (recur (conj :pop messages)
;                   (conj curr actions)
;                   (rest todo))
;          :else
;            (recur (conj curr messages)
;                   actions
;                   (rest todo)))))))

;(defmacro defspeaker
;  [symbol name color] ; TODO - add support for font

