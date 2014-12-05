; Storyteller is the underlying engine used for parsing, macroexpnding and
; setting up the storytelling engine with event hooks and storytelling
; routines.
(ns novelette.storyteller
  (:require-macros [novelette.syntax :as syntax])
  (:require [novelette.sound :as s]))

(declare add-sprite)
(declare remove-sprite)
(declare teleport-sprite)

(def RT-HOOKS (atom {
                     :play-bgm (fn [screen id]
                                 (s/play-bgm id)
                                 screen)
                     :stop-bgm (fn [screen]
                                 (s/stop-bgm)
                                 screen)
                     :add-sprite add-sprite
                     :remove-sprite remove-sprite
                     :teleport-sprite teleport-sprite
                     }))

(defrecord StoryTeller [machine ; State-machine of the entire game.
                        runtime-hooks ; Map of runtime hooks to in-text macros
                        timer ; Amount of milliseconds passed since last state transition
                        ])

; storyteller just takes a stack of instructions and executes them in order
; when it's done it sends the data back to the story screen and if the stack is over
; it returns the name of the last instruction (possibly a jump to a new state or screen)
;
; when the storyscreen receives such jump from the storyteller, it simply passes the new
; state to the storyteller and keeps going

(defn update
  [{:keys [storyteller] :as screen} elapsed-time]
  (assoc screen :storyteller (update-in storyteller [:timer] + elapsed-time)))

(defn add-sprite
  [screen id]
  (update-in screen [:spriteset] conj id))

(defn remove-sprite
  [screen id]
  (update-in screen [:spriteset] disj id))

(defn teleport-sprite
  [screen id position]
  (assoc-in screen [:sprites id] position))
