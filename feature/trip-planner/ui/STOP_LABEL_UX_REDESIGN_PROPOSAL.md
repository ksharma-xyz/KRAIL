# Stop-label UX redesign — v4 (external review incorporated)

Status: **shipped.** ViewModel/navigation wiring landed (`ManageStopLabelsEntry.kt`,
`ManageStopLabelsRoute`); the v2 `ManageStopLabelsSheet.kt` referenced below has been
deleted. See `SEARCH_STOP_UX.md` for the current, up-to-date description of what's
live — this doc is kept as a historical design record. v4 folds in an external UX
review pack
(`~/Downloads/krail-review/KRAIL Stop Labels - Review Pack.md` + `Stop Label Flows
v4.html`, journeys J1–J4, J6–J8, EX) on top of the v3 draft below — the "what changed
v2 → v3" section is kept for history; **what changed v3 → v4 is the part that matters
now.**

Component files for this version, as shipped:
- `components/AssignToLabelIcon.kt` — the "+" icon (bare rotating chevron; the "✓"
  described in the original mock was dropped — pills carry that signal instead)
- `searchstop/StopLabelAssignRow.kt` — the expand-in-place row (replaces the v2
  "Assign sheet"). **Note:** the EX collapsed-assigned state below describes
  "Change label"/"Remove" text actions on an already-assigned row — that part did
  **not** ship; an assigned row is fully locked (no expand, no actions). Reassignment
  only happens via Manage's Remove + a fresh assign elsewhere.
- `components/AssignNewLabelSheet.kt` — simplified new-label sheet (only for genuinely
  creating a brand-new label)
- `components/ConfirmLabelActionSheet.kt` — restyled confirm sheet, shared by Remove
  and Delete (the Replace mode described below did not ship — see EX note above)
- `managestoplabels/ManageStopLabelRow.kt` — expand/collapse instead of pencil + kebab
  menu; adds the "Remove assignment" action
- `managestoplabels/ManageStopLabelsScreen.kt` — full-screen route
  (`ManageStopLabelsRoute`), wired via `ManageStopLabelsEntry.kt`. v2's
  `ManageStopLabelsSheet.kt` (old bottom-sheet Manage design) has been deleted.

## What changed v2 → v3 (history, unchanged from the original proposal)

v2 still had a small "Assign sheet" (bottom sheet listing labels) plus a separate
"+New label" inline-expand inside it, plus a full-screen Manage with pencil+kebab
per row. Feedback: assigning still routed through a sheet at all, and pencil+kebab
side-by-side was two different-looking buttons for one concept.

**v3 principle: one interaction model everywhere — tap a row, it expands in place to
show what you can do.** Matches `JourneyCard` / `TripSearchListItem`'s existing
expand/collapse behaviour exactly (`animateContentSize`), so nothing new is being
taught to users of this app; it's the same gesture they already know.

## What changed v3 → v4 (external review pack)

The review pack pressure-tested the v3 core model (label vs. stop, solid/outline pill
states, Assign/Replace/Remove/Delete verbs) and came back with four decisions plus
three concrete gaps. All are resolved in code now:

**Decisions (confirmed, no further debate needed):**
1. **Two-state pills, no tick** — ship as-is. Tapping a pill set elsewhere opens the
   Replace sheet, which names the origin stop explicitly, so confusion self-corrects
   at tap time rather than needing a third visual state.
2. **No snackbars** on assign/replace/remove — stays silent. Matches the existing
   `CLAUDE.md` rule (no snackbars in taj); the pill flipping solid/outline is the
   confirmation.
3. **Single-letter mode roundel** (T/M/L/B/F/C) instead of line codes — already what
   `TransportModeIcon` renders (`NswTransportConfig.nameFor(mode).first()`), so this
   was a no-op confirmation, not new work.
4. **EX collapse-wall** — build it. An already-assigned stop no longer shows the full
   label wall by default; see below.

**Gaps closed:**
1. **Replace sheet needed a third option.** J3's mock shows Cancel / Set-here / a
   middle **Remove assignment** action — take the label off its *current* stop
   without also setting it here, instead of forcing a straight move.
   `ConfirmLabelActionSheet` now takes optional `secondaryText`/`onSecondary` for
   this. Layout (per a later round of feedback): "Remove assignment" renders as a
   plain `TextButton` **above** the main decision, then Cancel and the primary action
   (e.g. "Set Home here") sit **side by side in one row** below it — the escape
   hatch reads as secondary, the real choice (Cancel vs. commit) reads as one unit.
