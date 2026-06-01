# MapLibre on iOS (Compose Multiplatform) — Pitfalls & Patterns

Hard-won lessons from the dual-pane map work. Every bug below was **iOS-only** —
Android rendered fine throughout — because MapLibre's map on iOS is a native
`UIKitView` (`MLNMapView`) bridged into Compose, and UIKitView interop has rules
Compose-on-Android doesn't.

**If a map is blank, mis-centred, or zoomed wrong on iOS but fine on Android, start here.**

> Scope: `feature/trip-planner/ui` maps — `searchstop/map/SearchStopMap.kt`
> (the nearby-stops `MapContent`, shared by SavedTrips / SearchStop / TimeTable
> dual-pane right panes and portrait "select on map") and
> `journeymap/JourneyMap.kt` (the TimeTable route map). Layout via
> `core/adaptive-ui/.../DualPaneScaffold.kt`.

---

## Golden rules (the short version)

1. **Never nest a map under an offscreen-compositing ancestor.** No
   `CloudGradientBackground` (or any `graphicsLayer { compositingStrategy = Offscreen }`)
   above a `MaplibreMap`. Put the gradient inside the *list* pane only; keep the map a
   **sibling**.
2. **Never create a `MaplibreMap` at a 0-size frame.** Gate creation behind a
   non-zero width **and** height, and **latch** that gate (never tear the map down on a
   transient 0 during rotation).
3. **Dual-pane = fixed-width list + weighted map**, never a flexible `widthIn` sibling
   next to the map's `weight(1f)`. Use `DualPaneScaffold`.
4. **Auto-center camera: key the effect on a stable boolean** (`hasUserLocation`), never
   on the churning `LatLng`. And use **one** camera effect, not two that race.
5. **Location-or-default:** centre on the user when a fix exists; fall back to the Sydney
   default *only* when location permission is absent.

---

## 1. Blank map — nested under `CloudGradientBackground`

**Symptom:** right-pane map blank on iOS; the identical composable rendered fine in
another screen. Blank persisted across rotation. Pane size + window position were
*identical* to the working screen, so it wasn't layout.

**Cause:** `CloudGradientBackground` composites its whole subtree offscreen:

```kotlin
.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
.drawBehind { /* blobs */ }
```

A `UIKitView` (native `MLNMapView`) **cannot composite into an offscreen GPU buffer on
iOS** → invisible. Android has no such restriction. SearchStop had wrapped the *entire*
dual-pane split (list **and** map) in the gradient; SavedTrips kept its map a sibling,
which is why SavedTrips always worked. It was a regression from when the map loaded.

**Fix:** gradient wraps **only the list pane**; the map is a **sibling** under the Row.

**Diagnosis tip:** if `onSizeChanged` + `onGloballyPositioned` show the pane has identical
size **and** window position to a working screen yet it's still blank — stop chasing
layout. Look at ancestor compositing (`graphicsLayer` / offscreen / `drawBehind`).

---

## 2. Blank / zoomed-to-world — map created at a 0×0 frame

**Symptom:** map blank, or (TimeTable) zoomed all the way out to the whole of Australia.

**Logs (the fingerprint):**
```
🟢 Map will start loading
CAMetalLayer ignoring invalid setDrawableSize width=0.000000 height=0.000000
[CAMetalLayer nextDrawable] returning nil because allocation failed.
🟢 frame = (0 0; 0 0)
```

**Cause:** on a dual-pane / rotation transient the container is briefly measured at a 0
axis (the adaptive window flips through `840×0dp` / `0×480dp`). If `MaplibreMap` is
instantiated then, the native `MLNMapView` is created at `(0 0; 0 0)`; its Metal layer
fails to allocate a drawable and the camera projects against a dead viewport → blank or
degenerate zoom.

**Fix:** gate creation until the container reports non-zero **width AND height**:

```kotlin
var hasHadValidSize by remember { mutableStateOf(false) }
Box(
    modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { if (it.width > 0 && it.height > 0) hasHadValidSize = true },
) {
    if (hasHadValidSize) {
        MaplibreMap(/* … */)
    }
}
```

---

## 3. Blank on FIRST rotate, map only on SECOND rotate — gate not latched

**Symptom:** intermittent. Rotate → blank right pane; rotate again → map appears.
Sometimes happened, sometimes not (timing-dependent).

**Cause:** a naïve gate that reset to `false` on every 0 size. Rotation emits a burst of
size callbacks: `…832×828` → transient `0×1792` → `…832×828`. If the last callback
before a frame drew was a 0, the gate closed, the native `MLNMapView` was **torn down**,
and recreated — blank until the next rotation re-opened it. Whether the 0 landed on a
draw boundary was a race → "sometimes."

**Fix:** **latch** the flag — set `hasHadValidSize = true` once and never reset it (see
the snippet in §2). Once mounted, the map stays mounted; MapLibre resizes its own
surface as the container settles. Removes the race entirely.

---

## 4. Dual-pane layout — fixed-width list, not flexible

**Symptom:** map blank / native frame never settling on iOS in dual-pane.

