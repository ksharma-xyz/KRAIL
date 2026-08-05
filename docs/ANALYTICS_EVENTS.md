# Analytics Events — Design Rules

Read this before adding or modifying any event in
`core/analytics/src/commonMain/kotlin/xyz/ksharma/krail/core/analytics/event/AnalyticsEvent.kt`.

## The budget

Firebase Analytics hard-caps the app at **500 unique event names, forever**. GA never
lets a name be reclaimed from history — a shipped event name is a permanently spent
slot, even if the code stops sending it. Budget as of 2026-08-05: 52 events defined in
code plus ~10 historical names, roughly 438 slots left. Update this count when adding
or removing events.

### The other three caps, and which one actually bites

The 500 event names are the famous limit. Three more exist, and the one that constrains
this app is not the one people reach for. Measured on `main`, 2026-08-05:

| Cap | Limit | Where we are |
|---|---|---|
| Params per event | 25 custom | **11** on the widest (`device_window`, `app_start` at 10 each, plus `pane`). Everything else is 7 or below |
| Event-scoped custom dimensions | 50 per GA4 property | **118 distinct param names** across 52 events |
| User-scoped custom dimensions | 25 per property | **3** used (`device_form_factor`, `window_width_class`, `pane_mode`) |

Params-per-event has plenty of room, including for `pane`, which rides every event. The
binding constraint is the **event-scoped dimension cap**: the app emits more than twice
what a standard GA4 property can surface, and has done for a while.

What that means in practice:

- **BigQuery is unaffected.** Every param lands on the row whether or not it is registered,
  so BigQuery analysis never hits this cap. Most KRAIL-Analytics queries live there.
- **The GA4 UI only shows registered dimensions.** An unregistered param is invisible, and
  invisible looks exactly like absent - the same failure mode as a dropped param. If a param
  seems missing in the UI, check registration before assuming the app stopped sending it.
- Adding a param is cheap for the app and not free for analytics. That is another reason
  the checklist below prefers reusing an existing param over minting one.

## Decision checklist: new event name vs param

**A new event NAME is justified only when ALL of these hold:**

1. It captures a new user intent — not a new gesture or surface for an existing intent.
2. It shares no surface AND no param set with an existing event.
3. It would be charted standalone on a dashboard.

**Otherwise extend an existing event with a param.** Params are cheap for the app - the
limit is 25 per event and the widest event sits at 11 - but not free downstream: each new
param name competes for a registerable dimension slot, see the caps section above.

| Situation | Pattern | Example |
|---|---|---|
| Different gesture, same surface, same params | `action` value on the existing event | Departures icon fires `timetable_stop_header_click` with `action = open_departures` — not a separate `timetable_departures_icon_click` |
| Outcomes of one interaction (accept/dismiss, on/off, success/failure) | ONE event with a boolean or enum param | `save_trip_prompt_action(accepted: Boolean, dismissCount)` — not `_accepted` + `_dismissed` |
| Same event fired from several surfaces | `source` param | `DepartureBoardSource` enum, `SaveTripClickEvent.source = star \| prompt` |
| Feature state changes (error/retry/loading) | `{feature}_status(action)` | `dep_board_status(action: error \| retry)` — not one event per state |

## Double-counting check

Before instrumenting a tap that opens a screen or sheet, check whether the destination
already fires an equivalent event. Example: opening the departure board fires
`dep_board_screen_view(stopId, stopName, source)` from `DeparturesAnalytics.kt` — a
click event for the same open would count the same action twice.

## Why folding beats splitting (beyond slot-thrift)

A feature that moves between gestures keeps a single-event query timeline:

```sql
WHERE event_name = 'timetable_stop_header_click'
  AND (action IS NULL OR action = 'open_departures')
```

Separate names force every dashboard and derivation to UNION two event names to trace
one feature across a change. Missing param on historical rows = "before the change",
which gives before/after splits for free.

## Search funnel join model: `searchSessionId`

Analytics never carry raw search query text (a typed query can be a street address;
see `SearchQueryAnalyticsRedaction` in `:feature:trip-planner:ui` for the one narrow
zero-result carve-out). The correlation the text used to provide comes from
`searchSessionId` instead: a random 64-bit hex string minted in
`SearchStopViewModel.onSearchTextChanged` for every settled non-blank query.

Semantics:

- **One ID per settled query.** Typing "cen" then "central" mints two IDs. Every
  event describing the same query instance carries the same ID.
- **Carried by** `search_stop_query` (both the `resultSource = local` and
  `resultSource = address` firings), `stop_selected`, and `load_timetable_click`.
- **Closes at the timetable, not the selection.** `stop_selected` only proves a stop was
  picked; `load_timetable_click` is where the rider actually reaches departure times, so
  the id is carried across via `SearchSessionStore` (`:feature:trip-planner:ui`). Two
  rules keep that attribution honest: the pending id is **consumed once** (loading the
  same trip again is a repeat view, not a second conversion), and it is only attached when
  the loaded trip still contains **the stop that was selected in that session** (searching
  and then tapping an unrelated saved trip attributes nothing). A selection with no live
  query records a null id, which also clears any earlier pending one.
- **Null when there is no live query.** Selections from recents, empty-state stops,
  and map picks attach no ID; joining them to a search would be wrong.
