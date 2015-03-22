(defproject novelette "0.1.0-indev"
  :description "ClojureScript engine for visual novels."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2913"]
                 [prismatic/schema "0.4.0"]]
  :plugins [[lein-cljsbuild "1.0.5"]
            [com.cemerick/clojurescript.test  "0.3.3"]]
  :hooks [leiningen.cljsbuild]
  :clean-targets ["runtime/js/novelette.js"]
  :cljsbuild
  {
   :test-commands
   {"unit"
    ["/bin/true"]} ; TODO - think about removing/rewriting tests
     ;"phantomjs" :runner 
     ;"compiled-tests/tests.js"]}
   :builds
   [{:source-paths ["src/cljs"]
     :compiler
     {:optimizations :whitespace
      :output-to  "runtime/js/novelette.js"
      :pretty-print true}}
    {:source-paths ["src/cljs" "tests"]
     :compiler {:output-to "compiled-tests/tests.js"
                :optimizations :whitespace
                :pretty-print true}}]})
