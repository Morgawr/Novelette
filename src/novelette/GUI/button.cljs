(ns novelette.GUI.button
  (:require-macros [schema.core :as s])
  (:require [novelette.schemas :as sc]
            [novelette-sprite.schemas :as scs]
            [schema.core :as s]
            [novelette.render]
            [novelette.utils :as u]
            [novelette.GUI :as GUI]))

(s/defn render
  [{{ctx :context
     bg-color :bg-color
     fg-color :fg-color
     text :text
     font-size :font-size} :content
    position :position} :- sc/GUIElement
   ancestors :- [sc/GUIElement]]
  (let [abs-position (GUI/absolute-position ancestors position)
        text-center (GUI/absolute-position ancestors
                                           (update (u/get-center-coordinates position)
                                                   1 #(- % (/ font-size 2))))]
    (novelette.render/draw-rectangle ctx bg-color abs-position)
    (novelette.render/draw-text-centered ctx text-center text (str font-size "px") fg-color))
  nil)

(s/defn create
  "Creates a new button GUI element with sane defaults."
  [ctx :- js/CanvasRenderingContext2D
   id :- sc/id
   text :- s/Str
   position :- scs/pos
   z-index :- s/Int
   extra :- {s/Any s/Any}]
  (let [content (merge {:context ctx
                        :bg-color "white"
                        :fg-color "black"
                        :text text
                        :font-size 20} extra)]
    (sc/GUIElement. :button
                    id
                    position
                    content
                    []
                    {}
                    false ; focus
                    false ; hover
                    z-index
                    render)))
