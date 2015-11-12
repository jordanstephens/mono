(ns mono.synth)

(defonce context (js/AudioContext.))

(defn defosc [context]
  (let [osc (.createOscillator context)]
    (do (.start osc 0)
        (set! (.-type osc) "triangle")
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
  (let [now (.-currentTime context)
        osc-frequency (.-frequency osc1)
        osc-gain (.-gain osc1-gain)]
    (do
      (.setValueAtTime osc-frequency frequency now)
      (.setValueAtTime osc-gain 1.0 now))))

(defn note-off []
  (.setValueAtTime (.-gain osc1-gain) 0.0 (.-currentTime context)))
