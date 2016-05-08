(defproject novelette "0.1.0-indev"
  :description "ClojureScript engine for visual novels."
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [prismatic/schema "1.0.1"]
                 [lein-doo "0.1.6"]
                 [novelette-text "0.1.3"]
                 [novelette-sprite "0.1.0.2"]]
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-doo "0.1.6"]]
  :hooks [leiningen.cljsbuild]
  :clean-targets ["runtime/js/*"]
  :cljsbuild
  {
   :builds
   [
    {:id "novelette"
     :source-paths ["src/"]
     :compiler
     {:optimizations :simple
      :output-dir "runtime/js"
      :output-to  "runtime/js/novelette.js"
      :pretty-print true
      :source-map "runtime/js/novelette.js.map"
      :closure-output-charset "US-ASCII"
      }}
    {:id "tests"
     :source-paths ["src/" "tests"]
     :compiler {:output-to "compiled-tests/tests.js"
                :optimizations :whitespace
                :main "novelette.tests.runner"
                :pretty-print true}}]})
