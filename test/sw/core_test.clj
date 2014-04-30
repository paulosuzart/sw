(ns sw.core-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.meltdown.streams :as ms :refer [create consume accept reduce*]]
            [clojurewerkz.meltdown.stream-graph :as msg]
            [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer [$ match-all]]
            [sw.core :refer :all]))

(deftest a-test
  (testing "Simple 5 sec window"
    (let [tickets (mr/create)
        last-5-secs (sliding-window tickets (match-all) 5000)
        last-sum (ref 0)]
      (on-tick last-5-secs 
        (fn [b]
          (dosync
          (ref-set last-sum (apply + b)))))
      (is (= 0 @last-sum))
      (mr/notify tickets "camisa" 20)
      (Thread/sleep 2000)
      (is (= 20 @last-sum))
      (mr/notify tickets "chapa" 30)
      (Thread/sleep 2000)
      (is (= 50 @last-sum))
      (Thread/sleep 3000)
      (mr/notify tickets "tenis" 20)
      (Thread/sleep 2000)
      (is (= 20 @last-sum)))))
