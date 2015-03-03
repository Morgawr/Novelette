(ns novelette.GUI)

; This file contains the whole codebase for the Novelette GUI engine.

; A GUIElement is the basic datatype used to handle GUI operations.
(defrecord GUIElement [type ; The element type. (More about it later)
                       id ; Name/id of the GUI element
                       position ; Coordinates of the element [x y w h]
                       content ; Local state of the element (i.e.: checkbox checked? radio selected? etc)
                       children ; Vector of children GUIElements.
                       events ; Map of events. (More about it later)
                       focus? ; Whether or not the element has focus status.
                       z-index ; Depth of the Element in relation to its siblings. lower = front
                       render ; Render function called on the element.
                       ])

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
  "Generic render function called recursively on all GUI elements on the screen."
  [element ancestors]
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

(defn render-gui
  "Recursively calls into the registered elements render functions and displays
  them on the screen."
  [{:keys [GUI]}]
  (render GUI '()))

(defn create-canvas-element
  "Creates a new canvas GUI element with sane defaults."
  [canvas ctx]
  (let [element (GUIElement. :canvas
                             :canvas ; id
                             [0 0 (.-width canvas) (.-height canvas)]
                             {:entity canvas :context ctx}
                             [] ; no children yet
                             {} ; no events yet
                             true ; focus
                             10000 ; very low priority in depth
                             identity)] ; TODO - add render function for canvas
    element))

(defn add-element-to-GUI
  "Add a GUIElement to the GUI tree recursively looking for the specified
  parent."
  [element parent {:keys [GUI] :as screen}]
  (let [search (fn search [GUI-tree walk-list]
                 (let [elements (into [] (zipmap (:children GUI-tree) (range)))
                       found (some #(when (= (:id (first %)) parent)
                                      (second %)) elements)]
                   (cond
                     (seq elements) [walk-list false]
                     (seq found) [(conj walk-list found) true]
                     :else
                      (let [recursive-values (map search elements)
                            result (filter second recursive-values)]
                        (if (seq result)
                          (first result)
                          [walk-list false])))))
        search-result (search GUI [])
        tree-result (first search-result)
        sequence-of-steps (into [:GUI :children]
                                (interleave tree-result (repeat :children)))]
    (when-not (first search-result)
      (throw (js/Error. (str parent " id not found in GUI element list.") )))
    (update-in screen sequence-of-steps conj element)))
