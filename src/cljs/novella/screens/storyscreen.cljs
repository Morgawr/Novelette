(ns novella.screens.storyscreen
  (:require [novella.render :as r]
            [novella.screen :as gscreen]
            [novella.sound :as gsound]
            [novella.screen.dialoguescreen]))

(def MESSAGES [])

(defn update
  [screen on-top elapsed-time]
  (if (:first-time screen)
    (let [ctx (:context screen)
          canvas (:canvas screen)]
      (assoc screen
        :next-frame (fn [state]
                      (let [screen-list (:screen-list state)
                            dialog (novella.screen.dialoguescreen/init ctx canvas nil MESSAGES)
                            new-list (gscreen/push-screen dialog screen-list)]
                        (assoc state :screen-list new-list)))))
    screen))

(defn render
  [screen on-top]
  screen)

(defn handle-input
  [screen on-top mouse]
  (assoc screen :mouse mouse))

(defn init
  [ctx canvas]
  (-> gscreen/BASE-SCREEN
      (into {
             :id "StoryScreen"
             :update update
             :render render
             :handle-input handle-input
             :context ctx
             :canvas canvas
             :deinit (fn [s] nil)
             :first-time true
             })))
