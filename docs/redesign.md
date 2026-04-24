# KRAIL Redesign — Master Plan

> Single source of truth. All other redesign docs deleted.
> Last updated: 2026-04-25

---

## The Core Problem

Users open KRAIL for one of these reasons:

| Intent                         | What they need                            | What they get today                  |
|--------------------------------|-------------------------------------------|--------------------------------------|
| "Am I going to make it?"       | Real-time countdown + live vehicle on map | Two separate screens behind a toggle |
| "Where is my bus right now?"   | Map with live vehicle dot                 | Buried: Track Trip → tap Map         |
| "What time is the next train?" | Departure list, fast                      | Timetable — reasonably good          |
| "Get me to work"               | Instant one-tap launch                    | Flat list to scan                    |
| "Is there parking?"            | Quick parking check                       | Scroll past trips to find it         |
| "I don't drive"                | Ability to hide parking                   | None                                 |
| "Find the nearest stop"        | Map of nearby stops                       | Hidden, undiscoverable button        |

The map is the biggest gap. Everything transit-critical lives on a map — stops, vehicles, routes.
The map should be first-class, not buried.

---

## Your Notes (Captured)

These are your thoughts from our design sessions, preserved verbatim:

**Home/Work Labels**
> "A better way would be that there is a pill like Home and says set home. User taps and then we
> somehow tell user that they can only set public stops here as home or whatever and they select the
> stop. Stop should be named as home or work label. And there could be other labels user can add and
> set like a row of labels in the beginning like Home, Work and + button pill. When user adds home
> fine then set work pill is empty and + button still there. When they select + then they set label
> and add an emoji to it. And it stays as pill, can be deleted/changed/edited in settings in a
> separate screen. If user saved home to work then no need to save reverse again as timetable screen
> already has a reverse button."

**Sort Placement**
> "Option B sounds good." — inline chip above the list, not in the title bar.

**Plan a Trip FAB**
> "Floating pill with rounded corners that look like the shape of the text field we have would be
> awesome. We also want users to select stops on the map, so we could promote that somehow? And typing
> is like last resort? But someone who needs it, it is still there."

**Map on Home Screen**
> "I like map view when user launches app and then the section to see list when they swipe. Maybe we
> use a horizontal pager? But that will conflict with map swipe panning as well. Or bottom section we
> have pills to select list or map? Any better ways? If we can have good UX then we can build it in
> start only rather than later."

**Parking**
> "Option to show parking and select, show below/above or order things. We currently show parking
> for all saved stops only. A way to add and see where parking is available — park and ride stops — so
> users don't have to save a trip but just be able to add a parking stop. By default keep setting on
> for showing all parking for saved trips. Parking stops also have a location so we should show those
> on map too, separately like P icons. And a navigate button on parking info so users can use Apple or
> Google Maps to navigate."

**Journey Screen Bottom Sheet**
> "Nice — bottom sheet must have max 85% with scroll. On tablet it might be slightly different or
> let Material 3 handle that for us? Scroll is important and at end of content enough padding so
> content is scrollable if required so it does not sit below the navigation bars."

**Section Reorder**
> "There should be a separate button that when clicked only then drag and drop or reordering will be
> enabled. There would be different UI for that or a different screen, whatever is easier to implement
> and of high quality with no bugs." → Settings sub-screen confirmed.

**Bottom Sheet Components**
> "Circle can be a Canvas draw — you can animate easily, better control." (for stop dots in half
> state)
> "Reuse existing card styling for now." (for leg headers in full state)

**Departure Board Pinning**
> "Option A for now — pinned departure board entries surface into the main list above the section
> header."

**Animations / Jitter Bug**
> "I found some bugs sometimes that user location dot or transport mode icons if we animate may
> slightly vibrate at a position. We should have proper tests that it won't happen and ways to avoid
> it, with good robust quality for all kinds of state and data that may come in. Handle gracefully all
> things."

**No Arrows in User-Facing Copy**
> "We never use → in text to display. We need proper UI like professional UI to display info with
> proper spacing and alignment."

**Phase 0 Approach**
> "We will first build components and build dummy in Phase 0 with dummy UI data, build screen and
> experience, then redesign if required and actual integration comes in the end."

---

