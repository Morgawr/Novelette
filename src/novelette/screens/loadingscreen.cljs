(ns novelette.screens.loadingscreen
  (:require-macros [schema.core :as s])
  (:require [novelette.render :as r]
            [novelette.screen :as gscreen]
            [novelette.schemas :as sc]
            [novelette.sound :as gsound]
            [novelette.screens.storyscreen]
            [schema.core :as s]))

; This is the loading screen, it is the first screen that we load in the game
; and its task is to load all the resources (images, sounds, etc etc) of the
; game before we can begin playing.
(s/defn can-play?
  [type :- s/Str]
  (not (empty? (. (js/Audio.) canPlayType type))))

(s/defn set-audio-extension
  [audio :- {s/Keyword s/Str}
   ext :- s/Str]
  (into {} (for [[k v] audio] [k (str v ext)])))

(s/defn generate-audio-list
  [audio :- {s/Keyword s/Str}]
  (let [canplayogg (can-play? "audio/ogg; codecs=vorbis")
        canplaymp3 (can-play? "audio/mpeg")]
    (cond
     canplayogg (set-audio-extension audio ".ogg")
     canplaymp3 (set-audio-extension audio ".mp3")
     :else (throw (js/Error. "Your browser does not seem to support the proper audio standards. The game will not work.")))))

(s/defn handle-input
  [screen :- sc/Screen
   input :- {s/Any s/Any}]
  (if (and (:complete screen) ((:clicked? input) 0) (not (:advance screen)))
    (assoc screen :advance true)
    screen))

(s/defn maybe-handle-input
  [screen :- sc/Screen
   on-top :- s/Bool
   input :- {s/Any s/Any}]
  (if on-top
    (handle-input screen input)
    screen))

(s/defn load-main-menu
  [screen :- sc/Screen]
  (let [ctx (:context screen)
        canvas (:canvas screen)]
    (assoc screen
      :next-frame
      (fn [state]
        (let [screen-list (:screen-list state)
              new-list (gscreen/replace-screen (:to-load screen) screen-list)]
          (assoc state :screen-list new-list))))))

(s/defn percentage-loaded
  [imgcount :- s/Int
   sndcount :- s/Int]
  (let [max (+ (count @r/IMAGE-MAP)
               (count @gsound/SOUND-MAP))
        ptg (* (/ max (+ imgcount sndcount)) 100)]
    (int ptg)))

(s/defn everything-loaded
  [screen :- sc/Screen]
  (let [complete true
        message "Finished loading, click to continue"]
    (assoc screen :complete complete :message message
      :percentage (percentage-loaded
                   (count (:image-list screen)) (count (:audio-list screen))))))

(s/defn has-loaded?
  [num :- s/Int
   res-map :- {s/Any s/Any}]
  (= num (count res-map)))

(s/defn all-loaded?
  [imgcount :- s/Int
   sndcount :- s/Int]
  (and (has-loaded? imgcount @r/IMAGE-MAP)
       (has-loaded? sndcount @gsound/SOUND-MAP)))

(s/defn load-sounds
  [screen :- sc/Screen]
  (doseq [[k v] (:audio-list screen)] (gsound/load-sound v k))
  (assoc screen :loading-status 1))

(s/defn screen-update
  [screen :- sc/Screen
   elapsed-time :- s/Int]
  (let [images (count (:image-list screen))
        sounds (count (:audio-list screen))]
    (cond
     (:advance screen)
       (load-main-menu screen)
     (:complete screen)
       screen
     (and (zero? (:loading-status screen))
          (has-loaded? images @r/IMAGE-MAP))
       (load-sounds screen)
     (all-loaded? images sounds)
       (everything-loaded screen)
     :else
       (assoc screen :percentage (percentage-loaded images sounds)))))

(s/defn draw
  [screen :- sc/Screen] ; TODO - set coordinates to proper resolution
  (r/draw-text-centered (:context screen) [690 310]
                        (:message screen) "25px" "white")
  (r/draw-text-centered (:context screen) [690 360]
                        (str (:percentage screen) "%") "25px" "white")
  screen)

(s/defn init
  [ctx :- js/CanvasRenderingContext2D
   canvas :- js/HTMLCanvasElement
   images :- {s/Any s/Any}
   audios :- {s/Any s/Any}
   to-load :- sc/Screen]
  (doseq [[k v] images] (r/load-image v k))
  (-> gscreen/BASE-SCREEN
      (into {
             :id "LoadingScreen"
             :update (fn [screen _ elapsed-time] (screen-update screen elapsed-time))
             :render (fn [screen _] (draw screen))
             :handle-input maybe-handle-input
             :next-frame nil
             :context ctx
             :canvas canvas
             :deinit (fn [s] nil)
             :advance false
             :complete false
             :message "Loading..."
             :percentage 0
             :loading-status 0 ; 0 = image, 1 = sound
             :audio-list (generate-audio-list audios)
             :image-list images
             :to-load to-load ;(novelette.screens.storyscreen/init ctx canvas start-game)
             })))
