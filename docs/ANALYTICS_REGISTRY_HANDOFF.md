# Analytics registry handoff — pending KRAIL-Analytics registrations

Living ledger of event/param changes shipped in this repo that still need registering
in the **KRAIL-Analytics** repo's `docs/EVENT_REGISTRY.md` — confirmed path, see its
"Params registry" section. This is not a plan or a proposal doc — every row here is
already merged to `main`. Keep it accurate the same way `feature/trip-planner/ui/SEARCH_STOP_UX.md`
and similar per-feature docs stay accurate: **update it in the same PR** that changes
`AnalyticsEvent.kt`, not after the fact.

## How to use this file

**Adding an entry** — when a PR adds or changes a param/event in `AnalyticsEvent.kt`:
1. Add a row to the table below with `Status = Pending`.
2. Fill in the PR link and merge date once merged.
3. Leave `Owner story link` blank if there's no ticket yet — don't block the PR on it.

**Clearing an entry** — once KRAIL-Analytics confirms the row is registered, flip
`Status` to `Registered`. Don't delete the row; it's a useful audit trail and this
table will never be large (Firebase caps the app at 500 event names, ever — see
`docs/ANALYTICS_EVENTS.md`).

**New event name vs new param on an existing event** — both go in this ledger the same
way, distinguished by the `Event` column. Read `docs/ANALYTICS_EVENTS.md` before adding
either; most changes should be params on an existing event, not a new name.

## Pending / registered