2. **Manage row had no "Remove assignment" action.** Only Save + Delete existed.
   `ManageStopLabelRow`'s expanded content now stacks three actions top-to-bottom:
   **Save** (monochrome, primary) → **Remove assignment** (errorContainer-tinted,
   only when the label has a stop) → **Delete label** (subtle, error text, only when
   not protected).
3. **Delete had zero confirmation.** `onDeleteLabel` fired straight through with no
   sheet. Now reuses `ConfirmLabelActionSheet` (title "Delete Gym?", body "Bondi
   Junction will no longer be labelled Gym.", Cancel/Delete) — wiring this into
   `ManageStopLabelsScreen`/its future ViewModel is part of PR2 below.

## Flow (v4)

### Assigning a stop (from search results, saved trips, anywhere a stop row shows)

1. Row shows "+" (unassigned) or "✓" (already on some label). Tap → row expands.
2. **Unassigned, expanded:** header "Save this stop as", every label shown outlined.
   - Outlined pill → tap assigns this stop to it immediately, no confirmation.
   - Solid pill (set on a *different* stop) → tap opens `ConfirmLabelActionSheet` in
     Replace mode: title "Move Home to Central Station?", primary "Set Home here",
     secondary text button "Remove assignment", Cancel.
   - Trailing "+ New label" pill → opens `AssignNewLabelSheet` (title "Name this
     stop", stop name shown underneath, text field, "Save"). Confirming creates the
     label AND assigns this stop to it in one step.
3. **Assigned, collapsed (EX-B):** instead of falling back to a bare icon, the row
   shows "This stop is saved as" + the current label as a solid pill, plus two text
   actions: **Change label** (expands the wall, current label sorted first, header
   "Change to") and **Remove** (opens `ConfirmLabelActionSheet` in Remove mode: title
   "Remove Home from Central Station?", body "The Home label stays — you can point it
   at another stop later.", primary "Remove from this stop", destructive-tinted).
4. Tap "+"/"✓" again (or tap elsewhere) → collapses.

Showing **all** labels (not just unset ones) in the expanded/reassign view is
deliberate — it's how reassignment happens.

### Managing existing labels (rename, delete, reorder)

Full screen (`ManageStopLabelsScreen`), dedicated screen not a sheet (Google Maps
"Your Places" / Uber Settings > Saved Places shape). Each row:
- Tap the row → expands to a rename field + up to three stacked actions: Save,
  Remove assignment (label has a stop), Delete label (not protected).
- **Reorder is a separate top-bar toggle** ("Reorder" / "Done"), not part of the
  per-row expand — dragging and tapping-to-rename would otherwise fight each other.

This screen's job is purely the **label's own metadata** now — name, whether it
exists, its position, and (new in v4) whether it still points at a stop. Assigning
a stop to a label happens inline wherever that stop is shown, not here.

## Copy (v4 pass — see review pack table for the "why" on each)

| Where | Copy |
|---|---|
| J1 expand header | "Save this stop as" |
| J2 eyebrow | "Name this stop" |
| J2 placeholder | "e.g. Home, Gym, School…" |
| J2 button | "Save" |
| J3 title | "Move Home to Central Station?" |
| J3 secondary | "Remove assignment" |
| J4 title | "Remove Home from Central Station?" |
| J4 body | "The Home label stays — you can point it at another stop later." |
| J4 primary | "Remove from this stop" |
| J6/Manage rename button | "Save" |
| Manage remove button | "Remove assignment" |
| J7 title | "Delete Gym?" |
| J7 body | "Bondi Junction will no longer be labelled Gym." |
| Manage "not set" | "Not set" |

## Scope if approved (2 stacked PRs, unchanged shape from v3)

- **PR1**: `StopLabelAssignRow` (incl. EX collapse) replaces the star on
  stop-search-result rows; `AssignNewLabelSheet` + `ConfirmLabelActionSheet` (Replace
  + Remove modes) land; old star/sheet flow and choose-mode removed entirely.
- **PR2** (stacked): full-screen `ManageStopLabelsRoute` +
  `ManageStopLabelsViewModel` (reads/writes `Sandook` directly, no dependency on
  `SearchStopViewModel`), using the new expand/collapse `ManageStopLabelRow` incl.
  Remove-assignment, and wiring `ConfirmLabelActionSheet`'s Delete mode in front of
  `DeleteLabel`.

Full file-by-file implementation plan (Sandook API, `ResultEventBus` gotchas, the
already-dead `SearchStopFieldType.LABEL` plumbing to revive) is unchanged from the
prior version and still in the session's plan file:
`~/.claude/plans/gentle-greeting-hollerith.md` — worth a re-read once wiring starts,
since it predates v4 and doesn't yet know about the Replace-sheet third option or the
Manage-row Remove-assignment action.
