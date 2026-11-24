# SearchStopScreen — UX rules

The screen is built around three ideas: **search for a stop**, **save stops as labels**
(assigned inline, no picker sheet), and **manage labels** (rename/remove-assignment/
delete/reorder, via the full-screen `ManageStopLabelsScreen`, opened from the Manage stop labels row in Settings, not from
this screen). This is the **v4**
shipped design — see `STOP_LABEL_UX_REDESIGN_PROPOSAL.md` for the history of how it
got here (v2 sheet-based → v3 tap-to-expand → v4 external review pack). Most rules
below are covered by tests. Anything not yet tested is in the
[TODO](#todo--future-work) section so the gap is explicit.

---

## Two states: the map state and the list state

The screen has two states and a two step back (`docs/MAP_HOME_UX.md` rule 39), held in one pure reducer,
`reduceSearchStopFlow` (`SearchStopFlow.kt`), and saved across rotation with `SearchStopFlowSaver`.

- **Map state** is how it opens, from either field: the map behind, one dark pill, no list and no keyboard.
  Under the pill, on the map state only, sits the row of mode chips (branch 5).
- **List state** is what tapping the bar raises: the keyboard on the bar's own field, then the recents card,
  or the results as the rider types. Gaining focus in the bar is the tap, so the field is the one trigger.
- **X** clears the text and does nothing else. It never leaves the screen and the recents stay.
- **Back**, the arrow in the bar, and the iOS swipe are the same step. List state goes to the map state, and
  the map state goes home. A tap on the map is the first step on its own (`MapTapped`).
- Tapping the other bar of the To and From block moves editing to it in place and raises the list state.
  Back from a start field that was switched to lands on the field the screen was opened on, with the
  destination kept.
- With no map to open on (the `maps_stop_search` flag is off for this rider) the list is the only state:
  `effectiveFlow` shows it and the keyboard comes up as it did before the map state existed. Back leaves in
  one step.

The map state carries the one filter the map has: a row of `TransportModeChip`s, one per mode the nearby map shows, directly under the
pill (`SearchStopModeChips`). Whole chips wrap, and over 130 percent text the row is one line that scrolls sideways with the first chip
fully visible and a fade at the right edge (`chipRowModeFor`, the same rule as the home chips). A tap turns a mode on or off and the map
reloads its stops. The list state has no chips. There is no Options sheet: the radius is Remote Config `nearby_stops_radius_km`
(default 3.0 km, read once as the map starts) and the compass is on with the scale bar off.

Only the single pane layout reads the mode. Tablet, foldable and phone landscape use `SearchStopScreenDualPane`,
which always draws the list left and the map right through `DualPaneScaffold`, with no keyboard on open.

Two invariants the old doc held still hold, in new words:

- The map is a SIBLING of the floating column and of any gradient, never a child. On iOS a MapLibre
  `UIKitView` cannot composite into an offscreen `graphicsLayer` and renders blank. The gradient only draws
  when there is no map at all.
- The camera moves to the rider once, keyed on the stable boolean `hasUserLocation` and never on the
  changing `LatLng`, or GPS jitter cancels the animation. That code is unchanged in `SearchStopMap`.

`SearchStopViewModel` now reads the maps flag into the first state, so the first frame already knows whether
there is a map. Before that a rider with no map saw the list for a frame, and a rider with one saw it flash.

The map starts once, as the screen opens, through the existing `InitializeMap` event, so opening costs the shared
map instance and no new network call.

## Labels

### Defaults & lifecycle
- On first install, `Home` and `Work` are seeded (both unset).
- `Home.isProtected == true` (`StopLabel.PROTECTED_LABEL`) — Delete and Rename are both
  hidden for Home in `ManageStopLabelRow`; `DeleteLabel`'s and `RenameLabel`'s
  ViewModel handlers also early-return for it as a second line of defence. Home *can*
  still be cleared
  (Remove assignment) — `isProtected` only blocks deletion and renaming, not clearing.
  So the labels list can never be empty, which means `observeStopLabels`'s empty-rows
  branch only ever runs once (on a truly fresh install).
- Sort order in the DB query (`StopLabels.sq`): `(stop_id IS NULL) ASC, sort_order ASC,
  label ASC` — labels with stops appear before unset labels; manual drag order
  (`MoveLabelToIndex`, Manage screen's Reorder mode) is preserved within the "has a
  stop" group only — unset labels aren't reorderable (see below).

### The label row (`LabelShortcutsRow`), set labels only, tap-only
The row is the first row of the recents card. It is one row that scrolls sideways, the exception rule 4 gives
it, so Home, Work and any custom label are whole chips and the first one starts fully visible. It shows **only
labels that already have a stop attached** (`labels.filter { it.isSet }`) as tappable pills and nothing after
them: there is no Manage button on this row. Unset labels render
nothing here — there is no "+", no unset pill, no way to start an assignment from this
row. (This is a deliberate v4 change from the earlier long-press-to-edit /
tap-unset-pill-to-choose-mode designs — see
[Retired: choose-mode](#retired-choose-mode-and-star-conflict-flow) below.)

- Tap a **set pill** → selects that stop as From / To (`onSetLabelClick`).
- Manage stop labels is not reachable from this screen. It is a row in Settings, directly under
  Change Theme, which navigates to `ManageStopLabelsScreen` (a real nav destination,
  `ManageStopLabelsRoute`, not a sheet). Back from it returns to Settings.

### Assigning a stop to a label (`StopLabelAssignRow`)
Every stop row (recents, search results, empty-state stops) is a `StopLabelAssignRow`.
It expands in place (`animateContentSize`, same pattern as `JourneyCard`/
`TripSearchListItem`) — there's no separate picker sheet.

A stop row has exactly two states, with no path between them from this row:
- **Unassigned** — shows a plain "+" (`AssignToLabelIcon`, bare rotating chevron, no
  circle background). Tap → expands to a wall of every **unset** label
  (`stopLabels.filterNot { it.isSet }`) as outlined pills, plus a trailing
  **"+ New label"** chip.
  - Tap an outlined pill → assigns this stop to it instantly (`AssignLabelStop`), no
    confirmation, row collapses. Because the wall only ever lists unset labels, there
    is no reassign-by-tapping-a-pill flow and no conflict to resolve here — a stop can
    only ever be on one label and a label on one stop, satisfied by construction.
  - Tap "+ New label" → opens `AssignNewLabelSheet` ("Save and assign" — creates the
    label AND assigns this stop to it in one step). Duplicate names are blocked with
    `ConfirmLabelActionSheet` instead of silently no-oping.
- **Assigned** — shows the label as a small, solid, non-interactive pill inline next to
  the transport-mode icons. No icon, no expand, no way to change or remove the label
  from this row — reassignment/removal only happens in Manage (see below).

### Managing labels (`ManageStopLabelsScreen`)
A real nav destination (`ManageStopLabelsRoute`), not a sheet — Google Maps
"Your Places" / Uber Settings > Saved Places shape. It opens from the **Manage stop labels** row in Settings
(`SettingsScreen`, under Change Theme); it used to open from a Manage button on the Search Stop label row. Title bar: "Manage your labels" +
a "Reorder"/"Done" toggle action.

- **Normal mode**: description text, then set labels (with stops) followed by a "Not
  Assigned" section for unset labels. Tap any row (`ManageStopLabelRow`) to expand it
  in place:
  - **Rename** field + **Save changes** button — hidden entirely for the protected
    Home label (renaming it would also un-protect it, silently making it deletable).
  - **Remove** (outlined button, only shown when the label has a stop) — fires
    `ClearLabelStop`.
  - **Delete** (destructive-tinted button, hidden for protected labels) — fires
    `onDeleteLabel`, which shows a `ConfirmLabelActionSheet` ("Delete "X" label?")
    before the ViewModel's `DeleteLabel` actually fires.
- **Reorder mode**: description hides, a banner appears ("Long press and then drag ⋮⋮
  to reorder your labels"), only **set** labels get a drag handle
  (`longPressDraggableHandle`, via `sh.calvin.reorderable`) — unset labels have no
  meaningful order (they don't drive the top pill row) so dragging them is a no-op and
  they render without a handle. Reorder fires `MoveLabelToIndex(labelKey,
  targetLabelKey)` per drop — keyed off the target label rather than a raw list index,
  since the LazyColumn also holds non-draggable rows (the banner, the "Not Assigned"
  header/rows) that would otherwise offset a plain index.
- There is currently **no "reassign to a different stop" action in Manage** — to move
  a label to a different stop, the only path is Remove (clears it) then assign it
  again from wherever the new stop is shown. (The original redesign proposal sketched
  a "Replace" sheet with this built in; it wasn't part of what shipped.)

---

### Label row visibility (`shouldShowLabelRow`)

The label row (the set labels) leads the card whenever a label has a stop and
there is a list to pick from:

| List state | Label row |
|---|---|
| Recent, a label is set | visible, with or without recents |
| Recent, no label is set | hidden |
| Results loading (no results yet) | hidden |
| Results with at least 1 result and a label is set | visible |
| NoMatch | hidden |
| Error | hidden |

The label row no longer waits for a recent stop: the card can hold nearest or suggested stops instead, and a
rider with Home set wants Home first either way. If every label is unset there is no label row at all.

---

### The recents card (`recentsCardFor`)

One vertical floating card under the form, the same card the results use (`SearchStopListContent`). With
nothing typed it holds, in this order:

1. The label row, when `shouldShowLabelRow` says so.
2. The rows, from one source:

| Rider has | Rows | Heading |
|---|---|---|
| any recents | the recents, at most `MAX_RECENT_ROWS` (5) | Recent, with Clear all |
| no recents, location known | the stops nearest the rider, from the map's loaded stops | Near you |
| no recents, location not known | the Intro's suggested stops, starts for a start field and ends for everything else | none |

A row is a whole item: the stop name, its mode badges and a plus at the end (`StopLabelAssignRow`). A suggested
row carries its own stop id and name, read from Remote Config `intro_suggested_stops` (or the bundled
`DEFAULT_SUGGESTED_STOPS`), so a tap works before the stops table has finished loading. It has no mode badges
because that list has none. With a location and the nearby stops still loading the card shows nothing and does
not borrow the suggested stops, so it never swaps one set for another a moment later.

Nearest and suggested rows report as the `empty_state` surface and as not a recent search, the way the curated
first-open stops always did. `ClearRecentSearchStops` still reports every recent the rider has, not the five shown.

The old hardcoded four (`EMPTY_STATE_STOPS`: Town Hall, Central, Parramatta, Wynyard) and `shouldShowEmptyStateStops`
are gone, replaced by the Intro's list so the stop ids live in one place.

The wall that opens under a row's plus is a `FlowRow` of whole chips. Nothing on this screen scrolls sideways
except the label row.

---

### The location pill (`LocationStatusPill`)

While location permission is not granted one pill sits directly under the form, centred, above the card, which
moves down by one pill and one gap. It is 32 dp tall inside a 48 dp hit area, white, and reads "Location
permission required", the wording the old banner on this map used. `locationPillFor` decides it:

| Permission | Pill | Tap |
|---|---|---|
| not looked up yet, or granted, or a fix in hand | none | none |
| not asked | shown | asks (`allowPermissionRequest`, which `TrackUserLocation` acts on) |
| denied or restricted | shown | opens the app's Settings, and sends `LocationPermissionSettingsClicked` |

The status is looked up again on every return to the screen, so coming back from Settings with it granted drops the
pill at once. This replaces the old "Location Permission Required" badge and the denied banner on the map.

---

### The form (`SearchStopForm`)

The app's To and From block (`ToFromBlock`) with the search field typed into the bar being edited, in place.
The bar being edited is taj's dark `TextField` and the other bar is taj's light `TextFieldButton`, so tapping the
other bar swaps which is which and no row moves.

- With From and To, one back button stands above the card. A single field keeps its back arrow inside the pill. The origin
  dot stays on the From bar in every state.
- The X sits in the To bar's end column and shows only while the field has text.
- A bar holding the field has no tap target of its own (the field takes the tap), so it has no merged label.
- Placeholders: "Choose destination", "Choose starting point", "Choose a stop" for a label, and "Change origin" or
  "Change destination" for one leg of a trip. The label and the leg edit one bar, with no From bar.
- From fills itself with the nearest stop within 1 km when location is known (`autoFromStop`, the radius Home and
  Work use). A destination pick sends that start home with it, and home only takes it where it has no start yet.
- The go circle is drawn only when `goVisible` says From and To both have a stop. This screen holds no destination
  while one is being chosen, so it does not show until the home pill (branch 11) gives it one at open.
- The microphone is never on this screen.

---

## Stops & saving

### Saving auto-pins to recents
When a stop is attached to a label (via `StopLabelAssignRow`'s wall or "+ New label"),
the same stop is also written to the recent search stops table — that way the saved
stop shows up in Recents next time the screen opens, so users can reach it again
quickly even if they remove the label later.

### Add label sheet (`AssignNewLabelSheet`)
- Only ever opened from "+ New label" inside an expanded `StopLabelAssignRow` — the
  stop is always known going in, so there's no "no stop yet" branch and no suggestion
  chips (unlike the old, now-retired `AddLabelBottomSheet`).
- Title + stop name (with its mode roundel underneath) + a single "Label name" field +
  "Save and assign".
- Live-typed name is normalised through `normaliseLabelName`: leading/trailing/embedded
  emoji stripped, whitespace collapsed. A duplicate (case-insensitive, after
  normalisation) blocks Save with a `ConfirmLabelActionSheet`, not a silent no-op.

---

## Retired: choose-mode and star/conflict flow

The following existed in earlier iterations (v2/v3):

- **Choose-mode.** Deleted. `SearchStopScreen`'s `assigningLabel` state, the
  `onUnsetLabelClick` callback, the "Choose your ‹label› stop" placeholder swap, and
  the contextual hint banner (`pillRowBannerText`) were unreachable — `LabelShortcutsRow`'s
  `LazyRow` only ever iterates `isSet` labels (see
  [Top pill row](#top-pill-row-labelshortcutsrow--set-labels-only-tap-only) above), so
  the branch that called `onUnsetLabelClick` could never fire. Removed entirely, along
  with `LabelShortcutPill`'s now-unreachable unset-pill branch and `UnsetLabelPill`'s
  scale-animation parameter.
- **Star-button / conflict-sheet flow.** Deleted. `conflictForAssign`/`AssignConflict`/
  `savedStopIds` in `SearchStopRules.kt`, and the `StopSearchListItem` component
  (star icon, `ic_star`/`ic_star_filled`) had no production callers left —
  `StopSearchListItem` wasn't used in `SearchStopScreen.kt` at all any more, fully
  replaced by `StopLabelAssignRow`. Removed entirely.

---

## Layout / search status

- Recent header text and "Clear all" both use `KrailColors.label` (not `softLabel`) for
  readability across themes.
- Stop-row dividers respect `pageHorizontalPadding` (don't go edge-to-edge).
- The "searching…" indicator is a small floating `LoadingDotsPill` (taj component),
  positioned at top-center on the z-axis above the LazyColumn — does not claim vertical
  layout space, so the pill row stays anchored when typing.

---

## State management

- All UI orchestration state in `SearchStopScreen` is `rememberSaveable`, with
  `Saver`s for complex types (`SearchStopFlow`, `NewLabelTarget?`). The typed text is the hoisted
  `TextFieldState`, which saves itself, so the X can clear it and a rotation keeps it.
  Rotation, dark-mode toggle, locale change and process death do not drop in-flight
  sheets / expand state.
- The DB observer is launched in `init {}` rather than `_uiState.onStart {}` so it runs
  for the VM's lifetime, surviving brief UI subscription gaps.
- All label mutations (`AssignLabelStop` / `CreateLabel` / `ClearLabelStop` /
  `DeleteLabel` / `RenameLabel` / `MoveLabelToIndex`) are **optimistic**: the in-memory
  state updates synchronously inside `updateUiState { … }` before the IO write fires.
  The DB observer's later emission is a no-op in the happy path.

---

## TODO / future work

Tracked here so we can crank through the gaps without losing them.

### Cleanup
All dead code described in [Retired](#retired-choose-mode-and-star-conflict-flow)
above — choose-mode (`assigningLabel`/`onUnsetLabelClick`/`pillRowBannerText`) and the
star/conflict-sheet flow (`conflictForAssign`/`AssignConflict`/`savedStopIds`/
`StopSearchListItem`) — has been deleted.

### Compose UI tests
Covered in `SearchStopScreenInteractionTest.kt` (`src/androidHostTest`):
- ✅ `pillRow_isHidden_onFreshInstallWithNoRecents`
- ✅ `pillRow_isShown_whenAtLeastOneRecentExists`
- ✅ `recentHeader_isHidden_onFreshInstall`
- ✅ `assignedStop_hidesAssignIcon`
- ✅ `assignedStop_showsLabelPillInline`
- ✅ `tappingSetPill_invokesOnStopSelectWithUnderlyingStop`
- ✅ `thereIsNoManageButtonToTap_whenALabelIsSet`
- ✅ `theLabelRowHoldsOnlyTheSetLabels_withNothingAfterTheLastOne`
- ✅ `manageButton_isHidden_whenNoLabelIsSetYet`
- ✅ `clearAllButton_firesClearRecentSearchStopsEventWithCount`
- ✅ `recentHeader_showsRecentAndClearAllLabelsWhenRecentsExist`
- ✅ `singlePane_opensOnTheMapState_withTheBarAndNoListAndNoKeyboard`
- ✅ `tappingTheBar_raisesTheListState`
- ✅ `theArrowIsTwoStepsBack_listToMapToHome`
- ✅ `theX_showsOnlyWithText_clearsItAndKeepsTheRecents`
- ✅ `withNoMapToOpenOn_theListIsTheScreen`
- ✅ `theCardShowsAtMostFiveRecents`
- ✅ `withNoRecentsAndNoLocation_theSuggestedStopsShowAsRows`
- ✅ `theLabelRowScrollsAndLeadsTheCard`
- ✅ `theLocationPillIsOneItemUnderTheForm_onlyWhenPermissionIsNotGranted`

Outstanding (gesture-heavy or sheet-based — held back to keep CI deterministic):
- `dragging_row_in_ManageStopLabelsScreen_fires_MoveLabelToIndex` — drag gesture racing
  with the reorderable lib's `longPressDraggableHandle` is not portable to Robolectric;
  covered by snapshot tests visually instead.
- `addingDuplicateLabel_isBlockedWithInlineError` (`AssignNewLabelSheet`) — needs
  `ModalBottomSheet` to open in test, which requires advancing the animation clock
  through to settle.
- `ManageStopLabelRow` Save/Remove/Delete button taps — not yet covered by a Compose UI
  test (only VM-level `RenameLabel`/`ClearLabelStop`/`DeleteLabel` handlers are
  tested); would need the row's expand state driven from a hoisted test surface.

### Not implemented, so not tested
- Product-class filtering of stop results (excluding modes from the result list) does not
  exist in `RealStopResultsManager` and has no test; `StopFilterByProductClassTest.kt` held
  only a commented-out draft against a deleted function and has been removed.

### ViewModel tests
Covered in `SearchStopViewModelLabelHandlersTest.kt`:
- AssignLabelStop: state + sandook + recents pinning.
- CreateLabel: append, case-insensitive duplicate skip, blank skip, surrounding-emoji
  normalisation.
- ClearLabelStop: detaches in state and DB.
- RenameLabel: renames, blocks duplicates.
- DeleteLabel: removes non-protected, preserves Home (incl. mixed case).
- MoveLabelToIndex: reorders by target label key + re-numbers sort_order; same-key and
  unknown-key no-ops.
- observeStopLabels: empty-DB seeds defaults; populated DB mirrors into state.

### Snapshot coverage
Add `@PreviewScreen` for states we don't render today:
- `ManageStopLabelsScreen` in Reorder mode with a mix of set/unset labels
- `StopLabelAssignRow` with a long unset-label wall that wraps/scrolls
- `AssignNewLabelSheet_DuplicateError`

### Documentation
- Keep this file in sync with `STOP_LABEL_UX_REDESIGN_PROPOSAL.md`'s Status line as
  further wiring PRs land, and delete the proposal doc entirely once nothing in it is
  still aspirational (i.e. once the [Cleanup](#cleanup) items above are done and the
  "Replace sheet" gap is either shipped or dropped from scope).
