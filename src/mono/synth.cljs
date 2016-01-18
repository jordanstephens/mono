(ns mono.synth)

(defonce context
  (if
    (exists? js/AudioContext.)
    (js/AudioContext.)
    (if (exists? (js/webkitAudioContext.))
      (js/webkitAudioContext.)
      (js/alert "Error: The Web Audio API is not support in this browser."))))

; https://paulbakaus.com/tutorials/html5/web-audio-on-ios/
(defonce unlocked (atom false))
(defonce max-gain (atom 1.0))
(defonce waveform-index (atom 0))

(defonce waveforms
  [{:name "sine" :label "∿"}
   {:name "triangle" :label "⋀"}
   {:name "square" :label "⊓"}])

(defn defosc [context]
  (.createOscillator context))

(defn defgain [context]
  (let [gain (.createGain context)]
    (do (set! (.-value (.-gain gain)) 0)
        gain)))



(defonce osc1 (defosc context))
(defonce osc1-gain (defgain context))

(.connect osc1 osc1-gain)
(.connect osc1-gain (.-destination context))



(defn current-waveform []
  (get waveforms @waveform-index))

(defn change-waveform []
  (let [new-waveform-index (mod (+ @waveform-index 1) 3)
        new-waveform (:name (get waveforms new-waveform-index))]
    (do
      (reset! waveform-index new-waveform-index)
      (set! (.-type osc1) new-waveform))))

(defn unlock []
  (do
    (.start osc1 0)
    (reset! unlocked true)))

(defn note-on [frequency]
  (let [now (.-currentTime context)
        osc-frequency (.-frequency osc1)
        osc-gain (.-gain osc1-gain)]
    (do
      (if (= false @unlocked) (unlock))
      (.setValueAtTime osc-frequency frequency now)
      (.setValueAtTime osc-gain @max-gain now))))

(defn note-off []
  (.setValueAtTime (.-gain osc1-gain) 0.0 (.-currentTime context)))
