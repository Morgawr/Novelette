; This is a standalone file providing all the syntax transformation for the novelette
; syntactical engine.
(ns novelette.syntax)

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
  `(let [result#  { :name ~name :color ~color }
         msgs# (parse-format ~text)]
     (assoc result#
       :messages (first msgs#)
       :actions (second msgs#))))

(defmacro speaker
  [name color]
  `(fn [text#]
     (speak* ~name text# ~color)))

(defmacro defspeaker
  [symbol name color] ; TODO - add support for font
  `(def ~symbol
     (speaker ~name ~color)))

(defn option
  [id jump & args]
  (loop [opt args res {:jump jump}]
    (cond
     (empty? opt) {:options {id res}}
     (= :pre (first (first opt)))
       (recur (rest opt) (assoc res :pre (second (first opt))))
     (= :post (first (first opt)))
       (recur (rest opt) (assoc res :post (second (first opt))))
     :else ; ignore it
       (recur (rest opt) res))))

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
            {:type :explicit :text text#}
            args#)))

(defmacro choice-implicit*
  [args]
  `{:type :implicit
    :options (map :options ~args)})

(defn choice
  [& args]
  (if (string? (first args))
    (choice-explicit* (first args) (rest args))
    (choice-implicit* args)))

(defmacro defscene
  [name & body]
    `(def ~name { :name ~(str name)
                  :body [~@body] }))
