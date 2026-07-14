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

The stop-label lifecycle analytics work (`docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`) has
been scoped — 4 existing events get new params, 0 new event names — but nothing has
shipped yet. It will get rows here once a PR actually merges, not before. See that plan
doc for the full proposed param table in the meantime.