| Date | Event | Param(s) | Type / values | Trigger | PR | Status | Owner story |
|---|---|---|---|---|---|---|---|
| 2026-07-14 | `stop_selected` | `locationKind` | `transit_stop｜address` (default `transit_stop`) | A stop, address, or POI is selected from search results, recents, empty-state, or trip-stop click | [#1711](https://github.com/ksharma-xyz/KRAIL/pull/1711) | Registered | — |
| 2026-07-14 | `stop_selected` | `addressType` | Allowlisted `singlehouse｜street｜poi｜unknown`; omitted for transit stops | Same as above, only present when `locationKind = address` | [#1711](https://github.com/ksharma-xyz/KRAIL/pull/1711) | Registered | — |
| 2026-07-15 | `search_stop_query` | `queryLength` | Int, character count of typed query | Settled search query resolves (success or error) | [#1715](https://github.com/ksharma-xyz/KRAIL/pull/1715) | Registered | — |
| 2026-07-15 | `search_stop_query` | `searchSessionId` | Random hex string per settled query; joins to `stop_selected` | Same as above | [#1715](https://github.com/ksharma-xyz/KRAIL/pull/1715) | Registered | — |
| 2026-07-15 | `search_stop_query` | `query` (semantics change) | Raw text now sent ONLY under the zero-result carve-out: zero results everywhere, no digits, ≤ 25 chars (`SearchQueryAnalyticsRedaction`). Previously sent on every firing | Zero-result fuzzy-diagnostics carve-out only | [#1715](https://github.com/ksharma-xyz/KRAIL/pull/1715) | Registered | — |
| 2026-07-15 | `stop_selected` | `searchQuery` (REMOVED) | Param deleted: carried raw typed text, which can be a street address (privacy policy promises no PII in analytics) | No longer fires | [#1715](https://github.com/ksharma-xyz/KRAIL/pull/1715) | Registered | — |
| 2026-07-15 | `search_stop_query` | `resultSource` | `local｜address` (default `local`) | New second firing per settled query from the address pipeline on fetch completion (cache hits excluded); join firings on `searchSessionId` | [#1716](https://github.com/ksharma-xyz/KRAIL/pull/1716) | Registered | — |
| 2026-07-15 | `stop_selected` | `searchSessionId` | Random hex string; joins to `search_stop_query` firings | Only when selection happens with a live (non-blank) query; recents/map picks omit it | [#1717](https://github.com/ksharma-xyz/KRAIL/pull/1717) | Registered | — |
| 2026-07-15 | `stop_selected` | `displayedLocalCount` | Bucket `0｜1_3｜4_10｜11_plus` | Local results on screen at selection time; omitted without a live query | [#1717](https://github.com/ksharma-xyz/KRAIL/pull/1717) | Registered | — |
| 2026-07-15 | `stop_selected` | `displayedAddressCount` | Bucket `0｜1_3｜4_10｜11_plus` | Address/POI results on screen at selection time; omitted without a live query | [#1717](https://github.com/ksharma-xyz/KRAIL/pull/1717) | Registered | — |
| 2026-07-15 | `stop_label_created` | `creationSurface`, `labelCountBucket`; REMOVED `labelName`/`emoji`/`totalLabelsCountAfter` | `search_result｜recent｜empty_state｜address_result`; bucket `1｜2｜3_5｜6_plus` | New custom label persisted; raw label text dropped (privacy) | [#1719](https://github.com/ksharma-xyz/KRAIL/pull/1719) | Registered | — |
| 2026-07-15 | `stop_label_stop_assigned` | `assignmentSurface`, `assignmentMode`, `locationKind`, `labelKind`; REMOVED `labelName`/`stopId`/`stopName`/`source` | Surfaces as above; `existing_label｜new_label`; `transit_stop｜address`; `protected_default｜custom`. Historical `source` values (`choose_mode｜star_sheet`) refer to deleted v2/v3 flows, do not reinterpret | Location pinned to a label; raw stop identity dropped (privacy) | [#1719](https://github.com/ksharma-xyz/KRAIL/pull/1719) | Registered | — |
| 2026-07-15 | `stop_label_removed` | `labelKind`, `surface`; REMOVED `labelName` | `protected_default｜custom`; `surface` always `manage_labels` | Assignment cleared or label deleted in Manage Labels | [#1720](https://github.com/ksharma-xyz/KRAIL/pull/1720) | Registered | — |
| 2026-07-15 | `stop_label_reordered` | `labelKind`, `moveDistanceBucket`, `setLabelCountBucket`, `surface`; REMOVED `labelName`/`previousIndex`/`newIndex`/`totalCount` | Distance `1｜2_3｜4_plus`; set-count `1｜2｜3_5｜6_plus`; `manage_labels`. NOW FIRES ONCE PER COMPLETED CHANGED DRAG - historical rows fired per swap, counts inflated, not comparable | Drag released in Manage Labels with a different final order | [#1720](https://github.com/ksharma-xyz/KRAIL/pull/1720) | Registered | — |
| 2026-07-15 | `view_screen` | `name = ManageStopLabels` (new value) | Existing screen-view event, new screen name | ManageStopLabelsScreen becomes visible (once per entry, rotation-safe) | [#1720](https://github.com/ksharma-xyz/KRAIL/pull/1720) | Registered | — |

Registered in KRAIL-Analytics `docs/EVENT_REGISTRY.md`'s "Params registry" table,
2026-07-14.

## Backfill: events that shipped before this ledger existed

This ledger starts 2026-07-14. Sixteen event names (plus one new param on an existing
event) shipped before that date and were never registered on the KRAIL-Analytics side, so
they arrived in BigQuery with no label and no registry row — invisible to every dashboard
that keys off the registry. Found by diffing `krail_defined_events` (parsed from
`AnalyticsEvent.kt` by KRAIL-Analytics `sync-krail.ts`) against `EVENT_REGISTRY.md`.

All rows below were registered in KRAIL-Analytics `docs/EVENT_REGISTRY.md` and
`dashboard/lib/eventLabels.ts` on 2026-07-22 (KRAIL-Analytics#2). Kept for the audit
trail. That same change added a `defined-but-unregistered` check which now runs daily and
fails the analytics build, so this class of gap cannot silently reopen.

| Shipped | Event | Param(s) | Type / values | Trigger | Status |
|---|---|---|---|---|---|
| 2026-07-07 | `save_trip_prompt_shown` | `variant` | `plain｜commute` | "Save this trip?" prompt shown on the timetable for an unsaved origin-destination pair | Registered |
| 2026-07-07 | `save_trip_prompt_action` | `accepted`, `variant`, `dismissCount` | Bool; `plain｜commute`; Int, dismissals for this OD pair including this one, always 0 when `accepted` — prompt stops at 2 | User accepted or dismissed the save prompt (one event for both outcomes) | Registered |
| 2026-07-07 | `save_trip_click` | `source` (new param) | `star｜prompt`; historical rows carry none — treat null as `star` | Existing event, now distinguishes title-bar star from the prompt | Registered |
| 2026-06-04 | `retry_api` | `source` | `timetable` | User taps Retry after an API/load failure; unified across surfaces rather than one event per screen | Registered |
| 2026-05-16 | `no_entries_detected` | `topLevelRoute` | Simple class name of the active top-level route | Nav back stack produced zero entries and the `NoEntriesUI` fallback appeared. **Bug canary — should be silent; it is not.** See "Open items" below | Registered |
| 2026-05-03 | `saved_trip_card_reordered` | `fromStopId`, `toStopId`, `previousIndex`, `newIndex`, `totalCount` | Stop IDs; Ints | Saved-trip card reordered by drag in edit mode | Registered |
| 2026-05-03 | `timetable_stop_header_click` | `stopId`, `stopName`, `isOrigin`, `tripFromStopId`, `tripToStopId`, `action` | `action` = `edit_search｜open_departures`; historical rows carry no `action` and were departures opens, so the sheet's full timeline is `action IS NULL OR action = 'open_departures'` | Stop header tapped inside a timetable | Registered |
| 2026-04-19 | `dep_board_show_previous` | `stopId`, `show`, `source` | Bool `show` (`true` = opened); `DepartureBoardSource` | "Show / hide previous departures" panel toggled | Registered |
| 2026-02-22 | `search_stop_map_options_opened` | — | — | Options button tapped on the map (SearchStopMap only) | Registered |
| 2025-09-20 | `clear_recent_search_stops` | `recentSearchCount` | Int | Recent-searches list cleared | Registered |
| 2025-08-31 | `info_tile_interaction` | `key`, `expand`, `dismiss`, `cta_click` | Tile key; optional bools; CTA URL. Each optional param present only when that interaction happened | Info tile expanded, dismissed, or its CTA tapped | Registered |
| 2024-12-24 | `from_field_click` | — | — | From field tapped on the Save Trips screen | Registered |
| 2024-12-24 | `to_field_click` | — | — | To field tapped on the Save Trips screen | Registered |
| 2024-12-19 | `delete_saved_trip_card_click` | `fromStopId`, `toStopId` | Stop IDs | Saved trip deleted | Registered |
| 2024-12-17 | `back_click` | `fromScreen` | `AnalyticsScreen` name | Back navigation from a tracked screen | Registered |
| 2024-12-17 | `reverse_stop_click` | — | — | From/To stops swapped on the Save Trips screen | Registered |
| 2024-12-17 | `reverse_time_table_click` | `fromStopId`, `toStopId` | Stop IDs | Trip reversed from the timetable | Registered |

## Open items for KRAIL-Analytics maintainers

- ~~`EVENT_REGISTRY.md` exact path unconfirmed~~ — **Resolved**: it already existed at
  `docs/EVENT_REGISTRY.md`, and now has a "Params registry" table added specifically
  for this handoff.
- **`park_ride_card_click` loses its `facilityId` param.** Firebase rejects the parameter
  and drops it, appending `firebase_error` / `error_value = facilityId` to the event
  instead. The event itself arrives fine, so nothing downstream noticed — but the param
  is absent on essentially every firing since the event shipped, and Park & Ride facility
  analysis has no data behind it. Needs an app-side fix (check the param name and value
  against Firebase's naming and length limits at the call site in the P&R card handler).
  Historical facility data cannot be recovered; it was never recorded. Tracked in
  [#1762](https://github.com/ksharma-xyz/KRAIL/issues/1762).
- **`no_entries_detected` is firing.** Its KDoc says the event should stay silent after
  the `resetRoot()` / duplicate `toEntries()` fixes, and that the `NoEntriesUI` fallback
  can be removed if it does. It is arriving in production data, most recently 2026-07-19.
  The nav bug is not fully fixed — do not remove the fallback. Group by `topLevelRoute`
  to find the path that triggers it.
- **Historical note for `stop_selected`**: rows recorded before 2026-07-14 have no
  `locationKind`/`addressType` — treat a missing value as `transit_stop`/absent, not as
  a distinct category, same convention already used for `StopLabelStopAssignedEvent`'s
  `source` field (see that event's KDoc in `AnalyticsEvent.kt`).

## Not yet in this ledger (scoped, not implemented)

Nothing at the moment. The stop-label lifecycle analytics work
(`docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`) shipped 2026-07-15 - all four events
reshaped to bounded params, reorder moved to drag completion, Manage screen view
added; rows above.
