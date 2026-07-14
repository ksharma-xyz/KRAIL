# Stop-label analytics plan

## Status

Exploratory design only. This document defines the measurement model for the
stop-label lifecycle: creation, assignment, removal, deletion, renaming, and
reordering. It makes no analytics or runtime change.

Read `docs/ANALYTICS_EVENTS.md` and register every approved parameter in the
KRAIL-Analytics `EVENT_REGISTRY.md` before implementation.

## Product questions

1. Do people turn labels into useful shortcuts by assigning a location?
2. Which available surface leads to assignment: a search result, a recent, an
   empty-state stop, or a future address/place result?
3. Do people create a new label at the moment of assignment, or use Home/Work
   and other existing empty labels?
4. Is Manage Labels used to clear, delete, rename, or reorder shortcuts?
5. Do users use the resulting label shortcut to select From/To later?

The plan intentionally does **not** measure every expand/collapse, keystroke,
sheet opening, or confirmation dismissal. Those are interaction noise unless a
specific UX decision requires them.

## Current instrumentation and gaps

Four event names already exist:

| Existing event | Current lifecycle action | Gap |
|---|---|---|
| `stop_label_created` | New custom label saved | Carries raw label text and emoji; no creation surface. |
| `stop_label_stop_assigned` | Location assigned to a label | Uses obsolete `choose_mode`/`star_sheet` source values and sends raw stop ID/name. |
| `stop_label_removed` | Clear assignment or delete label | Does not identify the action surface. |
| `stop_label_reordered` | Label moved | Fires from `MoveLabelToIndex`, which currently runs per drag swap rather than once per completed drag. |

Rename currently has no event. Manage Labels reuses `SearchStopViewModel`, so
clear/delete/reorder already reach the existing event handlers. Assignment comes
from `StopLabelAssignRow`, used by search results, recents, and empty-state
stops; creating a label in its sheet produces a create followed by assignment.

## Privacy boundary

Do not add raw label names, emojis, location display names, NSW IDs, search
queries, or address/POI IDs to label analytics. A custom label or an address can
identify a home, workplace, or person.

Before extending the events, review the existing raw `labelName`, `stopId`, and
`stopName` properties. The recommended follow-up replaces those properties for
new rows with bounded classifications; it does not create a second event solely
to duplicate the same action with safer fields. Any compatibility decision for
historical dashboards belongs in the analytics review.

## Recommended event model

Reuse the four existing event names. Add bounded parameters only where they
answer a product question.

| Event | Fire once when | Recommended bounded parameters |
|---|---|---|
| `stop_label_created` | A new custom label is successfully persisted | `creation_surface = search_result \| recent \| empty_state \| address_result`; `label_count_bucket = 1 \| 2 \| 3_5 \| 6_plus` |
| `stop_label_stop_assigned` | A location is successfully assigned to a label | `assignment_surface`; `assignment_mode = existing_label \| new_label`; `location_kind = transit_stop \| address`; `label_kind = protected_default \| custom`; `is_reassignment` |
| `stop_label_removed` | An assignment is cleared or a label is deleted in Manage Labels | existing `action = clear \| delete`; `surface = manage_labels`; `label_kind`; `had_assignment` |
| `stop_label_reordered` | A drag is released and its final order differs | `surface = manage_labels`; `label_kind`; `move_distance_bucket = 1 \| 2_3 \| 4_plus`; `set_label_count_bucket` |

`assignment_surface` values are:

| Value | Meaning |
|---|---|
| `search_result` | Local transit-stop search result row. |
| `recent` | Recent location row. |
| `empty_state` | First-open curated stop row. |
| `address_result` | NSW address/POI result, when address labels ship. |

Use `new_label` for the assignment that immediately follows the New Label sheet;
the creation and assignment are two distinct user outcomes, so retaining both
events is not double counting.

## Rename decision

Do not add a rename event in the first analytics pass. It does not answer one
of the core shortcut-adoption questions, and it risks collecting user-authored
text. Revisit only if there is a concrete decision, such as determining whether
the Manage Labels naming UI is confusing.

If that decision is approved, a single new `stop_label_renamed` event is
justified only after the 500-name checklist: rename is a distinct completed
intent with no current event/parameter set. It must contain only
`surface = manage_labels` and `label_kind`; never old or new label text.

## Manage Labels screen measurement

First verify whether the app's standard screen-view tracking records
`ManageStopLabelsScreen`. If it does not, add it to the existing screen-view
taxonomy rather than minting a `manage_labels_open` click event. Do not fire
both an open click and the destination screen view for the same navigation.

Manage actions map as follows:

| User action | Event decision |
|---|---|
| Expand/collapse a row | Do not track. |
| Save a rename | No event in the initial plan. |
| Remove assignment | `stop_label_removed(action = clear, surface = manage_labels)`. |
| Confirm delete | `stop_label_removed(action = delete, surface = manage_labels)`. Do not fire for opening or cancelling the confirmation sheet. |
| Reorder mode toggle | Do not track. |
| Drag reorder | One `stop_label_reordered` only after a completed changed drag. |

## Implementation sequence

1. Write the analytics contract and obtain registry/privacy approval before
   touching `AnalyticsEvent.kt`.
2. Introduce typed enums for every bounded value; do not pass free-form strings
   from composables.
3. Carry assignment surface and mode in `SearchStopUiEvent.AssignLabelStop`.
   Set them at the UI boundary, where the source row is known.
4. Replace legacy assignment source constants in a backwards-compatible query
   plan; do not reinterpret historical `choose_mode`/`star_sheet` rows as the
   new surfaces.
5. Change reorder tracking to occur on drag completion. Retain the final order
   in UI state or add an explicit drag-end callback; do not emit once per swap.
6. For address-label work, add only `location_kind = address` and
   `assignment_surface = address_result`; never address text or ID.

## Test and validation plan

- Unit-test the event payload for each allowed surface, assignment mode, label
  kind, and location kind.
- Verify the new-label flow emits exactly one create and one assignment event.
- Verify a direct assignment emits one assignment event and no create event.
- Verify clear/delete emit exactly one removal event only after the state change;
  delete cancellation and protected-Home no-ops emit none.
- Verify a drag across several rows emits one reorder event with final movement,
  not one per swap.
- Verify no payload contains user-entered label text, location display text,
  address ID, stop ID, or search query.
- Validate the final event/parameter list against `EVENT_REGISTRY.md` and the
  Firebase 25-parameter-per-event limit before merge.
