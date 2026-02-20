# Permission Module

Handles runtime permission checks and requests for the KRAIL app via a shared `PermissionController`
interface with platform-specific implementations for Android and iOS.

---

## PermissionStatus states

```
NotDetermined  — permission not yet requested
Granted        — user has granted the permission
Denied         — user has denied the permission
```

---

## How each platform maps OS state to PermissionStatus

### iOS
Uses `CLLocationManager.authorizationStatus` which gives exact native states:

| OS value | PermissionStatus |
|---|---|
| `.notDetermined` | `NotDetermined` |
| `.authorizedWhenInUse` / `.authorizedAlways` | `Granted` |
| `.denied` / `.restricted` | `Denied` |

iOS remembers this across app restarts natively — no extra storage needed.

### Android
`checkSelfPermission()` only returns `GRANTED` or `DENIED`. It cannot distinguish between
"never asked" and "explicitly denied". The codebase resolves this with a **persistent**
flag stored in `SandookPreferences` that marks a permission as requested the moment the
OS dialog is launched. Status is derived as:

```
checkSelfPermission == GRANTED                    → Granted
checkSelfPermission == DENIED && wasRequested     → Denied
checkSelfPermission == DENIED && !wasRequested    → NotDetermined
```

The flag is stored in the `KrailPref` table via `SandookPreferences` and **survives app restarts**.

---

## Behaviour across app restarts

### iOS — always correct
The OS stores authorization state permanently. After any number of restarts:
- Never asked → `NotDetermined`
- Denied → `Denied`
- Granted → `Granted`

### Android — persistent across restarts

Because the `wasRequested` flag is stored in `SandookPreferences`, it survives process death.

| Scenario | Same session | After app restart |
|---|---|---|
| Never requested | `NotDetermined` | `NotDetermined` ✓ |
| Denied once (can ask again) | `Denied` | `Denied` ✓ |
| Permanently denied ("Don't ask again") | `Denied` | `Denied` ✓ |

**Q: If permanently denied, then the app is killed and reopened — will the denied banner show?**

Yes, immediately. `checkPermissionStatus` returns `Denied` on the first call after restart
because the persistent flag remembers that the permission was previously requested.

---

## Ask-once policy

Both platforms enforce a single in-app permission request:

- **iOS**: The OS itself only ever shows the dialog once. Subsequent calls go silently to denied.
- **Android**: `resolveExistingStatus()` checks `wasRequested` before launching the dialog —
  if already requested and denied, it returns `PermissionResult.Denied` immediately and skips
  the OS call, directing the user to Settings instead.

This means once the user denies (in any session), the app will never show the OS dialog
again. It will only show the in-app settings-redirect banner.

---

## Showing different UI for "never asked" vs "denied"

Currently the UI does **not** differentiate between these two states — both result in no banner
being shown until the user explicitly taps the location button. A future design could check
`wasPermissionRequested()` when `allowPermissionRequest` becomes `true` to distinguish a
first-ever request (show onboarding animation) from a re-request (show settings redirect).
This is only relevant on Android — iOS knows natively via `CLAuthorizationStatus.notDetermined`.
