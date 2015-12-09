(ns novelette.tests.utils
  (:require [cljs.test :refer-macros [deftest is run-tests]]
            [novelette.utils :as u]))

(deftest get-center-coordinates-test
  (is (= [50 50](u/get-center-coordinates [0 0 100 100])))
  (is (= [200 100] (u/get-center-coordinates [100 0 200 200]))))

(deftest inside-bounds?-test
  (is (= true (u/inside-bounds? [10 10] [0 0 100 100])))
  (is (= true (u/inside-bounds? [50 50] [20 10 150 300])))
  (is (= false (u/inside-bounds? [0 0] [10 10 100 100])))
  (is (= false (u/inside-bounds? [1000 500] [0 10 200 1000]))))

(def sprite-test-1
  {:id :sprite-test-1
   :z-index 100
   :position [0 0 0 0]})

(def sprite-test-2
  {:id :sprite-test-2
   :z-index 50
   :position [0 0 0 0]})

(def sprite-test-3
  {:id :sprite-test-3
   :z-index 1000
   :position [0 0 0 0]})

(def sprite-test-4
  {:id :sprite-test-4
   :z-index 1
   :position [0 0 0 0]})

(deftest sort-z-index
  (is (= [sprite-test-3 sprite-test-1 sprite-test-2 sprite-test-4]
         (u/sort-z-index [sprite-test-1 sprite-test-2
                          sprite-test-3 sprite-test-4])))
  (is (not (= (reverse [sprite-test-3 sprite-test-1 sprite-test-2 sprite-test-4])
         (u/sort-z-index [sprite-test-1 sprite-test-2
                          sprite-test-3 sprite-test-4])))))
