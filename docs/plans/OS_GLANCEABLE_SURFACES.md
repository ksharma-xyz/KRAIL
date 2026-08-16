# OS-level glanceable surfaces

Research notes for showing KRAIL data outside the app: Live Activities, Dynamic Island,
Android Live Updates, home screen and lock screen widgets, Control Center controls,
Quick Settings tiles, StandBy, watch complications.

Status: research only. No implementation, no decision taken.

## Why this document exists

The original ask was "a home screen widget". Scoping it revealed that the widget is one of
about ten OS surfaces that can answer a rider's question without launching the app, that it
is the most expensive of them against a shared API quota, and that it is not the one that
delivers the most value. The rest of this file records what was found so the next person
does not have to rediscover it.

## Four moments, not one surface

| Moment | Rider's question | Surfaces |
|---|---|---|
| Live journey | "Am I still on time, which platform, is my connection safe?" | iOS Live Activity + Dynamic Island; Android Live Update + status bar chip; both on lock screen and always-on display |
| Passive glance | "When is the next one for my usual trip?" | Home screen widget; lock screen accessory; StandBy; watch complication |
| One-tap ask | "Tell me, but only when I ask." | iOS Control / Action Button; Android Quick Settings tile; Siri and Assistant |
| It tells me | "Do I need to do something differently?" | Departure alarm; disruption notification on a saved trip |

## Constraint 1: one API key, shared by every install

`ANDROID_NSW_TRANSPORT_API_KEY` and `IOS_NSW_TRANSPORT_API_KEY` come from `local.properties`
through BuildKonfig into every `*HttpClient` as an `Authorization: apikey ...` header. There
is no per-user key, so every install spends from the same bucket.

The publicly documented TfNSW Bronze plan is 60,000 calls per day and 5 calls per second, per
key. Exceeding the daily quota returns HTTP 403 with `Account Over Quota Limit` for **every**
caller, not just the surface that caused it. Trip planning, stop search, departures and Park
and Ride all fail until the window rolls.

Consequence: any surface that fetches while nobody is looking needs a remote kill switch and a
remotely tunable cadence, not as a nicety but as a precondition for shipping.

### Widget cost ladder

`D` = devices with a widget placed. Daily calls is roughly `D x (calls per device per day)`.
The right column is how many such devices the whole quota covers before any in-app usage.

| Strategy | Calls / device / day | Devices covered |
|---|---:|---:|
| WorkManager every 15 min, always on | 96 | 625 |
| `updatePeriodMillis` 30 min, always on | 48 | 1,250 |
| 30 min, 05:00 to 23:00 only | 36 | 1,666 |
| Two 3-hour travel windows, 15 min | 24 | 2,500 |
| Adaptive cadence inside travel windows | ~10 | 6,000 |
| Adaptive + screen-on gate + shared cooldown | ~6 | 10,000 |
| Same, through a caching BFF | ~0.5 upstream | 120,000+ |

The 5-per-second throttle is a separate failure mode from the daily quota. Scheduled work lands
on round clock boundaries and both platforms batch background wake-ups, so a morning window is a
thundering herd even when the daily total is fine. Every scheduled fetch needs a stable
per-install jitter, for example `abs(installId.hashCode()) % 300` seconds.

## Constraint 2: the best surfaces are designed for push

| Surface | Intended update path | What KRAIL can do today | Gap |
|---|---|---|---|
| iOS Live Activity | APNs push to an ActivityKit push token | Update only while the app runs; hand the system a full journey up front and let it render free countdowns and known leg transitions | Cannot report a delay that appeared after the screen went dark |
| Android Live Update | Push, or the app's own process | A foreground service tied to the tracked trip can keep posting for the whole journey | None for a tracked trip |
| Disruption notification | Push from something watching the rider's trips | Nothing | Needs a server |
| Home screen widget | Scheduled local fetch | Works, at a quota cost | - |
| Control / Quick Settings tile | Read on demand | Works, at near-zero cost | None |

Android can deliver the complete live-journey experience today. iOS ships the same surface with
a stated limitation until there is a server to push from. `BffEndpointResolver` exists and
`BFF_ROLLOUT_ARMED` is `false`; arming it is what closes both this gap and the quota one.

## Cost per surface

| Surface | When it does work | Calls / device / day |
|---|---|---:|
| Control, Quick Settings tile | Only when a human opens the panel | < 1 |
| Departure alarm | Once, at a time already known | 0 |
| Live Activity (iOS) | Only during a trip | 0 to 2 |
| Live Update (Android) | Only during a trip | ~40 per journey, bounded by trip length |
| Lock screen, StandBy, watch | Shares the widget's data | 0 extra |
| Home screen widget | All day, whether or not anyone looks | 6 to 96 |

A Live Activity is cheaper than a home screen widget despite being far richer, because it only
exists while the rider is travelling. Bounded lifetime is the best cost control available here,
and it comes free with the surfaces that are also the most useful.

## What already exists in this repo

- `feature/track` models a live journey end to end: `TrackedJourney` with legs, stops, deviation
  and realtime trip ids, `TripPoller` on a 60 second cadence, GTFS-realtime matching, a deep-link
  encoder and a `TrackingCard`. This is the Live Activity and Live Update payload already built.
- `TimeTableState.JourneyCardInfo` already carries every field a passive-glance surface needs:
  `timeText`, `platformNumber`, `transportModeLines`, `totalUniqueServiceAlerts`,
  `departureDeviation`, `scheduledOriginTime`.
- `/v1/tp/trip` with `calcNumberOfTrips=6` returns roughly 30 to 90 minutes of departures in one
  response. Advancing from one departure to the next is therefore a local operation, not a fetch.
