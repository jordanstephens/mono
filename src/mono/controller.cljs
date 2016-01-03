(ns mono.controller
  (:require [reagent.core :as reagent :refer [atom]]
            [mono.synth :as synth]))

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

(defonce key-map
  (merge keyboard-map control-key-map))

; app state
(defonce keyboard-keys-down (atom '()))
(defonce control-keys-down (atom '()))
(defonce octave-offset (atom 0))
(defonce mousedown (atom false))



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



(defn key-code [event]
  (.-keyCode event))


(defn valid-key? [key-code]
  (contains? key-map key-code))

(defn with-key-code-in-map [key-code map f]
  (if (contains? map key-code)
    (f key-code)))

(defn with-keyboard-code [key-code f]
  (with-key-code-in-map key-code keyboard-map f))

(defn with-control-code [key-code f]
  (with-key-code-in-map key-code control-key-map f))

(defn octave-up []
  (if (< @octave-offset octave-offset-maximum)
    (reset! octave-offset (+ @octave-offset 1))))

(defn octave-down []
  (if (> @octave-offset octave-offset-minimum)
    (reset! octave-offset (- @octave-offset 1))))

(defn handle-control-keypress [key-code]
  (case key-code
    81 (synth/change-waveform)
    219 (octave-down)
    221 (octave-up)))

(defn key-is-down? [key-code set]
  (boolean (some #{key-code} set)))

(defn keyboard-key-is-down? [key-code]
  (key-is-down? key-code @keyboard-keys-down))

(defn control-key-is-down? [key-code]
  (key-is-down? key-code @control-keys-down))

(defn record-keydown [set key-code]
  (swap! set (fn [xs x] (cons x xs)) key-code))

(defn record-keyup [set key-code]
  (swap! set (fn [xs x] (remove #(= % x) xs)) key-code))

(defn keyboard-keydown [key-code]
  (do
    (record-keydown keyboard-keys-down key-code)
    (synth/note-on (key-code-frequency key-code))))

(defn keyboard-keyup [key-code]
  (do
    (record-keyup keyboard-keys-down key-code)
    (if (empty? @keyboard-keys-down)
        (synth/note-off)
        (synth/note-on (key-code-frequency (first @keyboard-keys-down))))))

(defn keydown [key-code]
  (do
    (with-keyboard-code key-code
      (fn [key-code]
        (keyboard-keydown key-code)))
    (with-control-code key-code
      (fn [key-code]
        (do
          (handle-control-keypress key-code)
          (record-keydown control-keys-down key-code))))))

(defn keyup [key-code]
  (do
    (with-keyboard-code key-code
      (fn [key-code]
        (keyboard-keyup key-code)))
    (with-control-code key-code
      (fn [key-code]
        (record-keyup control-keys-down key-code)))))



(defn handle-keydown [event]
  (let [key-code (key-code event)]
    (if (valid-key? key-code)
      (do
        (.preventDefault event)
        (keydown key-code)))))

(defn handle-keyup [event]
  (keyup (key-code event)))

(defn handle-key-mousedown [key-code]
  (do
    (reset! mousedown true)
    (keydown key-code)))

(defn handle-key-mouseup [key-code]
  (do
    (reset! mousedown false)
    (keyup key-code)))

(defn handle-key-mouseenter [key-code]
  (if @mousedown
    (keydown key-code)))

(defn handle-key-mouseleave [key-code]
  (keyup key-code))

(defn handle-doc-mouseup []
  (do
    (reset! mousedown false)
    (synth/note-off)))

(defn octave-indicator-light? [active-values]
  (if (contains? active-values @octave-offset)
    "true"
    "false"))



(defn octave-offset-light [active-values]
  [:li {:data-light (octave-indicator-light? active-values)} "•"])

(defn waveform-control-keys []
  [:div {:className "control-group waveform"}
    [:div {:className "control-label"}
      [:ul {:className "waveform-value"}
        [:li (:label (synth/current-waveform))]]]
    [:ol {:className "control-keys"}
      [:li {:key 0 :data-key-down (control-key-is-down? 81)}
        [:button
          [:div "q"]]]]])

(defn octave-control-keys []
  [:div {:className "control-group octave"}
    [:div {:className "control-label"}
      [:ul {:className "octave-offset-value"}
        (octave-offset-light #{-2 -1})
        (octave-offset-light #{-1 0 1})
        (octave-offset-light #{1 2})]]

    [:ol {:className "control-keys"}
      (doall
        (map-indexed
          (fn [idx [key-code {label :label}]]
            [:li {:key idx :data-key-down (control-key-is-down? key-code)}
              [:button
                [:div label]]])
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
          [:li {:key idx :data-key-down (keyboard-key-is-down? key-code)}
            [:button {:onMouseDown  (.bind handle-key-mousedown nil key-code)
                      :onMouseUp    (.bind handle-key-mouseup nil key-code)
                      :onMouseEnter (.bind handle-key-mouseenter nil key-code)
                      :onMouseLeave (.bind handle-key-mouseleave nil key-code)}
              [:div label]]])
        keyboard-map))])
