(ns novelette.screen
  (:require-macros [schema.core :as s])
  (:require [goog.dom :as dom]
            [novelette.input]
            [novelette.render]
            [schema.core :as s]))

(def function (s/pred fn? 'fn?))

; This is the screen, a screen is the base data structure that contains
; all the data for updating, rendering and handling a single state instance
; in the game. Multiple screens all packed together make the full state of the
; game.
(s/defrecord Screen [id :- ( s/maybe (s/either s/Str s/Keyword)) ; Unique identifier of the screen
                     handle-input :- (s/maybe function) ; Function to handle input
                     update :- (s/maybe function) ; Function to update state
                     render :- (s/maybe function) ; Function to render on screen
                     deinit :- (s/maybe function) ; Function to destroy screen
                     canvas :- js/HTMLCanvasElement ; canvas of the game
                     context :- js/CanvasRenderingContext2D ; context of the canvas
                     next-frame :- (s/maybe function) ; What to do on the next game-loop
                     ]
  {s/Any s/Any})

; TODO - purge a lot of old data and cruft

(def BASE-SCREEN (Screen. "" nil nil nil nil nil nil nil))

(s/defrecord State [screen-list :- [Screen]
                    curr-time :- s/Num
                    context :- js/CanvasRenderingContext2D ; TODO - maybe invert order of this and canvas for consistency
                    canvas :- js/HTMLCanvasElement]
  {s/Any s/Any})

(defn get-animation-method []
  (let [window (dom/getWindow)
        methods ["requestAnimationFrame"
                 "webkitRequestAnimationFrame"
                 "mozRequestAnimationFrame"
                 "oRequestAnimationFrame"
                 "msRequestAnimationFrame"]
        options (map (fn [name] #(aget window name))
                     methods)]
    ((fn [[current & remaining]]
       (cond
        (nil? current)
          #((.-setTimeout window) % (/ 1000 30))
        (fn? (current))
          (current)
        :else
          (recur remaining)))
     options)))

(s/defn screen-loop
  [screen :- Screen
   on-top :- s/Bool
   elapsed-time :- s/Num]
  (let [update (:update screen)
        render (:render screen)
        input (:handle-input screen)]
    (-> screen
        (input on-top @novelette.input/INPUT-STATE)
        (update on-top elapsed-time)
        (render on-top))))

(s/defn iterate-screens
  [screen-list :- [Screen]
   elapsed-time :- s/Num]
  (let [length (count screen-list)]
    (doall
     (map-indexed (fn [n s]
                    (screen-loop s (= (dec length) n)
                                 elapsed-time))
                  screen-list))))

(s/defn remove-next-step
  [screen-list :- [Screen]]
  (if (empty? screen-list)
    []
    (doall
     (map #(assoc % :next-frame nil) screen-list))))

(s/defn push-screen
  [screen :- Screen
   screen-list :- [Screen]]
  (-> screen-list
      (remove-next-step)
      (#(conj (vec %) screen))))

(s/defn pop-screen
  [screen-list :- [Screen]]
  (let [oldscreen (last screen-list)]
    ((:deinit oldscreen) oldscreen))
  (pop (vec screen-list)))

(s/defn replace-screen
  [screen :- Screen
   screen-list :- [Screen]]
  (->> screen-list
       (pop-screen)
       (push-screen screen)))

(s/defn restart-screens
  [screen :- Screen
   screen-list :- []]
  (loop [s screen-list]
    (if (empty? s)
      [screen]
      (recur (pop-screen s)))))

(s/defn schedule-next-frame
  [state :- State]
  "This function executes the scheduled init for the next screen if it is
  required."
  (let [next-frame (:next-frame (last (:screen-list state)))]
    (cond
     (nil? next-frame) state ; Nothing to be done
     (fn? next-frame) (next-frame state)
     :else (.log js/console "ERROR: next frame was something else?!"))))

(s/defn update-time
  [state :- State
   curr-time :- s/Num]
  (assoc state :curr-time curr-time))

(s/defn clear-screen 
  [state :- State]
  (novelette.render/fill-clear (:canvas state) (:context state) "black")
  state)

(s/defn main-game-loop 
  [state :- State]
  (let [screen-list (:screen-list state)
        curr-time (.getTime (js/Date.))
        elapsed-time (- curr-time (:curr-time state))]
    (-> state
        (clear-screen)
        (assoc :screen-list (iterate-screens screen-list elapsed-time))
        (update-time curr-time)
        (schedule-next-frame)
        ((fn [s] ((get-animation-method) #(main-game-loop s)))))
    (novelette.input/mouse-declick)))
