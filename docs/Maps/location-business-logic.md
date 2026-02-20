# Map Location Business Logic

Covers every user-facing location scenario across `SearchStopMap` and `JourneyMap`.

---

## 1. First GPS fix — auto-center (SearchStopMap only)

`autoCenter = true` (default). `TrackUserLocation` tracks a local `hasAutoCentered` flag inside `LifecycleStartEffect`. On the very first fix it calls `cameraFollowState.animateTo(zoom = AUTO_CENTER_ZOOM, durationMs = 1500ms)` then never auto-centers again. Does **not** start follow mode — the camera stays fixed after that one pan, giving the user control. `JourneyMap` passes `autoCenter = false` because the camera is already positioned at the journey origin.

---

## 2. Returning from background — re-center to current location (SearchStopMap)

`LifecycleStartEffect` re-runs every `ON_START`. Its local `hasAutoCentered` variable resets to `false` each time. So when the user leaves the app (train ride), comes back, the first fresh GPS fix triggers another auto-center. The map jumps to wherever the user is now, not where they were when they left.

---

## 3. User taps location button — enter follow mode (both maps)

If `userLocation != null` (we already have a GPS fix): calls `cameraFollowState.startFollowing()` then `cameraFollowState.animateTo(zoom = RECENTER_ZOOM, durationMs = 1000ms)`. From this point every GPS update in `TrackUserLocation` sees `isFollowing = true` and calls `cameraFollowState.animateTo(durationMs = 800ms)`, keeping the camera locked on the user.
If `userLocation == null` (no fix yet): flips `allowPermissionRequest = true`, which re-triggers `LifecycleStartEffect` and starts the location flow (showing system permission dialog if needed).

---

## 4. User manually pans the map — disengage follow mode

`CameraFollowState.manualPan` is a `snapshotFlow` on `cameraState.position.target` filtered by `!isProgrammaticMove`. A `LaunchedEffect` in each map collects this flow and calls `cameraFollowState.stopFollowing()`. The `isProgrammaticMove` flag is set `true` for the entire duration of each `animateTo` call (guarded by `finally`) so our own camera animations do **not** trigger disengagement.

---

## 5. Walking / moving — camera tracks user (follow mode active)

Every GPS fix (every 5 000 ms) arrives in `TrackUserLocation.collect`. If `cameraFollowState.isFollowing` is true, calls `cameraFollowState.animateTo(latLng, durationMs = 800ms)` with no fixed zoom (preserves whatever zoom the user set). The 800 ms animation completes well before the next 5 s update, so animations never stack or overlap. Camera glides smoothly rather than jumping.

---

## 6. Location dot — smooth animation between GPS fixes (both maps)

`onLocationUpdate` launches a coroutine that calls `animatedUserLocation.snapTo(latLng)` on the first fix (avoids flying in from `(0, 0)`), then `animatedUserLocation.animateTo(latLng, LinearEasing, 800ms)` on every subsequent fix. `UserLocationLayer` / `JourneyMapLayers` receive `animatedUserLocation.value` (the interpolated position) not the raw GPS coordinate. The raw coordinate still goes to the ViewModel for button state and camera operations.

---

## 7. Location permission — denied (both maps)

`TrackUserLocation` calls `userLocationManager.checkPermissionStatus()`. If `Denied`, calls `onPermissionDeny(status)` which sets `showPermissionBanner = true`. A `LocationPermissionBanner` appears with "Go to Settings". No permission dialog is shown, no tracking starts. If the user grants permission in Settings and returns, `ON_START` re-fires, the status is now `Granted`, and tracking begins.

---

## 8. Location permission — not yet determined, user hasn't tapped button

`allowPermissionRequest` starts `false`. `TrackUserLocation` checks `!allowPermissionRequest && status !is Granted` → returns early silently. No dialog, no tracking. Waiting for explicit user intent (scenario 3).

---

## 9. Location permission — not yet determined, user taps button

`allowPermissionRequest` flips to `true` (the key for `LifecycleStartEffect`). Effect relaunches immediately, re-checks status, falls through to `userLocationManager.locationUpdates(...)` which triggers the system permission dialog. On grant → location flow starts. On deny → `catch` block re-checks status → `onPermissionDeny` → banner shown.

---

## 10. JourneyMap — location button, no follow by default

`JourneyMap` starts with `autoCenter = false` and `isFollowing = false`. The camera is at the journey origin. Tapping the location button (if a fix exists) calls `startFollowing()` + `animateTo(RECENTER_ZOOM)` — same as SearchStopMap. Without tapping the button, the dot moves on the map independently and the camera stays at the journey view.

---

## Key Constants (`UserLocationConfig`)

| Constant | Value | Purpose |
|---|---|---|
| `UPDATE_INTERVAL_MS` | 5 000 ms | GPS poll rate |
| `AUTO_CENTER_ZOOM` | 15.0 | Zoom on first fix |
| `RECENTER_ZOOM` | 16.0 | Zoom on button tap |
| `AUTO_CENTER_ANIMATION_MS` | 1 500 ms | First-fix camera animation |
| `RECENTER_ANIMATION_MS` | 1 000 ms | Button-tap camera animation |
| `FOLLOW_ANIMATION_MS` | 800 ms | Follow-mode camera + dot animation |
