(ns ^:figwheel-always mono.core
    (:require [reagent.core :as reagent :refer [atom]]
              [mono.synth :as synth]))

(enable-console-print!)

(defonce reference-frequency 440.0)
(defonce reference-frequency-key-idx 9)
(defonce octave-offset-minimum -2)
(defonce octave-offset-maximum 2)

; use an array-map to maintain key order
(defonce keyboard-map
  (array-map
    90  {:label "z" :x 0}
    83  {:label "s" :x 0.5}
    88  {:label "x" :x 1}
    68  {:label "d" :x 1.5}
    67  {:label "c" :x 2}
    86  {:label "v" :x 3}
    71  {:label "g" :x 3.5}
    66  {:label "b" :x 4}
    72  {:label "h" :x 4.5}
    78  {:label "n" :x 5}
    74  {:label "j" :x 5.5}
    77  {:label "m" :x 6}
    188 {:label "," :x 7}))

(defonce octave-key-map
  (array-map
    219 {:label "["}
    221 {:label "]"}))

(defonce waveform-key-map
  (array-map
    81 {:label "q"}))

(defonce control-key-map
  (merge octave-key-map waveform-key-map))

; app state
(defonce keyboard-keys-down (atom #{}))
(defonce control-keys-down (atom #{}))
(defonce octave-offset (atom 0))
(defonce waveform-index (atom 0))



(defn index [list x]
  (.indexOf (to-array list) x))

; twelfth root of 2
; https://en.wikipedia.org/wiki/Twelfth_root_of_two
(defn reference-frequency-multiplicand [idx]
  (Math/pow 2.0 (/ idx 12.0)))

(defn key-code-frequency [key-code]
  (let [key-codes (keys keyboard-map)
        key-idx (- (index key-codes key-code) reference-frequency-key-idx)
        frequency-idx (+ (* (/ @octave-offset 1.0) 12.0) key-idx)]
    (* reference-frequency (reference-frequency-multiplicand frequency-idx))))

(defn with-key-code-in-map [event map f]
  (let [key-code (.-keyCode event)]
    (if (contains? map key-code)
      (do
        (.preventDefault event)
        (f key-code)))))

(defn with-keyboard-code [event f]
  (with-key-code-in-map event keyboard-map f))

(defn with-control-code [event f]
  (with-key-code-in-map event control-key-map f))

(defn change-waveform []
  (reset! waveform-index (mod (+ @waveform-index 1) 3)))

(defn current-waveform []
  (get synth/waveforms @waveform-index))

(defn octave-up []
  (if (< @octave-offset octave-offset-maximum)
    (reset! octave-offset (+ @octave-offset 1))))

(defn octave-down []
  (if (> @octave-offset octave-offset-minimum)
    (reset! octave-offset (- @octave-offset 1))))

(defn map-control-key [key-code]
  (case key-code
    81 (change-waveform)
    219 (octave-down)
    221 (octave-up)))

(defn handle-keydown [event]
  (do
    (with-keyboard-code event
      (fn [key-code]
        (do
          (swap! keyboard-keys-down conj key-code)
          (synth/note-on (key-code-frequency key-code) (:name (current-waveform))))))
    (with-control-code event
      (fn [key-code]
        (do
          (map-control-key key-code)
          (swap! control-keys-down conj key-code))))))

(defn handle-keyup [event]
  (do
    (with-keyboard-code event
      (fn [key-code]
        (do
          (swap! keyboard-keys-down disj key-code)
          (if (empty? @keyboard-keys-down)
              (synth/note-off)
              (synth/note-on (key-code-frequency (first @keyboard-keys-down)) (:name (current-waveform)))))))
    (with-control-code event
      (fn [key-code]
        (do
          (swap! control-keys-down disj key-code))))))

(defn waveform-control-keys []
  [:div {:className "control-group waveform"}
    [:div {:className "control-label"}
      [:ul {:className "waveform-value"}
        [:li (:label (current-waveform))]]]
    [:ol {:className "control-keys"}
      [:li {:key 0 :data-key-down (contains? @control-keys-down 81)}
        [:span "q"]]]])

(defn octave-control-keys []
  [:div {:className "control-group octave"}
    [:div {:className "control-label"}
      [:ul {:className "octave-offset-value"}
        [:li {:data-light (if (contains? #{-2 -1} @octave-offset) "true" "false")}"•"]
        [:li {:data-light (if (contains? #{-1 0 1} @octave-offset) "true" "false")}"•"]
        [:li {:data-light (if (contains? #{1 2} @octave-offset) "true" "false")}"•"]]]

    [:ol {:className "control-keys"}
      (doall
        (map-indexed
          (fn [idx [key-code {label :label}]]
            [:li {:key idx :data-key-down (contains? @control-keys-down key-code)}
              [:span label]])
          octave-key-map))]])

(defn control-keys []
  [:div {:className "control-keys-container"}
    [waveform-control-keys]
    [octave-control-keys]])

(defn keyboard []
  [:ol {:className "keyboard"}
    (doall
      (map-indexed
        (fn [idx [key-code {label :label}]]
          [:li {:key idx :data-key-down (contains? @keyboard-keys-down key-code)}
            [:span label]])
        keyboard-map))])

(defn app []
  [:div {:className "keyboard-container"}
    [control-keys]
    [keyboard]])



(.addEventListener js/document "keydown" handle-keydown)
(.addEventListener js/document "keyup" handle-keyup)



(reagent/render-component [app]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (.removeEventListener js/document "keydown" handle-keydown)
  (.removeEventListener js/document "keyup" handle-keyup))
