(ns novelette.screens.loadingscreen
  (:require-macros [novelette.syntax :as syntax])
  (:require [novelette.render :as r]
            [novelette.screen :as gscreen]
            [novelette.sound :as gsound]
            [novelette.screens.storyscreen]))

; This is the loading screen, it is the first screen that we load in the game
; and its task is to load all the resources (images, sounds, etc etc) of the
; game before we can begin playing.
(def image-list {:bgtest "img/background.png"
                 :bgtest2 "img/background2.png"
                 :dialogue-ui "img/dialogbg.png"})

(def audio-list {:bgm-beginning "sound/beginning"})

(defn can-play?
  [type]
  (not (empty? (. (js/Audio.) canPlayType type))))

(defn set-audio-extension
  [ext]
  (set! audio-list (into {} (for [[k v] audio-list] [k (str v ext)]))))

(defn generate-audio-list
  []
  (let [canplayogg (can-play? "audio/ogg; codecs=vorbis")
        canplaymp3 (can-play? "audio/mpeg")]
    (cond
     canplayogg (set-audio-extension ".ogg")
     canplaymp3 (set-audio-extension ".mp3")
     :else (js/alert "Your browser does not seem to support the proper audio standards. The game will not work."))))

(defn handle-input
  [screen mouse]
  (if (and (:complete screen) (:clicked mouse) (not (:advance screen)))
    (assoc screen :advance true)
    screen))

(defn maybe-handle-input
  [screen on-top mouse]
  (if on-top
    (handle-input screen mouse)
    screen))

(syntax/defscene scene1
  (syntax/background :bgtest)
  ;(syntax/bgm :bgm-beginning)
  (syntax/background :bgtest2)
  (syntax/wait 1000)
  (syntax/set-ui :dialogue-ui [0 300])
  (syntax/show-ui))

(def start-game
  (into novelette.screens.storyscreen/BASE-STATE
        {:scrollfront (:body scene1)}))

(defn load-main-menu
  [screen]
  (let [ctx (:context screen)
        canvas (:canvas screen)]
    (assoc screen
      :next-frame
      (fn [state]
        (let [screen-list (:screen-list state)
              mmenu (novelette.screens.storyscreen/init ctx canvas
                                                        start-game) ; TODO - pass screen to load
              new-list (gscreen/replace-screen mmenu screen-list)]
          (assoc state :screen-list new-list))))))

(defn percentage-loaded
  [imgcount sndcount]
  (let [max (+ (count @r/IMAGE-MAP)
               (count @gsound/SOUND-MAP))
        ptg (* (/ max (+ imgcount sndcount)) 100)]
    (int ptg)))

(defn everything-loaded
  [screen]
  (let [complete true
        message "Finished loading, click to continue"]
    (assoc screen :complete complete :message message
      :percentage (percentage-loaded
                   (count image-list) (count audio-list)))))

(defn has-loaded?
  [num res-map]
  (= num (count res-map)))

(defn all-loaded?
  [imgcount sndcount]
  (and (has-loaded? imgcount @r/IMAGE-MAP)
       (has-loaded? sndcount @gsound/SOUND-MAP)))

(defn load-sounds
  [screen]
  (doseq [[k v] audio-list] (gsound/load-sound v k))
  (assoc screen :loading-status 1))

(defn update
  [screen elapsed-time]
  (let [images (count image-list)
        sounds (count audio-list)]
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

(defn draw
  [screen]
  (r/draw-text-centered (:context screen) [400 250]
                        (:message screen) "25px" "white")
  (r/draw-text-centered (:context screen) [400 300]
                        (str (:percentage screen) "%") "25px" "white")
  screen)

(defn init
  [ctx canvas]
  (generate-audio-list)
  (doseq [[k v] image-list] (r/load-image v k))
  (-> gscreen/BASE-SCREEN
      (into {
             :id "LoadingScreen"
             :update (fn [screen _ elapsed-time] (update screen elapsed-time))
             :render (fn [screen _] (draw screen))
             :handle-input maybe-handle-input
             :next-frame nil
             :context ctx
             :canvas canvas
             :images []
             :deinit (fn [s] nil)
             :advance false
             :complete false
             :message "Loading..."
             :percentage 0
             :loading-status 0 ; 0 = image, 1 = sound
             })))
