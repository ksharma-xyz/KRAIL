# Saved Trips — Naming & Unfavourite UX

Design proposal for two related changes on the saved-trip card:

1. **Trip naming** — let users give each saved trip a name ("Morning Commute", "Gym Run") so
   routines are identifiable beyond their stop pair.
2. **Unfavourite UX** — replace the easy-to-mistap star with a more deliberate gesture, since
   accidental tap currently removes the trip from favourites.

> **Sibling doc:** [`LABEL_DISPLAY_PLAN.md`](LABEL_DISPLAY_PLAN.md) covers the parallel epic (
> stop-level labels showing across timetable, recents, search). The two epics touch different files
> and ship independently — they can develop in parallel.

---

## Problem

Two real, related complaints:

- **"Home → Work" doesn't scale.** Multiple Home→Work variants (different times, different changes)
  all render identically. Users can't tell which card is which without tapping in.
- **Mistap on the star.** Star is the only unfavourite affordance. It's small, lives in the corner
  of every card, and a passing tap removes the trip — the user often doesn't even notice. Recovery
  requires re-saving from scratch.

These problems share an opportunity: a more visible "this is *my* trip — Morning Commute" affordance
can also be the deliberate destructive-action target. One UI element, two jobs.

## Goals

- Each saved trip can carry an optional **name** (free text, with suggested templates).
- Every card surfaces a clear, deliberate way to **remove the trip from saved**, distinct from
  accidental taps.
- Card UX stays composable with the stop-label feature already in flight (so a card can show a trip
  name + still benefit from `🏠 Home → 💼 Work` as a subtitle).
- Card layout degrades gracefully when no name is set — no regression for users who never engage
  with naming.

## Non-goals (this epic)

- Reorder of saved trips (separate sibling epic, lands after this).
- Trip-name search/filter (out of scope per `LABEL_DISPLAY_PLAN.md` — list is small).
- Per-stop click → details sheet (different surface, separate PR after the stop-label stack).

---

## Approach A — Inline name pill (preferred)

Each card grows a small pill at the top. Mirrors the stop-label pill vocabulary already shipped on
`SearchStopScreen`.

### Mockups

**Unnamed (rest mode):**

```
┌────────────────────────────────────────────┐
│  🚆     ╭──────────────────────╮           │
│         │ + Name this trip      │           │
│         ╰──────────────────────╯           │
│                                            │
│  Central Station                           │
│        ↓                                   │
│  Town Hall Station                         │
│  [Park & Ride row — UNCHANGED]             │
└────────────────────────────────────────────┘
```

- Placeholder pill is `OutlinedButton`-style (matches `+ Add` on the search-stop pill row):
  low-emphasis, low-contrast text.
- **No ✕ in rest mode.** The card has no visible destructive affordance — that lives behind a
  long-press gesture (see Edit mode below).

**Named (rest mode):**

```
┌────────────────────────────────────────────┐
│  🚆     ╭──────────────────────╮           │
│         │ Morning Commute       │           │
│         ╰──────────────────────╯           │
│                                            │
│  Central Station                           │
│        ↓                                   │
│  Town Hall Station                         │
│  [Park & Ride row]                         │
└────────────────────────────────────────────┘
```

- Pill is filled (themed dark surface, like `SetLabelPill`). Reuses the same component family, same
  colour vocabulary.
- Tap pill body → swaps in-place to inline `TextField` (see "Editing", below).

**Edit mode (entered via long-press on any saved-trip card):**

```
┌────────────────────────────────────────────┐
│  🚆     ╭──────────────────────╮       ✕  │  ← ✕ overlay appears
│         │ Morning Commute       │  (wiggling)│
│         ╰──────────────────────╯           │
│                                            │
│  Central Station                           │
│  ...                                       │
└────────────────────────────────────────────┘
```

- Long-press *anywhere on the card body* enters edit mode for **the whole list** (every card
  wiggles, every card shows ✕ at top-right).
- Tap ✕ → unfavourites that trip immediately. **No snackbar, no confirmation** — the long-press was
  the deliberate gesture, and ✕ is invisible at rest, so accidental removal is structurally
  impossible.
- Drag a card while in edit mode → reorder (lands later as a sibling epic; the gesture is reserved
  here).
- A `Done` affordance exits edit mode. Same pattern as `SearchStopScreen`'s label-pill edit mode.
  Placement TBD — see open questions.

This is the same idiom shipped in PR #1527 / #1528 for label-pill editing. Reuse the gesture model
and the wiggle animation directly.

**Editing (inline — tap the pill body):**

