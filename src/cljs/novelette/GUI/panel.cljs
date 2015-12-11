(ns novelette.GUI.panel
  (:require-macros [schema.core :as s])
  (:require [novelette.schemas :as sc]
            [schema.core :as s]
            [novelette.render]
            [novelette.utils :as u]
            [novelette.GUI :as GUI]))

(s/defn render
  [{{ctx :context
     bg-color :bg-color} :content
    position :position} :- sc/GUIElement
   ancestors :- [sc/GUIElement]]
  (let [abs-position (GUI/absolute-position ancestors position)]
    (novelette.render/draw-rectangle ctx bg-color abs-position)
  nil))

(s/defn create
  "Creates a new panel GUI element with sane defaults."
  [ctx :- js/CanvasRenderingContext2D
   id :- sc/id
   position :- sc/pos
   z-index :- s/Int
   extra :- {s/Any s/Any}]
  (let [content (merge {:context ctx
                        :bg-color "white"} extra)]
    (sc/GUIElement. :panel
                    id
                    position
                    content
                    []
                    {}
                    false ; focus
                    false ; hover
                    z-index
                    render)))
