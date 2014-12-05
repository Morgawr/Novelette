(ns novelette.sound
  (:require [goog.dom :as dom]))

(def SOUND-MAP (atom {}))

(def BGM (atom {}))

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
  [sound loop?]
  (set! (. sound -currentTime) 0)
  (set! (. sound -loop) loop?)
  (. sound play))

(defn stop-bgm
  []
  (let [sym (first (keys @BGM))
        bgm (@BGM sym)]
    (when sym
      (stop-audio bgm))
    (reset! BGM {})))

(defn play-bgm
  [sym]
  (let [curr-sym (first (keys @BGM))
        sound1 (@BGM curr-sym)
        sound2 (sym @SOUND-MAP)]
    (cond
     (nil? curr-sym)
       (do
         (play-audio sound2 true)
         (reset! BGM {sym sound2}))
     (= curr-sym sym)
       nil
     :else
         (do
           (stop-bgm)
           (play-bgm sym)))))

(defn screen-start-bgm
  [screen]
  (play-bgm (:bgm screen))
  screen)
