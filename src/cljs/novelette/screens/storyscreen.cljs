(ns novelette.screens.storyscreen
  (:require [novelette.render :as r]
            [novelette.screen :as gscreen]
            [novelette.sound :as gsound]
            novelette.screens.dialoguescreen
            [novelette.utils :as utils]))

; This is the storytelling state of the game. It is an object containing the whole set of
; past, present and near-future state. It keeps track of stateful actions like scrollback,
; sprites being rendered, bgm playing and other stuff. Ideally, it should be easy to
; save/load transparently.
(defrecord State [scrollback ; Complete history of events for scrollback purposes, as a stack.
                  current ; Current event being interpreted by the engine.
                  scrollfront ; Stack of events yet to be interpreted.
                  spritemap ; Map of sprites currently displayed on screen.
                  backgrounds ; Map of sprites currently in use as backgrounds.
                  bgm ; Current bgm being played.
                  points ; Map of points that the player obtained during the game
                  cps ; characters per second
                  ; TODO - add a "seen" map with all the dialogue options already seen
                  ;        to facilitate skipping of text.
                  ; TODO - add map of in-game settings.
                  ])

(def BASE-STATE (State. '() nil '() {} {} nil {} 0))

; A sprite is different from an image, an image is a texture loaded into the
; engine's renderer with an id assigned as a reference. A sprite is an instance
; of a texture paired with appropriate positioning and rendering data.
(defrecord Sprite [id ; image id loaded in the engine
                   position ; X/Y coordinates in a vector
                   z-index ; depth ordering for rendering, lower = front
                   ; TODO - add scale and rotation
                   ])

(defn update
  [screen on-top elapsed-time]
  screen)
  ;(if (:first-time screen)
  ;  (let [ctx (:context screen)
  ;        canvas (:canvas screen)]
  ;    (assoc screen
  ;      :next-frame (fn [state]
  ;                    (let [screen-list (:screen-list state)
  ;                          dialog (novelette.screens.dialoguescreen/init ctx canvas nil MESSAGES)
  ;                          new-list (gscreen/push-screen dialog screen-list)]
  ;                      (assoc state :screen-list new-list)))))

(defn render
  [{:keys [state ctx] :as screen} on-top]
  (let [bgs (utils/sort-z-index (:backgrounds state))
        sps (utils/sort-z-index (:spritemap state))]
    (doseq [s bgs]
      ((comp r/draw-sprite second) s))
    (when on-top
      (doseq [s sps]
        ((comp r/draw-sprite second) s))))
  screen)

(defn handle-input
  [screen on-top mouse]
  (assoc screen :mouse mouse))

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
    :first-time true
    :state gamestate
    }))