## 3 Approaches: How to Move Forward

### Option A — Progressive Enhancement (List-First, Safe Path)

Keep the home screen as a saved trips list, but make it dramatically better. Map is accessed via "
Pick from map" inside the trip planner flow — it is promoted but not primary.

**Home screen layout:**

```
KRAIL                                    [⚙ Settings]
──────────────────────────────────────────────────────
🏠 Home   💼 Work   🏋 Gym              ← pinned chips
──────────────────────────────────────────────────────
Sorted by: Recently used  ↕             ← inline sort chip
──────────────────────────────────────────────────────
[Trip card with emoji, colour, nickname, countdown]
[Trip card]
──────────────────────────────────────────────────────
Park & Ride                          [Hide]
[Parking cards]
──────────────────────────────────────────────────────
Departure Board                      [Hide]
[Departure board cards]
──────────────────────────────────────────────────────
                         [ + Plan a trip ]   ← floating pill
```

- Map remains behind the "Pick from map" button inside the trip planner
- All saved trips improvements ship (badge, pinning, sort, swipe, labels)
- Journey Screen unification ships (map-first with bottom sheet)
- No new architecture for the home screen

**Pros:** Lowest risk. Familiar to existing users. No gesture conflicts. Fastest to ship — ~8 weeks.
**Cons:** Map is still not first-class on the home screen. Users who want to explore nearby stops
must plan a trip first.

---

### Option B — Bottom Toggle: [Trips] ↔ [Map] ← Recommended

The home screen gets a persistent two-tab bottom toggle. No horizontal swipe (avoids map pan gesture
conflict). Tapping a tab switches the content area.

```
KRAIL                                    [⚙ Settings]
──────────────────────────────────────────────────────
          [  ≡ Trips  ]   [  🗺 Map  ]              ← bottom toggle
```

**Trips tab** — same as Option A (list-first, saved trips, parking, departure board).

**Map tab:**

```
┌──────────────────────────────────────────────────┐
│                                                  │
│            FULL-SCREEN MAP                       │
│   📍 User location                               │
│   ● Stop markers (coloured by mode)              │
│   P  Parking markers                             │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │  🏠 Home   💼 Work   🏋 Gym              │    │  ← pinned chips overlay
│  └──────────────────────────────────────────┘    │
│                                                  │
│                       [ + Plan a trip ]          │  ← pill FAB
└──────────────────────────────────────────────────┘
```

Tapping a stop marker → bottom sheet peek with stop name, lines, next departure.
Tapping a pinned chip → opens TimeTable for that saved trip.
Tapping "Plan a trip" → from/to bottom sheet with "Pick from map" promoted at top.

**Pros:** Map is first-class without gesture conflict. Pinned trips visible on map. Nearby stops
discoverable by default. Modern transit app feel (similar to Transit App). Builds on top of all
Option A work.
**Cons:** More to build — ~12 weeks total. Two contexts to keep in sync (map markers must reflect
saved trips). Location permission needed for full map tab experience (graceful degradation if
denied).

**Why this is the right call:**
The toggle pattern avoids the horizontal swipe conflict that a pager would create. Tabs are a
well-understood pattern. The map tab becomes a spatial dashboard — users can instantly see where
stops, parking, and their saved routes are, without planning a trip. This addresses every item in "
The Core Problem" table above.

---

### Option C — Map-First Home (Map Always Behind)

The home screen is a full-screen map. Saved trips live in a persistent bottom sheet (peek height by
default). The bottom sheet is always draggable up to see the full list.

```
┌──────────────────────────────────────────────────┐
│                                                  │
│            MAP always visible                    │
│                                                  │
├══════════════════════════════════════════════════╡
║  ━━  drag handle  ━━                             ║  ← peek: pinned chips
║  🏠 Home   💼 Work   🏋 Gym                      ║
╚══════════════════════════════════════════════════╝
```

Dragging the sheet up reveals the full saved trips list, parking, departure board.

**Pros:** Most immersive. Map is always live and visible. Feels like a real transit dashboard.
**Cons:** Map is always loaded = battery/data/performance cost even when user just wants to check
timetable. Bottom sheet fighting with Journey Screen bottom sheet creates UX confusion (two layered
sheets). Complex to implement correctly. Risk of getting it wrong — hard to recover.

