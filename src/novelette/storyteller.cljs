; Storyteller is the underlying engine used for parsing, macroexpnding and
; setting up the storytelling engine with event hooks and storytelling
; routines.
(ns novelette.storyteller
  (:require-macros [schema.core :as s])
  (:require [novelette.sound :as snd]
            [clojure.string]
            [novelette.schemas :as sc]
            [novelette-sprite.schemas :as scs]
            novelette-sprite.loader
            novelette-sprite.render
            [novelette.GUI :as GUI]
            [novelette.GUI.story-ui :as ui]
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

; TODO - Probably extract these message utilities into their own module for
; simpler re-use (easier with memoization too!)
(s/defn interpolate-message
  "Take the message string and split it into multiple <style></style> groups."
  [msg :- s/Str]
  (let [regex-match (re-pattern "<[/]?style[=]?[^>]*>")
        matches (clojure.string/split msg regex-match)
        seqs (re-seq regex-match msg)]
    {:msg-list matches
     :sequences seqs}))

; TODO - I am adding unnecessary " " in-between the style tags because of a bug
; with the novelette-text library -> Novelette-text/issues/1
(s/defn extract-message
  "Given a message sequence and a character count, retrieve a well-formatted
  message."
  [{:keys [msg-list sequences]} :- s/Any ; TODO - Do we want proper typing here?
   char-count :- s/Int]
  (cond
    (>= char-count (count (apply str msg-list)))
    (str (apply str (map str msg-list sequences))   ; Merge together the whole message + styles
         (if (> (count msg-list) (count sequences)) ; add closing tags if any :)
           (apply str (drop (count sequences) msg-list))
           (str " " (clojure.string/join
                      " " (drop (count msg-list) sequences)))))
    :else
    (loop [taken 0 msg "" msg-list msg-list sequences sequences]
      (let [next-msg (first msg-list)]
        (cond
          (or (>= taken char-count)
              (not (seq next-msg)))
          (str msg " " (clojure.string/join " " sequences))
          (> (+ taken (count next-msg)) char-count)
          (str msg (apply str (take (- char-count taken) next-msg))
               (clojure.string/join " " sequences))
          :else
          (let [msg (apply str msg next-msg (first sequences))
                taken (+ taken (count next-msg))]
            (recur taken msg (rest msg-list) (rest sequences))) )))))

; This is for optimization purposes, memory usage might raise unnecessarily
; so it might be nice to have a "purge" command to free memoization resources.
; TODO - implement a "purge" command inbetween screen transitions
(def memo-interpolate-message (atom (memoize interpolate-message)))
(def memo-extract-message (atom (memoize extract-message)))
(s/defn update-dialogue
  [{:keys [state storyteller] :as screen} :- sc/Screen]
  (let [{{current-token :current-token timer :timer
          {:keys [cps end? display-message]} :state} :storyteller
         {:keys [input-state]} :state} screen
        message (first (:messages current-token))
        cpms (if (zero? cps) 0 (/ 1000 cps)) ; TODO fix this shit
        char-count (if (zero? cpms) 10000 (int (/ timer cpms)))
        clicked? (get-in storyteller [:clicked? 0])]
     (cond
      end?
        (let [next (assoc-in screen
                             [:storyteller :state :display-message] message)]
          (if clicked?
            (advance-step next true)
            [next true]))
      (or clicked?
          (> char-count ((comp count str :msg-list)
                         (@memo-interpolate-message message))))
        [(update-in screen [:storyteller :state]
                    merge {:display-message message :end? true}) true]
      :else ; Update display-message according to cps
       (let [msg (@memo-extract-message
                   (@memo-interpolate-message message) char-count)]
        [(assoc-in screen [:storyteller :state :display-message] msg) true]))))

(s/defn reset-clicked
  "Reset the clicked? status in the storyteller for future events."
  [[screen yield?] :- [(s/cond-pre sc/Screen s/Bool)]]
  [(assoc-in screen [:storyteller :clicked?] [false false]) yield?])

(s/defn init-explicit-choice
  [{:keys [storyteller state] :as screen} :- sc/Screen]
  (cond-> screen
    (:first? storyteller)
    (->
      (update-in [:storyteller :state]
                 merge {:choice-text (get-in storyteller [:current-token :text])
                        :option-names
                        (keys (get-in storyteller [:current-token :options]))})
      (ui/spawn-explicit-choice-gui))))

; 1 - Storyteller must create GUI elements for multiple choice
; 2 - Storyteller must not be aware of the GUI composition and/or interface details
; 3 - Storyteller must retrieve multiple choice data from a shared variable with each button

(s/defn update-explicit-choice
  [{:keys [storyteller] :as screen} :- sc/Screen]
  (let [storyteller-state (:state storyteller)]
    (if (:choice storyteller-state)
      (let [next (-> storyteller
                     (get-in [:current-token :options])
                     ((fn [s] (s (:choice storyteller-state))))
                     (get-in [:jump :body]))]
        (-> screen
            (assoc-in [:state :scrollfront] next)
            (#(GUI/remove-element :choice-panel %))
            (advance-step true)))
      [screen true])))

; TODO - change this into self-hosted parsing with :update, maybe.
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
           (update-dialogue)
           (reset-clicked))
     (= :dummy (:type step))
       (do
         (.log js/console "Dummy step")
         (advance-step screen))
     :else
      (do
        (.log js/console "Storyteller: ")
        (.log js/console (pr-str (:storyteller screen)))
        (.log js/console "State: ")
        (.log js/console (pr-str (:state screen)))
        (.log js/console "Screen: ")
        (.log js/console (pr-str (dissoc screen :state :storyteller)))
        (throw
          (js/Error. (str "Error: unknown type -> " (pr-str (:type step)))))))))

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
   id :- sc/id]
  (-> screen
      (update-in [:state :spriteset] conj id)
      (update-in [:state :sprites id] novelette-sprite.render/start-sprite)
      (advance-step)))

