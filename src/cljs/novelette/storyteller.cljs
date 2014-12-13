; Storyteller is the underlying engine used for parsing, macroexpnding and
; setting up the storytelling engine with event hooks and storytelling
; routines.
(ns novelette.storyteller
  (:require [novelette.sound :as s]
            [clojure.string]))

(defrecord StoryTeller [runtime-hooks ; Map of runtime hooks to in-text macros
                        current-token ; Current token to parse.
                        timer ; Amount of milliseconds passed since last token transition
                        state ; Local storyteller state for transitioning events
                        first? ; Is this the first frame for this state? TODO - I dislike this, I need a better option.
                        ])

; storyteller just takes a stack of instructions and executes them in order.
; It constantly fetches tokens from the script and interprets them. When it requires a user
; action or some time to pass (or other interactive stuff), it yields back to the storyscreen
; which continues with the rendering and updating.

;(.log js/console (str "Added: " (pr-str (:current-token storyteller)))) ; TODO - debug flag
(defn init-new
  [screen]
  (update-in screen [:storyteller] merge {:state {} :first? true :timer 0}))

(defn mark-initialized
  [screen]
  (assoc-in screen [:storyteller :first?] false))

(defn init-dialogue-state
  [{:keys [storyteller state] :as screen}]
  (cond-> screen
          (:first? storyteller)
          (update-in [:storyteller :state] merge {:cps (:cps state)
                                                  :end? false})))

(defn advance-step
  ([{:keys [storyteller state] :as screen} yield?]
   [(-> screen
        (assoc-in [:storyteller :current-token] (first (:scrollfront state))) ; TODO - add support for end-of-script
        (update-in [:state :scrollback] conj (:current-token storyteller)) ; TODO - conj entire state screen onto history
        (update-in [:state :scrollfront] rest)
        (init-new)) yield?])
  ([screen] (advance-step screen false)))

(defn update-dialogue
  [{:keys [state storyteller] :as screen}]
  (let [{{current-token :current-token timer :timer
          {:keys [cps end? display-message]} :state} :storyteller
         {:keys [input-state]} :state} screen
        message (first (:messages current-token))
        cpms (if (zero? cps) 0 (/ 1000 cps)) ; TODO fix this shit
        char-count (if (zero? cpms) 10000 (int (/ timer cpms)))
        clicked? (get-in input-state [:mouse :clicked?])]
     (cond
      end?
        (let [next (assoc-in screen [:storyteller :state :display-message] message)]
          (if clicked?
            (advance-step next true)
            [next true]))
      (or clicked? (> char-count (count message)))
        [(update-in screen [:storyteller :state] merge {:display-message message :end? true}) true]
      :else ; Update display-message according to cps
        [(assoc-in screen [:storyteller :state :display-message]
                   (clojure.string/join (take char-count message))) true])))

(defn init-explicit-choice
  [{:keys [storyteller state] :as screen}]
  (cond-> screen
          (:first? storyteller)
          (update-in [:storyteller :state]
                     merge {:choice-text (get-in storyteller [:current-token :text])
                            :option-names (keys (get-in storyteller [:current-token :options]))})))

(defn update-explicit-choice
  [screen]
  (if (get-in screen [:state :input-state :mouse :clicked?])
    (let [{:keys [storyteller state]} screen]
      (let [y (get-in state [:input-state :mouse :y])
            options (get-in storyteller [:state :option-names])]
        (loop [opts options acc 0]
          (let [y-base (+ 285 (* acc 45))]
            (cond
             (or (empty? opts) (>= acc (count options))) [screen true]
             (< y-base y (+ y-base 44))
               (let [next (-> storyteller
                              (get-in [:current-token :options])
                              ((fn [s] (s (first opts))))
                              (get-in [:jump :body]))]
                 (-> screen
                     (assoc-in [:state :scrollfront] next)
                     (advance-step true)))
             :else
               (recur (rest opts) (inc acc)))))))
    [screen true]))

; XXX - change this into self-hosted parsing with :update, maybe.
(defn parse-event
  [screen]
  (let [{{step :current-token} :storyteller} screen]
    (cond
     (= :function (:type step))
       (let [{{hooks :runtime-hooks} :storyteller} screen
             {fn-id :hook params :params} step]
         (apply (fn-id hooks) screen params))
     (= :implicit-choice (:type step))
       [screen false] ; TODO - Add implicit choices.
     (= :explicit-choice (:type step))
       (-> screen
           (init-explicit-choice)
           (mark-initialized)
           (update-explicit-choice))
     (= :speech (:type step))
       (-> screen
           (init-dialogue-state)
           (mark-initialized)
           (update-dialogue))
     (= :dummy (:type step))
       (do
         (.log js/console "Dummy step")
         (advance-step screen))
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
  [{:keys [storyteller] :as screen} elapsed-time]
  (-> screen
      (update-in [:storyteller :timer] + elapsed-time)
      ((fn [screen yield?]
         (cond
          yield? screen
          :else (let [[screen yield?] (parse-event screen)]
                  (recur screen yield?)))) false)))

; RUNTIME HOOKS

(defn add-sprite
  [screen id]
  (-> screen
      (update-in [:state :spriteset] conj id)
      (advance-step)))

(defn remove-sprite
  [screen id]
  (-> screen
      (update-in [:state :spriteset] disj id)
      (advance-step)))

(defn clear-sprites
  [screen id]
  (-> screen
      (assoc-in [:state :spriteset] #{})
      (advance-step)))

(defn teleport-sprite
  [screen id position]
  (-> screen
      (assoc-in [:state :sprites id :position] position)
      (advance-step)))

(defn decl-sprite
  [screen id img pos z-index]
  (-> screen
      (assoc-in [:state :sprites id] {:id img
                                      :position pos
                                      :z-index z-index})
      (advance-step)))

(defn pop-background
  [screen]
  (-> screen
      (update-in [:state :backgrounds] rest)
      (advance-step)))

(defn push-background
  [screen id]
  (-> screen
      (update-in [:state :backgrounds] conj id)
      (advance-step)))

(defn clear-backgrounds
  [screen]
  (-> screen
      (assoc-in [:state :backgrounds] '())
      (advance-step)))

(defn show-ui
  [screen]
  (-> screen
      (assoc-in [:state :show-ui?] true)
      (advance-step)))

(defn hide-ui
  [screen]
  (-> screen
      (assoc-in [:state :show-ui?] false)
      (advance-step)))

(defn set-ui
  [screen id pos]
  (-> screen
      (update-in [:state :ui-img] merge {:id id :position pos})
      (advance-step)))

(defn wait
  [screen msec]
  (cond
   (<= msec (get-in screen [:storyteller :timer]))
     (advance-step screen)
   :else
     [screen true]))

(defn get-next-scene
  [screen {:keys [body]}]
  (-> screen
      (assoc-in [:state :scrollfront] body)
      (advance-step)))

(defn set-cps
  [screen amount]
  (-> screen
      (assoc-in [:state :cps] amount)
      (advance-step)))

(defn set-dialogue-bounds
  [screen x y w h]
  (-> screen
      (assoc-in [:state :dialogue-bounds] [x y w h])
      (advance-step)))

(defn set-nametag-position
  [screen pos]
  (-> screen
      (assoc-in [:state :nametag-position] pos)
      (advance-step)))

(def RT-HOOKS (atom {
                     :play-bgm (fn [screen id]
                                 (s/play-bgm id)
                                 (advance-step screen)) ; TODO remove from here
                     :stop-bgm (fn [screen]
                                 (s/stop-bgm)
                                 (advance-step screen)) ; TODO remove from here
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
