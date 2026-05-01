# SearchStopScreen — UX rules

The screen is built around three ideas: **search for a stop**, **save stops as labels**, and **reorder/delete labels**. Most rules below are covered by tests. Anything not yet tested is in the [TODO](#todo--future-work) section so the gap is explicit.

---

## Labels

### Defaults & lifecycle
- On first install, `Home` and `Work` are seeded (both unset).
- `Home.isProtected == true` — the ✕ delete chip is hidden on Home and the `DeleteLabel` ViewModel handler early-returns for it. So the labels list can never be empty, which means `observeStopLabels`' empty-rows branch only ever runs once (on a truly fresh install).
- Sort order in the DB query: `(stop_id IS NULL) ASC, sort_order ASC, label ASC` — labels with stops appear before unset labels; manual drag order is preserved within each group.

### Tap behaviour (outside edit mode)
- Tap a **set pill** → selects that stop as From / To (also dismisses keyboard + clears focus).
- Tap an **unset pill** → toggles assigning mode for that label. Tapping the same pill again exits assigning mode.
- Tap **+ Add** → opens `AddLabelBottomSheet` for creating a new label.

### Long-press
- Long-press any pill → enters edit mode. Pills wiggle, `✕` overlays appear (except on Home), `+ Add` is replaced with a `Done` action button. The long-press ripple is contained inside the rounded shape (clip applied before clickable).
- Long-press triggers via a custom `awaitEachGesture`, *not* `combinedClickable`, so it doesn't compete with the reorderable library's `longPressDraggableHandle` for events.

### Edit mode (drag-to-reorder + inline delete)
- Drag a pill (long-press first, then keep finger down) → reorders via `MoveLabelToIndex` event.
- Tap **✕** on a pill → `DeleteLabel` event for that label.
- Tap the **Done** pill → exit edit mode.
- Tap a pill body in edit mode → still selects as From/To (consistent with "tap = select" rule).

---

### Pill row visibility (`shouldShowPillRow`)

The whole pill row + assigning banner only render when there's something for the
user to act on:

| List state | Pill row |
|---|---|
| Recent, no recents, no labels set | hidden |
| Recent, no recents, ≥ 1 label set | visible |
| Recent, ≥ 1 recent stop | visible |
| Results loading (no results yet) | hidden |
| Results with ≥ 1 result | visible |
| NoMatch | hidden |
| Error | hidden |

Reasoning: tapping an **unset** pill enters assigning mode, which requires at least
one stop with a star button below — showing it on an empty canvas is a dead-end.
Tapping a **set** pill navigates directly to that stop (From / To), so it is always
safe to show when a label is configured, even after the user clears their recents.

First-launch progression:
1. No labels set, no recents → pill row hidden (nothing useful to show yet).
2. User makes a few searches → recents appear → pill row shows (labels can now be assigned).
3. User assigns a label → label row stays visible even if recents are later cleared.

---

## Stops & saving

### Saving auto-pins to recents
When a stop is attached to a label (via the save sheet or the "Set as From/To then
assign" flow), the same stop is also written to the recent search stops table —
that way the saved stop shows up in Recents next time the screen opens, so users
can reach it again quickly even if they remove the label later.



### Star button (StopSearchListItem)
- Outlined `ic_star` when the stop isn't saved against any label. Tap → opens `SaveStopAsLabelSheet`.
- Filled `ic_star_filled` (themeColor) when the stop matches an existing label's stopId. Tap → fires `ClearLabelStop` for the matching label directly (no sheet).

### Save sheet
- Lists existing labels as chips; tapping a chip fires `AssignLabelStop`.
  - Chip is **solid** when the label is already set, **outlined** when unset (so you can see at a glance which labels still need a stop).
- Includes a `+ New label` chip that chains into `AddLabelBottomSheet`.

### 1:1 invariant + conflict warnings
A stop is associated with at most one label, and a label is associated with at most one stop. `conflictForAssign(target, stop, allLabels)` (in `SearchStopRules.kt`) returns:
- `StopAlreadyOnAnotherLabel` if `stop` is already saved on a different label → user is shown "Already saved … Move to Y?" sheet.
- `LabelHasDifferentStop` if `target` already has a different stop attached → "Already in use … Replace with Y?" sheet.
- `null` otherwise — assignment proceeds silently.

Stop-side conflict takes precedence when both apply.

### Add label sheet
- Title: "Add a new label".
- Sections: Title → (optional Stop chip) → Preview heading + pill → Suggestions chips → Name TextField → Save.
- Suggestions filter out names that already exist (case-insensitive, emoji-stripped via `labelNamesMatch`).
- Live-typed name is normalised through `normaliseLabelName`: leading/trailing/embedded emoji stripped, whitespace collapsed.
- Inline error + disabled Save when the typed name (after normalisation) duplicates an existing label.

---

## Contextual hint banner

Decided by `pillRowBannerText(editing, assigningLabel, stopLabels)` in `SearchStopRules.kt`. Wrapped in `Modifier.animateContentSize` so show/hide is smooth.

| State | Banner |
|---|---|
| Editing | `Long press and then drag the pill to reorder and select Done to save.` |
| Assigning, label still unset | `Tap the ⭐ next to a stop to save it as <name>` |
| Assigning, label became set | (collapsed — banner clears automatically) |
| Idle | (collapsed) |

A `LaunchedEffect` watching `(assigningLabel, stopLabels)` clears `assigningLabel` once the target label transitions to set, so there's no leftover state and no manual Cancel button.

---

## Layout / search status

- Recent header text and "Clear all" both use `KrailColors.label` (not `softLabel`) for readability across themes.
- `+ Add` pill border + text use `label` colour.
- `Done` pill uses `onSurface`/`surface` (inverse) so it reads as an action button distinct from the label pills.
- Stop-row dividers respect `pageHorizontalPadding` (don't go edge-to-edge).
- The "searching…" indicator is a small floating `LoadingDotsPill` (taj component), positioned at top-center on the z-axis above the LazyColumn — does not claim vertical layout space, so the pill row stays anchored when typing.

---

## State management

- All UI orchestration state in `SearchStopScreen` is `rememberSaveable`, with `mapSaver`-style `Saver`s for complex types (`StopLabel?`, `StopItem?`, `LabelConflict?`). Rotation, dark-mode toggle, locale change and process death do not drop in-flight sheets / edit mode / assigning state.
- The DB observer is launched in `init {}` rather than `_uiState.onStart {}` so it runs for the VM's lifetime, surviving brief UI subscription gaps.
- All label mutations (`AssignLabelStop` / `CreateLabel` / `ClearLabelStop` / `DeleteLabel` / `MoveLabelToIndex`) are **optimistic**: the in-memory state updates synchronously inside `updateUiState { … }` before the IO write fires. The DB observer's later emission is a no-op in the happy path.

---

## TODO / future work

Tracked here so we can crank through the gaps without losing them.

### Compose UI tests
Covered on `test/search-stop-compose-ui-tests`:
- ✅ `pillRow_isHidden_onFreshInstallWithNoRecents`
- ✅ `pillRow_isShown_whenAtLeastOneRecentExists`
- ✅ `recentHeader_isHidden_onFreshInstall`
- ✅ `savedStop_showsRemoveFromLabelsStar`
- ✅ `tappingFilledStar_firesClearLabelStopWithMatchingLabel`
- ✅ `tappingSetPill_invokesOnStopSelectWithUnderlyingStop`
- ✅ `tappingUnsetPill_showsAssigningBanner`
- ✅ `tappingUnsetPillTwice_togglesAssigningModeOff`
- ✅ `tappingUnsetPill_doesNotInvokeOnStopSelect`
- ✅ `addLabelPill_isVisible_inIdleMode`
- ✅ `clearAllButton_firesClearRecentSearchStopsEventWithCount`
- ✅ `recentHeader_showsRecentAndClearAllLabelsWhenRecentsExist`

Outstanding (gesture-heavy or sheet-based — held back to keep CI deterministic):
- `longPressOnPill_entersEditMode` — long-press timing is flaky in Robolectric.
- `dragging_pill_in_edit_mode_fires_MoveLabelToIndex` — drag gesture racing with
  the reorderable lib's longPressDraggableHandle is not portable to Robolectric.
- `home_pill_has_no_delete_overlay_in_edit_mode` — gated on entering edit mode
  programmatically; would need a hoisted-state test surface.
- `addingDuplicateLabel_isBlockedWithInlineError` — needs ModalBottomSheet to
  open in test, which requires advancing the animation clock through to settle.
- `assigningLabel_clearsAfterStopSelected` — needs mutable searchStopState so
  the AssignLabelStop emission can flow back. Pure-rule version is covered in
  `pillRowBannerText` ("banner clears once the assigning label has been satisfied").

### ViewModel tests
Covered on `test/search-stop-vm-label-handlers`
(`SearchStopViewModelLabelHandlersTest`):
- AssignLabelStop: state + sandook + recents pinning.
- CreateLabel: append, case-insensitive duplicate skip, blank skip, surrounding-emoji
  normalisation.
- ClearLabelStop: detaches in state and DB.
- DeleteLabel: removes non-protected, preserves Home (incl. mixed case).
- MoveLabelToIndex: reorders + re-numbers sort_order; same-slot and unknown-key
  no-ops.
- observeStopLabels: empty-DB seeds defaults; populated DB mirrors into state.

### Snapshot coverage
Add `@PreviewScreen` for states we don't render today:
- `EditMode_Wiggling` (pills wiggling, ✕ visible, Done pill, Home with no ✕)
- `AssigningModeWithBanner` (banner expanded, target pill highlighted)
- `LabelConflictSheet_StopSide` (already covered as Sheet preview but not screen-level)
- `LabelConflictSheet_LabelSide` (same)
- `SaveStopAsLabelSheet_OutlinedAndSolidChips` (mix of set + unset labels)
- `AddLabelBottomSheet_DuplicateError`

### Refactor
- Replace `LabelConflict` (sealed interface inside `SearchStopScreen.kt`) with `AssignConflict` from `SearchStopRules.kt` to remove the duplicate type.
- Extract more screen logic into `SearchStopRules.kt` as it grows — anything purely state-derived belongs there.

### Documentation
- Add a short "SearchStopScreen rules" section to `CLAUDE.md` linking to this file so future agent runs know the invariants.
