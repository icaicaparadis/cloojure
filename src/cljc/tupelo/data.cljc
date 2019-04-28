;   Copyright (c) Alan Thompson. All rights reserved.
;   The use and distribution terms for this software are covered by the Eclipse Public License 1.0
;   (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html at
;   the root of this distribution.  By using this software in any fashion, you are agreeing to be
;   bound by the terms of this license.  You must not remove this notice, or any other, from this
;   software.
(ns tupelo.data
  (:refer-clojure :exclude [load ->VecNode])
  (:use tupelo.core) ; #todo remove for cljs
  #?(:clj (:require
            [tupelo.core :as t :refer [spy spyx spyxx spyx-pretty grab glue map-entry indexed
                                       forv vals->map fetch-in
                                       ]]
            [tupelo.schema :as tsk]
            [tupelo.data.index :as tdi]
            [clojure.set :as set]
            [schema.core :as s]
            ))
  #?(:cljs (:require
             [tupelo.core :as t :refer [spy spyx spyxx spyx-pretty grab]] ; #todo :include-macros true
             [tupelo.schema :as tsk]
             [tupelo.data.index :as tdi]
             [clojure.set :as set]
             [schema.core :as s]
             ))
  )

; #todo add indexes
; #todo add sets (primative only or HID) => map with same key/value
; #todo copy destruct syntax for search

#?(:cljs (enable-console-print!))

; #todo Tupelo Data Language (TDL)

(def customers ; #todo be able to process this data & delete unwise users
  [{:customer-id 1
    :plants      [{:plant-id  1
                   :employees [{:name "Alice" :age 35 :sex "F"}
                               {:name "Bob" :age 25 :sex "M"}]}
                  {:plant-id  2
                   :employees []}]}
   {:customer-id 2}])
(def age-of-wisdom 30)

;---------------------------------------------------------------------------------------------------
; HID & :hid are shorthand for Hash ID, the SHA-1 hash of a v1/UUID expressed as a hexadecimal keyword
; format { :hid Node }
(def HID
  "The Plumatic Schema type name for a pointer to a tdb node (abbrev. for Hex ID)"
  s/Int)

(def IndexId (s/enum :idx-num :idx-str :idx-kw))

(def HidRootSpec
  "The Plumatic Schema type name for the values accepted as starting points (roots) for a subtree path search."
  (s/conditional ; #todo why is this here?
    int? HID
    set? #{HID}
    :else [HID]))

;-----------------------------------------------------------------------------
(declare hid->node)

(defprotocol IParentable
  (parent-hid [this]))

(defprotocol IDataNode
  (content [this])
  (edn [this]))

(defprotocol INavNode
  (nav [this key]))

(defprotocol IMapEntryNode
  (me-key [this])
  (me-val-hid [this]))

(defprotocol IArrayEntryNode
  (ae-idx [this])
  (ae-elem-hid [this]))

;-----------------------------------------------------------------------------
(s/defrecord MapNode ; Represents ths content of a Clojure map.
  ; a map from key to hid
  [parent :- (s/maybe HID)
   -content :- tsk/Map] ; a map from key to hid
  IParentable
    (parent-hid [this] (s/validate (s/maybe HID) parent))
  IDataNode
    (content [this]
      (t/validate map? -content))
    (edn [this]
      (apply t/glue
        (t/forv [[k v-hid] (t/validate map? -content)]
          {k (edn (hid->node v-hid))})))
  INavNode
    (nav [this key]
      (t/grab key (t/validate map? -content))))

(s/defrecord MapEntryNode
  [parent :- HID
   -me-key :- s/Any
   -me-hid :- s/Any]
  IParentable
    (parent-hid [this] (s/validate HID parent))
  IMapEntryNode
    (me-key [this]  -me-key)
    (me-val-hid [this]  -me-hid) )

(s/defrecord ArrayNode ; Represents ths content of a Clojure vector (any sequential type coerced into a vector).
  ; stored is a vector of hids
  [parent :- (s/maybe HID)
   -content :- tsk/Map] ; a map from index to hid
  IParentable
    (parent-hid [this] (s/validate (s/maybe HID) parent))
  IDataNode
    (content [this]
      (t/validate vector? -content))
    (edn [this]
      (t/forv [elem-hid (t/validate vector? -content)]
        (edn (hid->node elem-hid))))
  INavNode
    (nav [this key]
      (if (= :* key)
        (content this)
        (nth (t/validate vector? -content) key))))

(s/defrecord ArrayEntryNode
  [parent :- HID
   -ae-idx :- s/Any
   -ae-hid :- s/Any]
  IParentable
    (parent-hid [this] (s/validate HID parent))
  IArrayEntryNode
    (ae-idx [this]  -ae-idx)
    (ae-elem-hid [this]  -ae-hid))