**Not recommended for V1.** Worth revisiting after Option B ships.

---

## Recommended Path: Option B

Build everything in this order:

1. All the saved trips improvements (badge, pin, sort, swipe, label pills)
2. Journey Screen unification (map-first with bottom sheet, replaces TrackTrip + JourneyMap)
3. Home screen map tab (uses the same map infrastructure from Journey Screen)
4. Parking + departure board personalisation

This means the map tab ships _after_ the Journey Screen map work is done — shared map
infrastructure, no duplicate effort.

---

## Agreed Design Decisions

### Home/Work Labels

- A pill row at the top of saved trips: `[ 🏠 Home ] [ 💼 Work ] [ + Add ]`
- Tapping an empty pill → user selects a public stop → that stop is labelled with that pill
- Set in Settings (separate screen); cannot be changed from main saved trips screen
- These are stop labels, not trip labels
- No need to save reverse trip — timetable already has a reverse button
- What happens when tapping a filled pill: **open for decision** — timetable for that stop?
  Departure board?

### Sort

- Inline chip above the trip list: `Sorted by: Recently used  ↕`
- Tapping opens a bottom sheet with 4 options: Recently used / A–Z / Transport mode / Custom
- Shown only when user has 3+ trips
- Persisted in `DataStore`

### Plan a Trip FAB

- Floating pill button: `[ + Plan a trip ]` — matches shape/corner radius of search text field
- Tapping opens bottom sheet with "Pick from map" promoted above text fields
- For first-time users (no saved trips): full search row shown expanded by default
- Map selection is the primary; typing is the fallback

### Journey Screen

- Single `JourneyScreen` replaces both `TrackTripScreen` and `JourneyMapScreen`
- Full-screen map with 3-anchor bottom sheet: Peek / Half / Full
- Peek (default): route summary, on-time badge, departure countdown, platform
- Half: stop list (past dimmed via alpha, current = filled Canvas circle, future = outline circle)
- Full: all legs, walking transfers, per-leg expand/collapse
- Sheet max 85% height. Always scrollable. Bottom padding for nav bar.
- Two modes: Static (planned route, "Start Tracking" FAB) and Tracking (live vehicle, live times)
- Back gesture: first press collapses sheet to Peek; second navigates back

### Camera & Vehicle Behaviour

- User GPS is the camera anchor (follows user, not vehicle)
- Vehicle dot glides with `Animatable<LatLng>` at 800 ms; uses `conflate()` on position Flow to
  avoid stutter
- User dot uses `conflate()` + 2 m minimum movement threshold
- Camera unlocks on manual pan → Re-centre button appears → tapping flyTo user, 400 ms
- Tests required: 10 rapid-fire position updates → assert final position correct, no overshoot

### Stop Tap Interactions (Journey Screen)

- Tap stop circle → sheet scrolls to that stop, highlights it
- Tap vehicle dot → callout: line name, destination, last updated
- Tap elsewhere on map → dismiss callouts; if sheet was Half, snap to Peek
- Pinch-to-zoom → works freely always

### Parking

- Default: show parking for all saved trip stops automatically (AUTO source)
- User can add arbitrary park-and-ride stops manually (MANUAL source)
- Individual stops can be hidden (soft delete, restorable from Settings)
- Navigate button: opens `geo:lat,lng` intent (Android) / Apple Maps URL (iOS)
- P markers on map (Journey Screen and Map tab) — later iteration
- No real-time parking availability in V1; navigate then details shown
- Parking info uses same components and card styling as today

### Departure Board Pinning

- Option A: pinned departure board entries surface into the main list (above section header)
- Add + remove model, same as parking
- Works with section hide/show

### Section Reorder

- Separate button → opens Settings sub-screen with drag handles for Parking / Departure Board /
  Saved Trips
- Not a long-press gesture (too undiscoverable, too hard to test)
- Bottom sheet with drag will not work well with gestures → Settings sub-screen confirmed

### Nearby Stops (Map Tab / Stop Selection Map)

- 1 km radius from user location
- Mode filter chip row: `[ All ] [ 🚆 Train ] [ 🚌 Bus ] [ ⛴ Ferry ] [ 🚃 Light Rail ]`
- Multi-select vs single-select: **open for decision**
- Stop markers coloured by dominant transport mode at that stop
- Tap stop → bottom sheet peek with stop name, lines, next departures

