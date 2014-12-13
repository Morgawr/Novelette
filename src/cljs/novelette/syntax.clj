; This is a standalone file providing all the syntax transformation for the novelette
; syntactical engine.
(ns novelette.syntax)

; TODO - add an optional init function?

(defmacro parse-format
  [s]
  `(loop [messages# '() actions# '() remaining# ~s]
     (let [idx1# (.indexOf remaining# "{(")
           idx2# (.indexOf remaining# ")}")]
       (if (or (= -1 idx1#)
               (= -1 idx2#))
         [(reverse (conj messages# remaining#))  (reverse actions#)]
         (let [half1# (->> remaining#
                           (split-at idx1#)
                           (first)
                           (apply str))
               half2# (->> remaining#
                           (split-at (+ idx2# 2))
                           (second)
                           (apply str))
               match# (subs remaining# idx1# (+ idx2# 2))]
           (if (empty? half1#)
             (recur (conj messages# :pop)
                    (conj actions# match#)
                    half2#)
             (recur (concat [:pop half1#] messages#)
                    (conj actions# match#)
                    half2#)))))))

(defmacro speak*
  [name text color] ; TODO - add support for font
  `(let [result#  { :name ~name
                    :color ~color
                    :type :speech}
         msgs# (parse-format ~text)]
     (assoc result#
       :messages (first msgs#)
       :actions (second msgs#))))

(defmacro speaker
  [name color]
  `(fn [text#]
     (speak* ~name text# ~color)))

(defmacro narrate
  [text]
  `(speak* "" ~text :red))

(defmacro defspeaker
  [symbol name color] ; TODO - add support for font
  `(def ~symbol
     (speaker ~name ~color)))

(defmacro default
  [id]
  `{:default ~id})

(defmacro choice-explicit*
  [text args]
  `(let [args# ~args
         text# ~text]
     (apply merge-with
            (fn [a# b#]
              (assoc a# (first (keys b#)) (first (vals b#))))
            {:type :explicit-choice :text text#}
            args#)))

(defmacro choice-implicit*
  [args]
  `{:type :implicit-choice
    :options (map :options ~args)})

(defmacro defscene
  [name & body]
    `(def ~name { :name ~(str name)
                  :body (into [] (filter seq (flatten [~@body])))}))

(defmacro defblock
  [name & body]
  `(defn ~name []
     [~@body]))

(defmacro clear-sprites
  "Removes all sprites from the screen."
  []
  `{:type :function
    :hook :clear-sprites})

(defmacro sprite
  "Adds a sprite on screen."
  [id]
  `{:type :function
    :hook :add-sprite
    :params [~id]})

(defmacro no-sprite
  "Removes a sprite from the screen."
  [id]
   `{:type :function
     :hook :remove-sprite
     :params [~id]})

(defmacro teleport-sprite
  "Teleports a sprite at a given position."
  [id position]
  `{:type :function
    :hook :teleport-sprite
    :params [~id ~position]})

(defmacro move-sprite
  "Moves a sprite at a given position in a given time (milliseconds)."
  [id position duration]
  `{:type :function
    :hook :move-sprite
    :params [~id ~position ~duration]})

(defmacro bgm
  "Starts playing new bgm."
  [id]
  `{:type :function
    :hook :play-bgm
    :params [~id]})

(defmacro no-bgm
  "Stops playing the bgm."
  []
  `{:type :function
    :hook :stop-bgm
    :params []})

(defmacro declare-sprite
  "Declares a sprite bound to the current screen"
  [id image-id position z-index]
  `{:type :function
    :hook :decl-sprite
    :params [~id ~image-id ~position ~z-index]})

(defmacro clear-backgrounds
  "Empties the background stack"
  []
  `{:type :function
    :hook :clear-backgrounds
    :params []})

(defmacro background
  "Adds a new background on top of the stack"
  [id]
  `{:type :function
    :hook :push-background
    :params [~id]})

(defmacro no-background
  "Removes the topmost background from the stack"
  []
  `{:type :function
    :hook :pop-background
    :params []})

(defmacro show-ui
  "Shows UI on screen."
  []
  `{:type :function
    :hook :show-ui
    :params []})

(defmacro hide-ui
  "Hides UI on screen."
  []
  `{:type :function
    :hook :hide-ui
    :params []})

(defmacro set-ui
  "Sets the UI image to be used."
  [id pos]
  `{:type :function
    :hook :set-ui
    :params [~id ~pos]})

(defmacro set-cursor
  "Sets the glyph image used as cursor."
  [id]
  `{:type :function
    :hook :set-cursor
    :params [~id]})

(defmacro set-cps
  "Sets the characters per second."
  [amount]
  `{:type :function
    :hook :set-cps
    :params [~amount]})

(defmacro set-bounds
  "Sets the dialogue bounds for the UI textbox."
  [x y w h]
  `{:type :function
    :hook :set-dialogue-bounds
    :params [~x ~y ~w ~h]})

(defmacro set-nametag-position
  "Sets the nametag position in the UI textbox."
  [pos]
  `{:type :function
    :hook :set-nametag-position
    :params [~pos]})

(defmacro wait
  "Waits for a given amount of milliseconds."
  [msec]
  `{:type :function
    :hook :wait
    :params [~msec]})

(defmacro jump-to-scene
  "Transitions onto the next scene."
  [name]
  `{:type :function
    :hook :get-next-scene
    :params [~name]})
