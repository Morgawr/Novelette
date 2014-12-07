(ns novelette.screens.storyscreen
  (:require [novelette.render :as r]
            [novelette.screen :as gscreen]
            [novelette.sound :as gsound]
            novelette.screens.dialoguescreen
            [novelette.storyteller :as s]
            [novelette.utils :as utils]
            [clojure.string :as string]))

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

(defn advance-step
  [screen]
  (let [next-step? ((comp :next-step? :state) screen)
        scroll-front ((comp :scrollfront :state) screen)]
    (if (and next-step?
             (not (empty? scroll-front)))
      (as-> screen s
            (update-in s [:state :scrollback] conj ((comp :current-state :storyteller) s))
            (assoc-in s [:state :next-step?] false)
            (assoc-in s [:storyteller :current-state] ((comp first :scrollfront :state) s))
            (assoc-in s [:storyteller :done?] false)
            (assoc-in s [:storyteller :first?] true)
            (assoc-in s [:state :scrollfront] ((comp rest :scrollfront :state) s)))
      screen)))

(defn evaluate
  [screen]
  (if (:done? (:storyteller screen))
    (assoc-in screen [:state :next-step?] true)
    screen))

(defn update-cursor
  [{:keys [state] :as screen} elapsed-time]
  (let [cursor-delta (:cursor-delta state)]
    (if (> (+ cursor-delta elapsed-time) 800)
      (assoc-in screen [:state :cursor-delta]
                (+ cursor-delta elapsed-time -800))
      (assoc-in screen [:state :cursor-delta]
                (+ cursor-delta elapsed-time)))))

(defn update
  [screen on-top elapsed-time]
  (if on-top
    (-> screen
        (advance-step)
        (s/update elapsed-time)
        (update-cursor elapsed-time)
        (evaluate))
    screen))

(defn render-dialogue
  [{:keys [state storyteller context] :as screen}]
  (let [{:keys [cursor cursor-delta
                dialogue-bounds nametag-position]} state
        [x y w h] dialogue-bounds
        step (+ 50 (int (/ w (r/measure-text-length context "m"))))
        words (string/split ((comp :display-message :state) storyteller) #"\s")
        nametag ((comp :name :current-state) storyteller)
        namecolor ((comp :color :current-state) storyteller)
        lines (if (> (count words) 1)
                     (loop [ws '() acc [] curr words]
                       (cond
                        (empty? curr)
                          (reverse (conj ws (string/join " " acc)))
                        (< step (count (string/join " " (conj acc (first curr)))))
                          (recur (conj ws (string/join " " acc)) [] curr)
                        :else
                          (recur ws (conj acc (first curr)) (rest curr))))
                     words)
        iterators (zipmap (drop-last lines) (range (count (drop-last lines))))
        offset (cond (< 0 cursor-delta 101) -2
                     (or (< 100 cursor-delta 201)
                         (< 700 cursor-delta 801)) -1
                     (or (< 200 cursor-delta 301)
                         (< 600 cursor-delta 701)) 0
                     (or (< 300 cursor-delta 401)
                         (< 500 cursor-delta 601)) 1
                     (< 400 cursor-delta 501) 2
                     :else 0)]
    (.save context)
    (set! (. context -shadowColor) "black")
    (set! (. context -shadowOffsetX) 1.5)
    (set! (. context -shadowOffsetY) 1.5)
    (doseq [[s i] iterators]
      (r/draw-text context [x (+ y (* i 35))] s "25px" "white"))
    (when-not (nil? (last lines))
      (r/draw-text-with-cursor context [x (+ y (* (dec (count lines)) 35))]
                               (last lines)
                               "25px" "white" cursor offset))
    (when-not (empty? nametag)
      (r/draw-text context nametag-position nametag "bold 29px" (name namecolor))))
    (.restore context))

(defn render-choice
  [{:keys [state storyteller context] :as screen}]
  (.save context)
  (set! (. context -shadowColor) "black")
  (set! (. context -shadowOffsetX) 1.5)
  (set! (. context -shadowOffsetY) 1.5)
  (let [name ((comp :choice-text :state) storyteller)
        options ((comp :option-names :state) storyteller)
        pos-w (int (/ (r/measure-text-length context name) 2))]
    (r/draw-image context [415 180] :choicebg)
    (r/draw-text-centered context [640 220] name "30px" "white")
    (doseq [[s i] (zipmap options (range (count options)))]
      (r/draw-text context [(- 590 pos-w) (+ 285 (* i 45))] s "25px" "white")))
  (.restore context))

(defn render
  [{:keys [state context] :as screen} on-top]
  (let [bgs (reverse (:backgrounds state))
        sps (utils/sort-z-index (:spriteset state))]
    (doseq [s bgs]
      (r/draw-image context [0 0] s))
    (when on-top
      (doseq [s sps]
        (r/draw-sprite context (s (:sprites state))))
      (when (:show-ui? state)
        (r/draw-sprite context (:ui-img state)))
      (when ((comp :display-message :state :storyteller) screen)
        (render-dialogue screen))
      (when ((comp :choice-text :state :storyteller) screen)
        (render-choice screen))))
  screen)

(defn handle-input
  [screen on-top mouse]
  (if on-top
    (assoc-in screen [:state :input-state :mouse] mouse)
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
    :storyteller (s/StoryTeller. @s/RT-HOOKS {:type :dummy} 0 {} false true)
    }))
