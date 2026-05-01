# Stop Labels — Display Across Surfaces

Plan for showing user-labelled stops everywhere a stop name is rendered: timetable header, recents,
search results, and the search-stop "From" / "To" pill.

> **Sibling doc:** [`SAVED_TRIPS_NAMING_PLAN.md`](SAVED_TRIPS_NAMING_PLAN.md) covers the parallel
> epic (trip-level naming + ✕-instead-of-star on the saved-trip card). The two epics touch different
> files and ship independently.

This document is the source of truth before review. UX, UI components, and tech changes each get
their own section. Decisions and open questions are at the end.

---

## Goals & non-goals

### Goals (current scope)

- A user who has labelled a stop sees that label, not the raw stop name, in: timetable header,
  recent searches list, search results list, and the search stop row's "From"/"To" pill once a stop
  is selected.
- The label resolution is **one pure function**, not duplicated per surface.
- `OriginDestination` becomes a clean component that takes a display model and is ready to be made
  click-to-details (timetable only) in a later PR.
- Park & Ride card and the saved-trip card are **not touched** in this stack.

### Non-goals (current scope)

- Saved-trip card redesign (deferred — see Future epic 1).
- Reordering saved trips (deferred — see Future epic 2).
- Trip-level sort (deferred — see "Parked"; only if reorder turns out insufficient).
- Wiring stop-details bottom sheet to clicks on the timetable header (deferred — separate follow-up
  PR after this stack).

---

## UX

### Surface matrix

| Surface                                      | Primary                      | Subtitle                             | Click                           |
|----------------------------------------------|------------------------------|--------------------------------------|---------------------------------|
| Search row "From" / "To" — stop selected     | label if set, else stop name | (none — tight space)                 | already opens search            |
| **Recents list — labelled**                  | **stop name**                | **label, small + muted** (with icon) | already selects stop for search |
| Recents list — unlabelled                    | stop name                    | (none — unchanged)                   | already selects stop for search |
| Search results — labelled                    | stop name                    | label, small + muted (with icon)     | already selects stop for search |
| Search results — unlabelled                  | stop name                    | (none — unchanged)                   | already selects stop for search |
| **Timetable `OriginDestination` — labelled** | **label (with icon)**        | **stop name, small + muted**         | future: open stop-details sheet |
| Timetable `OriginDestination` — unlabelled   | stop name                    | (none — unchanged)                   | future: open stop-details sheet |
| TrackTrip's 3× `OriginDestination`           | stop name                    | (none — unchanged)                   | none planned                    |
| Intro `OriginDestination`                    | stop name (demo data)        | (none — unchanged)                   | none                            |
| **Saved trip card**                          | **untouched in this stack**  | —                                    | —                               |
| **Park & Ride card**                         | **untouched**                | —                                    | —                               |

Recents reads honestly: "you searched for Central — by the way you've labelled it Home". Timetable
reads as routine: "you're going Home, which is Central Station".

### Mockups

#### `OriginDestination` — both stops labelled

```
●   [home icon]  Home              ← titleLarge, themed timeline color
│                Central Station   ← bodySmall, alpha 0.7
│
●   [work icon]  Work
                 Town Hall Station
```

#### `OriginDestination` — only "from" labelled

```
●   [home icon]  Home
│                Central Station   ← subtitle
│
●   Town Hall Station              ← titleLarge, single line, no subtitle
```

#### `OriginDestination` — unknown label name (e.g. "Mom's")

Falls back to `ic_location_on`:

```
●   [location icon]  Mom's
│                    Bondi Junction
```

Same fallback `SetLabelPill` and the bottom sheets already use.

#### `OriginDestination` — neither labelled

Identical to today. No regression.

#### Recents list — mixed

```
🕒  Central Station
    [home icon] Home              ← small, alpha 0.65, with icon
🕒  Town Hall Station
    [work icon] Work
🕒  Wynyard                        ← unchanged (no label)
```

#### Search results — mixed

```
📍  Central Station
    [home icon] Home              ← small, alpha
📍  Central Park                   ← unchanged
📍  Centennial Park                ← unchanged
```

#### Search row "From" / "To" pills — stop selected

```
[ Starting from: [home icon] Home ]
[ Destination:   [work icon] Work ]
```

