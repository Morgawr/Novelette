(defproject novelette "0.1.0-indev"
  :description "ClojureScript engine for visual novels."
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [prismatic/schema "1.0.1"]]
  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-doo "0.1.6"]]
  :hooks [leiningen.cljsbuild]
  :clean-targets ["runtime/js/*"]
  :cljsbuild
  {
   :builds
   [
    {:id "novelette"
     :source-paths ["src/cljs"]
     :compiler
     {:optimizations :whitespace
      :output-dir "runtime/js"
      :output-to  "runtime/js/novelette.js"
      :pretty-print true
      :source-map "runtime/js/novelette.js.map"}}
    {:id "tests"
     :source-paths ["src/cljs" "tests"]
     :compiler {:output-to "compiled-tests/tests.js"
                :optimizations :whitespace
                :main "novelette.tests.runner"
                :pretty-print true}}]})
