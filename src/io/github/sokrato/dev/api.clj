(ns io.github.sokrato.dev.api
  (:refer-clojure :exclude [compile])
  (:require
    [potemkin :refer [import-vars]]
    [io.github.sokrato.dev.build])
  (:gen-class))

(import-vars [io.github.sokrato.dev.build
              clean
              compile
              copy
              jar
              uberjar])