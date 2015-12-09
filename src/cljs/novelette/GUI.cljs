(ns novelette.GUI
  (:require-macros [schema.core :as s])
  (:require [novelette.schemas :as sc]
            [schema.core :as s]
            [novelette.input]
            [novelette.utils :as utils]))

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

(s/defn absolute-position
  "Calculate the absolute position given a list of ancestors and the currently
  relative position within the parent element."
  [ancestors :- [sc/GUIElement]
   position :- sc/pos]
   (vec (reduce #(map +  (:position %2) %1) position ancestors)))

(s/defn handle-input-event
  "Given a GUIElement, screen and event type, act on the individual event."
  [element :- sc/GUIElement
   event :- sc/GUIEvent
   screen :- sc/Screen]
  (if (some? (event (:events element)))
    ((event (:events element)) element screen)
    [screen true]))

; Event types are :clicked :on-focus :off-focus :on-hover :off-hover
; as specified in the schema.cljs file
(s/defn retrieve-event-trigger
  "Analyze the current input state to apply the correct event to the given
  GUI element."
  [element :- sc/GUIElement
   ancestors :- [sc/GUIElement]
   screen :- sc/Screen]
  (let [{x :x y :y
         clicked? :clicked?} @novelette.input/INPUT-STATE
        bounding-box (absolute-position ancestors (:position element))])
    ; if the mouse is inside the bounding box:
    ;   -> check if the element has hover? as true
    ;      -> if yes, do nothing
    ;      -> if no, enable hover? and call the :on-hover event trigger
    ; if the mouse is outside the bounding box:
    ;   -> check if the element has hover? as true
    ;      -> if yes, disable hover? and call the :off-hover event trigger
    ;      -> if no, do nothing
    ; if the mouse has any button state as clicked:
    ;   -> check if the mouse is inside the bounding box:
    ;      -> if yes, call the :clicked event on the element
    ;      -> if no, do nothing
    ; TODO: figure out how to deal with on/off focus (keyboard tab-selection?)
    ;(.log js/console (pr-str bounding-box)))
  [screen true])

(s/defn walk-GUI-events
  "Walk through the GUI tree dispatching events."
  [element :- sc/GUIElement
   ancestors :- [sc/GUIElement]
   screen :- sc/Screen]
  (let [walk-fn (fn [[screen continue?] child]
                  (if continue?
                    (walk-GUI-events
                      child (conj ancestors element)
                      screen) ; Recursion, be careful on the depth!
                    [screen continue?]))
        [new-screen continue?] (reduce walk-fn [screen true] (:children element))]
    (if continue?
      (retrieve-event-trigger element ancestors new-screen)
      [new-screen continue?])))

(s/defn handle-input
  "Handle the input on the GUI engine. It delves into the subtree branch
  of the GUI element tree in a DFS and calls the appropriate handle-input
  function for each element until one of them returns false and stops
  propagating upwards."
  [{:keys [GUI] :as screen}]
  (if (:enabled? @novelette.input/INPUT-STATE)
    (first (walk-GUI-events GUI '() screen))
    screen))

(s/defn render
  "Generic render function called recursively on all GUI elements on the screen."
  [element :- sc/GUIElement
   ancestors :- [sc/GUIElement]]
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

(s/defn find-GUI-element-path
  "Recursively look for a specific ID of a GUI element in a GUI tree.
  If the element is found, return a path of IDs to reach it, otherwise nil."
  [id :- (s/cond-pre s/Str s/Keyword)
   GUI-tree :- sc/GUIElement
   walk-list :- [s/Any]]
  (let [found? (seq (filter #(= (:id %) id) (:children GUI-tree)))
        next-walk-list (conj walk-list (:id GUI-tree))]
    (cond
      (= id (:id GUI-tree)) []
      (empty? (:children GUI-tree)) nil
      found? next-walk-list
      :else (let [result
                  (keep identity
                        (map #(find-GUI-element-path id % next-walk-list)
                             (:children GUI-tree)))]
              (when (seq result) (first result))))))

(s/defn create-GUI-children-path
  "Given a list of IDs and the root of the tree, create a path."
  [root :- sc/GUIElement
   ancestors :- [s/Any]]
  (loop [element root remaining (rest ancestors) path []]
    (cond
      (not (seq remaining)) path
      :else
      (let [result (keep-indexed
                     (fn [idx el]
                       (when (= (:id el) (first remaining)) idx))
                     (:children element))]
        (when (seq result)
          (recur ((:children element) (first result))
                 (rest remaining)
                 (conj (conj path :children) (first result))))))))

(s/defn render-gui
  "Recursively calls into the registered elements render functions and displays
  them on the screen."
  [{GUI :GUI} :- sc/Screen]
  (render GUI '()))

(s/defn replace-GUI-element
  "Find an element in the GUI tree and replace it with the new one."
  [element :- sc/GUIElement
   id :- (s/cond-pre s/Str s/Keyword)
   {:keys [GUI] :as screen} :- sc/Screen]
  (let [search (find-GUI-element-path id GUI [])]
    (when (nil? search)
      (throw (js/Error. (str id " id not found in GUI element list."))))
    (if (seq search)
      (let [path (create-GUI-children-path GUI search)]
        (assoc-in screen (concat [:GUI :children] path) element))
      (assoc screen :GUI element))))

(s/defn add-GUI-element
  "Add a GUIElement to the GUI tree given the specified parent ID."
  [element :- sc/GUIElement
   parent :- (s/cond-pre s/Str s/Keyword)
   {:keys [GUI] :as screen} :- sc/Screen]
  (let [search (find-GUI-element-path parent GUI [])]
    (when (nil? search)
      (throw (js/Error. (str parent " id not found in GUI element list."))))
    (let [path (create-GUI-children-path GUI search)]
      (update-in screen (concat [:GUI :children] path [:children])
                 conj element))))
