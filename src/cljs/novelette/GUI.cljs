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
   (vec (reduce #(update (update %1 0 + ((:position %2) 0))
                         1 + ((:position %2) 1)) position ancestors)))

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
        bounding-box (absolute-position ancestors (:position element))
        in-bounds? (utils/inside-bounds? [x y] bounding-box)]
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
    (cond
      (and (some true? clicked?) in-bounds?)
      (handle-input-event element :clicked screen)
      (and (not (:hover? element)) in-bounds?)
      (handle-input-event element :on-hover screen)
      (and (:hover? element) (not in-bounds?))
      (handle-input-event element :off-hover screen)
      :else
      [screen true])))

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

(s/defn find-element-path
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
                        (map #(find-element-path id % next-walk-list)
                             (:children GUI-tree)))]
              (when (seq result) (first result))))))

(s/defn create-children-path
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

(s/defn replace-element
  "Find an element in the GUI tree and replace it with the new one."
  [element :- sc/GUIElement
   id :- (s/cond-pre s/Str s/Keyword)
   {:keys [GUI] :as screen} :- sc/Screen]
  (let [search (find-element-path id GUI [])]
    (when (nil? search)
      (throw (js/Error. (str id " id not found in GUI element list."))))
    (if (seq search)
      (let [path (create-children-path GUI (conj search id))]
        (assoc-in screen (concat [:GUI] path) element))
      (assoc screen :GUI element))))

(s/defn add-element
  "Add a GUIElement to the GUI tree given the specified parent ID."
  [element :- sc/GUIElement
   parent :- (s/cond-pre s/Str s/Keyword)
   {:keys [GUI] :as screen} :- sc/Screen]
  (let [search (find-element-path parent GUI [])]
    (when (nil? search)
      (throw (js/Error. (str parent " id not found in GUI element list."))))
    (let [path (create-children-path GUI (conj search parent))]
      (update-in screen (concat [:GUI] path [:children])
                 conj element))))

(s/defn remove-element
  "Remove a GUIElement from the GUI tree given the ID."
  [id :- (s/cond-pre s/Str s/Keyword)
   {:keys [GUI] :as screen} :- sc/Screen]
  (let [search (find-element-path id GUI [])]
    (when (nil? search)
      (throw (js/Error. (str id " id not found in GUI element list."))))
    (let [path (create-children-path GUI (conj search id))
          removal-path (concat [:GUI] (pop path))
          split-index (last path)
          children (get-in screen removal-path)
          new-children (vec (concat (subvec children 0 split-index)
                                    (subvec children (inc split-index))))]
      (assoc-in screen removal-path new-children))))

; TODO - Maybe write test for this?
(s/defn update-element
  "Update the state of a given element inside the active GUI tree."
  [id :- (s/cond-pre s/Str s/Keyword)
   screen :- sc/Screen
   keys :- [s/Keyword]
   func :- sc/function
   & args]
  (let [search (find-element-path id (:GUI screen) [])]
    (when (nil? search)
      (throw (js/Error. (str id " id not found in GUI element list."))))
    (let [path (create-children-path (:GUI screen) (conj search id))]
      (update-in screen (concat [:GUI] path keys) #(apply func % args)))) )

; TODO - Maybe write test for this?
(s/defn assoc-element
  "Replace the state of a given element inside the active GUI tree."
  [id :- (s/cond-pre s/Str s/Keyword)
   screen :- sc/Screen
   keys :- [s/Keyword]
   newstate :- s/Any]
  (let [search (find-element-path id (:GUI screen) [])]
    (when (nil? search)
      (throw (js/Error. (str id " id not found in GUI element list."))))
    (let [path (create-children-path (:GUI screen) (conj search id))]
      (assoc-in screen (concat [:GUI] path keys) newstate))))

(s/defn add-event-listener
  "Add a new event listener to the GUI element. It overrides any previous one."
  [element :- sc/GUIElement
   event-type :- sc/GUIEvent
   target :- sc/function]
  (assoc-in element [:events event-type] target))

(s/defn remove-event-listener
  "Remove the event listener of the given type from the GUI element."
  [element :- sc/GUIElement
   event-type :- sc/GUIEvent]
  (update element :events dissoc event-type))
