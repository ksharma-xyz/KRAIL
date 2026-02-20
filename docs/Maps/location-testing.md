# Map Location — Testing Guide

What can be tested today, what requires MapLibre, and the specific cases for each layer.

---

## What is and isn't testable

| Layer | Testable today? | Reason |
|---|---|---|
| `CameraFollowState` logic | ✅ Yes | Pure Kotlin state + coroutines, no map surface needed |
| `UserLocationConfig` constants | ✅ Yes | Trivial; guards against accidental regressions |
| `TrackUserLocation` permission branches | ✅ Yes | Inject fake `UserLocationManager`, assert callbacks |
| `TrackUserLocation` location flow | ✅ Yes | Fake flow, assert `onLocationUpdate` calls |
| `calculateInitialCameraPosition` | ✅ Yes | Pure function, no Compose needed |
| `LatLngToVector` converter | ✅ Yes | Pure math, round-trip check |
| ViewModel / event handling | ✅ Yes | Standard Molecule/StateFlow tests |
| `Animatable` dot interpolation | ⚠️ Partial | Can assert `snapTo` vs `animateTo` calls; final `.value` needs `TestScope` with `advanceTimeBy` |
| `CameraState.animateTo` | ❌ Hard | Requires live MapLibre surface |
| `UserLocationLayer` rendering | ❌ Hard | MapLibre GeoJSON layer, no observable output in unit tests |
| Manual pan detection (`manualPan` flow) | ❌ Hard | Needs real `CameraState` to mutate `.position.target` |

---

## Test module placement

```
core/maps/ui/src/commonTest/   → CameraFollowState, LatLngToVector
core/maps/state/src/commonTest/ → UserLocationConfig
feature/trip-planner/ui/src/commonTest/ → TrackUserLocation, calculateInitialCameraPosition, Animatable dot
feature/trip-planner/ui/src/commonTest/ → ViewModel tests (already exists)
```

---

## `CameraFollowState` — unit tests

All tests use a fake `CameraState` (or mock) and a `TestScope`.

```
isFollowing starts false
startFollowing() sets isFollowing = true
stopFollowing() sets isFollowing = false
animateTo() sets isProgrammaticMove = true during animation, false after
animateTo() resets isProgrammaticMove = false even if coroutine cancelled (finally block)
animateTo() with null zoom uses cameraState.position.zoom (preserves current zoom)
animateTo() with explicit zoom passes that zoom to CameraPosition
```

---

## `UserLocationConfig` — constant regression tests

```
UPDATE_INTERVAL_MS == 5_000L
AUTO_CENTER_ZOOM == 15.0
RECENTER_ZOOM == 16.0
AUTO_CENTER_ANIMATION_MS == 1_500L
RECENTER_ANIMATION_MS == 1_000L
FOLLOW_ANIMATION_MS == 800L
FOLLOW_ANIMATION_MS < UPDATE_INTERVAL_MS  (animation always finishes before next fix)
```

---

## `TrackUserLocation` — permission branch tests

Use a fake `UserLocationManager` with a controllable `checkPermissionStatus()` return and a `locationUpdates()` that returns `emptyFlow()` or a synthetic flow.

```
status=Denied, allowPermissionRequest=false  → onPermissionDeny called, no location updates
status=Denied, allowPermissionRequest=true   → onPermissionDeny called, no location updates
status=Granted, allowPermissionRequest=false → tracking starts immediately, onLocationUpdate called
status=Granted, allowPermissionRequest=true  → tracking starts, onLocationUpdate called
status=NotDetermined, allowPermissionRequest=false → no tracking, no callback
status=NotDetermined, allowPermissionRequest=true  → locationUpdates() called (triggers dialog)
locationUpdates() throws → catch fires, re-checks status, calls onPermissionDeny if Denied
```

---

## `TrackUserLocation` — camera behavior tests

Inject a fake `CameraFollowState` (or a real one with a fake `CameraState`) and a synthetic location flow.

```
autoCenter=true, first fix → cameraFollowState.animateTo called with AUTO_CENTER_ZOOM
autoCenter=true, second fix, isFollowing=false → no camera call (no auto-center, no follow)
autoCenter=true, second fix, isFollowing=true  → cameraFollowState.animateTo called (follow)
autoCenter=false, first fix, isFollowing=false → no camera call
autoCenter=false, first fix, isFollowing=true  → cameraFollowState.animateTo called (follow)
```

---

## `LatLngToVector` — round-trip test

```
LatLng(-33.8727, 151.2057) → AnimationVector2D → back to LatLng within Float precision (~0.00001°)
LatLng(0.0, 0.0) round-trips correctly
LatLng(-90.0, -180.0) and LatLng(90.0, 180.0) round-trip correctly (boundary values)
```

---

## Animated dot — `Animatable` behavior tests

Use `runTest` with `TestCoroutineScheduler` / `advanceTimeBy`.

```
First location fix → snapTo called, animatedUserLocation.value == first fix position immediately
Second fix arrives → animateTo called with FOLLOW_ANIMATION_MS duration
Mid-animation value is between old and new position (interpolation is live)
Third fix arrives before animation ends → previous animation cancelled, new one starts (MutatorMutex)
hasReceivedFirstFix = false before any fix → UserLocationLayer receives null (dot hidden)
hasReceivedFirstFix = true after first fix → UserLocationLayer receives animated position
```

---

## `calculateInitialCameraPosition` — pure function tests

```
mapState with ORIGIN stop → camera target == origin stop lat/lng
mapState with no ORIGIN but cameraFocus bounds → camera target == calculated center of bounds
mapState with no ORIGIN and no cameraFocus → camera target == MapConfig.DefaultPosition
zoom with cameraFocus → zoom == calculateZoomLevel(bounds)
zoom without cameraFocus → zoom == MapConfig.DefaultPosition.ZOOM
```

---

## What to do with MapLibre-dependent behavior

These are the scenarios that can't be unit-tested today:

- **`manualPan` flow** — integration test idea: write an Espresso/XCUITest that drags the map after tapping the location button and asserts the follow button visually becomes inactive.
- **Camera animation** — screenshot/snapshot test: run the follow animation, take a snapshot at t=400ms, assert the camera is between old and new position.
- **`UserLocationLayer` rendering** — screenshot test: inject a known `LatLng`, take a snapshot, assert the dot is visible at roughly the expected screen position.

These are best deferred to a dedicated instrumented/UI test module per platform when the map test infrastructure is set up.
