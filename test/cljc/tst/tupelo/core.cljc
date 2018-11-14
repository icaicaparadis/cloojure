(ns tst.tupelo.core
  (:require
    [clojure.string :as str]
    #?@(:clj [
              [schema.core :as s]
              [tupelo.test   :refer [define-fixture dotest is isnt is= isnt= nonblank= testing throws?]]
              [tupelo.core :as t]
              [tupelo.schema :as tsk]
              [tupelo.string :as ts]
             ])
    #?@(:cljs [
               [schema.core :as s]
               [tupelo.test-cljs :refer [define-fixture dotest is isnt is= isnt= nonblank= testing throws?]]
               [tupelo.core :as t :include-macros true]
               [tupelo.schema :as tsk]
               [tupelo.string :as ts :include-macros true]
              ])
  ))

#?(:cljs (enable-console-print!))

(define-fixture :once
     {:enter (fn [ctx] (println "*** TEST ONCE *** - enter "))
      :leave (fn [ctx] (println "*** TEST ONCE *** - leave "))})
;--------------------------------------------------------------------------------------------------

(dotest
  (println "tst.tupelo.core test 1")
  (is= 2 (+ 1 1))

  (is (t/truthy? true))
  (is (t/truthy? 5))
  (is (t/falsey? false))
  (is (t/falsey? nil)))

(dotest
  (let [inf-rng-1 (map inc (range))]
    (is= 42 (t/only [42]))
    (is= :x (t/only [:x]))
    (is= "hello" (t/only ["hello"]))

    ; #todo #wip
    (throws? (t/only []))
    (throws? (t/only [:x :y]))
    (throws? (t/only inf-rng-1))

    (is= [1 2 3] (t/onlies [[1] [2] [3]]))
    (throws? (t/onlies [[1] [2] [3 4]]))
    (throws? (t/onlies [[1] [] [3]]))

    (is= 5 (t/only2 [[5]]))
    (throws? (t/only2 [[1 2]]))
    (throws? (t/only2 [[1] [2]]))

    (is (t/single? [42]))
    (is (t/single? [:x]))
    (is (t/single? ["hello"]))
    (isnt (t/single? []))
    (isnt (t/single? [:x :y]))
    (isnt (t/single? inf-rng-1))

    (is (t/pair? [42 43]))
    (is (t/pair? [:x :y]))
    (is (t/pair? ["hello" "there"]))
    (isnt (t/pair? []))
    (isnt (t/pair? [:y]))
    (isnt (t/pair? inf-rng-1))

    (is (t/triple? [42 43 44]))
    (is (t/triple? [:x :y :z]))
    (is (t/triple? ["hello" "there" "you"]))
    (isnt (t/triple? []))
    (isnt (t/triple? [:y]))
    (isnt (t/triple? [:x :y]))
    (isnt (t/triple? inf-rng-1))

    (is (t/quad? [42 43 44 45]))
    (is (t/quad? [:x :y :z :99]))
    (is (t/quad? ["hello" "there" "again" "you"]))
    (isnt (t/quad? []))
    (isnt (t/quad? [:x]))
    (isnt (t/quad? [:x :y]))
    (isnt (t/quad? [:x :y :z]))
    (isnt (t/quad? inf-rng-1))))