; #todo need to enforce set uniqueness under mutation
(s/defrecord SetNode ; Represents ths content of a Clojure set
  ; a map from key to hid
  [parent :- (s/maybe HID)
   -content :- tsk/Set]
  IParentable
    (parent-hid [this] (s/validate (s/maybe HID) parent))
  IDataNode
    (content [this]
      (t/validate set? -content))
    (edn [this]
      (let [result-vec (t/forv [v-hid (t/validate set? -content)]
                         (edn (hid->node v-hid)))]
        (when-not (apply distinct? result-vec)
          (throw (ex-info "SetNode: non-distinct entries found!" (t/vals->map -content result-vec))))
        (set result-vec)))
  INavNode
    (nav [this key]
      (t/grab key (t/validate set? -content))))

; Represents a Clojure primitive (non-collection) type,
; (i.e. number, string, keyword, symbol, character, etc)
(s/defrecord LeafNode
  ; stored is a simple (non-collection) value
  [parent :- (s/maybe HID)
   -content :- s/Any]
  IParentable
    (parent-hid [this] (s/validate (s/maybe HID) parent))
  IDataNode
    (content [this]
      (t/validate #(not (coll? %)) -content))
    (edn [this]
      (t/validate #(not (coll? %)) -content)))

(def DataNode
  "The Plumatic Schema type name for a MapNode ArrayNode LeafNode."
  (s/cond-pre MapNode MapEntryNode ArrayNode ArrayEntryNode SetNode LeafNode))

;-----------------------------------------------------------------------------
(def ^:dynamic ^:no-doc *tdb* nil)

(defmacro with-tdb ; #todo swap names?
  [tdb-arg & forms]
  `(binding [*tdb* (atom ~tdb-arg)]
     ~@forms))

(defn new-tdb
  "Returns a new, empty db."
  []
  {:idx-hid (sorted-map)
   :idx-leaf (tdi/->sorted-set-avl)
   :idx-me (tdi/->sorted-set-avl) })

(s/defn hid->node :- DataNode
  "Returns the node corresponding to an HID"
  [hid :- HID]
  (grab hid (grab :idx-hid (deref *tdb*))))

(s/defn set-node! :- HID
  "Unconditionally sets the value of a node in the tdb"
  ([hid :- HID
    node :- DataNode]
    (swap! *tdb* assoc-in [:idx-hid hid] node)
    hid))

; #todo => tupelo.core
(s/defn mapentry->kv :- tsk/Pair ; #todo need test
  [mapentry :- tsk/MapEntry]
  [(key mapentry) (val mapentry)])

; #todo => tupelo.core
(s/defn solomap->kv :- tsk/Pair ; #todo need test
  [solo-map :- tsk/Map]
  (let [map-seq (seq solo-map)
        >>      (when-not #(= 1 (count map-seq))
                  (throw (ex-info "solo-map must be of length=1 " (t/vals->map solo-map))))]
    (mapentry->kv (only map-seq))))

(do ; keep these two in sync
  (s/defn leaf-val? :- s/Bool
    "Returns true iff a value is of leaf type (number, string, keyword)"
    [arg :- s/Any] (or (number? arg) (string? arg) (keyword? arg)))
  (def LeafType (s/cond-pre s/Num s/Str s/Keyword))) ; instant, uuid, Time ID (TID) (as strings?)

(comment  ; unused since added cc-cmp
  (s/defn ^:no-doc type-short
    [arg]
    (cond
      (number? arg) "num"
      (string? arg) "str"
      (keyword? arg) "kw"
      ;(symbol? arg) "sym"  ; #todo allow this?
      :else (throw (ex-info "invalid type found" (t/vals->map arg)))))

  (s/defn ^:no-doc val->idx-type-kw
    [leaf-val :- LeafType]
    (keyword (str "idx-" (type-short leaf-val))))

  (s/defn ^:no-doc mapentry->idx-type-kw
    [me :- tsk/MapEntry]
    (let [me-key-type (type-short (key me))
          me-val-type (type-short (val me))]
      (keyword (str "me-" me-key-type \- me-val-type))))
)

(s/defn update-index-leaf!
  [leaf-val :- LeafType
   hid-val :- HID]
  (swap! *tdb* (fn [tdb-map]
                 (update tdb-map :idx-leaf ; #todo make verify like fetch-in
                   (fn [index-avl-set]
                     (conj index-avl-set [leaf-val hid-val])))))
  nil)

(s/defn update-index-mapentry!
  [me-val :- LeafType
   me-key
   me-hid :- HID]
  (swap! *tdb* (fn [tdb-map]
                 (update tdb-map :idx-me ; #todo make verify like fetch-in
                   (fn [index-avl-set]
                     (conj index-avl-set [me-val me-key me-hid])))))
  nil)

(def ^:no-doc hid-count-base 1000)
(def ^:no-doc hid-counter (atom hid-count-base))

(defn ^:no-doc hid-count-reset
  "Reset the hid-count to its initial value"
  [] (reset! hid-counter hid-count-base))

(s/defn ^:no-doc new-hid :- HID
  "Returns the next integer HID"
  [] (swap! hid-counter inc))

(s/defn hid? :- s/Bool
  "Returns true iff the arg type is a legal HID value"
  [arg] (int? arg))

(s/defn array-like? :- s/Bool
  "Returns true for vectors, lists, and seq's."
  [arg] (or (vector? arg) (list? arg) (seq? arg)))

(s/defn add-edn :- HID ; #todo maybe rename:  load-edn->hid  ???
  ([edn-val :- s/Any] (add-edn nil edn-val))
  ([hid-parent :- (s/maybe HID)
    edn-val :- s/Any]
   (cond
     (map? edn-val) (let [hid-map-node (new-hid)]
                      (set-node! hid-map-node
                        (->MapNode hid-parent
                          (apply glue
                            (forv [[k v] edn-val]
                              (let [hid-me-node (new-hid)
                                    hid-leaf    (add-edn hid-me-node v)]
                                (when (leaf-val? v)
                                  (update-index-mapentry! v k hid-me-node))
                                (set-node! hid-me-node (->MapEntryNode hid-map-node k hid-leaf))
                                (map-entry k hid-me-node))))))
                      hid-map-node)

     (array-like? edn-val) (let [hid-array-node (new-hid)]
                             (set-node! hid-array-node
                               (->ArrayNode hid-parent
                                 (apply glue
                                   (forv [[idx elem] (indexed edn-val)]
                                     (let [hid-ae-node (new-hid)
                                           hid-leaf    (add-edn hid-ae-node elem)]
                                       (set-node! hid-ae-node (->ArrayEntryNode hid-array-node idx hid-leaf))
                                       (map-entry idx hid-ae-node))))))
                             hid-array-node)

     ;(set? edn-val) (let [hid-mapnode (new-hid)]
     ;                 (set-node! hid-this
     ;                   (->SetNode
     ;                     hid-parent
     ;                     (set (forv [elem edn-val]
     ;                            (add-edn hid-this elem)))))
     ;                 hid-mapnode)

     (leaf-val? edn-val) (let [hid-leafnode (new-hid)]
                           (update-index-leaf! edn-val hid-leafnode)
                           (set-node! hid-leafnode (->LeafNode hid-parent edn-val))
                           hid-leafnode)

     :else (throw (ex-info "unknown value found" (vals->map edn-val))))))

(s/defn hid->edn :- s/Any
  "Returns EDN data for the subtree rooted at hid"
  [hid :- HID]
  (edn (hid->node hid)))

(s/defn hid-nav :- s/Any
  [hid :- HID
   path :- tsk/Vec]
  (let [node       (hid->node hid)
        key        (t/xfirst path)
        path-rest  (t/xrest path)
        nav-result (nav node key)]
    (if (empty? path-rest)
      nav-result
      (if (hid? nav-result)
        (hid-nav nav-result path-rest)
        (forv [hid nav-result]
          (hid-nav hid path-rest))))))

(s/defn hid->parent-hid :- (s/maybe HID)
  "Returns the parent HID of the node at this HID"
  [hid :- HID]
  (parent-hid (hid->node hid)))

(s/defn ^:private ^:no-doc index-find-val-impl ; #todo inline below
  [idx-id :- IndexId
   target :- tsk/Vec]
  (let [idx-avl-set      (t/validate set? (grab idx-id (deref *tdb*)))
        matching-entries (grab :matches
                           (tdi/split-key-prefix target idx-avl-set))]
    matching-entries))

(s/defn index-find-val
  [target :- LeafType]
  (let [idx-id      (cond
                      (number? target) :idx-num
                      (string? target) :idx-str
                      (keyword? target) :idx-kw
                      :else (throw (ex-info "invalid index target" (vals->map target))))
        idx-entries (index-find-val-impl idx-id [target])
        hids        (mapv t/xsecond idx-entries)]
    hids))

(s/defn index-find-mapentry
  [tgt-me :- tsk/MapEntry]
  (let [  ; me-type-kw       (mapentry->idx-type-kw tgt-me)
        [me-key me-val] (mapentry->kv tgt-me)
        tgt-prefix       [me-val me-key]
        idx-avl-set      (t/validate set? (fetch-in (deref *tdb*) [:idx-me]))
        matching-entries (grab :matches
                           (tdi/split-key-prefix tgt-prefix idx-avl-set))
        hids             (mapv last matching-entries)]
    hids)
  )

(s/defn index-find-submap
  [target-submap :- tsk/KeyMap]
  (let [map-hids (apply set/intersection
                   (t/forv [tgt-me target-submap]
                     (set (index-find-mapentry tgt-me))))]
    map-hids))
















