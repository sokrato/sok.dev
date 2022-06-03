(ns io.github.sokrato.dev.build
  (:refer-clojure :exclude [compile])
  (:require
    [clojure.edn :as edn]
    [clojure.tools.build.api :as b]

    [io.github.sokrato.dev.util
     :refer [contains-source-file? exit fs-parents]])
  (:import
    (java.nio.file Path))
  (:gen-class))

(def defaults
  {:version           "0.0.1"
   :paths             ["src"]
   :target-dir        "target"
   :class-dir         "target/classes"
   :resource-paths    ["resources"]
   :java-source-paths ["java"]
   :javac-opts        ["-source" "8" "-target" "8"]
   :build/include-src true
   :build/compile-clj true})

(defn wrap-defaults [cfg]
  (merge defaults cfg))

(defn contains-deps-edn? [^Path path]
  (-> (.resolve path "deps.edn")
      .toFile
      .isFile))

(defn ^Path find-project-root!
  "find the directory containing deps.edn file, and cd into it."
  [path]
  (let [p (->> (fs-parents path)
               (filter contains-deps-edn?)
               first)
        _ (when-not p
            (exit "cannot find project root directory - one containing 'deps.edn'"))]
    (System/setProperty "user.dir" (str p))
    p))

(defn get-basis []
  ;; TODO: 1. parameterize :project
  (let [root (find-project-root! "")
        cfg (-> (.resolve root "deps.edn")
                .toFile
                slurp
                edn/read-string
                wrap-defaults)
        base (b/create-basis {:project "deps.edn"})]
    [base cfg]))

(defn clean' [cfg]
  (let [target (:target-dir cfg)
        classes (:class-dir cfg)]
    (b/delete {:path classes})
    (b/delete {:path target})
    ))

(defn clean [_]
  (let [[_ cfg] (get-basis)]
    (clean' cfg)))

(defn compile' [basis cfg]
  ;; compile java
  (if (some #(contains-source-file? % #{"java"}) (:java-source-paths cfg))
    (b/javac {:src-dirs   (:java-source-paths cfg)
              :class-dir  (:class-dir cfg)
              :javac-opts (:java-opts cfg)
              :basis      basis}))
  ;; compile clj
  (if (and (:build/compile-clj cfg)
           (some #(contains-source-file? % #{"clj" "cljc"}) (:paths cfg)))
    (b/compile-clj {:basis     basis
                    :src-dirs  (:paths cfg)
                    :class-dir (:class-dir cfg)})))

(defn compile [_]
  (let [[basis cfg] (get-basis)]
    (compile' basis cfg)))

(defn copy' [cfg]
  (let [src (if (:build/include-src cfg)
              (concat (:paths cfg) (:resource-paths cfg))
              (:resource-paths cfg))
        src (filter #(not= (:class-dir cfg) %) src)]
    (b/copy-dir {:src-dirs   src
                 :target-dir (:class-dir cfg)})))

(defn copy [_]
  (let [[_ cfg] (get-basis)]
    (copy' cfg)))

(defn require-lib! [cfg]
  (let [lib (:lib cfg)]
    (if-not (qualified-ident? lib)
      (System/exit "missing :lib in deps.edn"))
    lib))

(defn pom' [basis cfg]
  (b/write-pom {:class-dir (:class-dir cfg)
                :lib       (require-lib! cfg)
                :version   (:version cfg)
                :basis     basis
                :src-dirs  (:paths cfg)}))

(defn jar' [basis cfg]
  (compile' basis cfg)
  (pom' basis cfg)
  (copy' cfg)
  (b/jar {:class-dir (:class-dir cfg)
          :jar-file  (format "%s/%s-%s.jar"
                             (:target-dir cfg)
                             (name (require-lib! cfg))
                             (:version cfg))}))

(defn jar [_]
  (let [[basis cfg] (get-basis)]
    (jar' basis cfg)))

(defn require-main! [cfg]
  (let [m (:main cfg)]
    (if-not m
      (exit "missing :main in deps.edn"))
    m))

(defn uberjar' [basis cfg]
  (compile' basis cfg)
  (pom' basis cfg)
  (copy' cfg)
  (b/uber {:class-dir (:class-dir cfg)
           :uber-file (format "%s/%s-%s-standalone.jar"
                              (:target-dir cfg)
                              (name (require-lib! cfg))
                              (:version cfg))
           :main      (require-main! cfg)
           :basis     basis}))

(defn uberjar [_]
  (let [[basis cfg] (get-basis)]
    (uberjar' basis cfg)))

(defn -main [& args]
  (println "supported cmd: clean, compile, jar, uberjar"))