(dotest
  (let [inf-rng-1 (map inc (range))
        tst-map   (t/glue (sorted-map) {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6})]

    (throws? (t/xtake 1 []))

    (is= [1] (t/xtake 1 [1]))
    (is= [1] (t/xtake 1 [1 2]))
    (is= [1] (t/xtake 1 inf-rng-1))
    (is= [1 2] (t/xtake 2 [1 2]))
    (is= [1 2] (t/xtake 2 inf-rng-1))
    (is= {:a 1} (t/xtake 1 tst-map))
    (is= {:a 1 :b 2} (t/xtake 2 tst-map))

    (throws? (t/xfirst []))
    (is= 1 (t/xfirst [1]))
    (is= 1 (t/xfirst [1 2]))
    (is= 1 (t/xfirst inf-rng-1))
    ;(is= {:a 1} (t/xfirst tst-map))

    (throws? (t/xsecond []))
    (throws? (t/xsecond [1]))
    (is= 2 (t/xsecond [1 2]))
    (is= 2 (t/xsecond [1 2 3]))
    (is= 2 (t/xsecond [1 2 3 4]))
    (is= 2 (t/xsecond inf-rng-1))
    ;(is= {:b 2} (t/xsecond tst-map))

    (throws? (t/xthird []))
    (throws? (t/xthird [1]))
    (throws? (t/xthird [1 2]))
    (is= 3 (t/xthird [1 2 3]))
    (is= 3 (t/xthird [1 2 3 4]))
    (is= 3 (t/xthird inf-rng-1))
    ;(is= {:b 92} (t/xthird tst-map))

    (throws? (t/xfourth []))
    (throws? (t/xfourth [1]))
    (throws? (t/xfourth [1 2]))
    (throws? (t/xfourth [1 2 3]))
    (is= 4 (t/xfourth [1 2 3 4]))
    (is= 4 (t/xfourth [1 2 3 4 5]))
    (is= 4 (t/xfourth inf-rng-1))
    ;(is= {:b 92} (t/xfourth tst-map))

    (throws? (t/xlast nil))
    (throws? (t/xlast []))
    (is= 5 (t/xlast [1 2 3 4 5]))
    ;(is= {:b 92} (t/xlast tst-map))

    (is= [1 2 3 4] (t/xbutlast [1 2 3 4 5]))
    (is= [] (t/xbutlast [1]))
    (throws? (t/xbutlast []))
    (throws? (t/xbutlast nil))
    ;(is= {:b 92} (t/xbutlast tst-map))

    (throws? (t/xrest []))
    (is= [] (t/xrest [1]))
    (is= [2] (t/xrest [1 2]))
    (is= [2 3] (t/xrest [1 2 3]))
    (is= [2 3 4] (t/xrest [1 2 3 4]))
    (is= 2 (first (t/xrest inf-rng-1)))

    (throws? (t/xvec nil))
    (is= [] (t/xvec []))
    (is= [1] (t/xvec '(1)))
    (is= [1 2] (t/xvec [1 2]))
    ))

(dotest
  (is= :23 (t/int->kw  23))
  (is=  23 (t/kw->int :23))

  (println :01 (t/edn->json {:a 1 :b 2}))
  (prn     :02 (t/edn->json {:a 1 :b 2}))

  (is=  {:a  1 :b  2}  (t/json->edn (ts/quotes->double "{'a':1, 'b':2}")))
  (is= "{'a':1,'b':2}" (ts/quotes->single (t/edn->json  {:a  1  :b  2})))

  (is= 'abc (t/kw->sym :abc))
  (is= "abc" (t/kw->str :abc))
  (is= 'abc (t/str->sym "abc"))
  (is= :abc (t/str->kw "abc"))
  (is= :abc (t/sym->kw 'abc))
  (is= "abc" (t/sym->str 'abc)))

(dotest
  (let [orig  {:b #{3 2 1}
               :a [1 2 3 { 5 :five 6 :six 4 :four }]
               :c (list 4 5 6)}
        result (str/replace
                 (with-out-str (println (t/prettify orig)))
                 \, \space)
        expected "{:a  [1 2    3 {4 :four
                                  5 :five
                                  6 :six}]
                   :b #{1 2 3}
                   :c  [4 5 6]} " ]
    (nonblank= result expected )))

(dotest
  ; (t/spyx (s/check-fn t/truthy? ))

  (let [data [true :a 'my-symbol 1 "hello" \x false nil] ]
    (testing "basic usage"
      (let [truthies    (t/keep-if boolean data)       ; coerce to primitive type
            falsies     (t/keep-if not     data) ]     ; unnatural syntax
        (is (and  (= truthies [true :a 'my-symbol 1 "hello" \x] )
              (= falsies  [false nil] ) )))
      (let [truthies    (t/keep-if t/truthy? data)
            falsies     (t/keep-if t/falsey? data) ]
        (is (and  (= truthies [true :a 'my-symbol 1 "hello" \x] )
              (= falsies  [false nil] ) ))
        (is (every? t/truthy? [true :a 'my-symbol 1 "hello" \x] ))
        (is (every? t/falsey? [false nil] ))
        (is (t/has-none? t/falsey? truthies))
        (is (t/has-none? t/truthy? falsies))

        (isnt (every? t/truthy? [true false]))
        (is (every? t/truthy? [true "FALSE"]))
        (is (every? t/truthy? [true ]))
        (is (every? t/truthy? []))))

    (testing "improved usage"
      (let [count-if (comp count t/keep-if) ]
        (let [num-true    (count-if boolean data)   ; awkward phrasing
              num-false   (count-if not     data) ] ; doesn't feel natural
          (is (and  (= 6 num-true)
                (= 2 num-false) )))
        (let [num-true    (count-if t/truthy? data)   ; matches intent much better
              num-false   (count-if t/falsey? data) ]
          (is (and  (= 6 num-true)
                (= 2 num-false) ))))))

  (let [data [true :a 'my-symbol 1 "hello" \x false nil] ]
    (testing "basic usage"
      (let [notties   (t/keep-if t/not-nil? data)
            nillies   (t/drop-if t/not-nil? data) ]
        (is (and  (= notties [true :a 'my-symbol 1 "hello" \x false] )
              (= nillies [nil] )))
        (is (every?    t/not-nil? notties))
        (is (every?        nil? [nil] ))
        (is (t/has-none?     nil? notties))
        (is (t/has-none? t/not-nil? nillies))))

    (testing "improved usage"
      (let [count-if (comp count t/keep-if) ]
        (let [num-valid-1     (count-if some?    data)  ; awkward phrasing, doesn't feel natural
              num-valid-2     (count-if t/not-nil? data)  ; matches intent much better
              num-nil         (count-if nil?     data) ]    ; intent is plain
          (is (and  (= 7 num-valid-1 num-valid-2 )
                (= 1 num-nil) )))))))


(dotest
  (is= true   (t/has-some? odd? [1 2 3] ) )
  (is= false  (t/has-some? odd? [2 4 6] ) )
  (is= false  (t/has-some? odd? []      ) )

  (is= false  (t/has-none? odd? [1 2 3] ) )
  (is= true   (t/has-none? odd? [2 4 6] ) )
  (is= true   (t/has-none? odd? []      ) ))

(dotest
  (is (every?        t/not-empty? ["one" [1] '(1) {:1 1} #{1}     ] ))
  (is (t/has-none?   t/not-empty? [ ""   [ ] '( ) {}     #{ }  nil] ))

  (is (t/has-none?   empty? ["one" [1] '(1) {:1 1} #{1}     ] ))
  (is (every?        empty? [ ""   [ ] '( ) {}     #{ }  nil] ))

  (is= (map t/not-empty? ["1" [1] '(1) {:1 1} #{1} ] )
    [true true true true true]  )
  (is= (map t/not-empty? ["" [] '() {} #{} nil] )
    [false false false false false false ] )

  (is= (t/keep-if t/not-empty?  ["1" [1] '(1) {:1 1} #{1} ] )
    ["1" [1] '(1) {:1 1} #{1} ] )
  (is= (t/drop-if t/not-empty?  [""  []  '()  {}     #{}  nil] )
    [""  []  '()  {}     #{}  nil] )

  (throws? (t/not-empty? 5))
  (throws? (t/not-empty? 3.14)))

;-----------------------------------------------------------------------------
; spy stuff
(dotest
  (is= "(+ 2 3) => 5"
    (ts/collapse-whitespace
      (with-out-str
        (is= 5 (t/spyx (+ 2 3))))))

  ; #todo -> readme
  (is= (ts/collapse-whitespace   "(inc 0) => 1
                                  (inc 1) => 2
                                  (inc 2) => 3 " )
    (ts/collapse-whitespace
      (with-out-str
        (is= 3 (t/spyx (inc 0)
                 (inc 1)
                 (inc 2))))))

  ; #todo -> readme
  (is= (ts/collapse-whitespace   ":some-kw
                                  (inc 1) => 2
                                  (inc 2) => 3 " )
    (ts/collapse-whitespace
      (with-out-str
        (is= 3    (t/spyx :some-kw
                    (inc 1)
                    (inc 2)))))) )

; #todo blog about this nested (is= ...) testing technique
(dotest
  (is=
    (ts/collapse-whitespace  " a => 1
                               b => 5
                               (-> (inc a) (* 2) inc) => 5 " )
    (ts/collapse-whitespace
      (with-out-str
        (is= 13
          (t/let-spy [a (inc 0)
                      b (+ 2 3)]
                   (t/spyx (-> (inc a) (* 2) inc))
            (-> b (* 2) (+ 3)))))))

  (is= (ts/collapse-whitespace  " a => 1
                                  b => 5 " )
    (ts/collapse-whitespace
      (with-out-str
        (is= 17
          (t/let-spy [a (inc 0)
                    b (+ 2 3)]
                   (-> b (* (inc a)) (+ 7))))))))


(dotest
  (testing "basic usage"
    (let [side-effect-cum-sum (atom 0)  ; side-effect running total

          ; Returns the sum of its arguments AND keep a running total.
          side-effect-add!  (fn [ & args ]
                              (let [result (apply + args) ]
                                (swap! side-effect-cum-sum + result)
                                result)) ]
      (is= ":hi => 5"
        (ts/collapse-whitespace (with-out-str (t/spy (side-effect-add! 2 3) :hi))) )
      (is= ":hi => 5"
        (ts/collapse-whitespace (with-out-str (t/spy :hi  (side-effect-add! 2 3)))) )
      (is= ":after-inc => 2"
        (ts/collapse-whitespace (with-out-str (-> 1
                                                (inc)
                                                (t/spy :after-inc) ; add a custom keyword message
                                                (* 2)))))
      (is= ":after-inc => 2"
        (ts/collapse-whitespace (with-out-str (->> 1
                                                (inc)
                                                (t/spy :after-inc) ; add a custom keyword message
                                                (* 2)))))

      (is= "(side-effect-add! 2 3) => 5"
        (ts/collapse-whitespace (with-out-str (t/spyx (side-effect-add! 2 3)))) )
      (is= 15 @side-effect-cum-sum))

    (is= ":first => 5 :second => 25"
      (ts/collapse-whitespace
        (with-out-str (-> 2
                        (+ 3)
                        (t/spy :first )
                        (* 5)
                        (t/spy :second) ))))
    (is= ":first => 5 :second => 25"
      (ts/collapse-whitespace
        (with-out-str (->> 2
                        (+ 3)
                        (t/spy :first )
                        (* 5)
                        (t/spy :second) ))))

    (let [side-effect-cum-sum (atom 0)  ; side-effect running total

          ; Returns the sum of its arguments AND keep a running total.
          side-effect-add!  (fn [ & args ]
                              (let [result (apply + args) ]
                                (swap! side-effect-cum-sum + result)
                                result))
          ]
      (is= ":value => 5"
        (ts/collapse-whitespace (with-out-str (t/spy (side-effect-add! 2 3) :value))))
      (is= ":value => 5"
        (ts/collapse-whitespace (with-out-str (t/spy :value  (side-effect-add! 2 3)))))
      (is= 10 @side-effect-cum-sum)

      (is= ":value => 5" (ts/collapse-whitespace (with-out-str (t/spy :value (+ 2 3) ))))
      (is=   ":spy => 5" (ts/collapse-whitespace (with-out-str (t/spy        (+ 2 3) ))))

      (is= "(str \"abc\" \"def\") => \"abcdef\""
        (ts/collapse-whitespace (with-out-str (t/spyx (str "abc" "def") ))))

      ; (throws? (t/spy :some-tag "some-str" 42))  ; #todo how test in cljs?
    )))

(dotest
  (let [fn2   (fn []  (t/with-spy-indent
                        (t/spy :msg2 (+ 2 3))))
        fn1   (fn []  (t/with-spy-indent
                        (t/spy :msg1 (+ 2 3))
                        (fn2)))
        fn0   (fn [] (t/spy :msg0 (+ 2 3))) ]
    (is= ":msg2 => 5"            (ts/collapse-whitespace (with-out-str (fn2))))
    (is= ":msg1 => 5 :msg2 => 5" (ts/collapse-whitespace (with-out-str (fn1))))
    (is= ":msg0 => 5"            (ts/collapse-whitespace (with-out-str (fn0)))) ))


(dotest
  (is= 3 (t/let-some [a 1
                      b 2
                      c (+ a b)]
           c))
  (is= nil (t/let-some [a 1
                        b nil
                        c 3]
             [a b c]))

  (is= 5 (t/let-some [a (+ 2 3)]
           a))
  (is= 7 (t/let-some [a (+ 2 3)
                      b (inc a)
                      c (inc b)]
           c))
  (is= nil (t/let-some [a (+ 2 3)
                        b nil
                        c (inc b)]
             c))
  (is= nil (t/let-some [a (+ 2 3)
                        b (when (< 5 0) a)
                        c (inc b)]
             c))
  (is= [0 [1 2 3 4]] (t/let-some [tgt 5
                                  [x & others] (range tgt)]
                       [x others]))
  (is= nil (t/let-some [tgt nil
                        [x & others] (range tgt)]
             [x others])))

(dotest
  (testing "vecs"
    (let [coll (range 3)]
      (isnt (t/contains-elem? coll -1))
      (is   (t/contains-elem? coll  0))
      (is   (t/contains-elem? coll  1))
      (is   (t/contains-elem? coll  2))
      (isnt (t/contains-elem? coll  3))
      (isnt (t/contains-elem? coll  nil)))

    (let [coll [ 1 :two "three" \4]]
      (isnt (t/contains-elem? coll  :no-way))
      (isnt (t/contains-elem? coll  nil))
      (is   (t/contains-elem? coll  1))
      (is   (t/contains-elem? coll  :two))
      (is   (t/contains-elem? coll  "three"))
      (is   (t/contains-elem? coll  \4)))

    (let [coll [:yes nil 3]]
      (isnt (t/contains-elem? coll  :no-way))
      (is   (t/contains-elem? coll  :yes))
      (is   (t/contains-elem? coll  nil))))

  (testing "maps"
    (let [coll {1 :two "three" \4}]
      (isnt (t/contains-elem? coll nil ))
      (isnt (t/contains-elem? coll [1 :no-way] ))
      (is   (t/contains-elem? coll [1 :two]))
      (is   (t/contains-elem? coll ["three" \4])))
    (let [coll {1 nil "three" \4}]
      (isnt (t/contains-elem? coll [nil 1] ))
      (is   (t/contains-elem? coll [1 nil] )))
    (let [coll {nil 2 "three" \4}]
      (isnt (t/contains-elem? coll [1 nil] ))
      (is   (t/contains-elem? coll [nil 2] ))))

  (testing "sets"
    (let [coll #{1 :two "three" \4}]
      (isnt (t/contains-elem? coll  :no-way))
      (is   (t/contains-elem? coll  1))
      (is   (t/contains-elem? coll  :two))
      (is   (t/contains-elem? coll  "three"))
      (is   (t/contains-elem? coll  \4)))

    (let [coll #{:yes nil}]
      (isnt (t/contains-elem? coll  :no-way))
      (is   (t/contains-elem? coll  :yes))
      (is   (t/contains-elem? coll  nil)))))

(dotest
  (is   (t/contains-key?  {:a 1 :b 2} :a))
  (is   (t/contains-key?  {:a 1 :b 2} :b))
  (isnt (t/contains-key?  {:a 1 :b 2} :x))
  (isnt (t/contains-key?  {:a 1 :b 2} :c))
  (isnt (t/contains-key?  {:a 1 :b 2}  1))
  (isnt (t/contains-key?  {:a 1 :b 2}  2))

  (is   (t/contains-key?  {:a 1 nil   2} nil))
  (isnt (t/contains-key?  {:a 1 :b  nil} nil))
  (isnt (t/contains-key?  {:a 1 :b    2} nil))

  (is   (t/contains-key? #{:a 1 :b 2} :a))
  (is   (t/contains-key? #{:a 1 :b 2} :b))
  (is   (t/contains-key? #{:a 1 :b 2}  1))
  (is   (t/contains-key? #{:a 1 :b 2}  2))
  (isnt (t/contains-key? #{:a 1 :b 2} :x))
  (isnt (t/contains-key? #{:a 1 :b 2} :c))

  (is   (t/contains-key? #{:a 5 nil   "hello"} nil))
  (isnt (t/contains-key? #{:a 5 :doh! "hello"} nil))

  (throws? (t/contains-key? [:a 1 :b 2] :a))
  (throws? (t/contains-key? [:a 1 :b 2]  1)))

(dotest
  (is   (t/contains-val? {:a 1 :b 2} 1))
  (is   (t/contains-val? {:a 1 :b 2} 2))
  (isnt (t/contains-val? {:a 1 :b 2} 0))
  (isnt (t/contains-val? {:a 1 :b 2} 3))
  (isnt (t/contains-val? {:a 1 :b 2} :a))
  (isnt (t/contains-val? {:a 1 :b 2} :b))

  (is   (t/contains-val? {:a 1 :b nil} nil))
  (isnt (t/contains-val? {:a 1 nil  2} nil))
  (isnt (t/contains-val? {:a 1 :b   2} nil))

  (throws? (t/contains-val?  [:a 1 :b 2] 1))
  (throws? (t/contains-val? #{:a 1 :b 2} 1)))

(dotest
  (is= (t/forv [x (range 4)] (* x x))
    [0 1 4 9] )
  (is= (t/forv [x (range 23)] (* x x))
    (for  [x (range 23)] (* x x)))
  (is= (t/forv [x (range 5)  y (range 2 9)] (str x y))
    (for  [x (range 5)  y (range 2 9)] (str x y))))

(dotest
  (let [xs [1 2 3]
        ys [10 20 30]]
    (is= [11 22 33]
      (t/map-let [x xs y ys] (+ x y))
      (t/map-let* {:lazy false :strict false} [x xs y ys] (+ x y))
      (t/map-let* {:lazy false :strict true}  [x xs y ys] (+ x y))
      (t/map-let* {:lazy true :strict false}  [x xs y ys] (+ x y))
      (t/map-let* {:lazy true :strict true}   [x xs y ys] (+ x y)))
    (let [result-vec (t/map-let* {:lazy false :strict true} [x xs y ys] (+ x y))
          result-lazyseq (t/map-let* {:lazy true :strict true} [result-vec xs y ys] (+ result-vec y))]
      (t/spyx (type result-vec))
      (t/spyx (type result-lazyseq))

      (is (instance?
            #?(:clj clojure.lang.PersistentVector)
            #?(:cljs cljs.core/PersistentVector)
            result-vec))
      (is (instance?
            #?(:clj clojure.lang.LazySeq)
            #?(:cljs cljs.core/LazySeq)
            result-lazyseq)))
      )

  (let [xs [1 2 3]
        ys [10 20 30 40]]
    (throws?        (t/map-let                  [x xs y ys] (+ x y)))
    (throws?        (t/map-let* {:strict true}  [x xs y ys] (+ x y)))
    (is= [11 22 33] (t/map-let* {:strict false} [x xs y ys] (+ x y)))))

(dotest
  (is= (vector [])               [[]] )
  (is= (mapv identity [] [])      []  )

  (is= [[:a 0] [:b 1] [:c 2]]
    (t/zip-lazy [:a :b :c] [0 1 2])
    (t/zip-lazy [:a :b :c] (range)))
  (is= (t/zip-lazy [:a :b :c] [1 2 3])   [[:a 1] [:b 2] [:c 3]] )
  (is= (t/zip-lazy [:a] [1])             [[:a 1]] )
  (is= (t/zip-lazy [] [])                []  )
  (is= (t/zip-lazy [:A :B :C] [:a :b :c] [1 2 3])
    [[:A :a 1] [:B :b 2] [:C :c 3]] )
  (is (instance?
        #?(:clj clojure.lang.LazySeq)
        #?(:cljs cljs.core/LazySeq)
        (t/zip-lazy [:a :b :c] (range))))

  (is= (t/zip [:a :b :c] [1 2 3])   [[:a 1] [:b 2] [:c 3]] )   ; #todo fails when use Schema for append/prepend
  (is= (t/zip [:a] [1])             [[:a 1]] )                 ; #todo fails when use Schema for append/prepend
  (is= (t/zip [] [])                []  )
  (is= (t/zip [:A :B :C] [:a :b :c] [1 2 3])
    [[:A :a 1] [:B :b 2] [:C :c 3]] )
  (throws? (t/zip [:a :b :c] [1 2 3 4]))
  (is= (t/zip* {:strict false} [:a :b :c] [1 2 3 4]) [[:a 1] [:b 2] [:c 3]] )

  (is (instance?
        #?(:clj clojure.lang.PersistentVector)
        #?(:cljs cljs.core/PersistentVector)
        (t/zip*  {:trunc false} [:a :b :c] [1 2 3])))
  (let [keys   [:a :b :c]
        vals   [1 2 3]
        result (atom [])]
    (doseq [[k i] (t/zip keys vals)]
      (swap! result t/append {k i}))
    (is= [{:a 1} {:b 2} {:c 3}] @result))

  ; verify that zip throws if unequal lengths, even if some colls are infinite lazy seqs
  (throws? (t/zip            [:a :b :c] [1 2 3 4]))
  (throws? (t/zip [:A :B :C] [:a :b :c] [1 2 3 4]))
  (throws? (t/zip [:a :b :c] (range)))
  (is= (t/zip* {:strict false} [:a :b :c] (range))   [[:a 0] [:b 1] [:c 2]] )
  (is= (t/zip* {:strict false} [:a :b :c] [1 2 3 4]) [[:a 1] [:b 2] [:c 3]] )
  (is= (t/zip* {:strict false} [:A :B :C] [:a :b :c] [1 2 3 4]) [[:A :a 1] [:B :b 2] [:C :c 3]] ))

(dotest
  (is= (t/indexed [:a :b :c]) [[0 :a] [1 :b] [2 :c]])
  (is= [[0 0] [1 2] [2 4] [3 6] [4 8]]
    (take 5 (t/indexed (map #(* 2 %) (range))))) ; can work with infinite lazy lists
  (is= (t/indexed [:a :b :c]  (map #(+ 10 %) (range)))
    [ [0 :a 10]
     [1 :b 11]
     [2 :c 12] ] )
  (is= (take 5 (t/indexed (map #(+ 10 %) (range))))
    [ [0 10]
     [1 11]
     [2 12]
     [3 13]
     [4 14] ] ))

(dotest
  ; unexpected results
  (is (= (concat {:a 1} {:b 2} {:c 3} )
        [ [:a 1] [:b 2] [:c 3] ] ))
  (is (= (conj [1 2] [3 4])
        [1 2  [3 4] ] ))

  (let [objs   [ [] '()   {} (sorted-map)   #{} (sorted-set) ] ]
    (is= (map sequential? objs) [true  true    false false   false false] )
    (is= (map map?        objs) [false false   true  true    false false] )
    (is= (map set?        objs) [false false   false false   true  true ] ))

  (is= (t/glue [1 2]  [3 4] [5 6])        [1 2 3 4 5 6])
  (is= (t/glue [1 2] '(3 4) [5 6])        [1 2 3 4 5 6])
  (is= (t/glue [] [1 2] )                [1 2] )
  (is= (t/glue [1 2] [] )                [1 2] )
  (is= (t/glue [] [1 2] [] )             [1 2] )

  (is= (t/glue '(1 2) '(3 4) '(5 6))        [1 2 3 4 5 6])
  (is= (t/glue '(1 2)  [3 4] '(5 6))        [1 2 3 4 5 6])
  (is= (t/glue  [1 2] '(3 4) '(5 6))        [1 2 3 4 5 6])
  (is= (t/glue '() '(1 2) )                 [1 2] )
  (is= (t/glue '(1 2) '() )                 [1 2] )
  (is= (t/glue '() '(1 2) '() )             [1 2] )

  (is= (t/glue (range 3) (range 5))      [0 1 2 0 1 2 3 4] )

  (is= (t/glue {:a 1} {:b 2} {:c 3})      {:a 1 :c 3 :b 2})
  (is= (t/glue {:a 1} {:b 2} )            {:a 1 :b 2})
  (is= (t/glue {:a 1} {} )                {:a 1} )
  (is= (t/glue {} {:a 1} )                {:a 1} )
  (is= (t/glue {} {:a 1} {} )             {:a 1} )

  (is= (t/glue #{1 2} #{3 4} #{6 5})     #{1 2 6 5 3 4})
  (is= (t/glue #{} #{1 2} )              #{1 2} )
  (is= (t/glue #{1 2} #{} )              #{1 2} )
  (is= (t/glue #{} #{1 2} #{} )          #{1 2} )

  (is= (t/glue (sorted-map) {:a 1} {:b 2} {:c 3})   {:a 1 :b 2 :c 3} )
  (is= (t/glue (sorted-set) #{1 2} #{3 4} #{6 5})   #{1 2 3 4 5 6})

  (is=      (t/glue (sorted-map) (hash-map :a 1   :b 2   :c 3   :d 4   :e 5   :f 6))
    {:a 1   :b 2   :c 3   :d 4   :e 5   :f 6} )
  (is= (seq (t/glue (sorted-map) (hash-map :a 1   :b 2   :c 3   :d 4   :e 5   :f 6)))
    [ [:a 1] [:b 2] [:c 3] [:d 4] [:e 5] [:f 6] ] )

  (is= (t/glue  {:band :VanHalen :singer :Dave} {:singer :Sammy} )
                {:band :VanHalen                 :singer :Sammy} )

  (is= (t/glue \a )           "a" )
  (is= (t/glue "a")           "a" )
  (is= (t/glue \a "")         "a" )
  (is= (t/glue "" "a")        "a" )
  (is= (t/glue \a  \b)        "ab" )
  (is= (t/glue "a" "b")       "ab" )
  (is= (t/glue "a" \b)        "ab" )
  (is= (t/glue \a  "b")       "ab" )
  (is= (t/glue "" "a" \b)     "ab" )
  (is= (t/glue "" \a  "b")    "ab" )
  (is= (t/glue "a" "" \b)     "ab" )
  (is= (t/glue \a  "" "b")    "ab" )
  (is= (t/glue "a" \b  "")    "ab" )
  (is= (t/glue \a  "b" "")    "ab" )
  (is= (t/glue \a  "b" "")    "ab" )
  (is= (t/glue "I" \space "like " \a " nap!" )    "I like a nap!" )
  (is= (apply t/glue [ "I" \space "like " \a " nap!"] )    "I like a nap!" )

  (throws? (t/glue   [1 2]     {:a 1} ))
  (throws? (t/glue  '(1 2)     {:a 1} ))
  (throws? (t/glue   [1 2]    #{:a 1} ))
  (throws? (t/glue  '(1 2)    #{:a 1} ))
  (throws? (t/glue   [1 2]    "hello" ))
  (throws? (t/glue  '(1 2)    "hello" ))
  (throws? (t/glue   {:a 1}   #{:a 1} ))
  (throws? (t/glue   {:a 1}   "hello" ))
  (throws? (t/glue   #{:a 1}  "hello" ))
  (throws? (t/glue   [1 2]     nil    ))
  (throws? (t/glue  '(1 2)     nil    ))
  (throws? (t/glue   {:a 1}    nil    ))
  (throws? (t/glue   #{:a 1}   nil    ))
  (throws? (t/glue   "hello"   nil    )) )

(dotest
  (let [data [[0 1 2]
              []
              [3]
              [4 5]
              [6 7 8 9]]]
    (is= (t/thru 9) (t/glue-rows data))
    (is= (t/thru 9) (reduce into [] data))))

(dotest
  (throws?            (t/append  1 2        ))
  (throws?            (t/append [1 2]       ))
  (throws?            (t/append nil   3     ))
  (is= [1 2 3    ]    (t/append [1 2] 3     ))
  (is= [1 2 3 4  ]    (t/append [1 2] 3 4   ))
  (is= [1 2 3 4 5]    (t/append [1 2] 3 4 5 ))

  (throws?            (t/append '(1 2)       ))
  (is= [1 2 3    ]    (t/append '(1 2) 3     ))
  (is= [1 2 3 4  ]    (t/append '(1 2) 3 4   ))
  (is= [1 2 3 4 5]    (t/append '(1 2) 3 4 5 ))

  (throws?            (t/append   {:a 1} 99     ))
  (throws?            (t/append   {:a 1} {:b 2} ))
  (throws?            (t/append  #{:a 1} 99     ))
  (throws?            (t/append  #{:a 1} #{99}  ))

  (testing "old conjv tests"
    (is= [  2  ]    (t/append  []  2   ))
    (is= [  2  ]    (t/append '()  2   ))
    (is= [  2 3]    (t/append  []  2  3))
    (is= [  2 3]    (t/append '()  2  3))

    (is= [1 2 3]    (t/append  [1] 2  3))
    (is= [1 2 3]    (t/append '(1) 2  3))
    (is= [1 2 3]    (t/append  [1  2] 3))
    (is= [1 2 3]    (t/append '(1  2) 3))

    (is= [1 2 3 4]  (t/append  [1  2] 3 4))
    (is= [1 2 3 4]  (t/append '(1  2) 3 4))
    (is= [1 2 3 4]  (t/append  [1] 2  3 4))
    (is= [1 2 3 4]  (t/append '(1) 2  3 4))

    (is= [[1 2] [3 4] [5 6]] (t/append  [[1 2] [3 4]]  [5 6] ))

    (is= [0 1 2 3 4 5] (t/append (range 4) 4 5))
    (is= [0 1 2 3 4 5] (apply t/append [0] (range 1 6)))))

(dotest
  (throws?            (t/prepend       [2 1] ))
  (throws?            (t/prepend     3  nil  ))
  (is= [    3 2 1]    (t/prepend     3 [2 1] ))
  (is= [  4 3 2 1]    (t/prepend   4 3 [2 1] ))
  (is= [5 4 3 2 1]    (t/prepend 5 4 3 [2 1] ))

  (throws?            (t/prepend       '(2 1) ))
  (is= [    3 2 1]    (t/prepend     3 '(2 1) ))
  (is= [  4 3 2 1]    (t/prepend   4 3 '(2 1) ))
  (is= [5 4 3 2 1]    (t/prepend 5 4 3 '(2 1) ))

  (throws?            (t/prepend   99     {:a 1} ))
  (throws?            (t/prepend   {:b 2} {:a 1} ))
  (throws?            (t/prepend   99    #{:a 1} ))
  (throws?            (t/prepend  #{99}  #{:a 1} )))

(dotest
  (is= [1 2 3 4 5 6 7 8 9] (t/->vector 1 2 3 4 5 6 7 8 9))
  (is= [1 2 3 4 5 6 7 8 9] (t/->vector 1 (t/unwrap [2 3 4 5 6 7 8]) 9))
  (is= [1 2 3 4 5 6 7 8 9] (t/->vector 1 (t/unwrap [2 (t/unwrap [3 4 5 6 7]) 8]) 9))
  (is= [1 2 3 4 5 6 7 8 9] (t/->vector 1 (t/unwrap [2 (t/unwrap [3 (t/unwrap [4 5 6]) 7]) 8]) 9))
  (is= [1 2 3 4 5 6 7 8 9] (t/->vector 1 (t/unwrap [2 (t/unwrap [3 (t/unwrap [4 (t/unwrap [5]) 6]) 7]) 8]) 9))
  (is= [1 2 3 4 5 6 7 8 9] (t/->vector 1 2 3 (t/unwrap [4 5 6]) 7 8 9))
  (is= [1 2 3 4 5 6 7 8 9] (t/->vector 1 (t/unwrap [2 3 (t/unwrap [4 5 6]) 7 8]) 9))

  (is= [1 2 3 [4  5  6] 7 8 9] (t/->vector 1 (t/unwrap [2 3 [4  5  6] 7 8]) 9))
  (is= [1 2 3 [4 [5] 6] 7 8 9] (t/->vector 1 (t/unwrap [2 3 [4 [5] 6] 7 8]) 9))

  (is= [1 [2 3 4 [5] 6 7 8] 9] (t/->vector 1 `(2 3 ~(t/unwrap [4 [5] 6]) 7 8) 9))
  (is= [1 [2 3 4 [5] 6 7 8] 9] (t/->vector 1  [2 3  (t/unwrap [4 [5] 6]) 7 8] 9))

  (is= [1 2 3 4 5 6 7 8 9] (t/glue   [1] [2] [3] [4 5 6] [7] [8] [9]))
  (is= [1 2 3 4 5 6 7 8 9] (concat   [1] [2] [3] [4 5 6] [7] [8] [9]))
  (is= [1 2 3 4 5 6 7 8 9] (t/glue   [1   2   3] [4 5 6] [7   8   9]))
  (is= [1 2 3 4 5 6 7 8 9] (concat   [1   2   3] [4 5 6] [7   8   9])))

(dotest
  (isnt (t/increasing? [1 2] [1]))
  (isnt (t/increasing? [1 2] [1 1]))
  (isnt (t/increasing? [1 2] [1 2]))
  (is (t/increasing? [1 2] [1 2 nil]))
  (is (t/increasing? [1 2] [1 2 3]))
  (is (t/increasing? [1 2] [1 3]))
  (is (t/increasing? [1 2] [2 1]))
  (is (t/increasing? [1 2] [2]))

  (isnt (t/increasing-or-equal? [1 2] [1]))
  (isnt (t/increasing-or-equal? [1 2] [1 1]))
  (is (t/increasing-or-equal? [1 2] [1 2]))
  (is (t/increasing-or-equal? [1 2] [1 2 nil]))
  (is (t/increasing-or-equal? [1 2] [1 2 3]))
  (is (t/increasing-or-equal? [1 2] [1 3]))
  (is (t/increasing-or-equal? [1 2] [2 1]))
  (is (t/increasing-or-equal? [1 2] [2])))

(dotest
  (let [map1  { :a 1 :b 2 :c nil
               nil :NIL
               "hi" "hello"
               5 "five"}]
    (is= 1           (t/grab :a   map1))
    (is= 2           (t/grab :b   map1))
    (is= nil         (t/grab :c   map1))
    (is= :NIL        (t/grab nil  map1))
    (is= "hello"     (t/grab "hi"  map1))
    (is= "five"      (t/grab 5  map1))
    (throws?  (t/grab :z map1))
    (throws?  (t/grab 42 map1))
    ))

(dotest
  (testing "basic usage"
    (let [map1  {:a1 "a1"
                 :a2 { :b1 "b1"
                      :b2 { :c1 "c1"
                           :c2 "c2" }}
                 nil "NIL"
                 :nil nil} ]
      (is= "a1"  (t/fetch-in map1 [:a1]))
      (is= "b1"  (t/fetch-in map1 [:a2 :b1]))
      (is= "c1"  (t/fetch-in map1 [:a2 :b2 :c1]))
      (is= "c2"  (t/fetch-in map1 [:a2 :b2 :c2]))
      (is= "NIL" (t/fetch-in map1 [nil]))
      (is= nil   (t/fetch-in map1 [:nil]))
      (throws?   (t/fetch-in map1 [:a9]))
      (throws?   (t/fetch-in map1 [:a2 :b9]))
      (throws?   (t/fetch-in map1 [:a2 :b2 :c9])))))

(dotest
  (let [mm    {:a { :b { :c "c" }}} ]
    (is= (t/dissoc-in mm []         )          mm )
    (is= (t/dissoc-in mm [:a]       )          {} )
    (is= (t/dissoc-in mm [:a :b]    )          {:a  {}} )
    (is= (t/dissoc-in mm [:a :b :c] )          {:a  { :b  {}}} )
    (is= (t/dissoc-in mm [:a :x :y] )          {:a  { :b  { :c "c" }
                                                   :x  nil }} )
    (is= (t/dissoc-in mm [:a :x :y :z] )       {:a  { :b  { :c "c" }
                                                   :x  { :y nil }}} )
    (is= (t/dissoc-in mm [:k1 :k2 :k3 :kz] )   {:a  { :b  { :c  "c" }}
                                              :k1 { :k2 { :k3 nil }}} ))
  (let [mm    {:a1 "a1"
               :a2 { :b1 "b1"
                    :b2 { :c1 "c1"
                         :c2 "c2" }}} ]
    (is= (t/dissoc-in mm [:a1] )
      {:a2 { :b1 "b1"
            :b2 { :c1 "c1"
                 :c2 "c2" }}} )
    (is= (t/dissoc-in mm [:a2] )
      {:a1 "a1" } )
    (is= (t/dissoc-in mm [:a2 :b1] )
      {:a1 "a1"
       :a2 { :b2 { :c1 "c1"
                  :c2 "c2" }}} )
    (is= (t/dissoc-in mm [:a2 :b2] )
      {:a1 "a1"
       :a2 { :b1 "b1" }} )
    (is= (t/dissoc-in mm [:a2 :b2 :c1] )
      {:a1 "a1"
       :a2 { :b1 "b1"
            :b2 { :c2 "c2" }}} )
    (is= (t/dissoc-in mm [:a2 :b2 :c2] )
      {:a1 "a1"
       :a2 { :b1 "b1"
            :b2 { :c1 "c1" }}} )))

(dotest
  (t/try-catchall
    (throw (ex-info "some-big-error" {:answer 43}))
    (catch e
           (println "Caught:" e)))

  (let [x (t/with-exception-default :XXX
            (throw (ex-info "some-big-error" {:answer 43})))]
    (println :awt110 x)
    (is true)))

(dotest
  (let [map1  {:a 1 :b 2 :c 3 :d 4 :e 5}]
    (is= {:a 1 :b 2 :c 3 :d 4 :e 5} (t/submap-by-keys map1 #{ :a :b :c :d :e } ))
    (is= {     :b 2 :c 3 :d 4 :e 5} (t/submap-by-keys map1 #{    :b :c :d :e } ))
    (is= {          :c 3 :d 4 :e 5} (t/submap-by-keys map1 #{       :c :d :e } ))
    (is= {               :d 4 :e 5} (t/submap-by-keys map1 #{          :d :e } ))
    (is= {                    :e 5} (t/submap-by-keys map1 #{             :e } ))
    (is= {                        } (t/submap-by-keys map1 #{                } ))
    (throws? (t/submap-by-keys map1 #{:z}))

    (is= {:a 1 :b 2 :c 3 :d 4 :e 5} (t/submap-by-keys map1 #{ :a :b :c :d :e  :z } :missing-ok))
    (is= {     :b 2 :c 3 :d 4 :e 5} (t/submap-by-keys map1 #{    :b :c :d :e  :z } :missing-ok))
    (is= {          :c 3 :d 4 :e 5} (t/submap-by-keys map1 #{       :c :d :e  :z } :missing-ok))
    (is= {               :d 4 :e 5} (t/submap-by-keys map1 #{          :d :e  :z } :missing-ok))
    (is= {                    :e 5} (t/submap-by-keys map1 #{             :e  :z } :missing-ok))
    (is= {                        } (t/submap-by-keys map1 #{                 :z } :missing-ok))

    (is= {:a 1 :b 2 :c 3 :d 4 :e 5} (t/submap-by-vals map1 #{ 1 2 3 4 5 } ))
    (is= {     :b 2 :c 3 :d 4 :e 5} (t/submap-by-vals map1 #{   2 3 4 5 } ))
    (is= {          :c 3 :d 4 :e 5} (t/submap-by-vals map1 #{     3 4 5 } ))
    (is= {               :d 4 :e 5} (t/submap-by-vals map1 #{       4 5 } ))
    (is= {                    :e 5} (t/submap-by-vals map1 #{         5 } ))
    (is= {                        } (t/submap-by-vals map1 #{           } ))
    (throws? (t/submap-by-vals map1 #{ 99 }))

    (is= {:a 1 :b 2 :c 3 :d 4 :e 5} (t/submap-by-vals map1 #{ 1 2 3 4 5  99 } :missing-ok ))
    (is= {     :b 2 :c 3 :d 4 :e 5} (t/submap-by-vals map1 #{   2 3 4 5  99 } :missing-ok ))
    (is= {          :c 3 :d 4 :e 5} (t/submap-by-vals map1 #{     3 4 5  99 } :missing-ok ))
    (is= {               :d 4 :e 5} (t/submap-by-vals map1 #{       4 5  99 } :missing-ok ))
    (is= {                    :e 5} (t/submap-by-vals map1 #{         5  99 } :missing-ok ))
    (is= {                        } (t/submap-by-vals map1 #{            99 } :missing-ok ))
    (is= {                        } (t/submap-by-vals map1 #{               } :missing-ok ))

    (is= { 0 :even 2 :even } (t/submap-by-vals
                               { 0 :even 1 :odd 2 :even 3 :odd }
                                #{ :even } ))
    (is= { 0 :even 2 :even } (t/submap-by-vals
                               { 0 :even 1 :odd 2 :even 3 :odd }
                                #{ :even :prime } :missing-ok ))) )

(dotest             ; -1 implies "in order"
  ; empty list is smaller than any non-empty list
  (is (neg? (t/lexical-compare [] [2])))
  (is (neg? (t/lexical-compare [] [\b])))
  (is (neg? (t/lexical-compare [] ["b"])))
  (is (neg? (t/lexical-compare [] [:b])))
  (is (neg? (t/lexical-compare [] ['b])))

  ; nil is smaller than any non-nil item
  (is (neg? (t/lexical-compare [nil] [2])))
  (is (neg? (t/lexical-compare [nil] [\b])))
  (is (neg? (t/lexical-compare [nil] ["b"])))
  (is (neg? (t/lexical-compare [nil] [:b])))
  (is (neg? (t/lexical-compare [nil] ['b])))

  ; Cannot compare items from different classes:  number, char, string, keyword, symbol
  (throws? (t/lexical-compare [1] [\b]))
  (throws? (t/lexical-compare [1] ["b"]))
  (throws? (t/lexical-compare [1] [:b]))
  (throws? (t/lexical-compare [1] ['b]))

 #?(:clj (throws? (t/lexical-compare [\b] ["b"])))

  (throws? (t/lexical-compare [\b] [:b]))
  (throws? (t/lexical-compare [\b] ['b]))
  (throws? (t/lexical-compare ["b"] [:b]))
  (throws? (t/lexical-compare ["b"] ['b]))
  (throws? (t/lexical-compare [:b] ['b]))

  ; different positions in list can be of different class
  (is (neg? (t/lexical-compare [:a] [:b])))
  (is (neg? (t/lexical-compare [:a] [:a 1])))
  (is (neg? (t/lexical-compare [1 :a] [2])))
  (is (neg? (t/lexical-compare [:a] [:a 1])))
  (is (neg? (t/lexical-compare [1] [1 :a])))
  (is (neg? (t/lexical-compare [1 :a] [2])))

  ; same position in list can be of different class if sorted by previous positions
  (is (neg? (t/lexical-compare [1 :z] [2 9]))) ; OK since prefix lists [1] & [2] define order
  (throws?  (t/lexical-compare [1 :z] [1 2])) ; not OK since have same prefix list: [1]

  (is= (vec (sorted-set-by t/lexical-compare [1 :a] [1] [2]))
    [[1] [1 :a] [2]])
  (is= (vec (sorted-set-by t/lexical-compare [2 0] [2] [3] [3 :y] [1] [1 :a] [1 :b] [1 :b 3]))
    [[1]
     [1 :a]
     [1 :b]
     [1 :b 3]
     [2]
     [2 0]
     [3]
     [3 :y]]))

(dotest
  (is= 3 (t/validate pos? 3))
  (is= 3.14 (t/validate number? 3.14))
  (is= 3.14 (t/validate #(< 3 % 4) 3.14))
  (is= [0 1 2] (t/validate vector? (vec (range 3))))
  (is= nil (t/validate nil? (next [])))
  (is= [0 1 2] (t/validate #(= 3 (count %)) [0 1 2]))
  (throws? (t/validate number? "hello"))
  (throws? (t/validate t/truthy? nil)) )

(dotest
  (throws? (t/verify (= 1 2)))
  (is= 333 (t/verify (* 3 111))))

(dotest
  (let [m1 {:a 1 :b 2 :c 3}
        m2 {:a 1 :b 2 :c [3 4]}]
    (is= m1 (apply hash-map (t/keyvals m1)))
    (is= m2 (apply hash-map (t/keyvals m2)))))
; AWTAWT TODO: add test.check

(dotest
  (let [m1 {:a 1 :b 2 :c 3} ]
    (is= [ :a 1 :b 2      ] (t/keyvals-seq m1 [:a :b]))
    (is= [ :b 2 :a 1      ] (t/keyvals-seq m1 [:b :a]))
    (is= [ :a 1 :b 2 :c 3 ] (t/keyvals-seq m1 [:a :b :c]))
    (is= [ :c 3 :a 1 :b 2 ] (t/keyvals-seq m1 [:c :a :b]))
    (is= [ :c 3 :b 2 :a 1 ] (t/keyvals-seq m1 [:c :b :a]))
    (is= [ :a 1 :b 2 :a 1 ] (t/keyvals-seq m1 [:a :b :a]))

    (throws? (t/keyvals-seq m1 [:a :b :z]))
    (is= [:a 1 :b 2] (t/keyvals-seq {:missing-ok true
                                     :the-map    m1 :the-keys [:a :b :z]}))
    (is= [:b 2 :c 3] (t/keyvals-seq {:missing-ok true
                                     :the-map    m1 :the-keys [:z :b :c]})) ))

(dotest
  (is= 2 (t/it-> 1
           (inc it)
           (+ 3 it)
           (/ 10 it)))
  (let [mm  {:a {:b 2}}]
    (is= (t/it-> mm (:a it)          )  {:b 2} )
    (is= (t/it-> mm (it :a)  (:b it) )      2  ))
  (is= 48 (t/it-> 42
            (let [x 5]
              (+ x it))
            (inc it))))

(dotest
  (let [params {:a 1 :b 1 :c nil :d nil}]
    (is= (t/cond-it-> params
           (:a it)              (update it :b inc)
           (= (:b it) 2)        (assoc it :c "here")
           (= "here" (:c it))   (assoc it :d "again"))
      {:a 1, :b 2, :c "here", :d "again"}))

  (let [params {:a nil :b 1 :c nil :d nil}]
    (is= (t/cond-it-> params
           (:a it)                (update it :b inc)
           (= (:b it) 1)          (assoc it :c "here")
           (= "here" (:c it))     (assoc it :d "again"))
      {:a nil, :b 1, :c "here", :d "again"}))

  (let [params {:a 1 :b 1 :c nil :d nil}]
    (is= (t/cond-it-> params
           (:a it)        (update it :b inc)
           (= (:b it) 2)  (update it :b inc)
           (:c it)        (assoc it :d "again"))
      {:a 1, :b 3, :c nil :d nil})) )

(dotest
  (is= 8 (t/some-it-> 1
           (inc it)
           (* it 3)
           (+ 2 it)))
  (is (nil? (t/some-it-> nil
              (inc it)
              (* it 3)
              (+ 2 it))))
  (is (nil? (t/some-it-> 1 (inc it)
              (when false (* it 3))
              (+ 2 it)))) )

(dotest
  ; java way to throw
  #?(:clj
     (do (throws? (throw (RuntimeException. "bummer")))
         (is= nil (t/with-exception-default nil (throw (RuntimeException. "bummer"))))
         (is= :dummy (t/with-exception-default :dummy (throw (RuntimeException. "bummer dude"))))

         (is= 123 (t/with-exception-default 0 (Long/parseLong "123")))
         (is= 0 (t/with-exception-default 0 (Long/parseLong "12xy3")))))
  ; clojurescript way to throw
  #?(:cljs
     (do (throws? (throw (js/Error "bummer")))
         (is= nil (t/with-exception-default nil (throw (js/Error "bummer"))))
         (is= :dummy (t/with-exception-default :dummy (throw (js/Error "bummer dude"))))))
  ; cross-platform way to throw
  (do (throws? (throw (ex-info "some msg" :some-data)))
      (is= nil (t/with-exception-default nil (throw (ex-info "bummer"))))
      (is= :dummy (t/with-exception-default :dummy (throw (ex-info "bummer dude"))))))

(dotest
  (is= (t/validate-or-default t/not-nil? nil 0) 0)
  (is= (t/validate-or-default t/not-empty? "" "How you doin?") "How you doin?")
  (is= (mapv #(t/with-nil-default :some-default %)
         [0 1 "" [] nil true false])
    [0 1 "" [] :some-default true false]))



















