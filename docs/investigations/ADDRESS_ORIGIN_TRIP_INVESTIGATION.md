# Address-origin trip planning — investigation

## Bug report

After selecting an address from the new "Addresses & places" search section
(issue #1697, Phase 1), the Timetable screen showed nothing — no walking leg
from the address to the nearest stop, no journey at all. Journey map (when a
walk leg does come through) should render the walking path too, but that's
moot until the underlying journey shows up at all.

## What was tested

Live call against NSW's real API (not the KRAIL BFF), using the exact param
shape `RealTripPlanningService.appendTripQueryParams()` already sends today.

**1. `stop_finder` for a real address** (`type_sf=any`):
```
name_sf=123 Example St, Parramatta
```
Returned a `singlehouse` result with `isBest:true`:
```
id: streetID:1500000015:123:95346013:-1:Example St:Parramatta:Example St::Example St:2150:ANY:DIVA_SINGLEHOUSE:4871080:3749205:GDAV:nsw:0
coord: [-33.8, 151.0]
```

**2. `/v1/tp/trip` using that `streetID:...` as `name_origin`, `type_origin=any`**
(the app's current, unconditional hardcoded param — no coord branch, no
special-casing) — destination `200060` (Central):

```
systemMessages: [warning -10015 "itp-monomodal"]   ← benign, not blocking
journeys: 4
  leg 1: footpath | 123 Example St, Parramatta -> Parramatta Station, Parramatta
  leg 2: Sydney Trains Network | Parramatta Station, Platform 1 -> Central Station, Platform 5
```

**Walking leg + transit leg both came back correctly**, first try, no coord
branch needed.

## Conclusion: the API param shape is NOT the problem

The plan/issue (#1697) had flagged a hypothetical gap — "the existing 2-shape
ID model isn't enough, needs a third raw-coord shape + a `type_origin=coord`
branch" — based on the assumption that `type_origin=any` might not resolve a
`streetID:...` value the same way it resolves a normal stop ID. **That
assumption is wrong.** NSW's `/trip` endpoint resolves `type_origin=any` +
`streetID:...` exactly as it resolves `type_origin=any` + a plain stop ID —
same param, no branching required. The earlier confirmed `type_origin=coord`
+ `<lon>:<lat>:EPSG:4326` finding is still correct and still needed, but only
for the *current-location* case (a raw GPS fix has no `streetID`) — it was
never required for stop_finder-sourced addresses.

## BFF routing also ruled out

`BFF_ROLLOUT_ARMED = false` (`core/network/BffEndpointResolver.kt`) — while
disarmed, `NetworkSource.FOLLOW_RC` (the default, untouched by this bug
report) always resolves to NSW direct, in both debug and release, regardless
of the live `enable_proto_bff` RC value. Unless a developer manually flips
Debug Config → Network to `BFF_LOCAL`/`BFF_PROD`, this debug build hits NSW
direct — the same path the curl test above exercised. So a BFF backend that
doesn't understand `streetID:...` origins is not the explanation either
(worth re-checking only if someone *has* explicitly selected a BFF source).

## Extended capability matrix — what NSW's API can and can't do

All tested against the live NSW API, same param shape the app already sends
(`type_origin=any` / `type_destination=any`, no branching). Everything below
**works**, first try, no special-casing required:

| Origin/destination type | Result |
|---|---|
| `singlehouse` (full "number + street + suburb") | ✅ walk leg + transit leg(s), correct |
| `street` (no house number, e.g. "Example St, Parramatta") | ✅ resolves to a street-midpoint coord, same behavior |
| `poi` (e.g. "Sydney Opera House") | ✅ resolves fine, walk leg to nearest stop correct |
| **address → address** (both origin AND destination are addresses, no real stop on either end) | ✅ first-mile walk + multi-modal transit (train + light rail) + last-mile walk, all correct, 6 journeys returned |
| Raw GPS coord ("current location", no `stop_finder` involved) | ✅ but **needs a different param shape**: `type_origin=coord` + `name_origin=<lon>:<lat>:EPSG:4326` (confirmed separately, still the only case that needs branching) |

**Walk-leg polyline is already present and already mapped client-side.** The
raw NSW-direct JSON leg for every footpath leg above includes a `coords`
array (confirmed: 30 lat/lng points for the Example St → Parramatta Station
walk) — full turn-by-turn geometry, elevation info, stairs/accessibility
detail (`footPathInfo`). `TripResponse.Leg.coords` (network model,
`TripResponse.kt:105`) already deserializes this field. **This rules out
hypothesis #3 below** — JourneyMap does not need BFF-proto data to draw a
walk leg; the geometry is already flowing through the NSW-direct JSON path
the app uses by default.

### Bottom line

Nothing tested is an API limitation. Every location type `stop_finder` can
return (`singlehouse`, `street`, `poi`) resolves correctly through `/trip`
under the exact param shape already hardcoded in
`RealTripPlanningService.appendTripQueryParams()` — no new params, no
coord branch, no BFF dependency. The one exception (raw GPS "current
location") is a separate, already-solved case. **The blocker is 100%
client-side.**

## Full client-side trace (static code review)

Traced the entire path end to end against the real captured JSON from the
API tests above. Selection → `SearchStopEntry` (event bus, not a serialized
nav route — `StopSelectedResult` is a plain in-memory data class) →
`SavedTripsEntry.ResultEffect<StopSelectedResult>` → `SavedTripsViewModel
.onFromStopChanged/onToStopChanged` (`StopItem.toJsonString()` /
`fromJsonString()` round-trip — plain JSON, colons need no escaping, no
length limit hit) → `RealStopResultsManager.setSelectedFromStop/ToStop` →
`SavedTripsEntry.triggerTripSearch` / `onSavedTripCardClick` →
`navigateToTimeTable` (`TimeTableRoute` is a plain `@Serializable` data
class of four strings, in-memory navigation3 backstack, not URL-encoded) →
`TimeTableEntry` → `TimeTableViewModel.initializeTrip` → `onLoadTimeTable` →
`fetchTrip`/`loadTrip` → `tripPlanningService.trip()` (proven working, see
above) → `TripResponseMapper.buildJourneyListWithRawData` →
`TripResponseLegMapper` → `journeyId` generation (derived purely from
`Leg.TransportLeg.tripId`, never touches origin/destination `stopId`, so
address-shaped IDs can't collide or break it).

**Nothing in that chain rejects, truncates, or mishandles a `streetID:...`
value** — checked against the actual 2-leg (footpath + train) response
captured earlier. `isWalkingLeg()` correctly identifies the footpath leg via
`productClass == 99 || 100` (confirmed `class: 100` in the real response) so
`getFirstPublicTransportLeg()` correctly skips it and picks the train leg —
this is pre-existing, address-agnostic behavior (any walk-then-transfer
journey works the same way).

### Confirmed bug (real, but likely NOT the Timetable blocker)

`RecentSearchStops.stopId` has `FOREIGN KEY REFERENCES NswStops(stopId)`
(`sandook/.../RecentSearchStops.sq`), but no `PRAGMA foreign_keys=ON` exists
anywhere in the codebase (grepped, zero hits) — SQLite defaults FK
enforcement to **off**, so the `INSERT` when saving an address to Recents
(`RealStopResultsManager.saveRecentSearchStop`) almost certainly succeeds
without throwing. But `selectRecentSearchStops` uses `INNER JOIN NswStops`
— an address's orphaned row will never match that join, so **an
address-selected stop silently never appears in Recents**, forever. Real
bug, worth fixing, but doesn't explain a blank/broken Timetable screen.

### Ruled out

- ~~JourneyMap polyline dependency on BFF proto~~ — the `coords` polyline is
  present in the NSW-direct JSON and already deserialized into
  `TripResponse.Leg.coords`.
- ~~Nav-arg encoding~~ — no route/URL encoding is involved anywhere in this
  path; everything is in-memory (event bus + navigation3 backstack), so
  string length/shape can't be the break.

### Still open

Static review found nothing else that would blank or break the Timetable
screen. The symptom needs a live repro to pin down — next time it's
reproduced, capturing what's actually on screen (blank list? error state?
app crash? stuck loading?) plus logcat (`log("🗺️ ...")` lines are already
threaded through `TimeTableViewModel`/`TimeTableEntry`) would narrow it
immediately.

## Next step

Confirm which of the above (or something else) is the actual client-side
break — needs stepping through the real selection → navigation →
`TimeTableViewModel` flow with a debugger/logcat rather than more API testing,
since the API side is now cleared.