- **Meaningless by design.** Not stored on device, not derived from anything, adds
  zero information about the user. It only links events to each other.
- **Not `ga_session_id`.** Firebase's session ID spans a whole app sitting (many
  searches); `searchSessionId` is per query, which is where "were the results any
  good" lives.

What it buys: joining the three events per query instance answers "N local and M
address results were on screen, the user picked an address" without any query text.
Rows before 2026-07-15 have no `searchSessionId`; treat missing as "pre-join era",
only app-session-level funnels are possible there.

## Param sanitizing: location ids and the 100-char limit

Every event's properties pass through `AnalyticsParamSanitizer` in the `AnalyticsEvent`
base class, so no call site needs to hash or truncate. It exists because of two traps:

- **Firebase silently drops any String param value over 100 characters.** The event still
  lands, the param is just missing, so a dashboard reads "unused feature" rather than
  "rejected param". This is exactly what happened to address selections in v1.25: an EFA
  `streetID:...` id is 120+ chars, so `stop_selected` rows for addresses arrived with no
  `stopId`, address search looked unused, and stop rankings quietly omitted those
  selections.
- **Address ids and address display names are personal data.** An EFA address id embeds
  the street, suburb and postcode as plain text, and the display name is the address
  itself. Sending either would undo the query redaction described above.

Rules applied:

| Param | Value | Result |
|---|---|---|
| `stopId`, `fromStopId`, `toStopId` | transit stop id (no colon, ≤ 100 chars) | unchanged |
| `stopId`, `fromStopId`, `toStopId` | namespaced id (`streetID:`, `poiID:`, `coord:`) or over the limit | `addr_` + stable 64-bit hash |
| `stopName` | event also carries an address id | `address` |
| any String | over 100 chars | truncated to 100, ending in `~trunc` |

The hash is stable across launches and devices, so "which addresses get picked" stays
rankable while the address text never leaves the device. Address rows before this shipped
have **no** `stopId` at all — do not read their absence as "no address selections".

The `~trunc` suffix is deliberate. Truncation keeps the row instead of letting Firebase
drop the param, but a silently shortened value looks exactly like a real one, so a broken
call site would read as working data and the rejection signal that flags it would vanish.
Any value ending in `~trunc` is a call site sending the wrong thing, not data. Fix the
call site rather than widening the limit: `park_ride_card_click.facilityId` was joining
whole facility objects instead of their ids, which is a call-site bug, not a length one.

When adding a param that can carry a location id or a user-visible place name, add its key
to the relevant set in `AnalyticsParamSanitizer` rather than sanitizing at the call site.

## Window, fold and pane reporting: how to read it

`device_window`, `device_window_changed` and the `pane` param answer "is anyone on a large
screen, and what do they do there". Five things about them are counter-intuitive enough
that they have been misread once already, so they are written down rather than re-derived.
These match the addendum in KRAIL-Analytics' `docs/TRACKING_REQUEST_SCREEN_SIZE.md`.

- **An unfold emits two `device_window_changed` rows, not one.** The resize lands first;
  `androidx.window` delivers the `FoldingFeature` a second or two later. Both are true.
  **Count unfolds by `toFoldState = FLAT`, never by row totals.**
- **A wide window that stays `SINGLE` is a finding, not noise.** `widthSizeClass` is the
  input, `paneMode` is what the layout did with it. The gap between them is the whole point
  of tracking both.
- **`formFactor = PHONE` on a device whose model is a Fold is the used-closed segment.**
  A folded foldable reports no hinge inside the window, so the app cannot see it and does
  not guess. Crossed with `deviceModel`, that combination is the answer, not a defect.
- **`pane` is last-touch attribution.** For an event with a tap behind it, that is where the
  tap happened. For a passive event (screen view, polling, a window change) it is where the
  rider was last working - **never read it as "where the tap happened" for those**.
  `SINGLE` means a one-pane window or no touch yet, not "unknown".
- **Five width classes, not Material's three.** The app branches on `LARGE` and
  `EXTRA_LARGE` too, and reporting on a different axis than the layout uses would be
  pointless. Raw `widthDp` ships alongside, so any bucketing can be redone against history
  without an app change.

## How analytics reaches the dashboard

`AnalyticsEvent.kt` is the only thing this repo owns for analytics — it defines the event
names and their params, and that is the whole job here. There is **no contract file, no
per-PR analytics test, and no registration step** in KRAIL.

The **KRAIL-Analytics** repo reads `AnalyticsEvent.kt` at the latest **published release
tag** (never `main`, so unreleased events are not surfaced early), on a periodic schedule,
and builds its own registry from it. Everything editorial — human labels, how events group
into dashboard metrics, and rename/deprecation history — lives in KRAIL-Analytics, because
those are display decisions the app has no opinion on. KRAIL-Analytics detects a released
event it has not yet handled and fails its own build until it is; nothing here gates on it.

**So, to add or change an event:** just edit `AnalyticsEvent.kt` following the naming and
budget rules above. Nothing else in this repo. It becomes visible to analytics after it
ships in a release.

`docs/ANALYTICS_REGISTRY_HANDOFF.md` is kept only as a historical audit trail of the
registration handshake that predated this model; it is no longer a gate and new events do
not need a row.
