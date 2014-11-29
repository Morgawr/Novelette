(ns novelette.screen
  (:require [goog.dom :as dom]
            [novelette.input]
            [novelette.render]))

; This is the screen, a screen is the base data structure that contains
; all the data for updating, rendering and handling a single state instance
; in the game. Multiple screens all packed together make the full state of the
; game.
(defrecord Screen [id ; Unique identifier of the screen
                   handle-input ; Function to handle input
                   update ; Function to update state
                   render ; Function to render on screen
                   deinit ; Function to destroy screen
                   images ; list of images to be drawn on screen
                   ; TODO - have the images with positions properly
                   canvas ; canvas of the game
                   context ; context of the canvas
                   next-frame ; What to do on the next game-loop
                   bgm ; background music
                   ])

(def BASE-SCREEN (Screen. "" nil nil nil nil [] nil nil nil nil))

(defrecord State [screen-list curr-time context canvas])

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

(defn screen-loop
  [screen on-top elapsed-time]
  (let [update (:update screen)
        render (:render screen)
        input (:handle-input screen)]
    (-> screen
        (input on-top @novelette.input/MOUSE-STATE)
        (update on-top elapsed-time)
        (render on-top))))

(defn iterate-screens
  [screen-list elapsed-time]
  (let [length (count screen-list)]
    (doall
     (map-indexed (fn [n s]
                    (screen-loop s (= (dec length) n)
                                 elapsed-time))
                  screen-list))))

(defn remove-next-step
  [screen-list]
  (if (empty? screen-list)
    []
    (doall
     (map #(assoc % :next-frame nil) screen-list))))

(defn push-screen
  [screen screen-list]
  (-> screen-list
      (remove-next-step)
      (#(conj (vec %) screen))))

(defn pop-screen
  [screen-list]
  (let [oldscreen (last screen-list)]
    ((:deinit oldscreen) oldscreen))
  (pop (vec screen-list)))

(defn replace-screen
  [screen screen-list]
  (->> screen-list
       (pop-screen)
       (push-screen screen)))

(defn restart-screens
  [screen screen-list]
  (loop [s screen-list]
    (if (empty? s)
      [screen]
      (recur (pop-screen s)))))

(defn schedule-next-frame
  [state]
  "This function executes the scheduled init for the next screen if it is
  required."
  (let [next-frame (:next-frame (last (:screen-list state)))]
    (cond
     (nil? next-frame) state ; Nothing to be done
     (fn? next-frame) (next-frame state)
     :else (.log js/console "ERROR: next frame was something else?!"))))

(defn update-time
  [state curr-time]
  (assoc state :curr-time curr-time))

(defn clear-screen [state]
  (novelette.render/fill-clear (:canvas state) (:context state) "black")
  state)

(defn main-game-loop [state]
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
