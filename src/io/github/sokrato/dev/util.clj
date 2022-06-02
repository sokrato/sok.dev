(ns io.github.sokrato.dev.util
  (:require
    [clojure.java.io :as io])
  (:gen-class))

(defn exit
  "exit jvm"
  ([^String msg] (exit msg 1))
  ([^String msg ^long status]
   (println msg)
   (System/exit status)))

(defn fs-parents
  ([] (fs-parents ""))
  ([cd]
   (loop [cd (if (string? cd)
               (-> (io/file cd)
                   .toPath
                   .toAbsolutePath)
               cd)
          res [cd]]
     (let [p (.getParent cd)]
       (if p
         (recur p (conj res p))
         res)))))