```
┌────────────────────────────────────────────┐
│  🚆     ╭──────────────────────╮       ✕  │
│         │ Morning Commute│      │           │  ← TextField focused, IME up
│         ╰──────────────────────╯           │
│  ...                                       │
└────────────────────────────────────────────┘
```

The pill body swaps in-place to a `BasicTextField` with the existing name pre-filled (or empty when
starting from the placeholder). IME action `Done` saves and the pill returns to its read-only state.
Tapping outside the pill (anywhere on the screen, including elsewhere on the card) commits the
current value and dismisses the keyboard.

### Interaction model

**Rest mode:**

| User action                                 | Result                                                                                |
|---------------------------------------------|---------------------------------------------------------------------------------------|
| Tap unnamed pill (`+ Name this trip`)       | Pill swaps to inline `TextField` with focus + IME up                                  |
| Tap named pill                              | Pill swaps to inline `TextField` pre-filled with current name                         |
| IME `Done` while editing                    | Commits current text; non-empty saves the name, empty clears it (back to placeholder) |
| Tap anywhere outside the pill while editing | Same commit-and-dismiss as IME `Done`                                                 |
| Tap card body (outside pill, not editing)   | Opens timetable for the trip (today's behaviour, unchanged)                           |
| Long-press card body                        | Enters **edit mode for the whole saved-trips list**                                   |

**Edit mode (after long-press):**

| User action               | Result                                                                                                        |
|---------------------------|---------------------------------------------------------------------------------------------------------------|
| Tap ✕ on any card         | Unfavourites that trip immediately. No snackbar, no confirmation.                                             |
| Drag a card               | Reserved for future reorder (lands as separate epic).                                                         |
| Tap card body (outside ✕) | Same as rest mode — opens timetable. (Mirrors the search-stop label pill rule: tap is always primary action.) |
| Tap pill body             | Same as rest mode — swaps to inline `TextField`. Editing inside edit mode is fine.                            |
| Tap **Done**              | Exits edit mode.                                                                                              |

### Inline TextField (in place of a bottom sheet)

The pill itself hosts the editing UI — no separate component, no modal:

- **Implementation**: when the pill is tapped, swap its `Text` for the existing `taj.TextField` (
  using the hoisted-state pattern). The TextField sits inside the same pill shape, so the visual
  transition is essentially text → text-with-cursor.
- **Focus & IME**: tap requests focus on the TextField; soft keyboard opens automatically. IME
  action set to `ImeAction.Done`.
- **Commit**: IME `Done` or any tap outside the pill commits the current value. Non-empty text →
  save name. Empty → clear (revert to placeholder).
- **Cancel**: hardware back button dismisses without saving (text rolls back to the previous
  committed value).
- **Validation**: trim whitespace; max 30 chars (fits on the pill); duplicate names across trips are
  allowed — it's per-trip, not per-stop, and "Morning Commute (slow)" vs "Morning Commute" is the
  user's call.
- **Card tap-through guard**: while the TextField has focus, the card body's click handler must be
  disabled — a stray tap inside the card while editing should not navigate to the timetable.
  Re-enable on focus loss.

**Suggestions row** — deliberately deferred. Inline `TextField` doesn't have a clean place for
suggestion chips without growing the card height awkwardly. If users want suggestions, they go in a
follow-up PR (e.g. as a Gboard-style auto-complete or a small chip row that shows only while
focused). For v1, the placeholder text (`+ Name this trip`) is the only hint.

### Pros

- **Discoverable**. Every card displays a naming prompt; no hidden gestures.
- **Consistent vocabulary**. Pills, ✕, long-press edit mode are the language of `SearchStopScreen`.
  Users who learned stop labels need to learn nothing new here.
- **Mistap resistance**. ✕ is small and lives where the user has to deliberately reach. Star is
  gone.
- **Composes with stop labels**. When stops are also labelled, card subtitle reads
  `🏠 Home → 💼 Work`. Independent rendering.
- **Edit mode reuses gestures.** The same long-press → wiggle → ✕ idiom from search-stop label
  pills.

### Cons

- **Vertical space cost**. Every card grows ~40dp at the top, even when unnamed.
- **Visual noise on unlabelled cards**. The "+ Name this trip" placeholder shows on every unnamed
  trip — could feel pushy if user doesn't engage with naming.
    - Mitigation: very low-contrast placeholder; once dismissed, doesn't re-appear (a
      `hasSeenNamingPrompt` flag, dismissed by tapping anywhere or by a tiny ✕ on the placeholder
      itself). Becomes silent until user long-presses the card.
