; Storyteller is the underlying engine used for parsing, macroexpnding and
; setting up the storytelling engine with event hooks and storytelling
; routines.
(ns novelette.storyteller
  (:require-macros [schema.core :as s])
  (:require [novelette.sound :as snd]
            [clojure.string]
            [novelette.schemas :as sc]
            [schema.core :as s]))

; storyteller just takes a stack of instructions and executes them in order.
; It constantly fetches tokens from the script and interprets them. When it requires a user
; action or some time to pass (or other interactive stuff), it yields back to the storyscreen
; which continues with the rendering and updating.

;(.log js/console (str "Added: " (pr-str (:current-token storyteller)))) ; TODO - debug flag
(s/defn init-new
  [screen :- sc/Screen]
  (update-in screen [:storyteller] merge {:state {} :first? true :timer 0}))

(s/defn mark-initialized
  [screen :- sc/Screen]
  (assoc-in screen [:storyteller :first?] false))

(s/defn init-dialogue-state
  [{:keys [storyteller state] :as screen} :- sc/Screen]
  (cond-> screen
          (:first? storyteller)
          (update-in [:storyteller :state] merge {:cps (:cps state)
                                                  :end? false})))

(s/defn advance-step
  ([{:keys [storyteller state] :as screen} :- sc/Screen
    yield? :- s/Bool]
   [(-> screen
        (assoc-in [:storyteller :current-token] (first (:scrollfront state))) ; TODO - add support for end-of-script
        (update-in [:state :scrollback] conj (:current-token storyteller)) ; TODO - conj entire state screen onto history
        (update-in [:state :scrollfront] rest)
        (init-new)) yield?])
  ([screen :- sc/Screen] (advance-step screen false)))

(s/defn update-dialogue
  [{:keys [state storyteller] :as screen} :- sc/Screen]
  (let [{{current-token :current-token timer :timer
          {:keys [cps end? display-message]} :state} :storyteller
         {:keys [input-state]} :state} screen
        message (first (:messages current-token))
        cpms (if (zero? cps) 0 (/ 1000 cps)) ; TODO fix this shit
        char-count (if (zero? cpms) 10000 (int (/ timer cpms)))
        clicked? (get-in input-state [:clicked? 0])]
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

(s/defn init-explicit-choice
  [{:keys [storyteller state] :as screen} :- sc/Screen]
  (cond-> screen
          (:first? storyteller)
          (update-in [:storyteller :state]
                     merge {:choice-text (get-in storyteller [:current-token :text])
                            :option-names (keys (get-in storyteller [:current-token :options]))})))

(s/defn update-explicit-choice
  [screen :- sc/Screen]
  (if (get-in screen [:state :input-state :clicked? 0])
    (let [{:keys [storyteller state]} screen]
      (let [y (get-in state [:input-state :y])
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
(s/defn parse-event
  [screen :- sc/Screen]
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

(s/defn screen-update
  [{:keys [storyteller] :as screen} :- sc/Screen
   elapsed-time :- s/Num]
  (-> screen
      (update-in [:storyteller :timer] + elapsed-time)
      ((fn [screen yield?]
         (cond
          yield? screen
          :else (let [[screen yield?] (parse-event screen)]
                  (recur screen yield?)))) false)))

; RUNTIME HOOKS

(s/defn add-sprite
  [screen :- sc/Screen
   id :- s/Keyword]
  (-> screen
      (update-in [:state :spriteset] conj id)
      (advance-step)))

(s/defn remove-sprite
  [screen :- sc/Screen
   id :- s/Keyword]
  (-> screen
      (update-in [:state :spriteset] disj id)
      (advance-step)))

(s/defn clear-sprites
  [screen :- sc/Screen
   id :- s/Keyword]
  (-> screen
      (assoc-in [:state :spriteset] #{})
      (advance-step)))

(s/defn teleport-sprite
  [screen :- sc/Screen
   id :- s/Keyword
   position] ; TODO - Decide on the type for 2D position
  (-> screen
      (assoc-in [:state :sprites id :position] position)
      (advance-step)))

(s/defn decl-sprite
  [screen :- sc/Screen
   id :- s/Keyword
   img :- s/Keyword
   pos ; TODO - Decide on the type for 2D position
   z-index :- s/Int]
  (-> screen
      (assoc-in [:state :sprites id] {:id img
                                      :position pos
                                      :z-index z-index})
      (advance-step)))

(s/defn pop-background
  [screen :- sc/Screen]
  (-> screen
      (update-in [:state :backgrounds] rest)
      (advance-step)))

(s/defn push-background
  [screen :- sc/Screen
   id :- s/Keyword]
  (-> screen
      (update-in [:state :backgrounds] conj id)
      (advance-step)))

(s/defn clear-backgrounds
  [screen :- sc/Screen]
  (-> screen
      (assoc-in [:state :backgrounds] '())
      (advance-step)))

(s/defn show-ui
  [screen :- sc/Screen]
  (-> screen
      (assoc-in [:state :show-ui?] true)
      (advance-step)))

(s/defn hide-ui
  [screen :- sc/Screen]
  (-> screen
      (assoc-in [:state :show-ui?] false)
      (advance-step)))

(s/defn set-ui
  [screen :- sc/Screen
   id :- s/Keyword
   pos] ; TODO - Decide on the type for 2D position
  (-> screen
      (update-in [:state :ui-img] merge {:id id :position pos})
      (advance-step)))

(s/defn wait
  [screen :- sc/Screen
   msec :- s/Int]
  (cond
   (<= msec (get-in screen [:storyteller :timer]))
     (advance-step screen)
   :else
     [screen true]))

(s/defn get-next-scene
  [screen :- sc/Screen
   {:keys [body]} :- {s/Any s/Any}]
  (-> screen
      (assoc-in [:state :scrollfront] body)
      (advance-step)))

(s/defn set-cps
  [screen :- sc/Screen
   amount :- s/Int]
  (-> screen
      (assoc-in [:state :cps] amount)
      (advance-step)))

(s/defn set-dialogue-bounds
  [screen :- sc/Screen
   x :- s/Int y :- s/Int
   w :- s/Int h :- s/Int]
  (-> screen
      (assoc-in [:state :dialogue-bounds] [x y w h])
      (advance-step)))

(s/defn set-nametag-position
  [screen :- sc/Screen
   pos] ; TODO - Decide on the type for 2D position
  (-> screen
      (assoc-in [:state :nametag-position] pos)
      (advance-step)))

(s/defn play-bgm
  [screen :- sc/Screen
   id :- s/Keyword]
  (snd/play-bgm id)
  (advance-step screen))

(s/defn stop-bgm
  [screen :- sc/Screen]
  (snd/stop-bgm)
  (advance-step screen))

(def RT-HOOKS (atom {
                     :play-bgm play-bgm
                     :stop-bgm stop-bgm
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