### Bottom Sheet Rules (All Screens)

- Max 85% screen height (works for phones and tablets)
- Always scrollable content inside; always enough bottom padding for nav bar
- No bottom sheet opens another bottom sheet — use in-place expansion (`animateContentSize`) or
  `AnimatedContent`
- Drag handle: 32×4 dp centred pill, `KrailTheme.colors.onSurface.copy(alpha = 0.3f)`

### UI Copy Rules

- No `→` in any user-facing text (use proper UI spacing and components instead)
- On-time badge: "On time" / "3 min late" / "Early" — coloured pill
- Platform shown when available, not in parentheses

### Taj Design System Rules (All New Components)

- No hardcoded `dp` values — all via `SpacingTokens.*`
- No hardcoded `Color(...)` — all from `KrailTheme.colors.*`, `LocalThemeColor`, or
  `NswTransportConfig`
- No `MaterialTheme.*` — use `KrailTheme.*` equivalents
- Previews: light mode, dark mode, at least one non-default `LocalThemeColor`
- `@Stable` data classes as params; no inline lambda captures

### Location Permission

- Never request proactively — only on user-initiated action
- Always show in-app explanation card before OS dialog
- Every screen degrades gracefully without location
- `PermanentlyDenied` state (must go to Settings) vs `Denied` (can ask again) — split into two
  states in `PermissionStatus`

### Monetisation

- Core value (timetable, tracking, departure board, parking) always free
- Personalisation + power features gated at paid tier
- Free tier: 3 saved trips, 1 pinned, recently-used sort only, 1 pinned departure board stop, ads
- Premium: unlimited saved trips, 5 pinned, all sort options, drag reorder, unlimited dep board
  pins, ad-free
- Paywall: non-blocking bottom sheet, dismissible
- No interstitials. No full-screen ads. No ads during trip tracking
- Pricing placeholder: $3.99/mo AUD, $24.99/yr AUD

### Build Process

- Phase 0: dummy data components first, build the screen experience visually, integrate data after
- Never run build/compile commands — ask user to run and share output
- All PRs ≤ 500 lines, stacked via Graphite (`gt submit`)
- LazyColumn/LazyRow: every `item {}` must have an explicit `key`

---

## Feature List

### Saved Trips

- [ ] DB migration: 7 new columns (customLabel, emoji, colorHex, isPinned, pinOrder, customOrder,
  lastUsedAt)
- [ ] Transport mode resolved from DB after first timetable load (write-back); eliminates
  `primaryTransportMode = null` TODO
- [ ] Badge pill per card: emoji + transport mode colour + optional nickname
- [ ] Edit bottom sheet: label, emoji grid (20–30 curated emojis), colour picker
- [ ] Home/Work label pills: `[ 🏠 Home ] [ 💼 Work ] [ + Add ]` — set in Settings
- [ ] Pinned chip row: up to 3 trips as compact chips at top, one-tap to TimeTable
- [ ] Swipe left: Edit + Delete with undo snackbar (3 s)
- [ ] Swipe right: direct TimeTable shortcut
- [ ] Inline sort chip: Recently used / A–Z / Transport mode / Custom
- [ ] Drag-and-drop reorder (Custom sort, edit mode via separate button)
- [ ] First-save naming prompt (shown once, dismissed permanently on "Not now")
- [ ] Default screen on open: Settings entry → DataStore → cold-start routing

### Plan a Trip

- [ ] Replace sticky bottom row with floating pill FAB
- [ ] First-time user: full row expanded by default; collapses to pill after first saved trip
- [ ] "Pick from map" button in search UI — promoted above text fields
- [ ] Full-screen nearby stops map with tappable markers (1 km radius)
- [ ] Stop callout: name, lines, next departures, "Select this stop" button
- [ ] Mode filter chips on the nearby stops map
- [ ] Location permission: in-app explanation card before OS dialog

### Journey Screen (replaces TrackTrip + JourneyMap)

