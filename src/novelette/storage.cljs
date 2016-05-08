(ns novelette.storage
  (:require-macros [schema.core :as s])
  (:require [novelette.schemas :as sc]
            [cljs.reader :as reader]
            [schema.core :as s]
            [lzstr.LZString :as LZString]))

; This file contains all the functions related to storing and retrieving data
; from Javascript's LocalStorage

(s/defn save!
  "Save the given data into the local storage with the specified name."
  [data :- s/Any
   name :- s/Str]
  (.setItem js/localStorage name (LZString/compress (pr-str data))))

(s/defn clear!
  "Clear the data at the specified name location."
  [name :- s/Str]
  (.removeItem js/localStorage name))

(s/defn load
  "Retrieve the data from the specified name location and try to convert it to a
  Clojure datatype."
  [name :- s/Str]
  (let [to-decompress (.getItem js/localStorage name)]
    (if (nil? to-decompress)
      nil
      (reader/read-string (LZString/decompress to-decompress)))))
