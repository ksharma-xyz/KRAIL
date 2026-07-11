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

## Where the real bug likely is (not yet confirmed)

Since the API itself is proven to work, the break is somewhere in the
client's selection → navigation → `TimeTableViewModel` path. Candidates,
in rough order of suspicion, **none confirmed yet**:

1. **Nav-arg length/encoding** — `StopItem` is serialized to JSON
   (`toJsonString()`) and passed as a navigation argument. A `streetID:...`
   value is much longer (~140 chars, many colons) than a normal stop ID
   (≤32 chars, alphanumeric) — worth checking whether the nav route encoding
   truncates, mis-escapes, or otherwise mangles a string this shape/length.
2. **Local-DB name re-lookup** — if `TimeTableViewModel` (or anything on the
   way to it) calls something like `fetchLocalStopName(stopId)` /
   `sandook.selectStops(...)` to redisplay the origin's name, that lookup
   will find nothing for an address ID (it only exists in the local GTFS
   stops table for real transit stops) and may null out / blank the screen
   instead of falling back to the name already carried on the `StopItem`.
3. ~~JourneyMap polyline dependency on BFF proto~~ — **ruled out**, see the
   capability matrix above. The `coords` polyline is present in the
   NSW-direct JSON response and already deserialized into
   `TripResponse.Leg.coords`. If JourneyMap still doesn't draw the walk path
   once a journey does render, that would be a separate JourneyMap-rendering
   bug (e.g. UI code not reading `coords` for a footpath-only leg), not a
   missing-data problem.

## Next step

Confirm which of the above (or something else) is the actual client-side
break — needs stepping through the real selection → navigation →
`TimeTableViewModel` flow with a debugger/logcat rather than more API testing,
since the API side is now cleared.
