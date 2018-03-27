(ns erdos.redis-mapper
  (:require [taoensso.carmine :as car]))

(def ^:dynamic *redis-config*)

(defn set-redis-config! [x] (alter-var-root #'*redis-config* (constantly x)))
(defn clear-redis-config! [] (alter-var-root #'*redis-config* (constantly nil)))

(defn ->id [x]
  (cond
    (string? x) x
    (integer? x) x
    (map? x)     (-> x meta :id)
    :else
    (assert false "No id for obj")))

(defmulti validate! (comp :table meta))

(defmacro wcar* [& body]
  `(car/wcar *redis-config* ~@body))

(defmacro path-by-id [table id]
  `(let [id# ~id]
     (assert (or (string? id#) (integer? id#)))
     (str (name ~table) "/id:" id#)))

(defmacro path-by-idx [table k v]
  `(str (name ~table) "/" (name ~k) ":" ~v))

(def get-id (comp :id meta))
(def ^:private get-table (comp :table meta))

(defmulti get-indexes identity)

(defn persist!-unsafe [obj]
  (let [t       (get-table obj)
        indexes (get-indexes t)]
    (assert t "It is not an entity type!")
    (if-let [id (get-id obj)]
      ;; existing object has id
      (let [access    (path-by-id t id)
            [old ok?] (wcar* (car/get access) (car/set access obj))]
        (assert (= "OK" ok?))
        (wcar* (doseq [idx indexes
                       :let [old-val (old idx)
                             new-val (obj idx)]
                       :when (not= old-val new-val)]
                 (when (contains? old idx)
                   (car/srem (path-by-idx t idx old-val) id))
                 (when (contains? obj idx)
                   (car/sadd (path-by-idx t idx new-val) id))))
        ;; TODO: here should merge :original to meta.
        obj)
      ;; new object needs a new id
      (let [id  (wcar* (car/incr (str t "/meta:max_id")))
            obj (vary-meta obj assoc :id id)]
        (wcar* (car/set (path-by-id t id) obj)
               (doseq [idx indexes
                       :when (contains? obj idx)]
                 (car/sadd (path-by-idx t idx (obj idx)) id)))
        obj))))

(defn persist! [obj]
  (assert (map? obj))
  (assert (get-table obj))
  (persist!-unsafe (validate! obj)))

(defn- ->kw [x] (keyword (name x)))

(defn- emit-ctor [model-name opts]
  (let [ctor-unsafe (symbol (str "->" model-name "-unsafe"))
        ctor (symbol (str "->" model-name))
        ctor! (symbol (str "->" model-name "!"))]
    `(do
       (defn ~ctor-unsafe [& args#]
         (let [obj# (if (and (= 1 (count args#))
                             (map? (first args#)))
                      (first args#)
                      (apply hash-map args#))
               m# {:table ~(->kw model-name)
                   :original obj#}]
           (with-meta obj# m#)))
       (defn ~ctor [& args#]
         (validate! (apply ~ctor-unsafe args#)))
       (defn ~ctor! [& args#]
         (persist! (apply ~ctor args#))))))

(defn- emit-get [model-name opts]
  (let [t                (name model-name)
        get-fn-sym       (symbol (str "get-" (.toLowerCase t)))
        get-first-fn-sym (symbol (str "get-first-" (.toLowerCase t)))]
    `(do (defn ~get-fn-sym
           ([id#] (wcar* (car/get (path-by-id ~t id#))))
           ([idx# val#]
            (assert (contains? ~(set (:indices opts)) idx#))
            (let [ks# (wcar* (car/smembers (path-by-idx ~t idx# val#)))]
              ;; TODO:imeplementalhatjuk ezt a SINTER operatorral is
              (wcar* :as-pipeline
                     (doall (for [k# ks#]
                              (car/get (path-by-id ~t k#))))))))
         (defn ~get-first-fn-sym
           ([a# b#] (first (~get-fn-sym a# b#)))))))

(defn emit-indexes [model-name opts]
  `(defmethod get-indexes ~(->kw model-name) [_#] ~(:indices opts)))

(defn- emit-validate [model-name opts]
  (let [arg (gensym "argument")]
    `(defmethod validate! ~(->kw model-name) [~arg]
       ;; #_ ~(when-let [spec (:spec opts)] `(s/assert ~spec ~arg))
       ~(if-let [v (:validator opts)]
          (list v arg)
          arg))))


;; todo: set history object on a per object basis
(defn revert [obj]
  (assert (map? obj))
  (assert (get-table obj))
  (assert (-> obj meta :original))
  (with-meta (-> obj meta :original)
    {:table    (get-table obj)
     :id       (get-id obj)
     :original (-> obj meta :original)}))

(defmacro defmodel [model-name & {:as opts}]
  (assert (symbol? model-name))
  (assert (map? opts))
  `(do ~(emit-validate model-name opts)
       ~(emit-ctor model-name opts)
       ~(emit-indexes model-name opts)
       ~(emit-get model-name opts)
       true))
