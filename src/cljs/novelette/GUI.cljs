(ns novelette.GUI
  (:require-macros [schema.core :as s])
  (:require [novelette.schemas :as sc]
            [schema.core :as s]))

; This file contains the whole codebase for the Novelette GUI engine.

; Note on IDs: When creating a new GUI element it is possible to specify
; its parent ID. In case of duplicate IDs the function walks through the
; GUI entity graph as depth-first search and adds the new element to the
; first matching ID. This search's behavior is undefined if two or more
; elements have the same ID.

; Element types can be:
; :button
; :label
; :slider
; :radio
; :checkbox
; :dialog
; :canvas <-- There can only be one canvas in the whole game, it is the base GUI element
;             used to catch all the base events when nothing else is hit. It is the root of the tree.

(s/defn handle-input
  "Handles the input on the GUI engine. It delves into the subtree branch
  of the GUI element tree in a DFS and calls the appropriate handle-input
  function for each element until one of them returns false and stops
  propagating upwards."
  [screen :- sc/Screen]
  screen)

(s/defn render
  "Generic render function called recursively on all GUI elements on the screen."
  [element ;:- sc/GUIElement
   ancestors] ;:- [sc/GUIElement]]
  ((:render element) element ancestors)
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
    (render x (conj ancestors element))))

(s/defn render-gui
  "Recursively calls into the registered elements render functions and displays
  them on the screen."
  [{:keys [GUI]}] ;:- sc/Screen]
  (render GUI '()))

(s/defn create-canvas-element
  "Creates a new canvas GUI element with sane defaults."
  [canvas; :- js/HTMLCanvasElement
   ctx ];:- js/CanvasRenderingContext2D]
  (let [element (sc/GUIElement. :canvas
                             :canvas ; id
                             [0 0 (.-width canvas) (.-height canvas)]
                             {:entity canvas :context ctx}
                             [] ; no children yet
                             {} ; no events yet
                             true ; focus
                             10000 ; very low priority in depth
                             identity)] ; TODO - add render function for canvas
    element))

(s/defn add-element-to-GUI
  "Add a GUIElement to the GUI tree recursively looking for the specified
  parent."
  [element; :- sc/GUIElement
   parent; :- s/Keyword
   {:keys [GUI] :as screen}]
  (let [search (fn search [GUI-tree walk-list]
                 (let [elements (into [] (zipmap (:children GUI-tree) (range)))
                       found (some #(when (= (:id (first %)) parent)
                                      (second %)) elements)]
                   (cond
                     (not (seq elements)) [walk-list false]
                     (not (nil? found)) [(conj walk-list found) true]
                     :else
                     (let [recursive-values (map search elements)
                           result (filter second recursive-values)]
                       (if (seq result)
                         (first result)
                         [walk-list false])))))
        at-root (= (:id GUI) parent)] ; if parent ID is the root of the tree
    (if at-root
      (update-in screen [:GUI :children] conj element)
      (let [search-result (search GUI [])
            tree-result (first search-result)
            sequence-of-steps (into [:GUI :children]
                                    (interleave tree-result 
                                                (repeat :children)))]
        (when-not (second search-result)
          (throw (js/Error. (str parent 
                                 " id not found in GUI element list.") )))
        (update-in screen sequence-of-steps conj element)))))
