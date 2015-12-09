(ns novelette.tests.GUI
  (:require [cljs.test :refer-macros [deftest is run-tests]]
            [novelette.GUI :as GUI]))

(def data-paths
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


(deftest find-element-path-test
  (is (= [:first-layer]
         (GUI/find-element-path :second-layer-1 data-paths [])))
  (is (= [:first-layer]
         (GUI/find-element-path :second-layer-2 data-paths [])))
  (is (= [:first-layer :second-layer-2]
         (GUI/find-element-path :third-layer-3 data-paths [])))
  (is (= [:first-layer :second-layer-2 :third-layer-2]
         (GUI/find-element-path :fourth-layer-1 data-paths [])))
  (is (not (= [:first-layer :second-layer-2 :third-layer-3]
         (GUI/find-element-path :fourth-layer-1 data-paths []))))
  (is (= [] (GUI/find-element-path :first-layer data-paths [])))
  (is (= nil (GUI/find-element-path :non-existent-layer data-paths []))))

(deftest create-children-path
  (is (= [:children 1 :children 2]
         (GUI/create-children-path
           data-paths [:first-layer :second-layer-2 :third-layer-3])))
  (is (= [] (GUI/create-children-path data-paths [:first-layer])))
  (is (= nil (GUI/create-children-path data-paths [:first-layer :non-existent-layer :test]))))

(def data-elements
  {:id :first-layer
   :children [
              {:id :second-layer-1
               :children [
                          {:id :third-layer-1
                           :children []}
                          {:id :third-layer-2
                           :children []}]}
              {:id :second-layer-2
               :children []}]})

(def data-elements-replace
  {:id :first-layer
   :children [
              {:id :second-layer-1
               :children [
                          {:id :third-layer-1
                           :children [
                                      {:id :fourth-layer-1
                                       :children []}]}
                          {:id :third-layer-2
                           :children []}]}
              {:id :second-layer-2
               :children []}]})

(def data-elements-remove
  {:id :first-layer
   :children [
              {:id :second-layer-1
               :children [
                          {:id :third-layer-2
                           :children []}]}
              {:id :second-layer-2
               :children []}]})

(def data-elements-add
  {:id :first-layer
   :children [
              {:id :second-layer-1
               :children [
                          {:id :third-layer-1
                           :children []}
                          {:id :third-layer-2
                           :children []}
                          {:id :third-layer-3
                           :children []}]}
              {:id :second-layer-2
               :children []}]})

(def to-replace
  {:id :third-layer-1
   :children [
              {:id :fourth-layer-1
               :children []}]})

(deftest add-element-test
  (is (= {:GUI data-elements-add}
         (GUI/add-element {:id :third-layer-3
                           :children []} :second-layer-1 {:GUI data-elements}))))
(deftest replace-element-test
  (is (= {:GUI data-elements-replace}
         (GUI/replace-element to-replace :third-layer-1 {:GUI data-elements}))))

(deftest remove-element-test
  (is (= {:GUI data-elements-remove}
         (GUI/remove-element :third-layer-1 {:GUI data-elements}))))

(deftest add-event-listener-test
  (let [func (fn [a b] (+ a b))]
    (is (= func (:clicked (:events (GUI/add-event-listener
                                     {:id :button
                                      :events {}}
                                     :clicked func)))))
    (is (= func (:clicked (:events (GUI/add-event-listener
                                     {:id :button
                                      :events {:clicked identity}}
                                     :clicked func)))))))

(deftest remove-event-listener-test
  (let [func (fn [a b] (+ a b))]
    (is (= {:id :button
            :events {}}
           (GUI/remove-event-listener
             {:id :button
              :events {:clicked func}}
             :clicked)))))

(run-tests)
