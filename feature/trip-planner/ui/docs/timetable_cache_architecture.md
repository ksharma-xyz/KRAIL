# TimeTable Cache Architecture

## Overview

`TimeTableViewModel` maintains **three separate in-memory caches** for journey data. Each has
a distinct lifecycle so that background auto-refresh cannot accidentally overwrite data the user
has explicitly requested (load-more, show-previous).

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer                                │
│  previousJourneyList  │  journeyList (main + load-more merged) │
└────────────┬──────────┴──────────────┬──────────────────────────┘
             │                         │ updateUiStateWithFilteredTrips()
             │                         │
    ┌────────▼──────────┐   ┌──────────▼──────────┐   ┌──────────────────┐
    │ previousJourneys  │   │      journeys        │   │ loadMoreJourneys │
    │     Cache         │   │  (auto-refresh map)  │   │  (future pages)  │
    └───────────────────┘   └──────────────────────┘   └──────────────────┘
    Show Previous button    30s auto-refresh loop       Load More button
```

---

## Cache 1: `journeys` — the auto-refresh window

**Populated by:** `updateTripsCache(response)`, called every ~30 s by the auto-refresh loop.

**What it holds:** The API's "now" window — typically the next ~6 departures from the current time.
Includes a thin started-journey carry-over: departed-but-not-yet-arrived trips are kept for up to
`JOURNEY_ENDED_CACHE_THRESHOLD_TIME` (10 min) after arrival, capped at
`MAX_STARTED_JOURNEY_DISPLAY_THRESHOLD` (2) entries, so the list doesn't immediately vanish for a
user who is mid-journey.

**Key property:** This map is **cleared and rebuilt on every auto-refresh**. It always reflects
the latest real-time data returned by the API.

---

## Cache 2: `loadMoreJourneys` — future trip pages

**Populated by:** `onLoadMoreTrips()`, triggered by the user tapping "Load More Departures".

**Pagination strategy:** Each tap fetches from `lastJourneyInstant + 1 minute`, so the API returns
the next window of departures beyond what is already shown. Capped at `MAX_LOAD_MORE_COUNT` (3)
taps per session.

**Deduplication:** A new journey is only added to this map if its `journeyId` does not already
exist in either `journeys` or `loadMoreJourneys`. This prevents the same service appearing twice
when the auto-refresh window advances and overlaps with a previously fetched future page.

**Survives auto-refresh:** The auto-refresh only updates `journeys`; it never writes to
`loadMoreJourneys`. Future trips are therefore not discarded by background polling.

### Staleness Pruning

Over time the auto-refresh window shifts forward. A trip that was "future" when the user loaded
it may now fall inside the auto-refresh window — meaning the API already returns that trip with
fresh real-time data. Left unchecked, the `loadMoreJourneys` copy is **stale**: it cannot reflect
cancellations, platform changes, or delays that occurred after the load-more fetch.

**How pruning works** (implemented in `pruneStaleLoadMoreEntries()`, called at the end of every
`updateTripsCache()` run):

```
latestFreshInstant = max departure time in the freshly-built `journeys` map

for each entry in loadMoreJourneys:
    if entry.originUtcDateTime <= latestFreshInstant → remove it
```

**Why `<=`:** Any trip at exactly the latest fresh instant is, by definition, now covered by the
auto-refresh window. The fresh copy (with current real-time data) already lives in `journeys` and
will appear in the merged UI list via `distinctBy { journeyId }`.

**Cancellations:** If a trip was in `loadMoreJourneys` and the API stops returning it (cancellation,
operator change), `journeys` will not contain it. When `updateTripsCache` runs:
- The cancelled trip is within the fresh window → pruned from `loadMoreJourneys`.
- It no longer appears in `journeys` either.
- Result: the journey disappears from the UI on the next auto-refresh tick. ✓

**Trips still beyond the window are not pruned:** A trip with `originUtcDateTime > latestFreshInstant`
is genuinely future and cannot yet be validated by the API. It survives untouched.

**`loadMoreCount` is NOT decremented on pruning.** The count tracks how many times the user has
explicitly requested more data, not how many future trips are currently cached. Decrementing it
would let the user bypass the session cap by simply waiting for auto-refresh to catch up.

---

## Cache 3: `previousJourneysCache` — past trip pages

**Populated by:** `onLoadPreviousTrips()`, triggered by the user tapping "Show Previous Departures".

**Pagination strategy:** Fetches a `PREVIOUS_TRIPS_WINDOW_MINUTES` (60 min) window ending just
before the earliest currently shown departure. Only trips whose `originUtcDateTime` is strictly
before `firstJourneyInstant` are added, preventing overlap with the main list.

**Auto-refresh isolation:** The auto-refresh loop **never touches** `previousJourneysCache`. Past
trips are not re-fetched and their timestamps are treated as historical records. The slight
inaccuracy (e.g., a past trip that was eventually cancelled) is acceptable — the user has already
seen those departures were real when they were shown.

---

## Merge strategy: `updateUiStateWithFilteredTrips()`

```kotlin
// Main list = auto-refresh window + load-more future pages, deduplicated, sorted chronologically
val mergedJourneys = (journeys.values + loadMoreJourneys.values).distinctBy { it.journeyId }

// Previous list = past pages, sorted chronologically
val previousJourneyList = previousJourneysCache.values.sortedBy { it.originUtcDateTime }
```

**Deduplication rule:** `journeys` is listed **first**, so when the same `journeyId` appears in
both caches the entry from `journeys` (fresh auto-refresh data) wins. This means real-time delay
and platform updates flow through to the UI automatically once a load-more trip enters the
auto-refresh window.

**journeyId stability:** `journeyId` is derived from `transportation.id + RealtimeTripId`. For a
substitute service assigned a new `RealtimeTripId`, the old ID remains in `loadMoreJourneys` until
it is pruned by time-based staleness detection.

---

## Cache reset

All three caches are cleared by `resetPaginationCaches()`, which is called when:
- The user switches to a different origin/destination trip (`onLoadTimeTable` with a new trip)
- The user changes the date/time selector (`onDateTimeSelectionChanged`)
- The user taps the reverse-trip button (`onReverseTripButtonClicked`)

After reset the `loadMoreCount` returns to 0, allowing a fresh 3-tap allowance.

---

## Visual summary

```
Time →

  [prev page]  [prev page]  │  [auto-refresh window]  │  [LM page 1]  [LM page 2]
                             │                         │
       previousJourneysCache │       journeys          │    loadMoreJourneys
                             │                         │
                             ▲                         ▲
                       firstInstant              latestFreshInstant
                                                  (prune boundary)
```

Trips to the left of `latestFreshInstant` that still live in `loadMoreJourneys` are pruned on
every auto-refresh tick. Trips to the right survive until the window catches up with them.