- **Long-press conflict with future reorder**. Long-press is already overloaded for label pills (
  enter edit mode) and will be for reorder of saved trips later.
    - Resolution: long-press *on the name pill* enters name-edit mode; long-press *on the card body*
      enters reorder mode (drag the whole card). Two distinct gesture targets; same gesture, no
      overlap because they're on different regions.

### Tech sketch

**Data**:

- Add `name: String?` column to the saved-trips table (Sandook). Migration: existing rows have null.
- New event: `SavedTripUiEvent.RenameTrip(tripId: String, name: String?)`. Null clears the name.
- New event: `SavedTripUiEvent.RemoveSavedTrip(tripId: String)` — replaces what star toggle did. (
  Star is gone from this surface.)

**State**:

- `Trip` data class gains `val name: String? = null`. Add nullable to keep existing call sites
  compiling.
- `SavedTripsState` unchanged structurally; existing `savedTrips: ImmutableList<Trip>` carries
  names.

**UI**:

- `SavedTripCard.kt`: add the name pill at the top, remove the star. Reuse `SetLabelPill` for the
  named-and-not-editing state, an `OutlinedButton` (chip-sized) for the unnamed placeholder. The
  editing state is the same pill shape with a `taj.TextField` inside.
- Card-internal state: `var nameEditing: Boolean` +
  `val textFieldState = rememberTextFieldState(initialName.orEmpty())`. On focus loss / IME `Done`,
  fire `RenameTrip(tripId, textFieldState.text.toString().trim().ifBlank { null })`.
- **Edit mode** (list-level): `SavedTripsScreen` owns `var editing: Boolean`. Long-press on any card
  toggles it on; tapping the Done affordance toggles it off. While `editing`, every card shows the ✕
  overlay at top-right and applies the wiggle animation. Tap ✕ fires `RemoveSavedTrip(tripId)`
  directly — no confirmation sheet, no snackbar (the long-press was the deliberate gesture).
- Reuse the wiggle animation + ✕ overlay components from `SearchStopScreen` if they're extracted;
  otherwise, mirror their implementation 1:1 to keep visual parity.
- **No bottom-sheet component**, **no snackbar host**.

**PRs (estimated):**

1. **`karan/trip-name-data`** — Trip table migration, `name: String?` on Trip, repository
   `RenameTrip` and `RemoveSavedTrip` methods + events. ~120 LOC.
2. **`karan/saved-trip-card-naming`** — card UI: name pill with inline `TextField` editing. Star
   untouched in this PR. ~180 LOC.
3. **`karan/saved-trips-edit-mode`** — list-level edit mode: long-press to enter, wiggle + ✕
   overlay, tap ✕ removes, Done exits. Star removed in this PR. ~180 LOC.

Total ~480 LOC. Splitting card-naming and edit-mode into separate PRs because the latter touches
list-level state and removes the existing star (higher review attention warranted).

---

## Approach B — Long-press menu (alternative)

Card stays close to today; all naming + favourite actions live behind a long-press context menu.

### Mockups

**Card (named or unnamed) — visually almost the same as today:**

```
┌────────────────────────────────────────────┐
│  🚆                                    ⭐  │
│                                            │
│  Morning Commute                           │  ← when named, header line
│  Central Station                           │
│        ↓                                   │
│  Town Hall Station                         │
│  [Park & Ride row]                         │
└────────────────────────────────────────────┘
```

(When unnamed, header line absent — exactly like today.)

**Long-press → context menu (modal sheet or popover):**

```
   ╭───────────────────────────╮
   │ Rename trip               │
   │ ─────────────────────────  │
   │ Remove from saved         │
   ╰───────────────────────────╯
```

(Reorder will be added here later as a third item.)

### Interaction model

| User action                | Result                                                                |
|----------------------------|-----------------------------------------------------------------------|
| Tap card                   | Opens timetable (unchanged)                                           |
| Tap star                   | Removes from saved + shows undo snackbar (with mitigation; see below) |
| Long-press card            | Opens context menu sheet                                              |
| Menu → "Rename trip"       | Opens `EditTripNameSheet`                                             |
| Menu → "Remove from saved" | Removes + undo snackbar                                               |

### Star mistap mitigation (without removing the star)

Snackbar-based undo is forbidden by the design system (no snackbars rule). Mitigation options:

- **Confirm `ModalBottomSheet`**: tap star → a sheet asks "Remove Morning Commute from
  saved? [Cancel] [Remove]". Adds friction but prevents mistaps fully. Same pattern as
  `LabelConflictSheet`.
