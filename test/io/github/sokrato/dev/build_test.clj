(ns io.github.sokrato.dev.build-test
  (:require
    [clojure.test :refer :all]
    [io.github.sokrato.dev.build :refer :all]
    [io.github.sokrato.dev.util :refer :all])
  (:gen-class))

(deftest test-fs-parents
  (let [ps (fs-parents)
        top (str (last ps))]
    (is (= "/" top)))

  (is (contains-deps-edn? (find-project-root! "")))
  ;;
  )
