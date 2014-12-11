; Storyteller is the underlying engine used for parsing, macroexpnding and
; setting up the storytelling engine with event hooks and storytelling
; routines.
(ns novelette.storyteller
  (:require [novelette.sound :as s]
            [clojure.string]))

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

;(.log js/console (str "Added: " (pr-str (:current-state storyteller)))) ; TODO - debug flag
(defn init-new
  [screen new?]
  (cond-> screen
          new?
          (update-in [:storyteller] merge {:state {} :done? false :timer 0})))

(defn init-dialogue-state
  [{:keys [storyteller state] :as screen}]
  (cond-> screen
          (:first? storyteller)
          (update-in [:storyteller :state] merge {:cps (:cps state)
                                                  :end? false})))

(defn update-dialogue
  [{:keys [state storyteller] :as screen}]
  (let [{{current-state :current-state
          timer :timer
          {:keys [cps end? display-message]} :state} :storyteller
         {:keys [input-state]} :state} screen
        message (first (:messages current-state))
        cpms (if (zero? cps) 0 (/ 1000 cps)) ; TODO fix this shit
        char-count (if (zero? cpms) 10000 (int (/ timer cpms)))]
     (cond
      end?
        (-> screen
            (assoc-in [:storyteller :state :display-message] message)
            (assoc-in [:storyteller :done?] (get-in input-state [:mouse :clicked])))
      (get-in input-state [:mouse :clicked])
        (update-in screen [:storyteller :state]
                   merge {:display-message message :end? true})
      (> char-count (count message))
        (update-in screen [:storyteller :state]
                   merge {:display-message message :end? true})
      :else ; Update display-message according to cps
        (assoc-in screen [:storyteller :state :display-message]
                  (clojure.string/join (take char-count message))))))

(defn init-explicit-choice
  [{:keys [storyteller state] :as screen}]
  (cond-> screen
          (:first? storyteller)
          (update-in [:state]
                     merge {:choice-text (get-in storyteller [:current-state :text])
                            :option-name (keys (get-in storyteller [:current-state :options]))})))

(defn update-explicit-choice
  [screen]
  (cond-> screen
          (get-in screen [:state :input-state :mouse :clicked])
          ((fn [{:keys [storyteller state]}]
             (let [y (get-in state [:input-state :mouse :y])
                   options (get-in storyteller [:state :option-names])]
               (loop [opts options acc 0]
                 (let [y-base (+ 285 (* acc 45))]
                   (cond
                    (or (empty? opts) (>= acc (count options))) screen
                    (< y-base y (+ y-base 44))
                      (let [next (-> storyteller
                                     (get-in [:current-state :options])
                                     ((fn [s] (s (first opts))))
                                     (get-in [:jump :body]))]
                        (-> screen
                            (assoc-in [:state :scrollfront] next)
                            (assoc-in [:storyteller :done?] true)))
                    :else
                      (recur (rest opts) (inc acc))))))))))

; TODO - change this into self-hosting parsing with :update
(defn parse-event
  [screen]
  (let [{{step :current-state} :storyteller} screen]
    (cond
     (= :function (:type step))
       (let [{{hooks :runtime-hooks} :storyteller} screen
             {fn-id :hook params :params} step]
         (apply (fn-id hooks) screen params))
     (= :implicit-choice (:type step))
       screen
     (= :explicit-choice (:type step))
       (-> screen
           (init-explicit-choice)
           (update-explicit-choice))
     (= :speech (:type step))
       (-> screen
           (init-dialogue-state)
           (update-dialogue))
     (= :dummy (:type step))
       (do
         (.log js/console "Dummy step")
         screen)
     :else
       (do
         (.log js/console "Storyteller: ")
         (.log js/console (pr-str (:storyteller screen))
         (.log js/console "State: ")
         (.log js/console (pr-str (:state screen))
         (.log js/console "Screen: ")
         (.log js/console (pr-str (dissoc screen :state :storyteller)))
         (throw (js/Error. (str "Error: unknown type -> " (pr-str (:type step)))))))))))

