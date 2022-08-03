(ns eyebrowse.build
  (:require [clojure.tools.build.api :as b]))

(defn run [& _]
  (b/javac {:src-dirs ["src"]
            :class-dir "src"
            :basis (b/create-basis {:project "deps.edn"})
            #_#_:javac-opts ["-source" "17" "-target" "17"]}))
