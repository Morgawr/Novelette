(ns novelette.sound
  (:require-macros [schema.core :as s])
  (:require [goog.dom :as dom]
            [schema.core :as s]))

(def SOUND-MAP (atom {}))

(def BGM (atom {}))

(declare load-sound)

(s/defn load-error
  [uri :- s/Str
   sym :- s/Keyword]
  (let [window (dom/getWindow)]
    (.setTimeout window #(load-sound uri sym) 200)))

(s/defn load-sound
  [uri :- s/Str
   sym :- s/Keyword]
  (let [sound (js/Audio.)]
    (set! (.-loop sound) true)
    (.addEventListener sound "loadeddata" #(swap! SOUND-MAP assoc sym sound))
    (set! (.-onerror sound) #(load-error uri sym))
    (set! (.-src sound) uri)))

(s/defn stop-audio
  [sound :- js/Audio]
  (.pause sound))

(s/defn play-audio
  [sound :- js/Audio
   loop? :- s/Bool]
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

(s/defn play-bgm
  [sym :- s/Keyword]
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
