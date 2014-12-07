(ns novelette.screens.loadingscreen
  (:require-macros [novelette.syntax :as syntax])
  (:require [novelette.render :as r]
            [novelette.syntax :as syntax]
            [novelette.screen :as gscreen]
            [novelette.sound :as gsound]
            [novelette.screens.storyscreen]))

; This is the loading screen, it is the first screen that we load in the game
; and its task is to load all the resources (images, sounds, etc etc) of the
; game before we can begin playing.
(def image-list {:bgtest "img/background.png"
                 :bgtest2 "img/background2.png"
                 :dialogue-ui "img/dialogbg.png"
                 :bestgirl "img/bestgirl.png"
                 :kurt "img/kurt.png"
                 :cursor "img/cursor.png"
                 :choicebg "img/choicebg.png"})

(def audio-list {:bgm-beginning "sound/beginning"})

(defn can-play?
  [type]
  (not (empty? (. (js/Audio.) canPlayType type))))

(defn set-audio-extension
  [audio ext]
  (into {} (for [[k v] audio] [k (str v ext)])))

(defn generate-audio-list
  [audio]
  (let [canplayogg (can-play? "audio/ogg; codecs=vorbis")
        canplaymp3 (can-play? "audio/mpeg")]
    (cond
     canplayogg (set-audio-extension audio ".ogg")
     canplaymp3 (set-audio-extension audio ".mp3")
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

; ------------------------------- TESTING STUFF --------------------------

(syntax/defspeaker morg "Morgawr" :red)
(syntax/defspeaker horo "Horo" :brown)
(syntax/defspeaker kurt "Kurt" :yellow)

;(syntax/defscene scene3
;  (syntax/wait 1000)
;  (syntax/clear-backgrounds)
;  (syntax/wait 1000)
;  (syntax/background :bgtest))
;
;(syntax/defblock testblock
;  (syntax/clear-backgrounds)
;  (syntax/wait 1000)
;  (syntax/background :bgtest))
;
;(syntax/defscene scene2
;  (syntax/wait 1000)
;  (syntax/sprite :horo)
;  (syntax/wait 1000)
;  (syntax/teleport-sprite :horo [100 200])
;  (syntax/wait 1000)
;  (syntax/no-sprite :horo)
;  (syntax/jump-to-scene scene3))

(syntax/defscene yes-scene
  (syntax/show-ui)
  (morg "Kawaii :3c"))

(syntax/defscene no-scene
  (syntax/show-ui)
  (morg "Fuck you :3c"))


(syntax/defscene scene1
  ;(syntax/background :bgtest)
  (syntax/set-cps 30)
  (syntax/set-ui :dialogue-ui [0 420]) ; blaze it
  (syntax/set-nametag-position [40 490])
  (syntax/set-bounds 40 540 (- 1280 80) (- 800 540))
  (syntax/declare-sprite :horo :bestgirl [300 200] 2)
  (syntax/declare-sprite :kurt :kurt [750 20] 1)
  ;(syntax/sprite :horo)
  (syntax/show-ui)

  (morg "What is going on here?")
  (syntax/sprite :kurt)
  (kurt "Yo, s'up buddy"))
  ;(syntax/sprite :horo)
  ;(horo "H-h-hi... My name is Horo, I am the ancient spirit of a god-wolf. I am cute as fuck :3c and this is some text that wraps around the UI box and is awesome as fuck.")
  ;
  ;(syntax/hide-ui)
  ;(syntax/choice
  ; "Will you follow her?"
  ; (syntax/option "Yes!" yes-scene)
  ; (syntax/option "No!" no-scene)
  ; (syntax/option "Do I have condoms?" no-scene)
  ; (syntax/default "Yes!"))
  ;
  ;(syntax/no-sprite :horo)
  ;(syntax/narrate "And thus, the young man found himself...")
  ;(syntax/narrate "...with a brand new \"game\" engine"))

(def start-game
  (into novelette.screens.storyscreen/BASE-STATE
        {:scrollfront (:body scene1)}))


; ------------------ NO MORE TESTING STUFF -----------------

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
                   (count (:image-list screen)) (count (:audio-list screen))))))

(defn has-loaded?
  [num res-map]
  (= num (count res-map)))

(defn all-loaded?
  [imgcount sndcount]
  (and (has-loaded? imgcount @r/IMAGE-MAP)
       (has-loaded? sndcount @gsound/SOUND-MAP)))

(defn load-sounds
  [screen]
  (doseq [[k v] (:audio-list screen)] (gsound/load-sound v k))
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
  [ctx canvas images audios to-load]
  (doseq [[k v] images] (r/load-image v k))
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
             :audio-list (generate-audio-list audios)
             :image-list images
             :to-load to-load ;(novelette.screens.storyscreen/init ctx canvas start-game)
             })))
