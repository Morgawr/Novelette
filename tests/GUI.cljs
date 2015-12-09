(ns novelette.tests.GUI
  (:require [cljs.test :refer-macros [deftest is run-tests]]
            [novelette.GUI :as GUI]))

(def test-data
  {:id :first-layer
   :children [
              {:id :second-layer-1
               :children []}
              {:id :second-layer-2
               :children [
                         {:id :third-layer-1
                          :children []}
                         {:id :third-layer-2
                          :children [
                                     {:id :fourth-layer-1
                                      :children []}
                                     {:id :fourth-layer-2
                                      :children []}
                                     {:id :fourth-layer-3
                                      :children [
                                                 {:id :fifth-layer-1
                                                  :children []}
                                                 {:id :fifth-layer-2
                                                  :children []}]}
                                     {:id :fourth-layer-4
                                      :children []}]}
                         {:id :third-layer-3
                          :children []}]}
              {:id :second-layer-3
               :children []}
              {:id :second-layer-4
               :children [
                          {:id :third-layer-4
                           :children [
                                      {:id :fourth-layer-5
                                       :children [
                                                  {:id :fifth-layer-3
                                                   :children []}]}
                                      {:id :fourth-layer-6
                                       :chilren []}]}
                          {:id :third-layer-5
                           :children []}]}]})


(deftest find-GUI-element-path-test
  (is (= [:first-layer]
         (GUI/find-GUI-element-path :second-layer-1 test-data [])))
  (is (= [:first-layer]
         (GUI/find-GUI-element-path :second-layer-2 test-data [])))
  (is (= [:first-layer :second-layer-2]
         (GUI/find-GUI-element-path :third-layer-3 test-data [])))
  (is (= [:first-layer :second-layer-2 :third-layer-2]
         (GUI/find-GUI-element-path :fourth-layer-1 test-data [])))
  (is (not (= [:first-layer :second-layer-2 :third-layer-3]
         (GUI/find-GUI-element-path :fourth-layer-1 test-data []))))
  (is (= [] (GUI/find-GUI-element-path :first-layer test-data [])))
  (is (= nil (GUI/find-GUI-element-path :non-existent-layer test-data []))))

(deftest create-GUI-children-path
  (is (= [:children 1 :children 2]
         (GUI/create-GUI-children-path
           test-data [:first-layer :second-layer-2 :third-layer-3])))
  (is (= [] (GUI/create-GUI-children-path test-data [:first-layer])))
  (is (= nil (GUI/create-GUI-children-path test-data [:first-layer :non-existent-layer :test]))))

(run-tests)
