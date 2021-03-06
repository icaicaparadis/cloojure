(ns tst.runner
  (:require
    [cljs.test :refer [report] :refer-macros [run-tests]]
    [figwheel.main.async-result :as async-result]

    ; ********** REQUIRE ALL TESTING NS **********
    ;-----------------------------------------------------------------------------
    [tst._bootstrap]

    [tst.flintstones.dino]
    [tst.flintstones.slate]
    ;[tst.flintstones.wilma]
    ;[tst.flintstones.pebbles]
   [tst.flintstones.bambam]

    [tst.tupelo.core]
    [tst.tupelo.string]
    [tst.tupelo.data.index]
    [tst.tupelo.data]
    [tst.tupelo.lexical]
    [tst.tupelo.array]

    ;-----------------------------------------------------------------------------

    ))

; tests can be asynchronous, we must hook test end
(defmethod report [:cljs.test/default :end-run-tests] [test-data]
  (if (cljs.test/successful? test-data)
    (async-result/send "Tests passed!!")
    (async-result/throw-ex (ex-info "Tests Failed" test-data))))


(defn -main [& args]
  (run-tests

    ; ********** MUST REPEAT HERE ALL TEST NS FROM ABOVE `(:require ...)` **********
    ;-----------------------------------------------------------------------------
    'tst._bootstrap

    'tst.flintstones.dino
    'tst.flintstones.slate
    ;'tst.flintstones.wilma
    ;'tst.flintstones.pebbles
   'tst.flintstones.bambam

    'tst.tupelo.core
    'tst.tupelo.string
    'tst.tupelo.data.index
    'tst.tupelo.data
    'tst.tupelo.lexical
    'tst.tupelo.array
    ;-----------------------------------------------------------------------------

  )
  ; return a message to the figwheel process that tells it to wait
  [:figwheel.main.async-result/wait 5000] ; millis
)
