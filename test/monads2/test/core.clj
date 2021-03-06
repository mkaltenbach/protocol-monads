(ns monads2.test.core
  (:use [clojure.test])
  (:require [monads2.core :as m]))

(defn list-f [n]
  (list (inc n)))

(defn list-g [n]
  (list (+ n 5)))

(deftest first-law-list
  (is (= (m/bind (list 10) list-f)
         (list-f 10))))

(deftest second-law-list
  (is (= (m/bind '(10) list)
         '(10))))

(deftest third-law-list
  (is (= (m/bind (m/bind [4 9] list-f) list-g)
         (m/bind [4 9] (fn [x]
                         (m/bind (list-f x) list-g))))))

(deftest zero-law-list
  (is (= (m/bind '() list-f)
         '()))
  (is (= (m/bind '(4) (constantly '()))
         '()))
  (is (= (m/plus [(list 5 6) '()])
         (list 5 6)))
  (is (= (m/plus ['() (list 5 6)])
         (list 5 6))))


(defn vector-f [n]
  (vector (inc n)))

(defn vector-g [n]
  (vector (+ n 5)))

(deftest first-law-vector
  (is (= (m/bind [10] vector-f)
         (vector-f 10))))

(deftest second-law-vector
  (is (= (m/bind [10] vector)
         [10])))

(deftest third-law-vector
  (is (= (m/bind (m/bind [4 9] vector-f) vector-g)
         (m/bind [4 9] (fn [x]
                         (m/bind (vector-f x) vector-g))))))

(deftest zero-law-vector
  (is (= (m/bind [] vector-f)
         []))
  (is (= (m/bind '(4) (constantly []))
         []))
  (is (= (m/plus [(vector 5 6) []])
         (vector 5 6)))
  (is (= (m/plus [[] (vector 5 6)])
         (vector 5 6))))


(defn set-f [n]
  (hash-set (inc n)))

(defn set-g [n]
  (hash-set (+ n 5)))

(deftest first-law-set
  (is (= (m/bind #{10} set-f)
         (set-f 10))))

(deftest second-law-set
  (is (= (m/bind #{10} hash-set)
         #{10})))

(deftest third-law-set
  (is (= (m/bind (m/bind #{4 9} set-f) set-g)
         (m/bind #{4 9} (fn [x]
                          (m/bind (set-f x) set-g))))))

(deftest zero-law-set
  (is (= (m/bind #{} set-f)
         #{}))
  (is (= (m/bind #{4} (constantly #{}))
         #{}))
  (is (= (m/plus [(hash-set 5 6) #{}])
         (hash-set 5 6)))
  (is (= (m/plus [#{} (hash-set 5 6)])
         (hash-set 5 6))))


(def test-writer (m/writer #{}))

(defn writer-f [n]
  (test-writer (inc n)))

(defn writer-g [n]
  (test-writer (+ n 5)))

(deftest first-law-writer
  (is (= (deref (m/bind (test-writer 10) writer-f))
         (deref (writer-f 10)))))

(deftest second-law-writer
  (is (= (deref (m/bind (test-writer 10) test-writer))
         (deref (test-writer 10)))))

(deftest third-law-writer
  (is (= (deref (m/bind (m/bind (test-writer 3) writer-f) writer-g))
         (deref (m/bind (test-writer 3)
                        (fn [x]
                          (m/bind (writer-f x) writer-g)))))))

(deftest test-write
  (is (= [nil #{:written}]
         (deref (m/write test-writer :written)))))

(deftest test-listen
  (is (= [[nil #{:written}] #{:written}]
         (->> (m/write test-writer :written)
              m/listen
              deref))))

(deftest test-censor
  (is (= [nil #{:new-written}]
         (->> (m/write test-writer :written)
              (m/censor (constantly #{:new-written}))
              deref))))


(defn state-f [n]
  (m/state (inc n)))

(defn state-g [n]
  (m/state (+ n 5)))

(deftest first-law-state
  (let [mv1 (m/bind (m/state 10) state-f)
        mv2 (state-f 10)]
    (is (= (mv1 {}) (mv2 {})))))

(deftest second-law-state
  (let [mv1 (m/bind (m/state 10) m/state)
        mv2 (m/state 10)]
    (is (= (mv1 :state) (mv2 :state)))))

(deftest third-law-state
  (let [mv1 (m/bind (m/bind (m/state 4) state-f) state-g)
        mv2 (m/bind (m/state 4)
                    (fn [x]
                      (m/bind (state-f x) state-g)))]
    (is (= (mv1 :state) (mv2 :state)))))

(deftest test-update-state
  (is (= [:state :new-state]
         ((m/update-state (constantly :new-state)) :state))))

(deftest test-update-val
  (is (= [5 {:a 19}]
         ((m/update-val :a + 14) {:a 5}))))


(defn cont-f [n]
  (m/cont (inc n)))

(defn cont-g [n]
  (m/cont (+ n 5)))

(deftest first-law-cont
  (let [mv1 (m/bind (m/cont 10) cont-f)
        mv2 (cont-f 10)]
    (is (= (mv1 identity) (mv2 identity)))))

(deftest second-law-cont
  (let [mv1 (m/bind (m/cont 10) m/cont)
        mv2 (m/cont 10)]
    (is (= (mv1 identity) (mv2 identity)))))

(deftest third-law-cont
  (let [mv1 (m/bind (m/bind (m/cont 4) cont-f) cont-g)
        mv2 (m/bind (m/cont 4)
                    (fn [x]
                      (m/bind (cont-f x) cont-g)))]
    (is (= (mv1 identity) (mv2 identity)))))

(deftest deref-cont
  (is (= 10 @(m/cont 10))))


(def vect-state (m/state-t vector))
(defn state-t-f [n]
  (vect-state (inc n)))

(defn state-t-g [n]
  (vect-state (+ n 5)))

(deftest first-law-state-t
  (let [mv1 (m/bind (vect-state 10) state-t-f)
        mv2 (state-t-f 10)]
    (is (= (mv1 {}) (mv2 {})))))

(deftest second-law-state-t
  (let [mv1 (m/bind (vect-state 10) vect-state)
        mv2 (vect-state 10)]
    (is (= (mv1 :state-t) (mv2 :state-t)))))

(deftest third-law-state-t
  (let [mv1 (m/bind (m/bind (vect-state 4) state-t-f) state-t-g)
        mv2 (m/bind (vect-state 4)
                    (fn [x]
                      (m/bind (state-t-f x) state-t-g)))]
    (is (= (mv1 :state-t) (mv2 :state-t)))))

(deftest zero-law-state-t
    (is (= (m/bind '() state-t-f)
           '()))
    (is (= (m/bind '(4) (constantly '()))
           '()))
    (is (= (m/plus [(list 5 6) '()])
           (list 5 6)))
    (is (= (m/plus ['() (list 5 6)])
           (list 5 6))))


(defn maybe-f [n]
  (m/maybe (inc n)))

(defn maybe-g [n]
  (m/maybe (+ n 5)))

(deftest first-law-maybe
  (is (= @(m/bind (m/maybe 10) maybe-f)
         @(maybe-f 10))))

(deftest second-law-maybe
  (is (= @(m/bind (m/maybe 10) m/maybe)
         10)))

(deftest third-law-maybe
  (is (= @(m/bind (m/bind (m/maybe 5) maybe-f) maybe-g)
         @(m/bind (m/maybe 5) (fn [x]
                                (m/bind (maybe-f x) maybe-g))))))

(deftest zero-law-maybe
  (is (= (m/bind (m/zero (m/maybe nil)) maybe-f)
         (m/zero (m/maybe nil))))
  (is (= (m/bind (m/maybe 4) (constantly (m/zero (m/maybe nil))))
         (m/zero (m/maybe nil))))
  (is (= @(m/plus [(m/maybe 6) (m/zero (m/maybe nil))])
         @(m/maybe 6)))
  (is (= @(m/plus [(m/zero (m/maybe nil)) (m/maybe 6)])
         @(m/maybe 6))))


(deftest test-seq
  (is (= [[3 :a] [3 :b] [5 :a] [5 :b]]
         (m/seq [[3 5] [:a :b]])))
  (is (= [[]]
         (m/seq vector []))))

(deftest test-lift
  (let [lifted-+ (m/lift +)]
   (is (= [6]
         (apply lifted-+ (map vector (range 4)))))
   (is (= [6 :state]
          ((apply lifted-+ (map m/state (range 4))) :state)))))

(deftest test-chain
  (let [t (fn [x] (vector (inc x) (* 2 x)))
        u (fn [x] (vector (dec x)))
        st (fn [x] (m/state (inc x)))
        su (fn [x] (m/state (* 2 x)))]
    (is (= (map (fn [x] (m/do [y (t x) z (u y)] z)) (range 4))
           (map (m/chain [t u]) (range 4))))
    (is (= (m/do [x (range 4) y (t x) z (u y)] z)
           ((m/chain [range t u]) 4)))
    (is (= ((m/do [x (st 8) y (su x)] y) :state)
           (((m/chain [st su]) 8) :state)))))

#_(prn :do ((m/do
             [x (m/state 29)
              y (m/state 12)
              :let [z (inc x)]]
             [x y z])
            :state))