- Countdowns are free on both platforms: `Text(date, style: .relative)` on iOS, and a
  `Chronometer` with `setChronometerCountDown(true)` embedded via `AndroidRemoteViews` on Android.
  The Android form renders `MM:SS`, so `setFormat("in %s")` gives "in 05:12" rather than
  "in 5 min". That formatting question needs answering before any Glance layout is written.

## What is missing

- No Glance, WorkManager or DataStore entries in `libs.versions.toml`.
- No iOS extension targets in `iosApp.xcodeproj`, and no App Group entitlement.
- `POST_NOTIFICATIONS` is not requested, so no live-journey surface can appear on Android.
- The GTFS static bundle ships `NSW_STOPS.pb` and `NSW_BUSES_ROUTES.pb` only. There is no
  `stop_times` data on device, so there is no offline timetable fallback.
- The existing refresh gate does not survive process death. `DepartureBoardRepository` keeps
  `lastFetchTime` in a `mutableMapOf` and `NetworkRateLimiter` is an in-memory debounce created
  per use case. A widget host or tile process is normally dead between wake-ups, so two wake-ups
  seconds apart would each see "never fetched" and both spend a call. `lastFetchAt` has to be
  persisted per `tripId` and consulted by every surface, per the "one budget, one gate" rule in
  `docs/FEATURE_QUALITY_CHECKLIST.md`.
- On iOS the shared state cannot live in `krailSandook.db`. Extensions are separate processes, and
  opening the main database from several of them reintroduces the `SQLITE_BUSY` class of bug this
  project has already hit. A small purpose-built JSON file per trip in the App Group container,
  atomically replaced, holding pre-formatted display strings, is the safe shape.
- The journey-to-display mapper lives in `feature/trip-planner/ui/.../timetable/business/`, which is
  a Compose UI module. No OS extension may depend on it. Moving it into a UI-free module is a
  prerequisite refactor and should be its own change.

## Sketched refresh rules

Cadence follows time to departure rather than the clock:

| Time until next departure | Interval | Why |
|---|---|---|
| > 30 min | none | The cached list covers it; advancing is local |
| 10 to 30 min | 10 min | Deviation becomes actionable |
| < 10 min | 5 min | The only window where a refresh changes behaviour |
| Departed, list not exhausted | none | Roll to the next cached journey |
| Departed, list exhausted | once | Refill the window, then idle |

Plus: travel windows defaulting to weekday mornings and afternoons and learnable from
`SavedTrip.timestamp`; a screen-on check via `PowerManager.isInteractive`; a persisted circuit
breaker opened by a quota 403; and a 60 second floor between honoured manual refreshes, shared
with the scheduler rather than implemented separately. A surface a human is actively looking at
skips the travel-window check but not the other gates.

## Degradation ladder

| Age | Renders | Withheld |
|---|---|---|
| < 2 min | Everything | Nothing |
| 2 to 10 min | Everything, quiet timestamp | Nothing |
| 10 to 30 min | Dimmed content, prominent "Times as at ..." | Deviation removed entirely |
| > 30 min | Scheduled times only, explicit warning | Deviation, and platform if realtime-derived |
| Quota 403 | Last known plus "Live updates unavailable" | Any claim of currency |
| Nothing cached | Branded tile plus "Open KRAIL" | - |

Dropping the deviation while keeping the times is the difference between a surface that is out of
date and one that misinforms. A stale "on time" is the worst thing any of these can say.

## Design rules that apply to every surface

- No arrow glyph. Modes are separated by a filled dot, matching `SeparatorIcon`.
- Every colour signal carries a word. Lock screen accessories, iOS tinted mode, Android themed
  icons and always-on display all strip colour.
- The countdown numeral never shrinks; other rows drop first.
- Alert text only where it cannot truncate; the icon everywhere else.
- Every surface reads the same mapper as the journey card, enforced by module dependency direction.
- Tapping anything deep-links to that trip, never the home screen.
- No surface renders empty.
- A live-journey surface always offers a way to end it.

## Suggested order, if this is picked up

0. Persist `lastFetchAt`, make the app's own entry points share one gate, add jitter, add a call
   counter, and move the journey mapper out of the UI module. No new surface in this step.
1. Quick Settings tile and iOS Control. Smallest change, near-zero quota cost, and it proves the
   shared cache and the App Group before anything harder depends on them.
2. Live journey. Android first because it can deliver the complete experience today; iOS after,
   with the push limitation stated.
3. Home screen widget behind every gate above, and with it the lock screen accessories, StandBy
   and the watch complication essentially for free.
4. Arm the BFF. It unlocks APNs push for live iOS updates and disruption notifications, and it is
   the only thing that changes the shape of the quota problem rather than deferring it.

## Open questions

- Is the API key actually on the Bronze plan? Only Bronze is publicly documented; a higher tier
  may already be provisioned. This changes the whole feasibility table.
- Do the TfNSW terms permit unattended background polling by a widget? The Acceptable Use Policy
  was not reviewed as part of this research. This is a compliance gate.
- Is the Android `Chronometer` output format acceptable, or is a boundary-alarm fallback needed?
- Does a 403 surface distinguishably today, given `expectSuccess = true` on the base client?
- Which foreground-service type applies to the Android live-journey surface, and what are its
  start restrictions on current Android releases?
- What should a surface do when the saved trip it points at has been deleted?

## References

- TfNSW API basics, plans and quota: https://opendata.transport.nsw.gov.au/developers/api-basics
- Android Live Updates: https://developer.android.com/develop/ui/compose/notifications/live-update
- Glance widget updates: https://developer.android.com/develop/ui/compose/glance/glance-app-widget
- Wear OS tile and complication update limits: https://developer.android.com/training/wearables/tiles/update
