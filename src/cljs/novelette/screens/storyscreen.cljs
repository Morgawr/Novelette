(ns novelette.screens.storyscreen
  (:require [novelette.render :as r]
            [novelette.screen :as gscreen]
            [novelette.sound :as gsound]
            [novelette.storyteller :as s]
            [novelette.GUI :as g]
            [novelette.utils :as utils]
            [clojure.string :as string]))

; TODO - Move on-top and elapsed-time into the screen structure

; This is the storytelling state of the game. It is an object containing the whole set of
; past, present and near-future state. It keeps track of stateful actions like scrollback,
; sprites being rendered, bgm playing and other stuff. Ideally, it should be easy to
; save/load transparently.
(defrecord State [scrollback ; Complete history of events for scrollback purposes, as a stack (this should contain the previous state of the storyscreen too)
                  scrollfront ; Stack of events yet to be interpreted.
                  spriteset ; Set of sprite id currently displayed on screen.
                  sprites ; Map of sprites globally defined on screen.
                  backgrounds ; Stack of sprites currently in use as backgrounds.
                  points ; Map of points that the player obtained during the game
                  cps ; characters per second
                  next-step? ; Whether or not to advance to next step for storytelling
                  show-ui? ; Whether or not we show the game UI on screen.
                  ui-img ; UI image to show
                  input-state ; State of the input for the current frame.
                  cursor ; Image of glyph used to advance text
                  cursor-delta ; Delta to make the cursor float, just calculate % 4
                  dialogue-bounds ; x,y, width and height of the boundaries of text to be displayed
                  nametag-position ; x,y coordinates of the nametag in the UI
                  ; TODO - add a "seen" map with all the dialogue options already seen
                  ;        to facilitate skipping of text.
                  ])

; A sprite is different from an image, an image is a texture loaded into the
; engine's renderer with an id assigned as a reference. A sprite is an instance
; of a texture paired with appropriate positioning and rendering data.
(defrecord Sprite [id ; image id loaded in the engine
                   position ; X/Y coordinates in a vector
                   z-index ; depth ordering for rendering, lower = front
                   ; TODO - add scale and rotation
                   ])

(def BASE-STATE (State. '() '() #{} {} '() {} 0 true false
                        (Sprite. :dialogue-ui [0 0] 0) {} :cursor 0
                        [0 0 0 0] [0 0]))

(defn update-cursor ; TODO - move this into the GUI
  [{:keys [state] :as screen} elapsed-time]
  (let [cursor-delta (:cursor-delta state)]
    (if (> (+ cursor-delta elapsed-time) 800)
      (assoc-in screen [:state :cursor-delta]
                (+ cursor-delta elapsed-time -800))
      (assoc-in screen [:state :cursor-delta]
                (+ cursor-delta elapsed-time)))))

(defn update-gui
  [screen elapsed-time]
  (update-cursor screen elapsed-time)) ; TODO - move this into the GUI

(defn screen-update
  [screen on-top elapsed-time]
  (cond-> screen
          on-top
          (-> (s/screen-update elapsed-time)
              (update-gui elapsed-time))))

(defn render-dialogue
  [{:keys [state storyteller context] :as screen}]
  (let [{:keys [cursor cursor-delta
                dialogue-bounds nametag-position]} state
        [x y w h] dialogue-bounds
        step (+ 30 (int (/ w (r/measure-text-length context "m"))))
        words (string/split (get-in storyteller [:state :display-message] storyteller) #"\s")
        {{nametag :name
          namecolor :color} :current-token} storyteller ; TODO refactor this
        lines (cond-> words
                      (> (count words) 1)
                      ((fn [curr ws acc]
                         (cond
                          (empty? curr)
                            (reverse (conj ws (string/join " " acc)))
                          (< step (count (string/join " " (conj acc (first curr)))))
                            (recur curr (conj ws (string/join " " acc)) [] )
                          :else
                            (recur (rest curr) ws (conj acc (first curr))))) '() []))
        iterators (zipmap (drop-last lines) (range (count (drop-last lines))))
        offset (cond (< 0 cursor-delta 101) -2 ; TODO - this is terrible I hate myself.
                     (or (< 100 cursor-delta 201)
                         (< 700 cursor-delta 801)) -1
                     (or (< 200 cursor-delta 301)
                         (< 600 cursor-delta 701)) 0
                     (or (< 300 cursor-delta 401)
                         (< 500 cursor-delta 601)) 1
                     (< 400 cursor-delta 501) 2
                     :else 0)]
    (.save context) ; TODO - move this into the UI
    (set! (.-shadowColor context) "black")
    (set! (.-shadowOffsetX context) 1.5)
    (set! (.-shadowOffsetY context) 1.5)
    (doseq [[s i] iterators]
      (r/draw-text context [x (+ y (* i 35))] s "25px" "white"))
    (when-not (nil? (last lines))
      (r/draw-text-with-cursor context [x (+ y (* (dec (count lines)) 35))]
                               (last lines)
                               "25px" "white" cursor offset))
    (when (seq nametag)
      (r/draw-text context nametag-position nametag "bold 29px" (name namecolor))))
    (.restore context))

(defn render-choice
  [{:keys [state storyteller context] :as screen}]
  (.save context)
  (set! (.-shadowColor context) "black")
  (set! (.-shadowOffsetX context) 1.5)
  (set! (.-shadowOffsetY context) 1.5)
  (let [{{name :choice-text
          options :option-names} :state} storyteller
        pos-w (int (/ (r/measure-text-length context name) 2))]
    (r/draw-image context [415 180] :choicebg)
    (r/draw-text-centered context [680 220] name "25px" "white")
    (doseq [[s i] (zipmap options (range (count options)))]
      (r/draw-text context [(- 620 pos-w) (+ 285 (* i 45))] s "20px" "white")))
  (.restore context))

(defn render ; TODO - move this to GUI
  [{:keys [state context] :as screen} on-top]
  (.save context)
  (let [bgs (reverse (:backgrounds state))
        sps (if (seq (:spriteset state)) (utils/sort-z-index ((apply juxt (:spriteset state)) (:sprites state))) [])]
    (doseq [s bgs]
      (r/draw-image context [0 0] s))
    (when on-top
      (doseq [s sps]
        (r/draw-sprite context s))
      (when (:show-ui? state)
        (r/draw-sprite context (:ui-img state)))
      (when (get-in screen [:storyteller :state :display-message])
        (render-dialogue screen))
      (when (get-in screen [:storyteller :state :choice-text])
        (render-choice screen))))
  screen) ; TODO This might just return nothing? render in any case shouldn't be stateful

(defn handle-input ; TODO - send events to GUI hooks
  [screen on-top input]
  (cond-> screen
          on-top (assoc-in [:state :input-state] input)))

(defn init
  [ctx canvas gamestate]
  (into gscreen/BASE-SCREEN
   {
    :id "StoryScreen"
    :update screen-update
    :render render
    :handle-input handle-input
    :context ctx
    :canvas canvas
    :deinit (fn [s] nil)
    :state gamestate
    :storyteller (s/StoryTeller. @s/RT-HOOKS {:type :dummy} 0 {} false)
    :GUI (g/create-canvas-element canvas ctx)
    }))
