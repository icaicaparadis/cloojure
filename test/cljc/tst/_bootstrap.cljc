;   Copyright (c) Alan Thompson. All rights reserved.
;   The use and distribution terms for this software are covered by the Eclipse Public License 1.0
;   (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html at
;   the root of this distribution.  By using this software in any fashion, you are agreeing to be
;   bound by the terms of this license.  You must not remove this notice, or any other, from this
;   software.
(ns tst._bootstrap
  "This namespace is used to perform one-time tasks during testing, such as printing the
  Clojure version."
  (:require
    [clojure.string :as str]
    [schema.core :as s]

    #?(:clj  [tupelo.core :as t :refer [spy spyx spyxx spyx-pretty ]]
       :cljs [tupelo.core :as t :include-macros true :refer [spy spyx spyxx spyx-pretty]])

    #?(:clj [clojure.test]
       :cljs [cljs.test])
    #?(:clj  [tupelo.test :refer [deftest testing is dotest dotest-focus isnt is= isnt= is-set= is-nonblank= throws? throws-not? define-fixture]]
       :cljs [tupelo.test-cljs ; :include-macros true
              :refer [deftest testing is dotest isnt is= isnt= is-set= is-nonblank= throws? throws-not? define-fixture]])
  ))



#?(:cljs (enable-console-print!))

(s/set-fn-validation! true) ; enforce fn schemas

; Prismatic Schema type definitions
; #todo add to Schema docs
; (set! *warn-on-reflection* true)  ; #todo enable?

(dotest
  (t/print-versions)
  ;(spyx (s/fn-validation?))

  (is (= 5 (+ 2 3)))
  (isnt (= 2 3))
  )
