(defproject novelette "0.1.0-indev"
  :description "ClojureScript engine for visual novels."
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [prismatic/schema "1.0.1"]]
  :plugins [[lein-cljsbuild "1.1.0"]
            [com.cemerick/clojurescript.test  "0.3.3"]]
  :hooks [leiningen.cljsbuild]
  :clean-targets ["runtime/js/*"]
  :cljsbuild
  {
   :test-commands
   {"unit"
    ["/bin/true"]} ; TODO - think about removing/rewriting tests
     ;"phantomjs" :runner 
     ;"compiled-tests/tests.js"]}
   :builds
   [{:id "novelette"
     :source-paths ["src/cljs"]
     :compiler
     {:optimizations :whitespace
      :output-dir "runtime/js"
      :output-to  "runtime/js/novelette.js"
      :pretty-print true
      :source-map "runtime/js/novelette.js.map"}}
    {:source-paths ["src/cljs" "tests"]
     :compiler {:output-to "compiled-tests/tests.js"
                :optimizations :whitespace
                :pretty-print true}}]})
