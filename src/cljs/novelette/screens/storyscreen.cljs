(ns novelette.screens.storyscreen
  (:require-macros [schema.core :as s])
  (:require [novelette.render :as r]
            [novelette.GUI :as GUI]
            [novelette.GUI.canvas]
            [novelette.screen :as gscreen]
            [novelette.sound :as gsound]
            [novelette.storyteller :as st]
            [novelette.GUI :as g]
            [novelette.schemas :as sc]
            [novelette.utils :as utils]
            [clojure.string :as string]
            [schema.core :as s]))

; TODO - Maybe remove the base state? It's only used in the init.
(def BASE-STATE (sc/StoryState. '() '() #{} {} '() {} 0 true false
                                (sc/Sprite. :dialogue-ui [0 0] 0) {} :cursor 0
                                [0 0 0 0] [0 0]))

(s/defn update-cursor ; TODO - move this into the GUI
  [{:keys [state] :as screen} :- sc/Screen
   elapsed-time :- s/Int]
  (let [cursor-delta (:cursor-delta state)]
    (if (> (+ cursor-delta elapsed-time) 800)
      (assoc-in screen [:state :cursor-delta]
                (+ cursor-delta elapsed-time -800))
      (assoc-in screen [:state :cursor-delta]
                (+ cursor-delta elapsed-time)))))

(s/defn update-gui
  [screen :- sc/Screen
   elapsed-time :- s/Int]
  (update-cursor screen elapsed-time)) ; TODO - move this into the GUI

(s/defn screen-update
  [screen :- sc/Screen
   on-top :- s/Bool
   elapsed-time :- s/Int]
  (cond-> screen
          on-top
          (-> (st/screen-update elapsed-time)
              (update-gui elapsed-time))))

(s/defn render-dialogue
  [{:keys [state storyteller context] :as screen} :- sc/Screen]
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

(s/defn render-choice
  [{:keys [state storyteller context] :as screen} :- sc/Screen]
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

(s/defn render ; TODO - move this to GUI
  [{:keys [state context] :as screen} :- sc/Screen
   on-top :- s/Bool]
  (let [bgs (reverse (:backgrounds state))
        sps (if (seq (:spriteset state)) (utils/sort-z-index ((apply juxt (:spriteset state)) (:sprites state))) [])]
    (GUI/render-gui screen)
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

(s/defn handle-input ; TODO - send events to GUI hooks
  [screen :- sc/Screen
   on-top :- s/Bool
   input :- {s/Any s/Any}]
  (cond-> screen
    on-top (#(GUI/handle-input
               (assoc-in % [:state :input-state] input)))))

(s/defn init
  [ctx :- js/CanvasRenderingContext2D
   canvas :- js/HTMLCanvasElement
   gamestate :- sc/StoryState]
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
         :storyteller (sc/StoryTeller. @st/RT-HOOKS {:type :dummy} 0 {} false)
         :GUI (novelette.GUI.canvas/create-canvas-element canvas ctx "black")}))
; TODO - Find a way to properly pass user-provided init data to the canvas
; and other possible GUI elements. In this case it's the 'black' color.
