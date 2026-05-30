# Tablet & Foldable UX — adaptive layout rules

This doc captures how each screen should behave on tablets, foldables, and
phones in landscape. The adaptive primitives already exist in
`core:adaptive-ui`; this file is the per-screen contract on top of them.

Read this **before** touching any of the screens listed below. Any change
that contradicts a rule here should update the doc in the same commit.

---

## 1. Breakpoint contract

Breakpoints live in
[`core/adaptive-ui/.../AdaptiveLayoutInfo.kt`](../core/adaptive-ui/src/commonMain/kotlin/xyz/ksharma/krail/core/adaptiveui/AdaptiveLayoutInfo.kt).
The gate for dual-pane layouts is `shouldShowDualPane` (true at
width ≥ 600 dp).

| Class      | Width range  | Typical device                                                  | Layout                                |
|------------|--------------|------------------------------------------------------------------|---------------------------------------|
| COMPACT    | < 600 dp     | phone portrait, foldable closed                                  | single-pane                           |
| MEDIUM     | 600–840 dp   | phone landscape, foldable unfolded portrait, small tablet portrait | **dual-pane** (tight ratio)         |
| EXPANDED+  | ≥ 840 dp     | tablet landscape, foldable unfolded landscape, desktop           | **dual-pane** (roomy ratio)           |

Width and height are decided independently. `isCompactHeight`
(height < 480 dp, i.e. phone landscape) drives separate adaptations on
screens whose vertical density matters (SavedTrips — see §4).

Posture-aware adaptation (hinge detection via
`currentWindowAdaptiveInfo().windowPosture`) is **out of scope** today.
Width breakpoints cover the common cases. Revisit if device telemetry
shows half-folded / table-top usage worth handling.

---

## 2. SearchStopScreen

Files:
- [`SearchStopEntry.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/navigation/entries/SearchStopEntry.kt)
- [`SearchStopScreen.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/searchstop/SearchStopScreen.kt)
  (`SearchStopScreenDualPane` around line 762)
- Sister doc: [`feature/trip-planner/ui/SEARCH_STOP_UX.md`](../feature/trip-planner/ui/SEARCH_STOP_UX.md)

### Rules

- `SearchStopEntry` declares **no `ListDetailSceneStrategy` metadata** —
  SearchStop renders full-width and owns its own list+map split.
  (The old `listPane()` declaration caused a double-split: scaffold halved
  the window, then SearchStop's internal dual-pane halved it again,
  leaving ~25 % each. That is fixed.)
- Dual-pane at ≥ 600 dp width (`shouldShowDualPane`, unchanged).
  COMPACT widths keep the existing single-pane list/map toggle.
- **Pane ratio** in dual-pane: list pane = `widthIn(min = 320.dp,
  max = 480.dp)`; map pane = `weight(1f)`. Stops list stays
  ≈ phone-width so `StopSearchListItem` rows remain readable; map gets
  whatever is left:

  | Available width | List | Map  |
  |-----------------|------|------|
  | 720 dp          | 360  | ~360 |
  | 840 dp          | 400  | ~440 |
  | 1280 dp         | 480  | ~800 |

  Replaces today's `weight(1f) / weight(1f)`.
- "Select on map" CTA stays hidden in dual-pane (map is always visible).
  Already enforced via `isMapsAvailable = false` passed to
  `SearchStopListContent`.
- Map and list selection stay synchronous (existing `onStopSelect`
  callback wired through `MapDisplay.selectedStop`).
- Label / save-sheet / edit-mode / conflict behaviour is unchanged —
  see [`SEARCH_STOP_UX.md`](../feature/trip-planner/ui/SEARCH_STOP_UX.md).

---

## 3. TimeTableScreen

Files:
- [`TimeTableScreen.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/timetable/TimeTableScreen.kt)
- [`JourneyCard.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/components/JourneyCard.kt)
  ("Maps" button lives in `ExpandedJourneyCardContent`, lines 268–279)
- [`JourneyMap.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/journeymap/JourneyMap.kt)

### Rules (≥ 600 dp width)

- Left pane: existing TimeTable journey list, capped at
  `widthIn(max = 520.dp)` so cards keep phone-width proportions.
- Right pane: a **persistent `JourneyMap`** instance that stays mounted
  at all times — no appear/disappear flicker as journeys expand/collapse.
- `JourneyCard`'s "Maps" button is **hidden** when `shouldShowDualPane`
  is true. COMPACT keeps the button + the existing navigation to a
  full-screen `JourneyMapScreen`.
- **With no journey expanded**: right pane shows an empty map
  (`JourneyMapDisplay()` — no route, no stops). Camera centres at user
  location if GPS is available, else Sydney default. The map animates
  smoothly to the route when the user expands a card (600 ms fly-to).
- **With a journey expanded**: map shows the route polyline, stops, and
  leg colours. Camera flies to the route bounds / origin stop.
- Phone landscape (`isCompactHeight` and width ≥ 600 dp): dual-pane
  layout still applies. Cards remain full-width; vertical scroll handles
  density.

---

## 4. SavedTripsScreen

Files:
- [`SavedTripsScreen.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/savedtrips/SavedTripsScreen.kt)
- [`SavedTripsEntry.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/navigation/entries/SavedTripsEntry.kt)
- [`MapStopSelectionPane.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/mapstopselection/MapStopSelectionPane.kt)

