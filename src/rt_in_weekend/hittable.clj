(ns rt-in-weekend.hittable
  (:require [rt-in-weekend.vec :as vec]
            [rt-in-weekend.ray :as ray]))

(defprotocol Hittable
  (hit [this r t-min t-max]))

(defn hit-record [r t center radius material]
  (let [p (ray/point-at r t)
        outward-normal (vec// (vec/- p center) radius)
        front-face (< (vec/dot (:direction r) outward-normal) 0)]
    {:t t :p p :normal (if front-face outward-normal (vec/- outward-normal)) :material material :front-face front-face}))

(defrecord Sphere [center radius material]
  Hittable
  (hit [this r t-min t-max]
    (let [oc (vec/- (ray/origin r) (:center this))
          a (vec/length-squared (ray/direction r))
          half-b (vec/dot oc (ray/direction r))
          c (- (vec/length-squared oc) (* (:radius this) (:radius this)))
          discriminant (- (* half-b half-b) (* a c))]
      (when (pos? discriminant)
        (let [root (Math/sqrt discriminant)
              temp (/ (- (- half-b) root) a)]
          (if (and (< temp t-max) (> temp t-min))
            (hit-record r temp (:center this) (:radius this) (:material this))
            (let [temp (/ (+ (- half-b) root) a)]
              (when (and (< temp t-max) (> temp t-min))
                (hit-record r temp (:center this) (:radius this) (:material this))))))))))

; Moves linearly between center0 at time0 to center1 at time1. Outside that
; time interval it continues on, so those times need not match up with the
; camera aperture open and close.
(defrecord MovingSphere [center0 center1 time0 time1 radius material]
  Hittable
  (hit [this r t-min t-max]
    (let [center (fn [timestamp] (vec/+ center0
                                    (vec/* (vec/- center1 center0)
                                       (/ (- timestamp time0)
                                          (- time1 time0)))))
          oc (vec/- (ray/origin r) (center (:timestamp r)))
          a (vec/length-squared (ray/direction r))
          half-b (vec/dot oc (ray/direction r))
          c (- (vec/length-squared oc) (* (:radius this) (:radius this)))
          discriminant (- (* half-b half-b) (* a c))]
      (when (pos? discriminant)
        (let [root (Math/sqrt discriminant)
              temp (/ (- (- half-b) root) a)]
          (if (and (< temp t-max) (> temp t-min))
            (hit-record r temp (center (:timestamp r)) (:radius this) (:material this))
            (let [temp (/ (+ (- half-b) root) a)]
              (when (and (< temp t-max) (> temp t-min))
                (hit-record r temp (center (:timestamp r)) (:radius this) (:material this))))))))))

(defn hittable-list [world r t-min t-max]
  (let [closest-so-far (atom t-max)
        record (atom nil)]
    (doseq [i (range 0 (count world))]
      (do
        (if-let [rec (hit (get world i) r t-min @closest-so-far)]
          (do
            (reset! closest-so-far (:t rec))
            (reset! record rec)))))
    @record))