No subtitle — tight space, the user just picked the stop, context isn't lost.

### Per-surface rules

- **Subtitle alpha**: `LocalContentColor.current.copy(alpha = 0.7f)` on `OriginDestination`, `0.65f`
  on recents/search rows. Tweak on-device after first paint.
- **Subtitle style**: `bodySmall` everywhere it appears.
- **Icon source**: `stopLabelIcon(label)` (existing helper in `components/StopLabelIcons.kt`), with
  `ic_location_on` (trip-planner module) as fallback when no match.
- **Icon tint**: matches the same `LocalContentColor` / themed timeline color the surrounding text
  uses. Reads as part of the same line, not as a separate pip.
- **Whitespace / casing**: label text rendered verbatim — we display what the user stored.

### What stays unchanged

- `SetLabelPill` and `UnsetLabelPill` themselves (they already render the label correctly).
- Park & Ride card.
- Saved-trip card (deferred; see future epic).
- All click semantics on existing surfaces (recents, search row, etc.). Only new addition is
  *optional* click handlers on `OriginDestination`, defaulted to `null` (no-op).

---

## UI / components

### `StopDisplay` — boundary type

The single representation of "a stop, possibly labelled" that crosses VM ↔ UI:

```kotlin
@Stable
data class StopDisplay(
    val stopId: String,
    val name: String,
    val label: String? = null,
)
```

- Pure data, no UX policy (no `primary`/`secondary` getters).
- `stopId` carried so future click-to-details handlers can use it.
- No `emoji` field — drawables are the visual vocabulary; emojis are only used by `StopLabel` itself
  in the bottom-sheet pickers.

### `OriginDestination` — refactored API

Today:
`OriginDestination(trip: Trip, timeLineColor: Color, originSubtitle: String?, destinationSubtitle: String?)`.

After:

```kotlin
@Composable
internal fun OriginDestination(
    origin: StopDisplay,
    destination: StopDisplay,
    timeLineColor: Color,
    modifier: Modifier = Modifier,
    onOriginClick: ((StopDisplay) -> Unit)? = null,       // wired later, timetable only
    onDestinationClick: ((StopDisplay) -> Unit)? = null,  // wired later, timetable only
)
```

- Subtitle is now implicit — when `display.label != null`, label is primary and `display.name` is
  the subtitle. The two `originSubtitle`/`destinationSubtitle` params disappear.
- Icon (`stopLabelIcon(label) ?: ic_location_on`) is rendered inline next to the label, tinted with
  `timeLineColor`.
- Click handlers default to null. When non-null, that stop's `Column` wraps in `klickable` with a
  subtle ripple. No visible affordance at rest — matches the "less dramatic" preference.
- Existing animation behaviour (`AnimatedContent` slide+bounce on stop-name change) is preserved by
  switching the keyed value from `trip.fromStopName` to `origin.label ?: origin.name` (or the
  equivalent display string).

### `LabelTextWithIcon` — optional helper, deferred

If recents/search rows and `OriginDestination` end up with nearly-identical "icon + label text"
rendering, extract a small helper. Until then, render inline. **YAGNI**.

### What to keep dumb

- `StopSearchListItem` keeps its current `stopName: String` (and gains a `labelSubtitle: String?`) —
  it doesn't need to know about `StopDisplay`. Caller resolves and passes strings + a drawable.
- Composables under `OriginDestination` (the column with icon + label + subtitle) are inline — no
  top-level reusable yet.

---

## Tech / code changes

### File layout

New:

-
`feature/trip-planner/state/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/state/savedtrip/StopDisplay.kt`
    - `data class StopDisplay`
    - `fun StopItem.toDisplay(labels: List<StopLabel>): StopDisplay`
    - `fun Trip.fromDisplay(labels: List<StopLabel>): StopDisplay`
    - `fun Trip.toDisplay(labels: List<StopLabel>): StopDisplay`
    - `private fun List<StopLabel>.findLabelFor(stopId): String?`
- `feature/trip-planner/ui/src/commonTest/.../state/savedtrip/StopDisplayTest.kt`

Modified (across PRs):

- `feature/trip-planner/ui/src/commonMain/kotlin/.../components/OriginDestination.kt` — API
  refactor, icon + subtitle render.
