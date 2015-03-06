(ns novelette.tests.GUI
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest run-tests)])
  (:require [cemerick.cljs.test :as t]
            [novelette.GUI :as GUI]))

(defn dummy-render 
  [element ancestors]
  (println "Dummy render for" (:id  element) "called."))

(def dialog
  {:type :dialog
   :id :dialog-one
   :position [200 200 200 200]
   :content {:text "test-dialog?"}
   :children [{:type :button
               :id :button-one
               :position [10 10 50 50]
               :content {:text "Yes"}
               :children []
               :events {}
               :focus? true
               :z-index 4
               :render dummy-render}
              {:type :button
               :id :button-two
               :position [70 10 50 50]
               :content {:text "No"}
               :children []
               :events {}
               :focus? false
               :z-index 4
               :render dummy-render}]
   :events {}
   :focus? false
   :z-index 4
   :render dummy-render})

(def extra-button
  {:type :button
   :id :button-three
   :position [10 10 50 50]
   :content {:text "Maybe"}
   :children []
   :events {}
   :focus? true
   :z-index 4
   :render dummy-render})

(def dialog-extra-button
  {:type :dialog
   :id :dialog-one
   :position [200 200 200 200]
   :content {:text "test-dialog?"}
   :children [ {:type :button
                :id :button-one
                :position [10 10 50 50]
                :content {:text "Yes"}
                :children []
                :events {}
                :focus? true
                :z-index 4
                :render dummy-render}
              {:type :button
               :id :button-two
               :position [70 10 50 50]
               :content {:text "No"}
               :children []
               :events {}
               :focus? false
               :z-index 4
               :render dummy-render}
              extra-button]
   :events {}
   :focus? false
   :z-index 4
   :render dummy-render})

(def tree-no-dialog
  {:type :canvas
   :id :canvas
   :position [0 0 1920 1080]
   :content {}
   :children []
   :events {}
   :focus? false
   :z-index 10000
   :render dummy-render})

(def tree-with-dialog
  {:type :canvas
   :id :canvas
   :position [0 0 1920 1080]
   :content {}
   :children [dialog]
   :events {}
   :focus? false
   :z-index 10000
   :render dummy-render})

(def tree-with-extra-button-dialog
  {:type :canvas
   :id :canvas
   :position [0 0 1920 1080]
   :content {}
   :children [dialog-extra-button]
   :events {}
   :focus? false
   :z-index 10000
   :render dummy-render})

(defn build-fake-GUI-screen
  [GUI-element]
  {:GUI GUI-element})

(deftest add-element-to-GUI
  (is (= (build-fake-GUI-screen tree-with-dialog)
         (GUI/add-element-to-GUI dialog 
                                 :canvas
                                 (build-fake-GUI-screen tree-no-dialog))))
  (is (= (build-fake-GUI-screen tree-with-extra-button-dialog)
         (GUI/add-element-to-GUI extra-button 
                                 :dialog-one
                                 (build-fake-GUI-screen tree-with-dialog)))))
