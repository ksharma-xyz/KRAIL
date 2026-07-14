# Address and place labels

## Status

Exploratory design only. This document proposes the follow-up needed to let a
user assign an existing label (for example, Home or Work) to an NSW address or
POI result. It makes no runtime or schema change.

## Current state

Address/POI results can be selected and stored as recent locations, but they
render with `AddressSearchListItem`. That row has no label-assignment affordance.
Transit stops render with `StopLabelAssignRow`, which supplies the inline `+`,
unset-label choices, and "+ New label" flow.

The `StopLabels` database table already stores an opaque `stop_id` and has no
foreign key to GTFS. Technically, a `streetID:…` or `poiID:…` could be saved in
that column today. It is not sufficient to do so: labels currently retain only
an ID and display name, so a label cannot restore the address/place metadata
needed to render and select it consistently after process death.

## Recommended model

Evolve a label from a transit-stop shortcut to a location shortcut. Preserve
the existing `stop_id` and `stop_name` column names for compatibility, but treat
them as opaque location ID and display name in code.

Add these nullable/compatible fields to `StopLabels` in the next available
SQLDelight migration:

| Field | Values | Purpose |
|---|---|---|
| `location_kind` | `TRANSIT_STOP`, `ADDRESS` | Selects the row and navigation behaviour. |
| `address_type` | NSW address/POI type, null for stops | Lets a saved address keep its address-style presentation. |

Existing labels migrate to `TRANSIT_STOP` with a null `address_type`. No label
data is dropped. `product_classes` are not required for labels: they are only a
transit-row icon concern and the existing label shortcut does not store them.

The Kotlin `StopLabel` model and `toStopItem()` must carry `locationKind` and
`addressType`, matching the location metadata already used by the search
selection path. Avoid inferring type from `streetID:`/`poiID:` prefixes.

## UI and interaction changes

1. Give an address/POI result the same assignment entry point as a transit stop:
   an inline `+` when unassigned, the unset-label choices, and "+ New label".
   Reuse the assignment state machine; do not create a second label flow.
2. Build a location-aware assignment row or extract the assignment controls from
   `StopLabelAssignRow`. The address row should retain its address icon/type and
   should not show transit-mode icons just to share the component.
3. When assigning, pass the complete `StopItem` to the ViewModel. Persist its
   opaque ID, display name, `locationKind`, and `addressType` to the label.
4. When a set label is tapped from the shortcut row, reconstruct that complete
   `StopItem` and send it through the existing From/To selection path.
5. In Manage Labels, show an address-style secondary description/type for an
   address label. Existing transit labels remain visually unchanged.

The existing invariant remains: one label maps to one location and a displayed
location can be assigned only to an unset label. Moving a label still uses the
current Remove-then-assign flow.

## Persistence work

- Add the SQLDelight migration and current-schema columns.
- Extend generated-query adapter calls, `Sandook`, `RealSandook`, and every
  `Sandook` fake/test implementation.
- Map database rows to the richer `StopLabel` model in every consumer, including
  trip planner and timetable code that observes labels.
- Ensure the existing "assigning a location pins it to recents" behaviour calls
  the unified recent-location upsert with the address metadata.

No network call is needed when rendering or selecting a saved label. The label
must retain enough local metadata to work after the original search result has
gone away.

## Analytics and privacy

Do not create a new event name for label assignment. If product measurement is
needed, extend the existing label-assignment/selection event with the bounded
`location_kind` and allowlisted `address_type` properties described in
`ADDRESS_SEARCH_OBSERVABILITY_AND_ROLLOUT.md`. Never send an address display
name, raw query, or opaque address ID as a new property. Apply the analytics
event checklist and registry gate before changing analytics code.

## Test plan

- SQLDelight migration test: an existing transit label becomes
  `TRANSIT_STOP`/null-address-type with its name and ID preserved.
- Sandook tests: address label upsert/read round-trips its opaque ID, display
  name, kind, and address type.
- ViewModel tests: assigning an address updates optimistic state, persistence,
  and recents; selecting the label reconstructs an address `StopItem`.
- Compose tests: an address result exposes the assignment control, can assign an
  existing unset label, can create a new label, and later renders as assigned.
- Regression tests: transit label behaviour, protected Home rules, ordering,
  remove/reassign, and transit-only icons remain unchanged.

## Acceptance criteria

- A selected street or POI can be assigned to Home, Work, or a new label.
- The label survives restart and remains selectable as the same address/place.
- Existing saved transit labels remain intact after upgrade.
- Address labels do not trigger an address API request when displayed or tapped.
- No raw address/query/opaque ID is added to analytics or logs.