(s/defn remove-sprite
  [screen :- sc/Screen
   id :- sc/id]
  (-> screen
      (update-in [:state :spriteset] disj id)
      (update-in [:state :sprites id] novelette-sprite.render/stop-sprite)
      (advance-step)))

(s/defn clear-sprites
  [screen :- sc/Screen
   id :- sc/id]
  (-> screen
      (assoc-in [:state :spriteset] #{})
      (advance-step)))

(s/defn teleport-sprite
  [screen :- sc/Screen
   id :- sc/id
   position :- scs/pos]
  (-> screen
      (assoc-in [:state :sprites id :position] position)
      (advance-step)))

(s/defn decl-sprite
  [screen :- sc/Screen
   id :- sc/id
   model :- scs/SpriteModel
   pos :- scs/pos
   z-index :- s/Int]
  (-> screen
      (assoc-in [:state :sprites id] (novelette-sprite.loader/create-sprite
                                       model pos z-index))
      (update-in [:state :sprites id] novelette-sprite.render/pause-sprite)
      (advance-step)))

(s/defn pop-background
  [screen :- sc/Screen]
  (-> screen
      (update-in [:state :backgrounds] rest)
      (advance-step)))

(s/defn push-background
  [screen :- sc/Screen
   id :- sc/id]
  (-> screen
      (update-in [:state :backgrounds] conj id)
      (advance-step)))

(s/defn clear-backgrounds
  [screen :- sc/Screen]
  (-> screen
      (assoc-in [:state :backgrounds] '())
      (advance-step)))

; TODO: Add an extra show-dialogue because show-ui is too generic and it messes
;       up with the q.save/q.load and system menu options
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
   id :- sc/id
   pos :- scs/pos]
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
   {:keys [body]} :- {s/Keyword s/Any}]
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
   pos :- scs/pos]
  (-> screen
      (assoc-in [:state :nametag-position] pos)
      (advance-step)))

(s/defn play-bgm
  [screen :- sc/Screen
   id :- sc/id]
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
