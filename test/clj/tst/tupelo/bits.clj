;   Copyright (c) Alan Thompson. All rights reserved.
;   The use and distribution terms for this software are covered by the Eclipse Public License 1.0
;   (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html at
;   the root of this distribution.  By using this software in any fashion, you are agreeing to be
;   bound by the terms of this license.  You must not remove this notice, or any other, from this
;   software.
(ns tst.tupelo.bits
  (:use tupelo.bits tupelo.core tupelo.test) )

(dotest
  (is= (char->bit \0) 0)
  (is= (char->bit \1) 1)
  (throws? (char->bit \2))

  (is= (bit->char 0) \0)
  (is= (bit->char 1) \1)
  (throws? (bit->char 2))

  (is= (take-last 5 (long->bits-unsigned 5)) [0 0 1 0 1])
  (is= Long/MAX_VALUE (-> Long/MAX_VALUE
                        (long->bits-unsigned)
                        (bits-unsigned->long)))
  (is= 0 (-> 0
           (long->bits-unsigned)
           (bits-unsigned->long)))
  (is= 1234567 (-> 1234567
                 (long->bits-unsigned)
                 (bits-unsigned->long)))

  (let [invalid-bits (glue [1 1 1] (-> Long/MAX_VALUE
                                     (long->bits-unsigned)))]
    (throws? (bits-unsigned->long invalid-bits))))
