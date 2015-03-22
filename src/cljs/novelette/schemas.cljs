(ns novelette.schemas
  (:require-macros [schema.core :as s])
  (:require [schema.core :as s]))

; This file contains all the schemas used by the Novelette VN engine so it can
; be easily referred from any namespace without dependency problems.

(def function (s/pred fn? 'fn?))

; This is the screen, a screen is the base data structure that contains
; all the data for updating, rendering and handling a single state instance
; in the game. Multiple screens all packed together make the full state of the
; game.
(s/defrecord Screen [id :- ( s/maybe (s/either s/Str s/Keyword)) ; Unique identifier of the screen
                     handle-input :- (s/maybe function) ; Function to handle input
                     update :- (s/maybe function) ; Function to update state
                     render :- (s/maybe function) ; Function to render on screen
                     deinit :- (s/maybe function) ; Function to destroy screen
                     canvas :- js/HTMLCanvasElement ; canvas of the game
                     context :- js/CanvasRenderingContext2D ; context of the canvas
                     next-frame :- (s/maybe function) ; What to do on the next game-loop
                     ]
  {s/Any s/Any})

; TODO - purge a lot of old data and cruft


(s/defrecord State [screen-list :- [Screen]
                    curr-time :- s/Num
                    context :- js/CanvasRenderingContext2D ; TODO - maybe invert order of this and canvas for consistency
                    canvas :- js/HTMLCanvasElement]
  {s/Any s/Any})

; This is the storytelling state of the game. It is an object containing the whole set of
; past, present and near-future state. It keeps track of stateful actions like scrollback,
; sprites being rendered, bgm playing and other stuff. Ideally, it should be easy to
; save/load transparently.
(s/defrecord StoryState [scrollback :- [s/Any] ; Complete history of events for scrollback purposes, as a stack (this should contain the previous state of the storyscreen too)
                         scrollfront :- [{s/Any s/Any}] ; Stack of events yet to be interpreted.
                         spriteset :- #{s/Any} ; Set of sprite id currently displayed on screen.
                         sprites :- {s/Any s/Any} ; Map of sprites globally defined on screen.
                         backgrounds :- [s/Any] ; Stack of sprites currently in use as backgrounds.
                         points :- {s/Keyword s/Int} ; Map of points that the player obtained during the game
                         cps :- s/Int ; characters per second
                         next-step? :- s/Bool ; Whether or not to advance to next step for storytelling
                         show-ui? :- s/Bool ; Whether or not we show the game UI on screen.
                         ui-img :- s/Any ; UI image to show
                         input-state :- {s/Keyword s/Any} ; State of the input for the current frame.
                         cursor :- s/Keyword ; Image of glyph used to advance text
                         cursor-delta :- s/Num ; Delta to make the cursor float, just calculate % 4
                         dialogue-bounds :- [s/Int] ; x,y, width and height of the boundaries of text to be displayed
                         nametag-position :- [s/Int] ; x,y coordinates of the nametag in the UI
                         ; TODO - add a "seen" map with all the dialogue options already seen
                         ;        to facilitate skipping of text.
                         ]
  {s/Any s/Any})

(s/defrecord StoryTeller [runtime-hooks :- {s/Any s/Any} ; Map of runtime hooks to in-text macros
                          current-token :- {s/Any s/Any} ; Current token to parse.
                          timer :- s/Num ; Amount of milliseconds passed since last token transition
                          state :- {s/Any s/Any} ; Local storyteller state for transitioning events
                          first? :- s/Bool ; Is this the first frame for this state? TODO - I dislike this, I need a better option.
                          ] 
  {s/Any s/Any})
