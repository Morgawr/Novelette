(defproject novelette "0.1.0-indev"
  :description "ClojureScript engine for visual novels."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {
              :builds [{
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "runtime/js/novelette.js"
                                   :optimizations :whitespace
                                   :pretty-print true}
                        :jar true}]})
