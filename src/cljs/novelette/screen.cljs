(ns novelette.screen
  (:require-macros [schema.core :as s])
  (:require [goog.dom :as dom]
            [novelette.schemas :as sc]
            [novelette.input]
            [novelette.render]
            [schema.core :as s]))

(def BASE-SCREEN (sc/Screen. "" nil nil nil nil nil nil nil))

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
  [screen :- sc/Screen
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
  [screen-list :- [sc/Screen]
   elapsed-time :- s/Num]
  (let [length (count screen-list)]
    (doall
     (map-indexed (fn [n s]
                    (screen-loop s (= (dec length) n)
                                 elapsed-time))
                  screen-list))))

(s/defn remove-next-step
  [screen-list :- [sc/Screen]]
  (if (empty? screen-list)
    []
    (doall
     (map #(assoc % :next-frame nil) screen-list))))

(s/defn push-screen
  [screen :- sc/Screen
   screen-list :- [sc/Screen]]
  (-> screen-list
      (remove-next-step)
      (#(conj (vec %) screen))))

(s/defn pop-screen
  [screen-list :- [sc/Screen]]
  (let [oldscreen (last screen-list)]
    ((:deinit oldscreen) oldscreen))
  (pop (vec screen-list)))

(s/defn replace-screen
  [screen :- sc/Screen
   screen-list :- [sc/Screen]]
  (->> screen-list
       (pop-screen)
       (push-screen screen)))

(s/defn restart-screens
  [screen :- sc/Screen
   screen-list :- []]
  (loop [s screen-list]
    (if (empty? s)
      [screen]
      (recur (pop-screen s)))))

(s/defn schedule-next-frame
  [state :- sc/State]
  "This function executes the scheduled init for the next screen if it is
  required."
  (let [next-frame (:next-frame (last (:screen-list state)))]
    (cond
     (nil? next-frame) state ; Nothing to be done
     (fn? next-frame) (next-frame state)
     :else (.log js/console "ERROR: next frame was something else?!"))))

(s/defn update-time
  [state :- sc/State
   curr-time :- s/Num]
  (assoc state :curr-time curr-time))

(s/defn clear-screen 
  [state :- sc/State]
  (novelette.render/fill-clear (:canvas state) (:context state) "black")
  state)

(s/defn main-game-loop 
  [state :- sc/State]
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
