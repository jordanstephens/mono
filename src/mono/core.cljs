(ns ^:figwheel-always mono.core
    (:require [reagent.core :as reagent :refer [atom]]
              [mono.synth :as synth]))

(enable-console-print!)

(def key-width 64)
(def key-height 64)

; use an array-map to maintain key order
(def key-map (array-map
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
  188 {:label "," :x 7}
))

(defonce keys-down (atom #{}))
(defonce reference-frequency-idx 9)

(defn index [list x]
  (.indexOf (to-array list) x))

(defn key-code-frequency [key-code]
  (let [key-codes (keys key-map)
        idx (+ 40 (index key-codes key-code))]
    (* 440.0 (Math/pow 2.0 (/ (- idx 49) 12.0)))))

(defn with-valid-key-code [event f]
  (let [key-code (.-keyCode event)]
    (if (contains? key-map key-code)
      (do
        (.preventDefault event)
        (f key-code)))))

(defn handle-keydown [event]
  (with-valid-key-code event (fn [key-code]
    (do
      (swap! keys-down conj key-code)
      (synth/note-on (key-code-frequency key-code))))))

(defn handle-keyup [event]
  (with-valid-key-code event (fn [key-code]
    (do
      (swap! keys-down disj key-code)
      (if (empty? @keys-down)
          (synth/note-off)
          (synth/note-on (key-code-frequency (first @keys-down))))))))

(defn keyboard []
  [:ol {:className "keyboard"}
    (doall (map-indexed
      (fn [idx [key-code {label :label x :x y :y}]]
        [:li {:key idx :data-key-down (contains? @keys-down key-code)}
          [:span label]])
      key-map))])

(defn app []
  [:div {:className "keyboard-container"} [keyboard]])

(.addEventListener js/document "keydown" handle-keydown)
(.addEventListener js/document "keyup" handle-keyup)

(reagent/render-component [app]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (.removeEventListener js/document "keydown" handle-keydown)
  (.removeEventListener js/document "keyup" handle-keyup)
)
