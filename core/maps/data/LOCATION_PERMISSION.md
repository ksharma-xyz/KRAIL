# Location & Permission — Design Notes

## Module responsibilities

| Module | Responsibility |
|---|---|
| `core:permission:state` | `PermissionStatus`, `PermissionResult`, `AppPermission` — pure data types, no platform code |
| `core:permission:data` | `PermissionController` interface + Android/iOS implementations |
| `core:location:state` | `Location`, `LocationConfig`, `LocationError` — pure data types |
| `core:location:data` | `LocationTracker` interface + Android/iOS implementations |
| `core:maps:data` | `UserLocationManager` — **the only public API for features** |

Feature code should only import `UserLocationManager`. `PermissionController` and `LocationTracker` are implementation details.

---

## PermissionStatus values

```
NotDetermined  — permission never requested on this device
Granted        — permission granted, tracking can start
Denied         — permission denied; direct user to Settings (no re-request)
```

`Denied.Temporary` and `Denied.Permanent` do not exist — there is only one `Denied` state.
**Why:** Both platforms converge on the same ask-once policy:

- **iOS** — after the user denies the system dialog, `CLAuthorizationStatus` immediately becomes
  `kCLAuthorizationStatusDenied`. iOS never allows asking again in-app; the user must go to
  Settings. There is no "temporary" denial concept on iOS.

- **Android** — Android *does* distinguish first denial (`shouldShowRequestPermissionRationale = true`)
  from "Don't ask again" denial. However, this app uses an **ask-once policy** implemented in
  `AndroidPermissionController.checkPermissionStatus()`: once we have asked and the permission is
  not granted, the status is `Denied` regardless of Android's rationale flag. This matches iOS
  behaviour and avoids repeatedly prompting users.

---

## Permission request policy — when is the dialog shown?

**Rule: the system permission dialog is shown ONLY when the user explicitly taps the location button.**

`TrackUserLocation` (the composable effect) checks `checkPermissionStatus()` at the start of every
`LifecycleStartEffect`. Based on the result:

| Status at screen start | Behaviour |
|---|---|
| `Granted` | Starts location tracking immediately — no dialog |
| `Denied` | Shows the in-app permission banner — no dialog |
| `NotDetermined` and `allowPermissionRequest = false` | Does nothing — waits for button tap |
| `NotDetermined` and `allowPermissionRequest = true` | Calls `locationUpdates()` which triggers the system dialog |

`allowPermissionRequest` is a Compose state that starts `false` and flips to `true` only when
the user explicitly taps the location button. Changing the flag is the key for
`LifecycleStartEffect`, so the effect relaunches immediately while the screen is visible.

---

## Location button behaviour

| Condition | Action |
|---|---|
| Location dot is visible (tracking active) | Re-centers the camera on the current position |
| Status is `Denied` | Shows the in-app permission banner |
| Status is `NotDetermined` | Sets `allowPermissionRequest = true` → dialog shown |

---

## Scenario table

| Scenario | Behaviour |
|---|---|
| Map opens, permission never requested | `NotDetermined` → does nothing, no dialog |
| Map opens, permission already granted | `Granted` → starts tracking immediately |
| Map opens, permission denied | `Denied` → shows in-app banner, no dialog |
| User taps location button (not yet requested) | `allowPermissionRequest = true` → effect relaunches → system dialog shown |
| User taps button, denies, leaves and returns | On resume: `Denied` → banner shown, no re-request |
| User granted permission, then rotates device | `allowPermissionRequest` resets to `false`, but status is `Granted` → auto-resumes |
| User revokes in Settings, then returns | On resume: `Denied` → banner shown |

---

## Key design decisions

1. **No auto-request.** The app never shows the system permission dialog automatically. Proactively
   popping a dialog before user intent is known is poor UX and causes app store review issues.

2. **Ask-once policy.** After the first denial the user is directed to Settings. Asking multiple
   times trains users to dismiss permission dialogs without reading them.

3. **`shouldShowRequestPermissionRationale` is not used.** Android exposes this API to distinguish
   a first denial ("can ask again") from a "Don't ask again" denial. We deliberately ignore it:
   when the user taps the location button their intent is clear, so we show the system dialog
   directly. iOS also allows only one in-app request. There is no benefit to the extra complexity.

4. **No DataStore needed for permission state.** `PermissionStateTracker` is in-memory and resets
   on app restart. This is intentional — after a restart, if the user taps the location button
   again, Android either shows the dialog (soft denial) or returns denied silently ("Don't ask
   again"), both of which are handled correctly. Persisting to a database would add complexity
   with no meaningful UX improvement.

5. **`PermissionResult.isPermanent` removed.** The field existed to distinguish Android's
   "soft denial" from "never ask again". With the ask-once policy in place, the distinction is
   meaningless — both result in directing the user to Settings.

4. **`TrackUserLocation` always renders.** There is no outer `if (isTrackingEnabled)` gate;
   instead, the effect checks permission internally and exits early when appropriate. The single
   `allowPermissionRequest` flag carries user intent across recompositions.
