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
| 2026-07-15 | `stop_label_created` | `creationSurface`, `labelCountBucket`; REMOVED `labelName`/`emoji`/`totalLabelsCountAfter` | `search_result｜recent｜empty_state｜address_result`; bucket `1｜2｜3_5｜6_plus` | New custom label persisted; raw label text dropped (privacy) | TBD | Pending | — |
| 2026-07-15 | `stop_label_stop_assigned` | `assignmentSurface`, `assignmentMode`, `locationKind`, `labelKind`; REMOVED `labelName`/`stopId`/`stopName`/`source` | Surfaces as above; `existing_label｜new_label`; `transit_stop｜address`; `protected_default｜custom`. Historical `source` values (`choose_mode｜star_sheet`) refer to deleted v2/v3 flows, do not reinterpret | Location pinned to a label; raw stop identity dropped (privacy) | TBD | Pending | — |
| 2026-07-15 | `stop_label_removed` | `labelKind`, `surface`; REMOVED `labelName` | `protected_default｜custom`; `surface` always `manage_labels` | Assignment cleared or label deleted in Manage Labels | TBD | Pending | — |
| 2026-07-15 | `stop_label_reordered` | `labelKind`, `moveDistanceBucket`, `setLabelCountBucket`, `surface`; REMOVED `labelName`/`previousIndex`/`newIndex`/`totalCount` | Distance `1｜2_3｜4_plus`; set-count `1｜2｜3_5｜6_plus`; `manage_labels`. NOW FIRES ONCE PER COMPLETED CHANGED DRAG - historical rows fired per swap, counts inflated, not comparable | Drag released in Manage Labels with a different final order | TBD | Pending | — |
| 2026-07-15 | `view_screen` | `name = ManageStopLabels` (new value) | Existing screen-view event, new screen name | ManageStopLabelsScreen becomes visible (once per entry, rotation-safe) | TBD | Pending | — |

Registered in KRAIL-Analytics `docs/EVENT_REGISTRY.md`'s "Params registry" table,
2026-07-14.

## Open items for KRAIL-Analytics maintainers

- ~~`EVENT_REGISTRY.md` exact path unconfirmed~~ — **Resolved**: it already existed at
  `docs/EVENT_REGISTRY.md`, and now has a "Params registry" table added specifically
  for this handoff.
- **Historical note for `stop_selected`**: rows recorded before 2026-07-14 have no
  `locationKind`/`addressType` — treat a missing value as `transit_stop`/absent, not as
  a distinct category, same convention already used for `StopLabelStopAssignedEvent`'s
  `source` field (see that event's KDoc in `AnalyticsEvent.kt`).

## Not yet in this ledger (scoped, not implemented)

Nothing at the moment. The stop-label lifecycle analytics work
(`docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`) shipped 2026-07-15 - all four events
reshaped to bounded params, reorder moved to drag completion, Manage screen view
added; rows above.
