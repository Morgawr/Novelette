(ns novelette.screens.storyscreen
  (:require [novelette.render :as r]
            [novelette.screen :as gscreen]
            [novelette.sound :as gsound]
            novelette.screens.dialoguescreen
            [novelette.storyteller :as s]
            [novelette.utils :as utils]))

; This is the storytelling state of the game. It is an object containing the whole set of
; past, present and near-future state. It keeps track of stateful actions like scrollback,
; sprites being rendered, bgm playing and other stuff. Ideally, it should be easy to
; save/load transparently.
(defrecord State [scrollback ; Complete history of events for scrollback purposes, as a stack.
                  scrollfront ; Stack of events yet to be interpreted.
                  spriteset ; Set of sprite id currently displayed on screen.
                  sprites ; Map of sprites globally defined on screen.
                  backgrounds ; Stack of sprites currently in use as backgrounds.
                  points ; Map of points that the player obtained during the game
                  cps ; characters per second
                  next-step ; Boolean, whether or not to advance to next step for storytelling
                  ; TODO - add a "seen" map with all the dialogue options already seen
                  ;        to facilitate skipping of text.
                  ; TODO - add map of in-game settings.
                  ])

(def BASE-STATE (State. '() '() #{} {} '() {} 0 true))

; A sprite is different from an image, an image is a texture loaded into the
; engine's renderer with an id assigned as a reference. A sprite is an instance
; of a texture paired with appropriate positioning and rendering data.
(defrecord Sprite [id ; image id loaded in the engine
                   position ; X/Y coordinates in a vector
                   z-index ; depth ordering for rendering, lower = front
                   ; TODO - add scale and rotation
                   ])

(defn advance-step
  [screen]
  (let [next-step ((comp :next-step :state) screen)
        scroll-front ((comp :scrollfront :state) screen)]
    (if (and next-step
             (not (empty? scroll-front)))
      (as-> screen s
            (update-in s [:state :scrollback] conj ((comp :current-state :storyteller) s))
            (assoc-in s [:state :next-step] false)
            (assoc-in s [:storyteller :current-state] ((comp first :scrollfront :state) s))
            (assoc-in s [:storyteller :done?] false)
            (assoc-in s [:storyteller :first?] true)
            (assoc-in s [:state :scrollfront] ((comp rest :scrollfront :state) s)))
      screen)))

(defn set-storyteller
  [{:keys [state] :as screen}]
  (let [{:keys [storyteller scrollfront]} state]
    (if (nil? storyteller)
      (assoc-in screen [:storyteller] (s/StoryTeller. @s/RT-HOOKS {:type :dummy} 0 {} false true))
      screen)))

(defn step-storyteller
  [screen elapsed-time]
  (s/update screen elapsed-time))

(defn evaluate
  [screen]
  (if ((comp :done? :storyteller) screen)
    (assoc-in screen [:state :next-step] true)
    screen))

(defn update
  [screen on-top elapsed-time]
  (if on-top
    (-> screen
        (set-storyteller)
        (advance-step)
        (step-storyteller elapsed-time)
        (evaluate))
    screen))

(defn render
  [{:keys [state context] :as screen} on-top]
  (let [bgs (reverse (:backgrounds state))
        sps (utils/sort-z-index (:spriteset state))]
    (doseq [s bgs]
      (r/draw-image context [0 0] s))
    (when on-top
      (doseq [s sps]
        ((comp r/draw-sprite second) s))))
  screen)

(defn handle-input
  [screen on-top mouse]
  (if on-top
    (assoc screen :mouse mouse)
    screen))

(defn init
  [ctx canvas gamestate]
  (into gscreen/BASE-SCREEN
   {
    :id "StoryScreen"
    :update update
    :render render
    :handle-input handle-input
    :context ctx
    :canvas canvas
    :deinit (fn [s] nil)
    :state gamestate
    :storyteller nil
    }))
