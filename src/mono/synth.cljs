(ns mono.synth)

(defonce context (js/AudioContext.))

(defn defosc [context]
  (let [osc (.createOscillator context)]
    (do (.start osc 0)
        osc)))

(defn defgain [context]
  (let [gain (.createGain context)]
    (do (set! (.-value (.-gain gain)) 0)
        gain)))

(defonce osc1 (defosc context))
(defonce osc1-gain (defgain context))

(.connect osc1 osc1-gain)
(.connect osc1-gain (.-destination context))

(defn note-on [frequency]
  (do
    (set! (.-value (.-frequency osc1)) frequency)
    (set! (.-value (.-gain osc1-gain)) 1)))

(defn note-off []
  (set! (.-value (.-gain osc1-gain)) 0))
