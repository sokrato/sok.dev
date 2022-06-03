(ns io.github.sokrato.dev.util
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str])
  (:import
    [java.io File])
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

(defn file-ext [^File f]
  (-> (.getName f)
      (str/split #"\.")
      last))

(defn contains-source-file? [^String path ext]
  (let [f (io/file path)]
    (and (.isDirectory f)
         (some
           #(and (.isFile %)
                 (ext (file-ext %)))
           (file-seq f)))))
