# sw

A tiny lib with just a sliding-window [reactor](https://github.com/reactor) (if I can really name it like this) built on top of [meltdown](https://github.com/clojurewerkz/meltdown)

## Usage

    ; Create an ordinary reactor to serve as the event input for the window
    (def tickets (mr/create))

    ; Create the actuall window
    (def last-5-secs (sliding-window tickets (match-all) 5000))

    ; Start pumping data to the reactor
    (mr/notify tickets "shirt" 20)

    ; And register any handler as you would do with a reactor
    ; Notice this handler is called on every tick beat, not on 
    ; every notification on tickets reactor
    (on-tick last-5-secs 
        (fn [b]
          (dosync
          (println "Last 5 minutes we sold: " (apply + b)))))

    (mr/notify tickets "water" 1.25)
    (mr/notify tickets ...)

## Motivation

To see more details, please read [this post](http://paulosuzart.github.io/blog/2014/04/27/sliding-window-events-with-clojure/). But in summary I wanted a very simple time based sliding mechanism to create a simple dashboard. If one already uses meltdow, it is simple to add this reactor without having to deal with bigger/complex libs or tools.

## License

Copyright © 2014 Paulo Suzart

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## WARNING

No battle tested. Use by your own.