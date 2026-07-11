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
3. **JourneyMap polyline dependency on BFF proto** — `RealTripPlanningService`
   comments that the BFF proto path "carries the polyline data the
   journey-map needs." If JourneyMap's walk-leg rendering assumes proto-shaped
   data that only the BFF path provides, a walk leg arriving via the NSW-direct
   JSON path (the one actually exercised for addresses, per BFF routing above)
   might have no polyline to draw even once the journey itself displays
   correctly in the Timetable list.

## Next step

Confirm which of the above (or something else) is the actual client-side
break — needs stepping through the real selection → navigation →
`TimeTableViewModel` flow with a debugger/logcat rather than more API testing,
since the API side is now cleared.
