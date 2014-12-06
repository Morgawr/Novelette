; Storyteller is the underlying engine used for parsing, macroexpnding and
; setting up the storytelling engine with event hooks and storytelling
; routines.
(ns novelette.storyteller
  (:require [novelette.sound :as s]))

(defrecord StoryTeller [runtime-hooks ; Map of runtime hooks to in-text macros
                        current-state ; Current state to parse.
                        timer ; Amount of milliseconds passed since last state transition
                        state ; Local storyteller state for transitioning events
                        first? ; Is this the first frame for this state?
                        done? ; Are we done processing this state?
                        ])

; storyteller just takes a stack of instructions and executes them in order
; when it's done it sends the data back to the story screen and if the stack is over
; it returns the name of the last instruction (possibly a jump to a new state or screen)
;
; when the storyscreen receives such jump from the storyteller, it simply passes the new
; state to the storyteller and keeps going

(defn init-new
  [storyteller new? step]
  (if new?
    (assoc storyteller :state {} :first? false :done? false)
    storyteller))

(defn parse-event
  [storyteller state step]
  (cond
   (= :function (:type step))
     (let [hooks (:runtime-hooks storyteller)
           fn-id (:hook step)
           params (:params step)]
       (apply (fn-id hooks) state storyteller params))
   (= :implicit-choice (:type step))
     [state storyteller]
   (= :explicit-choice (:type step))
     [state storyteller]
   (= :speech (:type step))
     [state storyteller]
   (= :dummy (:type step))
     (do
       (.log js/console "Dummy step")
       [state storyteller])
   :else
     (do
       (.log js/console "Storyteller: ")
       (.log js/console (pr-str storyteller))
       (.log js/console "State: ")
       (.log js/console (pr-str state))
       (throw (js/Error. (str "Error: unknown type -> " (pr-str (:type step))))))))

(defn update
  [{:keys [storyteller state] :as screen} elapsed-time]
  (if (not (:done? storyteller))
    (let [step (:current-state storyteller)
          new? (:first? storyteller)
          temp-storyteller (-> storyteller
                               (init-new new? step)
                               (update-in [:timer] + elapsed-time))
          [new-state new-storyteller] (parse-event temp-storyteller state step)]
    (assoc screen
      :storyteller new-storyteller
      :state new-state))
    screen))

(defn add-sprite
  [state storyteller id]
  [(update-in state [:spriteset] conj id)
   (assoc storyteller :done? true)])

(defn remove-sprite
  [state storyteller id]
  [(update-in state [:spriteset] disj id)
   (assoc storyteller :done? true)])

(defn teleport-sprite
  [state storyteller id position]
  [(assoc-in state [:sprites id] position)
   (assoc storyteller :done? true)])

(defn pop-background
  [state storyteller]
  [(update-in state [:backgrounds] rest)
   (assoc storyteller :done? true)])

(defn push-background
  [state storyteller id]
  [(update-in state [:backgrounds] conj id)
   (assoc storyteller :done? true)])

(defn clear-backgrounds
  [state storyteller]
  [(assoc state :backgrounds '())
   (assoc storyteller :done? true)])

(def RT-HOOKS (atom {
                     :play-bgm (fn [state storyteller id]
                                 (s/play-bgm id)
                                 [state storyteller])
                     :stop-bgm (fn [state storyteller]
                                 (s/stop-bgm)
                                 [state storyteller])
                     :add-sprite add-sprite
                     :remove-sprite remove-sprite
                     :teleport-sprite teleport-sprite
                     :pop-background pop-background
                     :push-background push-background
                     :clear-backgrounds clear-backgrounds
                     }))