- [ ] `JourneyRoute` sealed class: `Static(journeyId)` + `Tracking(encodedData)`
- [ ] `JourneyViewModel`: merge TrackTrip + map logic; `JourneyViewState` sealed interface
- [ ] Full-screen map + `AnchoredDraggableState` 3-anchor bottom sheet
- [ ] Peek state: route summary, on-time badge, departure countdown, platform
- [ ] Half state: stop `LazyColumn` with Canvas stop dots, past/current/future treatment,
  auto-scroll
- [ ] Full state: all legs, walks, per-leg expand/collapse
- [ ] Static mode: "Start Tracking" FAB, auto-activates within 5 min of departure
- [ ] Tracking mode: live vehicle dot (`Animatable<LatLng>`, `conflate()`), live times
- [ ] User location dot: pulsing ring (`InfiniteTransition`), `conflate()` + 2 m threshold
- [ ] Camera follow (user GPS anchor), unlock on pan, Re-centre button, flyTo 400 ms
- [ ] Map interactions: stop tap → sheet scroll, vehicle tap → callout, tap elsewhere → dismiss
- [ ] Share button: deep link generation
- [ ] Delete `TrackTripScreen` + `JourneyMapScreen` after JourneyScreen ships
- [ ] Tablet: 85% sheet height, always scrollable

### Home Screen Map Tab (Option B)

- [ ] Bottom toggle: `[ ≡ Trips ] [ 🗺 Map ]`
- [ ] Map tab: full-screen map, stop markers (mode-coloured), parking P markers
- [ ] Pinned chips overlaid on map
- [ ] Plan a trip FAB on map tab
- [ ] Tap stop marker → bottom sheet peek (stop name, lines, next dep)

### Parking & Departure Board

- [ ] `SectionHeader` with "Hide" button → `DataStore` preference + `animateItem()` collapse
- [ ] Section reorder: Settings sub-screen with drag handles
- [ ] Parking: AUTO (derived from saved trips) + MANUAL stops; `ParkingStop` model
- [ ] Parking: individual hide/show; restore from Settings
- [ ] Parking: navigate button (`geo:` intent / Apple Maps URL)
- [ ] Departure board: pin 1 stop (free) / unlimited (premium) → surfaces to main list
- [ ] P markers on map (later iteration)

### Monetisation

- [ ] `PremiumStatus` in `DataStore` + `expect/actual PurchaseManager` in `core/purchase`
- [ ] `PaywallSheet`: non-blocking bottom sheet, dismissible
- [ ] `PremiumBadge` pill on locked features
- [ ] Free tier gates: saved trips count, pinned trips count, sort options, dep board pins
- [ ] Ad slot: InfoTile slot + native card between trip 2 and 3 (free tier only)
- [ ] Settings: "Rate KRAIL" + "Send feedback" entries
- [ ] In-app rating prompt (native `ReviewManager` / `SKStoreReviewController`) after 5 opens + 1
  saved trip + successful timetable load

---

## Build Order

