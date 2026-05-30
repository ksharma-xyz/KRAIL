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

### Why the old layout looked "squished"

`SearchStopEntry` currently declares
`metadata = ListDetailSceneStrategy.listPane()`. At ≥ 600 dp the Material 3
`ListDetailSceneStrategy` confines SearchStop to roughly half the window
(the list pane), and SearchStop's own internal dual-pane then splits that
half again. Net result: ~25 % screen width for stops list, ~25 % for map,
the remainder eaten by surrounding scaffold. This is the bug to fix first.

### Rules

- **Drop `ListDetailSceneStrategy.listPane()` from `SearchStopEntry`.**
  SearchStop owns its own list+map split — the scene strategy must not
  split the window a second time. With this single change the screen
  occupies the full window at every form factor and the existing
  dual-pane code starts looking right.
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
- Map ↔ list selection stays synchronous (existing `onStopSelect`
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
- Right pane: a **persistent `JourneyMap`** instance keyed on
  `expandedJourneyId` from `TimeTableViewModel`. The map composable
  stays mounted; only its data layer (route polyline, leg stops, live
  vehicle) re-renders as the user expands different journeys.
- `JourneyCard`'s "Maps" button is **hidden** when `shouldShowDualPane`
  is true. COMPACT keeps the button + the existing navigation to a
  full-screen `JourneyMapScreen`.
- With no journey expanded, the right pane shows a trip-overview map:
  origin + destination markers, framed to fit both.
- `JourneyMap` is already extensible via its `extraMapContent` slot —
  no refactor needed, only new call-sites.
- Phone landscape (`isCompactHeight` and width ≥ 600 dp): the
  dual-pane layout still applies. Left-pane cards should switch to a
  compact summary so 5–6 journeys fit above the fold.

---

## 4. SavedTripsScreen

File: [`SavedTripsScreen.kt`](../feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/savedtrips/SavedTripsScreen.kt)

`SavedTripsEntry` already declares `ListDetailSceneStrategy.listPane()` —
SavedTrips genuinely is the list owner of the list-detail pair with
`TimeTableRoute`. This stays.

### Width rules (≥ 600 dp)

Detail-pane behaviour when no specific detail route is pushed:

- **No saved trips, no recents** (first-open on tablet): detail pane
  shows a hero block — emoji + "Let's Go! Sydney" + larger CTA layout,
  plus an inline Discover preview if available. Phone (COMPACT) keeps
  the smaller `ErrorMessage` empty state.
- **Trip selected** (user taps a `SavedTripCard`): detail pane renders
  `TimeTableScreen` inline, re-using the existing composable with the
  current `TimeTableViewModel`. Phone (COMPACT) still navigates to a
  full-screen `TimeTableScreen` — unchanged.
- **No selection yet, has saved trips**: detail pane shows the
  DepartureBoard accordion in its expanded form plus Discover content,
  so the right column is never blank.

### Compact-height rules (`isCompactHeight`, height < 480 dp)

Phone landscape and similar tight-vertical contexts:

- The current bottom `SearchStopRow` (From + reverse + To + Search +
  Settings + Discover) eats ≥ 200 dp of vertical space. Collapse it
  into a single-line variant pinned to the title bar:
  - From and To as horizontally arranged `TextFieldButton`s.
  - Reverse-direction icon button between them.
  - Search action collapses to a small icon button on the right.
- The saved-trips list becomes the dominant element.
- `DepartureBoard` accordion stays collapsed by default; expanded
  state still scrolls under the list.
- Park & Ride and Discover info-tiles stay scroll-revealed, not
  pinned.
- These rules apply on every form factor that hits `isCompactHeight`,
  independent of the width-based dual-pane treatment above.

---

## 5. Navigation graph

File: [`KrailNavHost.kt`](../composeApp/src/commonMain/kotlin/xyz/ksharma/krail/KrailNavHost.kt)

`KrailNavHost` uses `ListDetailSceneStrategy` from Navigation 3.
Per-route `metadata` decides which pane each route lands in:

| Route               | Metadata today        | Should be                                         |
|---------------------|-----------------------|---------------------------------------------------|
| `SavedTripsRoute`   | `listPane()`          | unchanged                                         |
| `SearchStopRoute`   | `listPane()`          | **none** (owns its own list+map; render full-width) |
| `TimeTableRoute`    | `detailPane()`        | unchanged                                         |
| `JourneyMapRoute`   | (none today)          | phone-only destination; tablet uses inline map    |
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

Today's `@ScreenPreview` covers phones in light/dark + 2× font scale
plus one tablet portrait config. It does **not** cover:

- Tablet landscape (e.g. `spec:width=1280dp,height=800dp`).
- Phone landscape / compact-height.

When implementing the rules above, extend `@ScreenPreview` (or add
`@TabletScreenPreview` / `@CompactHeightPreview`) so every dual-pane and
compact-height variant is exercised in the IDE without each call-site
adding a custom `@Preview`.

---

## 8. Implementation order

These are the suggested follow-up tickets. Each is independent and small
enough to ship behind its own PR.

1. **Drop `listPane()` metadata from `SearchStopEntry`.** Single-line
   change; immediately removes the 25 % / 25 % cramped layout on
   tablets and foldable-unfolded.
2. Tighten the SearchStop pane ratio (320–480 dp list, map fills rest).
   Validate on phone landscape, foldable-unfolded portrait, tablet
   landscape.
3. Wire the TimeTable dual-pane with a persistent `JourneyMap`; hide
   the "Maps" button when `shouldShowDualPane`.
4. Wire the SavedTrips dual-pane (inline TimeTable detail +
   state-aware right pane).
5. SavedTrips compact-height: collapse `SearchStopRow` into a
   single-line bar pinned to the title bar.
6. Add `@TabletScreenPreview` (landscape) and `@CompactHeightPreview`
   (phone landscape) annotations; backfill previews on the screens
   touched above.
7. **Stretch**: posture-aware adaptation via
   `currentWindowAdaptiveInfo().windowPosture` for half-folded
   foldables. Defer unless telemetry shows usage.