`SavedTripsEntry` has **no `ListDetailSceneStrategy` metadata** — it renders
full-width and manages its own left/right split internally (same pattern as
SearchStop). `TimeTableRoute` navigation still works via back-stack push.

### Width rules (≥ 600 dp)

- **Left pane** (≤ 480 dp): the saved-trips list, DepartureBoard, Park & Ride,
  Discover tiles, and the SearchStopRow pill at the bottom — same as
  portrait, just width-capped.
- **Right pane** (`weight(1f)`): a **read-only nearby-stops map** powered by
  `MapStopSelectionPane` (singleton `MapStopSelectionViewModel`). Users can
  explore nearby stops and view stop details; tapping a stop has no
  trip-planning side-effect. The right pane is purely contextual.
- Trip planning is done via the SearchStopRow pill at the bottom of the left
  pane (same flow as portrait — pill collapses/expands SearchStopRow).

### Compact-height rules (`isCompactHeight`, height < 480 dp)

Phone landscape uses the **same pill + expand-SearchStopRow** behaviour as
portrait. No special single-line collapsed bar is needed — the pill already
collapses the row to a single button, and expanding it fills the remaining
height acceptably. `showPill` logic is the same in both orientations
(collapses when ≥ 2 saved trips, or 1 trip + Park & Ride).

---

## 5. Navigation graph

File: [`KrailNavHost.kt`](../composeApp/src/commonMain/kotlin/xyz/ksharma/krail/KrailNavHost.kt)

`KrailNavHost` uses `ListDetailSceneStrategy` from Navigation 3.
Per-route `metadata` decides which pane each route lands in:

| Route               | Metadata              | Notes                                             |
|---------------------|-----------------------|---------------------------------------------------|
| `SavedTripsRoute`   | **none**              | owns its own left/right split internally          |
| `SearchStopRoute`   | **none**              | owns its own list+map split internally            |
| `TimeTableRoute`    | **none**              | owns its own list+map split internally            |
| `JourneyMapRoute`   | (none)                | phone-only; tablet uses JourneyMap inline in TimeTable |
| `SettingsRoute`     | `detailPane()`        | unchanged                                         |
| `IntroRoute`        | `detailPane()`        | unchanged                                         |
| `DiscoverRoute`     | `detailPane()`        | unchanged                                         |
| `ThemeSelectionRoute` | `detailPane()`      | unchanged                                         |
| `OurStoryRoute`     | `detailPane()`        | unchanged                                         |
| `TrackTripRoute`    | `detailPane()`        | unchanged                                         |

Screens themselves detect their dual-pane mode via
`rememberAdaptiveLayoutInfo()` / `AdaptiveScreenContent` — route
declarations don't change beyond the SearchStop fix.

---

## 6. First-launch experience

File: [`IntroScreen.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/intro/IntroScreen.kt)
(7-page carousel, gated on `SandookPreferences.KEY_HAS_SEEN_INTRO`).

- Intro carousel stays single-pane on every form factor. It's a
  one-shot onboarding flow; multi-pane layouts add noise.
- After onboarding, the first impression on tablet lands on SavedTrips
  with the hero treatment described in §4. No additional seeding
  needed — the empty hero + curated `EMPTY_STATE_STOPS` from
  `SEARCH_STOP_UX.md` already give a brand-new user a path forward.

---

## 7. Preview coverage

File: [`PreviewAnnotations.kt`](../taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/preview/PreviewAnnotations.kt)

Today's `@PreviewScreen` covers phones in light/dark + 2× font scale
plus one tablet portrait config. It does **not** cover:

- Tablet landscape (e.g. `spec:width=1280dp,height=800dp`).
- Phone landscape / compact-height.

When implementing the rules above, extend `@PreviewScreen` (or add
`@TabletScreenPreview` / `@CompactHeightPreview`) so every dual-pane and
compact-height variant is exercised in the IDE without each call-site
adding a custom `@Preview`.

---

## 8. Implementation status

Items 1–5 are **shipped**. Remaining work:

1. ~~Drop `listPane()` from `SearchStopEntry`.~~ ✓ Done
2. ~~Tighten SearchStop pane ratio (320–480 dp list, map fills rest).~~ ✓ Done
3. ~~Wire TimeTable dual-pane with persistent `JourneyMap`; hide "Maps" button.~~ ✓ Done
4. ~~Wire SavedTrips dual-pane right pane.~~ ✓ Done — read-only nearby-stops map
5. ~~SavedTrips compact-height SearchStopRow adaptation.~~ ✓ Done — same pill+expand as portrait
6. Add `@TabletScreenPreview` (landscape) and `@CompactHeightPreview`
   (phone landscape) annotations; backfill previews on the screens
   touched above.
7. **Stretch**: posture-aware adaptation via
   `currentWindowAdaptiveInfo().windowPosture` for half-folded
   foldables. Defer unless telemetry shows usage.