- **Two-tap with hint**: first tap shows "Tap again to remove" inline near the star; second tap
  within 1.5s removes. No modal but extra logic.

### Pros

- **Card layout barely changes**. Today's design stays. Lower risk of regressing the visual.
- **Long-press menu scales naturally**. Adding "Reorder" later is a one-line addition.
- **Less visual clutter**. Cards aren't paying for naming UI on every render; only named cards add a
  header.
- **Smaller blast radius**. Less code touched.

### Cons

- **Naming is hidden**. New users won't discover the feature unless prompted (onboarding tile, info
  tile, etc).
- **Star stays — mistap risk persists**. Mitigated by undo, but undo is a safety net, not a fix.
  Users who don't read snackbars in time still feel the friction.
- **Different vocabulary from stop-labels**. Saved-trips uses long-press menu; search-stop uses
  inline pills + long-press to enter edit mode. Two patterns for "edit a labelled thing".
- **More taps to name a trip**. Long-press → menu → tap → sheet. Inline placeholder in A is 1-tap.

### Tech sketch

- Same data layer (Trip gains `name: String?`).
- New: `SavedTripContextMenu.kt` (small modal sheet listing Rename / Remove).
- `EditTripNameSheet` (bottom sheet for name edit — Approach B does not use inline editing).
- `SavedTripCard.kt`: add long-press handler that opens the menu; add named-header row.
- Star handler: wrap existing logic with **confirm `ModalBottomSheet`** OR two-tap detector. (
  Snackbars not allowed.)

**PRs (estimated):**

1. **`karan/trip-name-data`** — same as A. ~120 LOC.
2. **`karan/trip-name-edit-sheet`** — bottom-sheet name editor with suggestion chips + ViewModel
   events. ~180 LOC.
3. **`karan/saved-trip-card-context-menu`** — long-press menu, named header on card, confirm-sheet
   for star tap. ~180 LOC.

Total ~480 LOC.

---

## Comparison

| Concern                                | A — Inline pill + edit-mode ✕                         | B — Long-press menu + confirm sheet                           |
|----------------------------------------|-------------------------------------------------------|---------------------------------------------------------------|
| Naming discoverability                 | high — visible prompt on every card                   | low — must long-press to find Rename                          |
| Mistap risk for unfavourite            | **none structurally** — ✕ invisible at rest           | medium — star stays, mitigated only by confirm-sheet friction |
| Vertical space per card                | +40dp always (the name pill row)                      | +20dp when named only                                         |
| Visual noise on unnamed cards          | medium (low-contrast placeholder)                     | none                                                          |
| Visual consistency w/ stop-label pills | high — same edit-mode + ✕ + Done idiom                | low — context menu is a new pattern in the app                |
| Tap count to name a trip               | 1 tap → type → IME `Done`                             | 2 taps + sheet → save                                         |
| Tap count to unfavourite               | long-press → 1 tap on ✕ → done                        | 1 tap on star → confirm sheet → confirm                       |
| Future reorder integration             | folds into existing edit mode (drag inside edit mode) | needs separate "Reorder" menu item or distinct gesture        |
| Implementation surface area            | medium (~480 LOC, 3 PRs, mostly UI)                   | medium (~480 LOC, 3 PRs, sheet + menu + confirm)              |
| Risk of visual regression              | medium (card layout reflows)                          | low (card stays close to today)                               |
| Snackbar dependency                    | none                                                  | none (confirm sheet replaces snackbar)                        |

---

## Recommendation

**Approach A**, with two qualifiers below.

Why A:

1. **The two problems are deeply linked.** The user's report ("easy to mistap star") and ("can't
   tell my trips apart") share the same root cause: the card has no deliberate identity and no
   deliberate destructive action. Approach A solves both: the named pill is the identity, the
   long-press → ✕ pattern is the deliberate destructive action. Mistaps are *structurally*
   impossible because ✕ is invisible at rest.
2. **Consistency.** We already ship `SearchStopScreen`'s pill + edit-mode + ✕ idiom for label-pill
   management. Saved-trip cards adopting the exact same gesture vocabulary keeps the user's mental
   model intact across the app. Approach B introduces a context menu — a third pattern with no other
   home in the codebase.
3. **Discoverability.** Naming trips is a *core* feature of this epic; if users don't find it, the
   work is wasted. Approach A makes the prompt visible-but-low-contrast — present enough to
   discover, quiet enough to ignore. Approach B requires content marketing (info tile, intro screen)
   to be discovered.
