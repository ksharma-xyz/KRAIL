# SearchStopScreen — UX rules

The screen is built around three ideas: **search for a stop**, **save stops as labels**
(assigned inline, no picker sheet), and **manage labels** (rename/remove-assignment/
delete/reorder, via the full-screen `ManageStopLabelsScreen`). This is the **v4**
shipped design — see `STOP_LABEL_UX_REDESIGN_PROPOSAL.md` for the history of how it
got here (v2 sheet-based → v3 tap-to-expand → v4 external review pack). Most rules
below are covered by tests. Anything not yet tested is in the
[TODO](#todo--future-work) section so the gap is explicit.

---

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

### Top pill row (`LabelShortcutsRow`) — set labels only, tap-only
The row shows **only labels that already have a stop attached** (`labels.filter {
it.isSet }`) as tappable pills, plus a trailing **Manage** button. Unset labels render
nothing here — there is no "+", no unset pill, no way to start an assignment from this
row. (This is a deliberate v4 change from the earlier long-press-to-edit /
tap-unset-pill-to-choose-mode designs — see
[Retired: choose-mode](#retired-choose-mode-and-star-conflict-flow) below.)

- Tap a **set pill** → selects that stop as From / To (`onSetLabelClick`).
- Tap the trailing **Manage** button → hides the keyboard and clears search-field
  focus (same order as the back button's `NavActionButton`), then navigates to
  `ManageStopLabelsScreen` (a real nav destination, `ManageStopLabelsRoute` — not a
  sheet).

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
"Your Places" / Uber Settings > Saved Places shape. Title bar: "Manage your labels" +
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

### Pill row visibility (`shouldShowPillRow`)

The whole top pill row (+ trailing Manage button) only renders when there's something
below for the user to act on:

| List state | Pill row |
|---|---|
| Recent, no recents yet | hidden |
| Recent, ≥ 1 recent stop | visible |
| Results loading (no results yet) | hidden |
| Results with ≥ 1 result | visible |
| NoMatch | hidden |
| Error | hidden |

Note: since the row only ever shows *set* labels as pills now, "visible" can still mean
zero label pills + just the Manage button, if every label is currently unset.

---

### Empty-state stops (`shouldShowEmptyStateStops`)

On a true first open — Recent mode with **zero** recent stops — the screen would
otherwise be a near-blank canvas (just the "select on map" button). Instead we render
a small hardcoded curated list, `EMPTY_STATE_STOPS`: **Town Hall, Central, Parramatta,
Wynyard** (order intentional), so a brand-new user can reach a major interchange
without typing.

| List state | Empty-state stops |
|---|---|
| Recent, no recents yet | **visible** (the 4 curated stops) |
| Recent, ≥ 1 recent stop | hidden (recents replace them) |
| Results / NoMatch / Error | hidden |

Rules:
- **No header.** A brand-new user just sees a few tappable stops; no "Popular"/
  "Suggested" label (product decision).
- **Replace, never coexist.** They are a first-open fallback only. The instant the
  user has any recent stop, recents take over and the curated list disappears.
- **Same tap path as recents.** Each renders as a `StopLabelAssignRow` too, so tapping
  the stop name selects it as From/To, and its "+" works the same as any other row.
  Fires `TrackStopSelected(isRecentSearch = false)` — not a recent, not a search.

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
- Trailing **Manage** button uses `ButtonDefaults.monochromeButtonColors()` so it reads
  as an action distinct from the label pills.
- Stop-row dividers respect `pageHorizontalPadding` (don't go edge-to-edge).
- The "searching…" indicator is a small floating `LoadingDotsPill` (taj component),
  positioned at top-center on the z-axis above the LazyColumn — does not claim vertical
  layout space, so the pill row stays anchored when typing.

---

## State management

- All UI orchestration state in `SearchStopScreen` is `rememberSaveable`, with
  `mapSaver`-style `Saver`s for complex types (`StopLabel?`, `NewLabelTarget?`).
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
- ✅ `manageButton_isVisible_wheneverPillRowRenders`
- ✅ `tappingManageButton_invokesOnManageLabelsClick`
- ✅ `tappingManageButton_hidesKeyboardBeforeInvokingCallback`
- ✅ `clearAllButton_firesClearRecentSearchStopsEventWithCount`
- ✅ `recentHeader_showsRecentAndClearAllLabelsWhenRecentsExist`

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