```
Phase 0: Dummy screens
  └── Build all new screens with hardcoded dummy data first
      Validate the experience visually before wiring real data
      Redesign if needed

Phase 1: Foundation (no visible user changes)
  A1. PermissionStatus → add PermanentlyDenied
  A2. DB migration: 7 new columns + SQL queries
  A3. Tier 1 atoms: DragHandle, OnTimeBadge, StopTimeRow, SectionHeader, DotIndicatorBadge
  A4. InAppPermissionCard, FloatingPill, improved LocationStatusBar

Phase 2: Saved trips visual identity
  B1. TripBadgePill + NicknameLabel components
  B2. Apply badge to SavedTripCard; resolve primaryTransportMode in VM
  B3. TripEditSheet: label, emoji grid, colour picker
  B4. FirstSavePrompt (shown once)

Phase 3: Saved trips pinning + sort
  C1. PinnedTripChip + PinnedChipRow components
  C2. Pin/unpin events, DB writes, pinnedTrips in state
  C3. Sort inline chip + SortBottomSheet + DataStore

Phase 4: Plan a Trip FAB
  D1. Replace sticky row with floating pill; first-timer detection

Phase 5: Stop selection map
  E1. "Pick from map" button + navigation wiring
  E2. NearbyStopMarker + StopSelectionCallout + nearby API
  E3. Permission card for stop map

Phase 6: Journey Screen
  F1. JourneyRoute + nav wiring; old routes redirect
  F2. JourneyViewModel (merge TrackTrip + map logic)
  F3. JourneyScreen scaffold: AnchoredDraggable sheet + map + Peek content
  F4. Half state: JourneyStopList + auto-scroll
  F5. Full state: JourneyFullTimeline + LegHeader + WalkTransferRow
  F6. Tracking mode: LiveTrackingOverlay + StartTrackingFab + auto-activate
  F7. Camera follow + Re-centre + vehicle animation
  F8. Map interactions: stop tap, vehicle tap callout
  F9. Permission UX on Journey Screen
  F10. Delete TrackTripScreen + JourneyMapScreen

Phase 7: Home screen map tab (Option B)
  G1. Bottom toggle + map tab scaffold
  G2. Stop markers on map tab
  G3. Pinned chips overlay + parking P markers
  G4. Stop callout bottom sheet on map tab

Phase 8: Parking + Departure Board personalisation
  H1. SectionHeader hide/show + DataStore + animateItem()
  H2. ParkingStop model (AUTO + MANUAL) + navigate button
  H3. Pinned departure board entry (surfaces to main list)
  H4. Section reorder Settings sub-screen

Phase 9: Saved trips swipe + reorder
  I1. SwipeableCard: swipe-left (edit + delete + undo), swipe-right to TimeTable
  I2. Drag-and-drop reorder: edit mode button, drag handles, customOrder DB writes

Phase 10: Monetisation
  J1. PremiumStatus + PurchaseManager (core/purchase module)
  J2. PaywallSheet + PremiumBadge + free tier gates
  J3. Ad slots (InfoTile + native card)

Phase 11: Settings + ratings
  K1. "Open app to" setting + cold-start routing
  K2. Home/Work label pills Settings screen
  K3. In-app rating prompt + "Rate KRAIL" + "Send feedback"
```

**Total PRs: ~38 | ~8 000 net new lines | ~1 500 deletions**
One developer: ~12–14 weeks. Two developers (split Phases 2–5 vs 6): ~7–8 weeks.

---

## Quality Gates

Every PR before merge:

1. `./gradlew testAndroidHostTest --continue` — all tests pass
2. `./gradlew detekt --continue` — clean (autoCorrect handles imports/commas)
3. Screenshot tests baseline updated for new components
4. Manual: dark mode, rotation (state preserved), no-permission (graceful degradation), offline (
   error states not crashes)
5. Ask user to run `./scripts/fullQualityChecks.sh` before merge to main

New ViewModel tests required (per block):

- `SandookMigrationTest`: 7 new columns survive round-trip
- `SavedTripsViewModelTest`: transport mode resolved, pin logic, sort persistence, 3-trip free limit
- `JourneyViewModelTest`: Static→Tracking transition, auto-activate, camera unlock/re-lock
- Animation jitter test: 10 rapid-fire position updates → assert no overshoot, final = last emission

---

## Open Questions

1. **Home/Work pill tap action** — when user taps a filled Home/Work pill, what opens? Options: (a)
   timetable for that stop, (b) departure board for that stop. Which?

2. **Nearby stops mode filter** — multi-select (show Train + Bus simultaneously) or single-select (
   show only one mode at a time)?

3. **Map tab default state** — when user opens map tab: (a) always fit all saved trip stops in
   view, (b) always centre on user location, (c) restore last camera position?

4. **Parking stops on map** — when user taps a P marker: show stop selection sheet (same as stop
   marker tap) plus parking info? Or a separate parking detail sheet?

5. **Home/Work pill → what stop** — the app only knows public transport stops, not addresses.
   First-time setup should make this clear. How? A short tooltip on the stop search screen when
   triggered from a Home/Work pill?

6. **Vehicle dot vs user dot z-order** — user dot always on top (higher z-order). Pulsing ring makes
   it distinct from the solid vehicle dot. Confirmed?

7. **Static mode data source** — if user deep-links directly to a journey in Static mode, we need
   the journey data. Encode enough in the route params (similar to `TripDeepLink`) or fetch from API
   with journeyId?

8. **Tablet layout** — 85% bottom sheet confirmed for V1. Side panel layout (sheet on leading edge,
   map 60%) deferred to later. Confirmed?
