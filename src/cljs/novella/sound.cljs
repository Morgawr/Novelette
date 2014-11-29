(ns novella.sound
  (:require [goog.dom :as dom]))

(def SOUND-MAP (atom {}))

(def BGM (atom []))

(declare load-sound)

(defn load-error
  [uri sym]
  (let [window (dom/getWindow)]
    (. window setTimeout #(load-sound uri sym) 200)))

(defn load-sound
  [uri sym]
  (let [sound (js/Audio.)]
    (set! (. sound -loop) true)
    (. sound addEventListener "loadeddata" #(swap! SOUND-MAP assoc sym sound))
    (set! (. sound -onerror) #(load-error uri sym))
    (set! (. sound -src) uri)))

(defn stop-audio
  [sound]
  (. sound pause))

(defn play-audio
  [sound]
  (set! (. sound -currentTime) 0)
  (set! (. sound -loop) true)
  (. sound play))

(defn play-bgm
  [sym]
  (let [curr-sym (second @BGM)
        sound1 (first @BGM)
        sound2 (sym @SOUND-MAP)]
    (cond
     (nil? curr-sym)
       (do
         (play-audio sound2)
         (reset! BGM [sound2 sym]))
     (= curr-sym sym)
       nil
     :else
       (do
         (stop-audio sound1)
         (play-audio sound2)
         (reset! BGM [sound2 sym])))))

(defn screen-start-bgm
  [screen]
  (play-bgm (:bgm screen))
  screen)
