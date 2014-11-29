(ns novelette.screen.dialoguescreen
  (:require [novelette.render :as r]
            [novelette.screen :as gscreen]))

(defn printed-full-message?
  [screen]
  (let [letters (:letters screen)
        msg (first (:messages screen))]
    (>= letters (count msg))))

(defn handle-input
  [screen mouse]
  (let [{:keys [x y clicked]} mouse]
    (if clicked
      (if (printed-full-message? screen)
        (assoc screen :advance true)
        (assoc screen :letters (count (first (:messages screen)))))
      screen)))

(defn maybe-handle-input
  [screen on-top mouse]
  (if on-top
    (handle-input screen mouse)
    screen))

(defn end-message
  [screen]
  (assoc screen
    :next-frame
    (fn [state]
      (let [screen-list (:screen-list state)
            new-list (gscreen/pop-screen screen-list)]
        (assoc state :screen-list new-list)))))

(defn next-letter
  [screen]
  (if (printed-full-message? screen)
  (:letters screen)
  (inc (:letters screen))))

(defn update
  [screen elapsed-time]
  (let [adv (:advance screen)
        interval (:interval screen)
        letters (:letters screen)
        msgs (:messages screen)
        speed (:speed screen)
        interval (+ (:interval screen) elapsed-time)
        newinterval (- interval speed)]
    (cond
     (and adv (empty? (rest msgs)))
       (end-message screen)
      adv
       (assoc screen
         :interval 0
         :messages (rest msgs)
         :advance false
         :letters 0)
     (pos? newinterval)
       (assoc screen
         :interval newinterval
         :letters (next-letter screen))
     :else
       (assoc screen :interval interval))))

(defn maybe-update
  [screen on-top elapsed-time]
  (if on-top (update screen elapsed-time) screen))

(defn draw
  [screen]
  (let [bg (:background screen)
        ctx (:context screen)
        msg (first (:messages screen))
        letters (:letters screen)]
    (r/draw-image ctx [0 400] bg)
    (r/draw-multiline-center-text ctx [400 450]
                                       (subs msg 0 letters)
                                       "25px" "white" 65 30)
    screen))

(defn maybe-draw
  [screen on-top]
  (if on-top (draw screen) screen))

(defn init [ctx canvas event-after messages]
  (-> gscreen/BASE-SCREEN
      (into {
             :id "DialogueScreen"
             :update maybe-update
             :render maybe-draw
             :handle-input maybe-handle-input
             :next-frame nil
             :background :dialoggui
             :context ctx
             :canvas canvas
             :images []
             :deinit (fn [s] nil)
             :event-after event-after
             :messages messages ; list of messages to print
             :letters 0 ; letters of current message already printed
             :interval 0 ; milliseconds passed since last letter printed
             :speed 50 ; number of milliseconds before printing another letter
             })))