- `feature/trip-planner/ui/src/commonMain/kotlin/.../timetable/TimeTableScreen.kt` — pass
  `StopDisplay`s.
- `feature/trip-planner/ui/src/commonMain/kotlin/.../tracktrip/TrackTripScreen.kt` — 3 call sites
  pass unlabelled `StopDisplay`s.
- `feature/trip-planner/ui/src/commonMain/kotlin/.../intro/IntroContentSaveTrips.kt` — 1 call site
  passes unlabelled `StopDisplay`s.
- `feature/trip-planner/state/src/commonMain/.../timetable/TimeTableState.kt` — add
  `stopLabels: ImmutableList<StopLabel>` (or pre-built `from`/`to` `StopDisplay`s — see "Decision:
  state shape", below).
- `feature/trip-planner/ui/src/commonMain/kotlin/.../timetable/TimeTableViewModel.kt` — observe
  `sandook.observeStopLabels()`, fold into state.
- `feature/trip-planner/ui/src/commonMain/kotlin/.../components/StopSearchListItem.kt` — add
  optional `labelSubtitle: String?` and `labelIcon: DrawableResource?`; render second line when
  present.
- `feature/trip-planner/ui/src/commonMain/kotlin/.../searchstop/SearchStopScreen.kt` — at the
  recents/search-results call sites, look up the label per row from `searchStopState.stopLabels` and
  pass to `StopSearchListItem`.
- `feature/trip-planner/ui/src/commonMain/kotlin/.../components/SearchStopRow.kt` —
  `fromStopItem.displayName` and `toStopItem.displayName` swap in. Component gains
  `labels: List<StopLabel>` param (or pre-built display) — see "Decision: state shape".

Untouched:

- `SavedTripsScreen.kt`, `SavedTripCard.kt`, `ParkRideCard.kt`.

### Resolver — pure function

```kotlin
fun StopItem.toDisplay(labels: List<StopLabel>): StopDisplay =
    StopDisplay(
        stopId = stopId,
        name = stopName,
        label = labels.findLabelFor(stopId),
    )

fun Trip.fromDisplay(labels: List<StopLabel>): StopDisplay =
    StopDisplay(stopId = fromStopId, name = fromStopName, label = labels.findLabelFor(fromStopId))

fun Trip.toDisplay(labels: List<StopLabel>): StopDisplay =
    StopDisplay(stopId = toStopId, name = toStopName, label = labels.findLabelFor(toStopId))

private fun List<StopLabel>.findLabelFor(stopId: String): String? =
    firstOrNull { it.isSet && it.stopId == stopId }?.label
```

Mirrors the `stopLabelIcon` precedent — stateless, case-sensitive lookup. `isSet` filter is
essential (a label without a stop assigned is irrelevant).

### State shape — VM produces display models, UI doesn't see labels list

| State                        | Holds today                | After                                                                                                                                                                                                                                   |
|------------------------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SearchStopState.stopLabels` | `ImmutableList<StopLabel>` | unchanged — that screen *also* renders the labels themselves (pill row), so the list belongs there. UI computes `StopDisplay` at render time from the same list.                                                                        |
| `TimeTableState`             | Trip data, no labels       | observe `sandook.observeStopLabels()` and either (a) hold a `stopLabels` list and let UI resolve, or (b) pre-compute `from: StopDisplay` and `to: StopDisplay` in state. **(b) is preferred** — UI doesn't see labels, cohesion higher. |
| `SavedTripsState`            | trips, no labels           | unchanged in this stack.                                                                                                                                                                                                                |

**Decision: state shape — pre-built `StopDisplay`s, not raw labels.**

For screens whose UI does not directly render the labels list, expose pre-built `StopDisplay`s.
Cleaner separation: VM does the lookup once per emission; UI is purely presentational. Saves rework
if the resolver later gets more complex (per-stop fallbacks, case-insensitive matching, etc.).

For `SearchStopScreen`, labels are already in state (the screen renders the pill row). Compute
display models inline at the recents/search-results call sites — no new state field.

### Per-PR breakdown

Stacked on `karan/savedtrips-pill-condition` (current top of stack).

#### PR 1 — `karan/stop-display-model`

- Add `StopDisplay.kt` with the data class + extension resolvers.
- Add unit tests `StopDisplayTest.kt`.
- **No call-site changes.** Pure addition.
- **Estimated**: ~80 LOC.

Tests:

- empty `labels` → returns `StopDisplay(stopId, name, label = null)`
- label exists but `isSet == false` → label = null
- label `isSet` and `stopId` matches → label populated
- multiple labels, one matches → first matching label
- two labels match same `stopId` (data corruption case) → first wins; document contract
- whitespace / case in label string → preserved verbatim

#### PR 2 — `karan/origin-destination-display-model`

- Refactor `OriginDestination` to take two `StopDisplay`s, drop the two subtitle params.
- Inline render of icon + label (when present) + name subtitle (when label set).
- Add nullable `onOriginClick` / `onDestinationClick` params, defaulted null.
- Add `stopLabels` observation to `TimeTableViewModel`, expose `from: StopDisplay` /
  `to: StopDisplay` in `TimeTableState`.
- Update 5 call sites: timetable (passes labelled displays), 3× TrackTrip (unlabelled), intro (
  unlabelled).
- Snapshot tests: `OriginDestination` labelled / unlabelled / mixed.
- **Estimated**: ~150 LOC.

#### PR 3 — `karan/recents-and-search-labels`

- `StopSearchListItem`: add optional `labelSubtitle: String?` + `labelIcon: DrawableResource?`
  params. When both provided, render a small second line below the stop name with icon + label,
  alpha 0.65.
- `SearchStopScreen` recents call site (~`:1261`) and results call site (~`:1147`): look up label
  per row from `searchStopState.stopLabels`, pass icon + subtitle.
- `SearchStopRow` already-selected From/To: swap display string from `stopName` to label-or-name. (
  No subtitle here.)
- Snapshot tests: `StopSearchListItem` labelled / unlabelled.
- **Estimated**: ~80 LOC.

### Testing strategy

- **Unit (state module)**: `StopDisplayTest` — exhaustive table of label / no-label / multi-match /
  `isSet=false` cases. Pure functions, fast, no Compose.
- **Behavioural (VM)**: extend `TimeTableViewModelTest` to assert that an emitted `StopLabel`
  matching the trip's `fromStopId` is reflected in `state.from.label`. We don't unit-test the
  resolver from the VM — that has its own tests.
- **Snapshot (Roborazzi)**: extend `TripPlannerUiSnapshotTest` with new variants for
  `OriginDestination` (labelled / mixed / unlabelled) and `StopSearchListItem` (labelled /
  unlabelled). Existing variants unchanged stay green.

---

## Future epics

These are NOT in the current 3-PR stack. Sketches only — flesh out in follow-up plans.

### Future epic 1 — Trip labels

Saved-trip cards aren't well-served by stop labels: a routine's identity is its *purpose* ("Morning
Commute"), not its *places* ("Home → Work"). Two Home→Work variants should be distinguishable; stop
labels alone can't.

#### UX

- Each saved trip can have an optional `name: String?` ("Morning Commute", "Gym Run", "Saturday
  Coffee").
- Saved-trip card primary becomes the trip name when set; otherwise today's stop-name treatment.
- Subtitle (small, alpha): `From → To` stop names. If both stops are also labelled, subtitle uses
  stop labels (`🏠 Home → 💼 Work`); composes cleanly with the stop-label feature.
- Edit affordance: pencil icon on the card → bottom sheet to enter name. Sheet mirrors the
  stop-label suggestion sheet — pre-canned chips (Morning Commute, Gym Run, etc.) plus a free-text
  field.
- **Suggestions based on context**: when the user saves a new trip, default suggestions consider
  time-of-day + day-of-week. Mon-Fri 8am → "Morning Commute". Sat morning → "Saturday Coffee". User
  can override or pick free text.

#### UI

- New: `EditTripNameSheet.kt` — mirrors `AddLabelBottomSheet` shape (suggestions chips + name
  field + save).
- `SavedTripCard.kt` gains a primary-line slot (trip name when set) and the subtitle treatment.
- Pencil icon inline on the card or in a long-press menu — TBD.

#### Tech

- Migration: add `name: String?` to the Trip table. Existing trips have null name → render today's
  UX (no regression).
- ViewModel: trip-name edits flow through `SavedTripUiEvent.RenameTrip(tripId, name)` →
  `sandook.updateTripName(...)`.
- No interaction with the stop-label feature at the VM layer — entirely separate data.

### Future epic 2 — Reorder saved trips

Same long-press-then-drag pattern as label pills on `SearchStopScreen`. Same reorderable library.

#### UX

- Long-press a saved-trip card → enters reorder mode (cards wiggle, optional ✕ if we want
  delete-while-reordering, otherwise just drag handle).
- Drag to reorder.
- Persists `position: Int` per trip.
- Should land **after** trip labels — reordering identical-looking "Home → Work" cards is hard;
  named cards are draggable-friendly.

#### UI

- Reuse the existing reorderable LazyColumn pattern from `SearchStopScreen`. The pill version proved
  the gestures.

#### Tech

- DB migration: add `position: Int` column to the trip table.
- `MoveSavedTripToIndex` event. Sandook update: write new positions.
- UI: `LongPressDraggableHandle` on each card.

### Parked

- **Sort menu**. Only if reorder turns out insufficient (e.g. users grow many trips and manual
  ordering gets tedious). Implicit options: alphabetical by trip name, last-used, manual (=
  reorder). Tiny menu in the trailing icon slot of the saved-trips title row. Wait for signal before
  building.
- **Stop-details sheet**. Only on timetable's `OriginDestination`. Wires the `onOriginClick` /
  `onDestinationClick` hooks added in PR 2 to a new `StopDetailsSheet`. Independent PR after the
  stop-label stack lands.

---

## Decisions log

- **VM produces display models, not raw labels list.** Cohesion: VM owns "look up label for this
  stop"; UI owns "render this `StopDisplay`". One screen exception (`SearchStopState`) keeps
  `stopLabels` because it *renders* the labels themselves.
- **`StopDisplay` is pure data — no `primary` / `secondary` getters.** Recents wants name primary,
  label subtitle. Timetable wants the inverse. Encoding either policy on the model breaks the other
  call site.
- **No `LabelledStopText` shared composable.** Each surface composes inline. Extract only when a
  second surface needs an identical layout (YAGNI).
- **No `emoji` field on `StopDisplay`.** Drawables (`stopLabelIcon`) are the visual vocabulary; the
  emoji on `StopLabel` is only used by the bottom-sheet pickers.
- **`OriginDestination` API takes two `StopDisplay`s, not `Trip`.** Cleaner cap on the component's
  domain dependencies and unblocks the future click-to-details wiring.
- **Click affordance on `OriginDestination` is invisible at rest.** No icon, no border, no chevron
  until usage shows it's needed.
- **Saved-trip card untouched in this stack.** Trip labels are the right tool for that surface;
  stop-label rendering would be throwaway work if shipped first.
- **Park & Ride card untouched.** Out of scope.
- **Click-to-details only on timetable's `OriginDestination`.** TrackTrip and intro pass null; same
  component, different opt-in per call site.
- **No filter button.** Saved trips lists are small (3-8 typical, ~15 power user); filter UI is
  overhead at that scale. Reorder + trip names give the user enough organisation. Out of scope,
  not "parked".

---

## Open questions

1. **`SearchStopRow` in selected state — does it need the icon next to the label?** Tight space;
   current sketch shows `[home icon] Home`. If the icon hurts readability inside the pill, drop it
   and show label text only.
2. **Subtitle alpha — `0.65f` (recents) vs `0.7f` (`OriginDestination`).** Pick one or keep them
   surface-tuned? Eyeballing on-device after PR 2 will decide.
3. **`SavedTripsViewModel` change later for trip labels.** Worth preparing the `Trip` row migration
   in advance, or wait until the trip-labels epic starts?
4. **Reorder + click-to-details on the timetable header.** If both eventually land, which gesture
   goes where? Reorder is unlikely on the timetable header (it's two specific stops, not a list), so
   probably no conflict. Flagging in case the rule changes.
5. **Where does `LabelTextWithIcon` (if it ever exists) live — `taj` or feature `components/`?**
   Default: feature first; promote to taj only when a second feature wants it.

---

## TODO / future work

- After PR 1-3 land, schedule a follow-up to wire stop-details bottom sheet from timetable's
  `OriginDestination` clicks.
- Trip-labels epic plan (`TRIP_LABELS_PLAN.md`) once we're ready to start it.
- Reorder-saved-trips PR (sibling to trip labels).
