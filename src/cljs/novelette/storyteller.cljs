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
  [storyteller new?]
  (if new?
    (do
      (.log js/console (str "Added: " (pr-str (:current-state storyteller))))
      (assoc storyteller :state {} :done? false :timer 0))
    storyteller))

(defn init-dialogue-state
  [state storyteller]
  [state
   (if (:first? storyteller)
     (-> storyteller
         (assoc-in [:state :cps] (:cps state))
         (assoc-in [:state :end?] false))
     storyteller)])

(defn update-dialogue
  [state storyteller]
  [state
   (let [{:keys [current-state timer]} storyteller
         {:keys [cps end? display-message]} (:state storyteller)
         {:keys [input-state]} state
         message (first (:messages current-state))
         cpms (if (zero? cps) 0
                (/ 1000 cps)) ; TODO fix this shit
         char-count (if (zero? cpms) 10000
                      (int (/ timer cpms)))]
     (cond
      end?
        (-> storyteller
            (assoc-in [:state :display-message] message)
            (assoc :done? ((comp :clicked :mouse) input-state)))
      ((comp :clicked :mouse) input-state)
        (-> storyteller
            (assoc-in [:state :display-message] message)
            (assoc-in [:state :end?] true))
      (> char-count (count message))
        (-> storyteller
            (assoc-in [:state :display-message] message)
            (assoc-in [:state :end?] true))
      :else ; Update display-message according to cps
        (assoc-in storyteller [:state :display-message]
                  (apply str (take char-count message)))))])

(defn parse-event
  [state storyteller]
  (let [step (:current-state storyteller)]
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
       (->> [state storyteller]
            (apply init-dialogue-state)
            (apply update-dialogue))
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
         (throw (js/Error. (str "Error: unknown type -> " (pr-str (:type step)))))))))

(defn update
  [{:keys [storyteller state] :as screen} elapsed-time]
  (if (not (:done? storyteller))
    (let [new? (:first? storyteller)
          temp-storyteller (-> storyteller
                               (init-new new?)
                               (update-in [:timer] + elapsed-time))
          [new-state new-storyteller] (parse-event state temp-storyteller)]
    (assoc screen
      :storyteller (assoc new-storyteller :first? false)
      :state new-state))
    screen))


; RUNTIME HOOKS

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
  [(assoc-in state [:sprites id :position] position)
   (assoc storyteller :done? true)])

(defn decl-sprite
  [state storyteller id img pos z-index]
  [(assoc-in state [:sprites id] {:id img
                                  :position pos
                                  :z-index z-index})
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

(defn show-ui
  [state storyteller]
  [(assoc state :show-ui? true)
   (assoc storyteller :done? true)])

(defn hide-ui
  [state storyteller]
  [(assoc state :show-ui? false)
   (assoc storyteller :done? true)])

(defn set-ui
  [state storyteller id pos]
  [(-> state
       (assoc-in [:ui-img :id] id)
       (assoc-in [:ui-img :position] pos))
   (assoc storyteller :done? true)])

(defn wait
  [state storyteller msec]
  (if (> msec (:timer storyteller))
    [state storyteller]
    [state
     (assoc storyteller :done? true)]))

(defn get-next-scene
  [state storyteller scene]
  [(assoc state :scrollfront (:body scene))
   (assoc storyteller :done? true)])

(defn set-cps
  [state storyteller amount]
  [(assoc state :cps amount)
   (assoc storyteller :done? true)])

(def RT-HOOKS (atom {
                     :play-bgm (fn [state storyteller id]
                                 (s/play-bgm id)
                                 [state (assoc storyteller :done? true)])
                     :stop-bgm (fn [state storyteller]
                                 (s/stop-bgm)
                                 [state (assoc storyteller :done? true)])
                     :add-sprite add-sprite
                     :remove-sprite remove-sprite
                     :teleport-sprite teleport-sprite
                     :decl-sprite decl-sprite
                     :pop-background pop-background
                     :push-background push-background
                     :clear-backgrounds clear-backgrounds
                     :show-ui show-ui
                     :hide-ui hide-ui
                     :set-ui set-ui
                     :wait wait
                     :get-next-scene get-next-scene
                     :set-cps set-cps
                     }))
