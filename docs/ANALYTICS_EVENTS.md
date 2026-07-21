# Analytics Events — Design Rules

Read this before adding or modifying any event in
`core/analytics/src/commonMain/kotlin/xyz/ksharma/krail/core/analytics/event/AnalyticsEvent.kt`.

## The budget

Firebase Analytics hard-caps the app at **500 unique event names, forever**. GA never
lets a name be reclaimed from history — a shipped event name is a permanently spent
slot, even if the code stops sending it. Budget as of 2026-07-22: 59 events defined in
code plus ~10 historical names, roughly 430 slots left. Update this count when adding
or removing events.

## Decision checklist: new event name vs param

**A new event NAME is justified only when ALL of these hold:**

1. It captures a new user intent — not a new gesture or surface for an existing intent.
2. It shares no surface AND no param set with an existing event.
3. It would be charted standalone on a dashboard.

**Otherwise extend an existing event with a param.** Params are effectively free: the
limit is 25 per event and nothing in the app is near it.

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
  `resultSource = address` firings) and `stop_selected`.
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

## Registration gate

Every new event or param must be registered in `docs/EVENT_REGISTRY.md` in the
**KRAIL-Analytics** repo before the app PR merges: params, trigger description, and
owner story link.