4. **No snackbars needed.** Approach A's edit-mode gating means we never need an undo affordance —
   accidental removal can't happen, so there's nothing to undo. Approach B sidesteps the snackbar
   ban with confirm sheets, but that adds friction on every legitimate removal.

Qualifiers:

- The placeholder on unnamed cards must be **genuinely low-contrast** (alpha 0.4–0.5 of `softLabel`)
  so it doesn't dominate. If user testing shows it still feels pushy, add a per-card "✕ this prompt"
  affordance that hides the placeholder permanently for that trip (one tap, persisted).
- Inline `TextField` requires the card-body click handler to be disabled while the field has focus,
  otherwise a stray tap inside the card during editing would navigate to the timetable. Critical to
  get right; mirror the keyboard-handling pattern from `AddLabelBottomSheet` (post-fix, hoisted
  state).

If A turns out wrong (e.g. on-device review reveals the cards feel bloated, or inline editing is
fiddly), B is a graceful fallback — the data layer is the same, only the UI changes.

---

## Open questions

1. **Where does the `Done` affordance go for exiting edit mode?** Options: (a) replace the screen's
   title with a "Done" text button while editing; (b) a floating button at the bottom of the
   screen; (c) an "always-visible" Done button on top-right of the screen header. `SearchStopScreen`
   puts Done in the trailing slot of the pill row — saved-trips list doesn't have an equivalent.
   Lean (a) for minimum new chrome, but worth seeing on-device.
2. **Should the placeholder pill on unnamed cards be dismissable?** A user who never wants to name
   their trips might find the constant prompt noisy. Two choices: (a) always show — the prompt is
   the discovery mechanism; (b) per-card "✕ this prompt" persistence. Lean (a) for v1; revisit if
   feedback says it's pushy.
3. **Where does the trip name come from on a brand-new save?** Two choices: (a) trip is saved as
   unnamed, user names later; (b) save flow auto-focuses the inline `TextField` on the new card.
   Lean (a) — don't shove a keyboard at the user immediately after save.
4. **Suggestions for trip names** (Approach A). Deferred. Inline TextField doesn't have a clean
   place for suggestion chips without growing the card height. If we want them later: (a)
   Gboard-style auto-complete row above the keyboard, (b) a small chip row that appears only while
   the TextField has focus. Decide when usage shows the need.
5. **Card-tap-through guard during inline editing.** While the TextField inside the pill has focus,
   the card-body click handler must be disabled — a stray tap inside the card while editing should
   not navigate to the timetable. Critical to get right. Plan: gate the `klickable` on
   `!nameEditing`.

---

## Decisions (locked)

- **Approach A** (inline pill + edit-mode ✕) is the chosen direction.
- **Named pill at top of saved-trip card.** Reuses `SetLabelPill` visual vocabulary for the
  read-only state.
- **Star is removed** from the saved-trip card. Unfavourite happens via edit-mode ✕.
- **✕ at card top-right is visible only in edit mode**, never at rest. Position: top-right corner of
  the card.
- **Inline `TextField` for naming** — no bottom sheet, no separate component. Tap pill → field gains
  focus → IME up → IME `Done` or outside-tap commits.
- **Long-press card body** enters list-level **edit mode**: every card wiggles, ✕ appears at
  top-right of every card. Mirrors `SearchStopScreen` label-pill edit mode.
- **Tap ✕ in edit mode** → unfavourite immediately. **No confirmation, no snackbar.** The long-press
  is the deliberate gesture; edit-mode-only ✕ makes mistaps structurally impossible.
- **Done** affordance exits edit mode (placement TBD — see open question 1).
- **Reorder gesture lives inside the same edit mode** as a future addition (drag a card while in
  edit mode). Not in this epic, but the gesture region is reserved.
- **Tap card body** (outside pill / ✕, in either mode) → opens timetable, unchanged.
- **No snackbars anywhere in this feature.** Per the taj design system rule.
- **Stop-label rendering on the saved-trip card is still deferred** — it lands inside the name
  pill's subtitle if / when we decide to compose the two features. Not in scope for this epic's
  first PR.

---

## TODO / future work

- Confirm naming sheet copy and suggestion list (esp. day-of-week heuristics).
- Confirm undo snackbar copy and timeout.
- Decide: dismissable placeholder (per open question 1)?
- Add user-research/testing pass after PR 3 merges and before adding reorder.
- Reorder epic plan (`SAVED_TRIPS_REORDER_PLAN.md`) — sibling document; depends on the data layer
  this epic establishes.
