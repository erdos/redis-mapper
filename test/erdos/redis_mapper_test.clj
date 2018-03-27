(ns erdos.redis-mapper-test
  (:require [erdos.redis-mapper :refer [defmodel ->id persist!]]
            [clojure.test :refer [deftest testing is are use-fixtures]]))

(defn redis-fixture [f]
  (let [redis (doto (new redis.embedded.RedisServer) (.start))]
    (try (f) (finally (.stop redis)))))

(use-fixtures :once #'redis-fixture)

(defmodel user
  :indices [:user-uuid :user-name]
  :validator #(do (assert (contains? % :user-name)) %))

(def user-1 {:user-name "Jose" :age 23})

(deftest test-entity-creation
  (testing "Simple creation"
    (is (thrown? AssertionError (->user {:asd 323})))
    (is (= user-1 (->user user-1)))
    (is (nil? (->id (->user user-1)))))

  (testing "Creation and persistence"
    (is (thrown? AssertionError (->user! {:asd 323})))
    (is (= user-1 (->user! user-1)) "Seems unchanged...")
    (is (some? (->id (->user! user-1))))))

(deftest test-simple-lifecycle
  (testing "Creation and lookup"
    (let [saved-user (persist! (->user user-1))]
      (is (= user-1 (get-user (->id saved-user))))
      (persist! (update saved-user :age inc))
      (is (= 24 (:age (get-user (->id saved-user))))))))

(deftest persist!-test
  (testing "Illegal values to persist"
    (is (thrown? AssertionError (persist! nil)))
    (is (thrown? AssertionError (persist! [{} {} {}]))))
  (testing "This map has unknown type"
    (is (thrown? AssertionError (persist! {:asdf "324"}))))
  (testing "persist call does not change id"
    (let [original-user (->user! {:user-name "John" :age 1})
          changed-user  (persist! (assoc original-user :age 2))]
      (is (not= original-user changed-user))
      (is (= (->id original-user) (->id changed-user))))))

(deftest test-indices
  (testing "Can lookup with index"
    (let [test-user (->user! {:user-name "John" :user-uuid 13})]
      (is (= test-user (get-first-user :user-uuid 13)))
      (is (= [test-user] (get-user :user-uuid 13)))
      (let [test-user (persist! (update test-user :user-uuid inc))]
        (is (empty? (get-first-user :user-uuid 13)) "A regi index alatt toroltuk!")
        (is (empty? (get-user :user-uuid 13)) "A regi index alatt toroltuk!")
        (is (= test-user (get-first-user :user-uuid 14)) "Az uj index alatt elerheto!"))))

  (testing "Can not lookup without index"
    (is (thrown? AssertionError (get-user :nonexisting-index 34))))

  (testing "Can lookup null values with index"
    (let [test-user (->user! {:user-name "John" :user-uuid nil})]
      (is (contains? (set (get-user :user-uuid nil)) test-user))
      (let [test-user (persist! (assoc test-user :user-uuid "ALABAMA"))]
        (is (not (contains? (set (get-user :user-uuid nil)) test-user)))
        (is (= test-user (get-first-user :user-uuid "ALABAMA")))
        (let [test-user (persist! (dissoc test-user :user-uuid))]
          (is (not (contains? (set (get-user :user-uuid "ALABAMA")) test-user)))
          (is (not (contains? (set (get-user :user-uuid nil)) test-user)))))))

  (testing "Can not lookup missing values with index"
    (let [test-user (->user! :user-name "Malac")]
      (is (not (contains? (set (get-user :user-uuid nil)) test-user))))
    (let [test-user (->user! :user-name "Maki" :user-uuid nil)]
      (is (contains? (set (get-user :user-uuid nil)) test-user)))))

;; (->id (->user! user-1))

; (test-entity-creation)

; (redis-fixture test-entity-creation)
