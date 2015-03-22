(ns novelette.GUI.canvas
  (:require-macros [schema.core :as s])
  (:require [novelette.schemas :as sc]
            [schema.core :as s]
            [novelette.render]))

(s/defn render-canvas
  [{{ctx :context 
     canvas :entity
     base-color :base-color} :content} :- sc/GUIElement
   ancestors :- [sc/GUIElement]]
  (novelette.render/fill-clear canvas ctx base-color )
  nil)

(s/defn create-canvas-element
  "Creates a new canvas GUI element with sane defaults."
  [canvas :- js/HTMLCanvasElement
   ctx :- js/CanvasRenderingContext2D
   base-color :- s/Str]
  (let [content {:entity canvas
                 :context ctx
                 :base-color base-color}]
    (sc/GUIElement. :canvas
                    :canvas ; id
                    [0 0 (.-width canvas) (.-height canvas)]
                    content
                    [] ; no children yet
                    {} ; no events yet
                    true ; focus
                    10000 ; very low priority in depth
                    render-canvas)))
