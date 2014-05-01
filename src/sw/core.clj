(ns sw.core
  (:require [clojurewerkz.meltdown.streams :as ms :refer [create consume accept reduce*]]
            [clojurewerkz.meltdown.stream-graph :as msg]
            [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer [$ match-all]]
            [clojure.core.reducers :as r])
  (:import [java.util Timer TimerTask]))

(defn ticker
  "Produces a default ticker that tickers timestamp every 1s"
  []
  (let [ticker-reactor (mr/create :event-routing-strategy :broadcast)
        timer          (Timer. "sw-1s-ticker" true)
        timer-task     (proxy [TimerTask] []
                         (run []
                           (mr/notify ticker-reactor
                                      "tick"
                                      {:time (System/currentTimeMillis)})))]
    (.scheduleAtFixedRate timer timer-task 0 1)
    ticker-reactor))

(defn buffer-cleaner 
  "Cleans the window buffer. Keeps just relevant data for the window interval."
  [buff interval]
  (let [event-time (System/currentTimeMillis)
        slide (- event-time interval)]
     (into [] (r/filter #(> (:event-time %) slide) buff))))

; Define Sliding operations. Where on-tick is used to invoke a given handler passing
; the valid buffer as argument on every ticker hit. 
(defprotocol SlideP
    (on-tick [this handler])
    (start [this]))

; Implements the sliding window itself;
; Uses a external ticker-reactor to control the steps of the window.
; tap reactor is the real source of events.
; size is how many miliseconds an entry should be kept available on buffer
(deftype SlidingWindow [ticker-reactor ticker-selector tap-reactor tap-selector buffer size]
    SlideP
    (on-tick [this handler]
      (mr/on ticker-reactor ticker-selector
        (fn [e] 
          (handler (map :event-data @buffer)))))

    (start [this]
      (mr/on tap-reactor tap-selector
        (fn [e] 
          (dosync
            (alter buffer conj {:event-data (:data e) :event-time (System/currentTimeMillis)}))))

      (mr/on ticker-reactor ticker-selector
        (fn [_] 
          (dosync 
            (ref-set buffer (buffer-cleaner @buffer size)))))))

(defn sliding-window
  "Produces a new sliding window. If not provided, a default ticker ticking on every 1s 
  is used."
  ([ticker-reactor ticker-selector tap-reactor tap-selector size]
    (let [window (new SlidingWindow ticker-reactor ticker-selector tap-reactor tap-selector (ref []) size)]
      (start window)
      window))
  ([tap-reactor tap-selector size]
    (sliding-window 
        (ticker)
        (match-all)
        tap-reactor tap-selector size)))
