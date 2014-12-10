(ns novelette.sound
  (:require [goog.dom :as dom]))

(def SOUND-MAP (atom {}))

(def BGM (atom {}))

(declare load-sound)

(defn load-error
  [uri sym]
  (let [window (dom/getWindow)]
    (.setTimeout window #(load-sound uri sym) 200)))

(defn load-sound
  [uri sym]
  (let [sound (js/Audio.)]
    (set! (.-loop sound) true)
    (.addEventListener sound "loadeddata" #(swap! SOUND-MAP assoc sym sound))
    (set! (.-onerror sound) #(load-error uri sym))
    (set! (.-src sound) uri)))

(defn stop-audio
  [sound]
  (.pause sound))

(defn play-audio
  [sound loop?]
  (set! (.-currentTime sound) 0)
  (set! (.-loop sound) loop?)
  (.play sound))

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