**Cause:** a flexible `widthIn(min, max)` list pane sitting next to the map's `weight(1f)`
gives the iOS UIKitView interop unstable, multi-pass width constraints, so the native
`MLNMapView` frame never settles.

**Fix:** **fixed-width** list pane (`Modifier.width(...)`), map takes the remainder via
`weight(1f)`. Encoded in `DualPaneScaffold` so screens can't diverge:

- list pane = fixed `DUAL_PANE_LIST_WIDTH`,
- right pane = `weight(1f)`, always a **sibling** of the list (never a descendant — see §1).

Both SavedTrips and SearchStop now delegate their split to `DualPaneScaffold`. Use it for
any new dual-pane screen.

---

## 5. Camera stuck at Sydney default until rotate — effect keyed on the wrong thing

**Symptom:** fresh load showed the Sydney default even with location permission; rotating
the device then showed the correct user location. On iOS the camera sometimes froze fully
zoomed out.

**Cause:** the "centre on user once" `LaunchedEffect` was keyed on the user-location
`LatLng`. GPS emits a stream of slightly-different fixes, so the effect re-launched on
every jitter and **cancelled the in-flight `animateTo`**. The once-guard had already
flipped true, so it never re-fired — the camera froze wherever the cancelled animation
stopped. Rotation only "fixed" it because `rememberCameraState(firstPosition = …)`
re-seeds at the now-known location (a different path).

**Fix:** key on a **stable boolean** that flips `false → true` once and stays true:

```kotlin
val hasUserLocation = userLocation != null
var hasAutoCentered by remember { mutableStateOf(false) }   // seed false, NOT (userLocation != null)
LaunchedEffect(hasUserLocation) {
    val loc = userLocation
    if (loc != null && !hasAutoCentered) {
        hasAutoCentered = true
        cameraState.animateTo(/* user location */)
    }
}
```

Seed the guard `false` — seeding it from `userLocation != null` lets a re-entry/rotation
re-seed it `true` while the camera is still at the default, suppressing the animation.

---

## 6. Empty map randomly Sydney vs user — two camera effects racing

**Symptom (TimeTable, no journey selected):** sometimes centred on Sydney, sometimes on
the user — non-deterministic, worse when the GPS fix was slow.

**Cause:** two `LaunchedEffect`s each called `cameraState.animateTo`:
- one keyed on `cameraFocus + stops` → read `userLocation` at fire time → null on fresh
  load → Sydney,
- one keyed on `hasUserLocation` → user.

Whichever ran last won.

**Fix:** **one** effect, single source of truth:

```kotlin
val hasUserLocation = userLocation != null
LaunchedEffect(mapState.cameraFocus, mapState.mapDisplay.stops, hasUserLocation) {
    val target = cameraTargetForState(mapState, userLocation) // journey wins; else user; else Sydney
    cameraState.animateTo(target, /* … */)
}
```

---

## 7. Content shoved down on iOS landscape — imePadding on the wrong node

**Symptom:** in dual-pane, list content pushed down by the keyboard height on iOS
landscape.

**Cause:** the search top bar carried `windowInsetsPadding(WindowInsets.ime)`
unconditionally. In single-pane it floats in a `Box` overlay (harmless); in dual-pane
it's the first child of the list `Column`, so ime padding inflated its height and shoved
the list down. Keyboard is at the bottom, top bar at the top — they never overlap, so the
padding was pointless there.

**Fix:** `SearchTopBar(applyImePadding = …)` — `false` in dual-pane. Also dropped
`imePadding()` from the dual-pane root and stopped auto-showing the keyboard in dual-pane.

---

## Debugging method that actually worked

The breakthrough was **comparison logging across a working screen and a broken one**, all
under one grep tag, logging the things that *could* differ:

- pane `onSizeChanged` (width × height),
- pane `onGloballyPositioned` (`positionInWindow` + size),
- the native `MLNMapView frame = (...)` lines MapLibre already prints,
- the `CAMetalLayer` / `nextDrawable` warnings.

When SavedTrips (works) and SearchStop (blank) logged **identical** size AND position yet
behaved differently, layout was ruled out and the offscreen-compositing ancestor became
the obvious suspect. Likewise the `frame = (0 0; 0 0)` + `CAMetalLayer width=0` lines
pinned the 0-size-creation bug, and the repeated `Deallocating MLNMapView` mid-rotation
pinned the un-latched gate.

Diagnostic logging is fine to add temporarily — strip it before merge.

---

## Checklist for any new map / dual-pane screen on iOS

- [ ] Map is **not** under `CloudGradientBackground` or any offscreen `graphicsLayer`.
- [ ] Map creation gated behind non-zero width **and** height, **latched**.
- [ ] Dual-pane via `DualPaneScaffold` (fixed-width list, sibling map).
- [ ] Camera auto-center keyed on a stable boolean; one effect, not two.
- [ ] No permission → Sydney default; fix present → user location.
- [ ] Tested on iOS: fresh load, repeated rotations, journey select/deselect — not just
      Android.
