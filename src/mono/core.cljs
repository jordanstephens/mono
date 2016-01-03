(ns ^:figwheel-always mono.core
    (:require [reagent.core :as reagent :refer [atom]]
              [mono.controller :as controller]))

(enable-console-print!)



(defn app []
  [:div {:className "keyboard-container"}
    [controller/control-keys]
    [controller/keyboard]])

(.addEventListener js/document "keydown" controller/handle-keydown)
(.addEventListener js/document "keyup" controller/handle-keyup)
(.addEventListener js/document "mouseup" controller/handle-doc-mouseup)



(reagent/render-component [app]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (.removeEventListener js/document "keydown" controller/handle-keydown)
  (.removeEventListener js/document "keyup" controller/handle-keyup)
  (.removeEventListener js/document "mouseup" controller/handle-doc-mouseup))
