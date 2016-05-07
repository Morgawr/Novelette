(ns novelette.GUI.story-ui
  (:require-macros [schema.core :as s])
  (:require [novelette.schemas :as sc]
            [novelette-sprite.schemas :as scs]
            [schema.core :as s]
            [novelette.render]
            [novelette.utils :as u]
            [novelette.GUI :as GUI]
            novelette.GUI.button
            novelette.GUI.panel
            novelette.GUI.label))

(s/defn create-dialogue-panel
  [screen :- sc/Screen]
  (GUI/add-element (novelette.GUI.panel/create
                     (:context screen)
                     :dialogue-panel
                     [30 500 1240 200] 10 ; TODO - Remove this hardcoded stuff and make it more general-purpose
                     {:bg-color "#304090"})
                   :canvas screen))

(s/defn create-multiple-choice-panel
  [id :- sc/id
   ctx :- js/CanvasRenderingContext2D]
  (novelette.GUI.panel/create ctx id [320 180 640 360]
                              20 {:bg-color "#405599"}))

(s/defn mult-clicked
  [element :- sc/GUIElement
   screen :- sc/Screen]
  [(assoc-in screen [:storyteller :state :choice] (:id element)) false])

(s/defn mult-on-hover
  [element :- sc/GUIElement
   screen :- sc/Screen]
  (let [on-hover-color-map (get-in element [:content :on-hover-color])]
    [(-> screen
         (#(GUI/assoc-element (:id element) % [:hover?] true))
         (#(GUI/update-element (:id element) % [:content]
                               merge on-hover-color-map)))
     false]))

(s/defn mult-off-hover
  [element :- sc/GUIElement
   screen :- sc/Screen]
  (let [off-hover-color-map (get-in element [:content :off-hover-color])]
    [(-> screen
         (#(GUI/assoc-element (:id element) % [:hover?] false))
         (#(GUI/update-element (:id element) % [:content]
                               merge off-hover-color-map)))
     false]))


(s/defn add-multiple-choice-buttons
  [ids :- [sc/id]
   {:keys [context] :as screen} :- sc/Screen]
  (let [gen-button-data (fn [id position]
                          (-> (novelette.GUI.button/create
                                context id id position 20
                                {:bg-color "#222222" ; TODO - this is ugly, fix it
                                 :fg-color "#772222"
                                 :on-hover-color {:bg-color "#FFFFFF"
                                                  :fg-color "#000000"}
                                 :off-hover-color {:bg-color "#222222"
                                                   :fg-color "#772222"}
                                 :font-size 20})
                              (GUI/add-event-listener :clicked
                                                      mult-clicked)
                              (GUI/add-event-listener :on-hover
                                                      mult-on-hover)
                              (GUI/add-event-listener :off-hover
                                                      mult-off-hover)))
        offset-y 60
        starting-pos [160 80 310 50]]
    (loop [screen screen counter 0 opts ids]
      (if (seq opts)
        (recur (GUI/add-element
                 (gen-button-data (first opts)
                                  (update starting-pos 1
                                          (partial + (* offset-y counter))))
                 :choice-panel screen)
               (inc counter)
               (rest opts))
        screen))))

(s/defn spawn-explicit-choice-gui
  [{:keys [storyteller state] :as screen} :- sc/Screen]
  (let [{{:keys [choice-text option-names]} :state} storyteller]
    (-> screen
        (#(GUI/add-element (create-multiple-choice-panel
                             :choice-panel (:context %)) :canvas %))
        (#(GUI/add-element (novelette.GUI.panel/create (:context %)
                                                       :choice-panel-title
                                                       [0 0 640 60] 19
                                                       {:bg-color "#607070"})
                           :choice-panel %))
        (#(GUI/add-element (novelette.GUI.label/create (:context %)
                                                       :choice-label-title
                                                       choice-text
                                                       [0 0 640 60] 19
                                                       {:fg-color "#000000"
                                                        :transparent? true
                                                        :font-size 30})
                           :choice-panel-title %))
        (#(add-multiple-choice-buttons option-names %)))))
