(ns novelette.GUI)

; This file contains the whole codebase for the Novelette GUI engine.

; A GUIElement is the basic datatype used to handle GUI operations.
(defrecord GUIElement [type ; The element type. (More about it later)
                       position ; Coordinates of the element [x y w h]
                       content ; Local state of the element (i.e.: checkbox checked? radio selected? etc)
                       children ; Vector of children GUIElements.
                       events ; Map of events. (More about it later)
                       focus? ; Whether or not the element has focus status.
                       z-index ; Depth of the Element in relation to its siblings. lower = front
                       render ; Render function called on the element.
                       ])


; Element types can be:
; :button
; :label
; :slider
; :radio
; :checkbox
; :dialog
; :canvas <-- There can only be one canvas in the whole game, it is the base GUI element
;             used to catch all the base events when nothing else is hit. It is the root of the tree.


; Events can be:
; :clicked
; :on-focus
; :off-focus
; :on-hover
; :off-hover

(defn handle-input
  "Handles the input on the GUI engine. It delves into the subtree branch
  of the GUI element tree in a DFS and calls the appropriate handle-input
  function for each element until one of them returns false and stops
  propagating upwards."
  [screen]
  screen)

(defn render
  [element]
  ((:render element) element)
  (doseq [x (reverse (sort-by :z-index (:children element)))]
    ; This could cause a stack-overflow but the depth of the children tree
    ; will never get that big. If it gets that big then you probably should
    ; rethink your lifestyle and choices that brought you to do this in the
    ; first place. Please be considerate with the amount of alcohol you drink
    ; and don't do drugs.
    ;
    ; Tests on a (java) repl showed no stackoverflow until the tree reached
    ; a level of 1000-1500 nested elements. I'm not sure about clojurescript's
    ; stack but it shouldn't be worse... I hope.
    (render x)))