(defn update
  [screen elapsed-time]
  (cond-> screen
          (not (:done? (:storyteller screen)))
          ((fn [{:keys [storyteller] :as screen}]
             (let [new? (:first? storyteller)]
               (-> screen
                   (init-new new?)
                   (update-in [:storyteller :timer] + elapsed-time)
                   (parse-event)
                   (assoc-in [:storyteller :first?] false)))))))

; RUNTIME HOOKS

(defn add-sprite
  [screen id]
  (-> screen
      (update-in [:state :spriteset] conj id)
      (assoc-in [:storyteller :done?] true)))

(defn remove-sprite
  [screen id]
  (-> screen
      (update-in [:state :spriteset] disj id)
      (assoc-in [:storyteller :done?] true)))

(defn clear-sprites
  [screen id]
  (-> screen
      (assoc-in [:state :spriteset] #{})
      (assoc-in [:storyteller :done?] true)))

(defn teleport-sprite
  [screen id position]
  (-> screen
      (assoc-in [:state :sprites id :position] position)
      (assoc-in [:storyteller :done?] true)))

(defn decl-sprite
  [screen id img pos z-index]
  (-> screen
      (assoc-in [:state :sprites id] {:id img
                                      :position pos
                                      :z-index z-index})
      (assoc-in [:storyteller :done?] true)))

(defn pop-background
  [screen]
  (-> screen
      (update-in [:state :backgrounds] rest)
      (assoc-in [:storyteller :done?] true)))

(defn push-background
  [screen id]
  (-> screen
      (update-in [:state :backgrounds] conj id)
      (assoc-in [:storyteller :done?] true)))

(defn clear-backgrounds
  [screen]
  (-> screen
      (assoc-in [:state :backgrounds] '())
      (assoc-in [:storyteller :done?] true)))

(defn show-ui
  [screen]
  (-> screen
      (assoc-in [:state :show-ui?] true)
      (assoc-in [:storyteller :done?] true)))

(defn hide-ui
  [screen]
  (-> screen
      (assoc-in [:state :show-ui?] false)
      (assoc-in [:storyteller :done?] true)))

(defn set-ui
  [screen id pos]
  (-> screen
      (update-in [:state :ui-img] merge {:id id :position pos})
      (assoc-in [:storyteller :done?] true)))

(defn wait
  [screen msec]
  (cond-> screen
          (<= msec (get-in screen [:storyteller :timer]))
          (assoc-in [:storyteller :done?] true)))

(defn get-next-scene
  [screen {:keys [body]}]
  (-> screen
      (assoc-in [:state :scrollfront] body)
      (assoc-in [:storyteller :done?] true)))

(defn set-cps
  [screen amount]
  (-> screen
      (assoc-in [:state :cps] amount)
      (assoc-in [:storyteller :done?] true)))

(defn set-dialogue-bounds
  [screen x y w h]
  (-> screen
      (assoc-in [:state :dialogue-bounds] [x y w h])
      (assoc-in [:storyteller :done?] true)))

(defn set-nametag-position
  [screen pos]
  (-> screen
      (assoc-in [:state :nametag-position] pos)
      (assoc-in [:storyteller :done?] true)))

(def RT-HOOKS (atom {
                     :play-bgm (fn [screen id]
                                 (s/play-bgm id)
                                 (assoc-in screen [:storyteller :done?] true))
                     :stop-bgm (fn [screen]
                                 (s/stop-bgm)
                                 (assoc-in screen [:storyteller :done?] true))
                     :add-sprite add-sprite
                     :remove-sprite remove-sprite
                     :teleport-sprite teleport-sprite
                     :clear-sprites clear-sprites
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
                     :set-dialogue-bounds set-dialogue-bounds
                     :set-nametag-position set-nametag-position
                     }))
