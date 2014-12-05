; Storyteller is the underlying engine used for parsing, macroexpnding and
; setting up the storytelling engine with event hooks and storytelling
; routines.
(ns novelette.storyteller
  (:require-macros [novelette.syntax :as syntax]))

(defrecord StoryTeller [machine ; State-machine of the entire game.
                        runtime-hooks ; Map of runtime hooks to in-text macros
                        ])

(defn update
  [{:keys [storyteller] :as screen} elapsed-time]
  screen)
