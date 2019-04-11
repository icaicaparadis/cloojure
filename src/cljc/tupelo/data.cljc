;   Copyright (c) Alan Thompson. All rights reserved.
;   The use and distribution terms for this software are covered by the Eclipse Public License 1.0
;   (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html at
;   the root of this distribution.  By using this software in any fashion, you are agreeing to be
;   bound by the terms of this license.  You must not remove this notice, or any other, from this
;   software.
(ns tupelo.data
  (:refer-clojure :exclude [load ->VecNode])
  #?(:clj (:require
            [tupelo.core :as t :refer [spy spyx spyxx spyx-pretty grab]]
            [tupelo.schema :as tsk]
            [clojure.data.avl :as avl]
            [schema.core :as s]
            ))
  #?(:cljs (:require
             [tupelo.core :as t :refer [spy spyx spyxx spyx-pretty grab] ] ; #todo :include-macros true
             [tupelo.schema :as tsk]
             [clojure.data.avl :as avl]
             [schema.core :as s]
             ))

  )

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
(def ^:dynamic ^:no-doc *tdb* nil)

(def HID
  "The Plumatic Schema type name for a pointer to a tdb node (abbrev. for Hex ID)"
  s/Int)

(s/defn hid->node ; :- Node
  "Returns the node corresponding to an HID"
  [hid :- HID]
  (t/grab hid (deref *tdb*)))

(defprotocol IDataNode
  (parent [this])
  (content [this])
  (edn [this]))
(defprotocol INavNode
  (nav [this key]))

(s/defrecord MapNode ; Represents ths content of a Clojure map.
  ; a map from key to hid
  [ parent :- (s/maybe HID)
   node-val :- tsk/Map ]
  IDataNode
  (parent [this] (s/validate (s/maybe HID) parent))
  (content [this]
    (t/validate map? node-val))
  (edn [this]
    (apply t/glue
      (t/forv [[k v-hid] (t/validate map? node-val)]
        {k (edn (hid->node v-hid))})))
  INavNode
  (nav [this key]
    (t/grab key (t/validate map? node-val))))

; #todo need to enforce set uniqueness under mutation
(s/defrecord SetNode ; Represents ths content of a Clojure set
  ; a map from key to hid
  [parent :- (s/maybe HID)
   node-val :- tsk/Set]
  IDataNode
  (parent [this] (s/validate (s/maybe HID) parent))
  (content [this]
    (t/validate set? node-val))
  (edn [this]
    (let [result-vec (t/forv [v-hid (t/validate set? node-val)]
                       (edn (hid->node v-hid)))]
      (when-not (apply distinct? result-vec)
        (throw (ex-info "SetNode: non-distinct entries found!" (t/vals->map node-val result-vec))))
      (set result-vec)))
  INavNode
  (nav [this key]
    (t/grab key (t/validate set? node-val))))

(s/defrecord VecNode ; Represents ths content of a Clojure vector (any sequential type coerced into a vector).
  ; stored is a vector of hids
  [ parent :- (s/maybe HID)
   node-val :- tsk/Vec ]
  IDataNode
  (parent [this] (s/validate (s/maybe HID) parent))
  (content [this]
    (t/validate vector? node-val))
  (edn [this]
    (t/forv [elem-hid (t/validate vector? node-val)]
      (edn (hid->node elem-hid))))
  INavNode
  (nav [this key]
    (if (= :* key)
      (content this)
      (nth (t/validate vector? node-val) key))))

; Represents a Clojure primitive (non-collection) type,
; (i.e. number, string, keyword, symbol, character, etc)
(s/defrecord LeafNode
  ; stored is a simple (non-collection) value
  [parent :- (s/maybe HID)
   node-val :- s/Any]
  IDataNode
  (parent [this] (s/validate (s/maybe HID) parent))
  (content [this]
    (t/validate #(not (coll? %)) node-val))
  (edn [this]
    (t/validate #(not (coll? %)) node-val)) )

(def DataNode
  "The Plumatic Schema type name for a MapNode VecNode LeafNode."
  (s/cond-pre MapNode SetNode VecNode LeafNode ))

(def HidRootSpec
  "The Plumatic Schema type name for the values accepted as starting points (roots) for a subtree path search."
  (s/conditional ; #todo why is this here?
    int? HID
    set? #{HID}
    :else [HID]))

(def ^:no-doc hid-count-base 1000)
(def ^:no-doc hid-counter (atom hid-count-base))

(defn ^:no-doc hid-count-reset
  "Reset the hid-count to its initial value"
  [] (reset! hid-counter hid-count-base))

(s/defn ^:no-doc new-hid :- HID
  "Returns the next integer HID"
  [] (swap! hid-counter inc))

(s/defn hid? :- s/Bool
  "Returns true if the arg type is a legal HID value"
  [arg] (int? arg))

; HID & :hid are shorthand for Hash ID, the SHA-1 hash of a v1/UUID expressed as a hexadecimal keyword
; format { :hid Node }
(defn new-tdb
  "Returns a new, empty db."
  []
  (sorted-map))

(defmacro with-tdb ; #todo swap names?
  [tdb-arg & forms]
  `(binding [*tdb* (atom ~tdb-arg)]
     ~@forms))

(s/defn set-node :- HID
  "Unconditionally sets the value of a node in the tdb"
  ([hid :- HID
    node  :- DataNode ]
    (swap! *tdb* t/glue {hid node})
    hid))

(s/defn load-edn :- HID ; #todo maybe rename:  load-edn->hid  ???
  ([edn-val :- s/Any]
    (load-edn nil edn-val))
  ([hid-parent :- (s/maybe HID)
    edn-val :- s/Any]
    (let [hid-creating (new-hid)]
      (cond
        (map? edn-val) (set-node hid-creating
                         (->MapNode
                           hid-parent
                           (apply t/glue
                             (t/forv [[k v] edn-val]
                               {k (load-edn hid-creating v)}))))

        (set? edn-val) (set-node hid-creating
                         (->SetNode
                           hid-parent
                           (set (t/forv [elem edn-val]
                                  (load-edn hid-creating elem)))))

        (or (set? edn-val) ; coerce sets to vectors
          (sequential? edn-val)) (set-node hid-creating
                                   (->VecNode
                                     hid-parent
                                     (t/forv [elem edn-val]
                                       (load-edn hid-creating elem))))

        (not (coll? edn-val)) (set-node hid-creating
                                (->LeafNode hid-parent edn-val))

        :else (throw (ex-info "unknown value found" (t/vals->map edn-val)))))))

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
        (t/forv [hid nav-result]
          (hid-nav hid path-rest))))))

(s/defn hid->parent :- (s/maybe HID)
  "Returns the parent HID of the node at this HID"
  [hid :- HID]
  (parent (hid->node hid)))

; #todo add indexes
; #todo add sets (primative only or HID) => map with same key/value
; #todo copy destruct syntax for search































