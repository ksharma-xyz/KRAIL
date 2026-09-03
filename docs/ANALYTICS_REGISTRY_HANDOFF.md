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

New-event rows (`Event` column marked `(NEW EVENT)`, or `Param(s)` starting `NEW
event:`) get flipped automatically: when KRAIL-Analytics labels the event, its CI
fires a `repository_dispatch` naming it, and `.github/workflows/analytics-registry-sync.yml`
opens a PR here flipping the row (labeled `analytics-sync`). Param and user-property
rows are never touched by the bot — they have no per-item registry surface on the
analytics side to check against, so mark those `Documented` by hand instead once
their shape is final (see `Status = Documented` below).

**New event name vs new param on an existing event** — both go in this ledger the same
way, distinguished by the `Event` column. Read `docs/ANALYTICS_EVENTS.md` before adding
either; most changes should be params on an existing event, not a new name.

**`Status = Documented`** — for param/user-property rows only, not event names.
KRAIL-Analytics' registry tracks event names and labels (`dashboard/lib/eventLabels.ts`);
it has no per-param or per-user-property surface since the per-PR contract file was
dropped (#1767). `Documented` means the shape is final and this row is the record of it
— there is nothing further to confirm on the KRAIL-Analytics side, so drift-lint should
not keep flagging it as `Pending`. Don't use it for new event names; those stay `Pending`
until they have a label.

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
| 2026-07-22 | `park_ride_user_facility` | NEW event: `facilityId`, `stopId`, `action`, `source` | `action` = `add｜remove`; `source` = `add_park_ride_screen｜saved_trips_screen` | Rider adds or removes a Park & Ride facility. Add/remove share one name and split on `action` rather than spending two slots; `source` distinguishes the manual entry point from the auto-sync one below | [#1740](https://github.com/ksharma-xyz/KRAIL/issues/1740) | Registered | — |
| 2026-08-11 | `park_ride_user_facility` | `source = saved_trips_screen` (now emitted; value renamed from reserved `home_screen`) | Previously reserved but unwired; now fires from the saved-trips-to-Park&Ride auto-sync | Saving/removing a trip changes which stops have a mapped Park & Ride facility, auto-adding or auto-removing that facility from the saved list (`SavedTripsViewModel.updateParkRideStopIdsInDb`, diffed against the prior synced set) | (local) | Documented | — |
| 2026-08-05 | `device_window` | NEW event: `widthDp`, `heightDp`, `smallestWidthDp`, `widthSizeClass`, `heightSizeClass`, `orientation`, `formFactor`, `paneMode`, `foldState`, `foldOrientation` | Ints for dp; `COMPACT｜MEDIUM｜EXPANDED｜LARGE｜EXTRA_LARGE` for width (the app's own axis, wider than the three classes requested - report on what the layout actually branches on); height `COMPACT｜MEDIUM｜EXPANDED`; `PORTRAIT｜LANDSCAPE`; `PHONE｜TABLET｜FOLDABLE_OPEN`; `SINGLE｜DUAL`; `NONE｜FLAT｜HALF_OPENED`; `NONE｜HORIZONTAL｜VERTICAL` | Once per launch, from composition at the nav host. Not on `app_start`: that fires before any window exists | (local) | Registered | `KRAIL-Analytics/docs/TRACKING_REQUEST_SCREEN_SIZE.md` |
| 2026-08-05 | `device_window_changed` | NEW event: `fromWidthClass`, `toWidthClass`, `fromFoldState`, `toFoldState`, `fromPaneMode`, `toPaneMode`, `screen` | Same enums as above; `screen` is an existing `AnalyticsScreen` name, or `Unmapped` for routes with no `AnalyticsScreen` (Splash, Discover, DateTimeSelector, TrackTrip) | Settled transition of width class, fold state or pane mode, debounced 400 ms and compared against the last reported state. **An unfold reports twice**: the resize first, then the hinge ~2 s later when `androidx.window` delivers the `FoldingFeature`. Count unfolds as rows with `toFoldState = FLAT`, not as row totals | (local) | Registered | `KRAIL-Analytics/docs/TRACKING_REQUEST_SCREEN_SIZE.md` |
| 2026-08-05 | (user properties, not an event) | `device_form_factor`, `window_width_class`, `pane_mode` | `PHONE｜TABLET｜FOLDABLE_OPEN`; width classes as above; `SINGLE｜DUAL`. Defined in `AnalyticsUserProperty.kt` next to `AnalyticsEvent.kt` | Set at launch and on every settled window change, so they attach to **every subsequent event**. This is what makes "what did riders do while unfolded" a filter on existing events rather than a new param on each one. Needs registering as custom dimensions before they appear in reports | (local) | Documented | `KRAIL-Analytics/docs/TRACKING_REQUEST_SCREEN_SIZE.md` |
| 2026-08-05 | **every event** | `pane` | `SINGLE｜LIST｜DETAIL` | Injected centrally in `RealAnalytics.track()`, so every event carries where the rider was working. Attribution is by **last touch**, set by `DualPaneScaffold`: in a dual-pane layout both panes are visible, so crediting the topmost screen would blame the detail pane for list-pane taps. `SINGLE` on phone-shaped windows and before the first touch. Events with no interaction behind them (polling, screen views) inherit the last touched pane | (local) | Documented | `KRAIL-Analytics/docs/TRACKING_REQUEST_SCREEN_SIZE.md` |
| 2026-08-05 | `social_connection_link_click` | (no change - status note) | — | **Dormant, not dead.** Its Settings entry point went in v1.25.0 (#1722), leaving only the Discover card's app-social row, and Discover sits behind the `is_discover_available` flag, currently off. It cannot fire on a shipped build today, so an empty stretch is the flag being off, not riders ignoring it. The class stays: it returns when Discover is enabled, and deleting it would mean re-minting a name against the 500-event budget | (local) | Pending | — |
| 2026-07-22 | `view_screen` | `name = AddParkRide` (new value) | Existing screen-view event, new screen name | Park & Ride picker becomes visible | [#1740](https://github.com/ksharma-xyz/KRAIL/issues/1740) | Documented | — |
| 2026-08-06 | `search_stop_query` | `localResultsCount` | Int | On-device stop matches for the query. Sent on the **address** firing only, where `resultsCount` is the address count — scores the address gate without needing a `searchSessionId` join back to the local firing | (local) | Documented | — |
| 2026-08-06 | `search_stop_query` | `addressSearchGate` | `DISABLED｜BLANK｜BELOW_THRESHOLD｜ELIGIBLE` | Address-pipeline decision for the query, on the **local** firing (which happens for every settled query). This is the only record of address calls that were *not* made: suppressed calls produce no address firing, so without this a threshold that went too far is invisible. `STOPS_ALREADY_SUFFICIENT` and `CACHE_HIT` were emitted on 2026-08-07 only, then removed with the stop-count gate and the result cache — historical rows may carry them | (local) | Pending | — |
| 2026-09-03 | `search_stop_query` | `query` (semantics change) | Typed text with **every digit masked to `#`**, trimmed, dropped above 40 chars (`SearchQueryAnalyticsRedaction.maskedQueryOrNull`). **Exception: an all-digit query is sent as typed** - route numbers and stop IDs are digits with no street beside them, so they identify no home; one non-digit character anywhere masks the whole query. Sent on **every** settled query, not only zero-result ones. Masking is client-side because analytics goes straight to Firebase; the KRAIL-Analytics pull/snapshot masking stays as a backstop for 1.26-and-earlier builds still in the field, which keep sending under the old carve-out (raw text, real digits, zero-result only). Rows are only comparable within an app version | Every settled local search, one firing per typing burst | (local) | Pending | — |
| 2026-09-03 | `search_stop_query` | `query` (no longer on the address firing) | The address firing (`resultSource = address`) carries no query text at all now; the local firing owns it. Join on `searchSessionId` | — | (local) | Pending | — |
| 2026-09-03 | `search_stop_query` | (firing-rate change, no param) | **Volume drops and is not comparable across the 1.26/1.27 boundary.** Text and event now fire once per typing burst rather than once per settled keystroke: the firing waits 600 ms of quiet after results render, and the next keystroke cancels it. Historical rows counted prefixes (`4`, `4 f`, `4 fu`) as searches in their own right, which inflated zero-result and `BELOW_THRESHOLD` counts. Also fixed here: a cancelled keystroke was reported as `isError = true`, so historical error rates on this event are overstated | — | (local) | Pending | — |
| 2026-08-06 | `search_stop_query` | `queryHasDigit` | Bool | Whether the typed query contains a digit, on the local firing (success and error). A house number is the cheapest address signal there is; a bool, never the text | (local) | Documented | — |
| 2026-08-15 | `stop_selected` | `resultIndex` | Int, zero-based; omitted without a live query | Row the picked result was sitting at inside its own section (`locationKind` says which section). The ranking-quality signal: `displayedLocalCount` says how many rows were showing, only this says the rider had to scroll to number nine. Raw rather than bucketed because first-versus-second is the whole question, and a number costs no string cardinality. A stop tapped inside an expanded trip card reports the trip's row. **Zero is a real value, not a missing one** - a top-result pick is the common case, so `resultIndex = 0` and `resultIndex IS NULL` must never be folded together | (local) | Pending | — |
| 2026-07-22 | `review_prompt_requested` (NEW EVENT) | `source` | `saved_trip_open` | App asks the platform for its review sheet (Play In-App Review / StoreKit). Counts asks, not ratings: neither platform reports whether the sheet appeared or what the user did, so no companion "shown"/"rated" event exists or can be built | TBD | Registered | [#1739](https://github.com/ksharma-xyz/KRAIL/issues/1739) |

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
- ~~`park_ride_card_click` loses its `facilityId` param~~ — **Resolved**: the call site was
  stringifying the whole `ParkRideFacilityDetail` object instead of joining `facilityId`s,
  running past Firebase's 100-char param limit. Fixed in
  [#1784](https://github.com/ksharma-xyz/KRAIL/pull/1784) (merged 2026-08-05, shipped in
  v1.26.0); `AnalyticsParamSanitizer` also now appends `~trunc` when it shortens a value, so
  a future regression truncates visibly instead of silently. Historical facility data before
  the fix cannot be recovered; it was never recorded. Was tracked in
  [#1762](https://github.com/ksharma-xyz/KRAIL/issues/1762) (closed).
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
