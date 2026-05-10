# KRAIL → BFF · Phase C integration report (proto consumers)

> Audience: KRAIL-BFF team. From: KRAIL app side. Drop into
> `KRAIL-BFF/docs/handover/` as the symmetric counterpart to your
> `MIGRATION_GUIDE.md` Phase C.
>
> Status: All three v0.3.0 proto endpoints consumed end-to-end on the
> KRAIL side, behind a single `IS_BFF_PROTO_ENABLED` flag (currently
> hard-coded `true` in this branch; future Phase B production wires
> this to Firebase RC `enable_proto_bff`).

---

## §1 · TL;DR

`KRAIL-API-PROTO v0.3.0` is fully wired in the KRAIL app. Three new
mappers, three service refactors, 23 new tests, all green.

| Endpoint | KRAIL service | KRAIL mapper | Tests |
|---|---|---|---|
| `GET /api/v1/trip/plan-proto` | `RealTripPlanningService.trip()` | `JourneyListMapper.kt` | 7 (commit `8a9087f07`) |
| `GET /api/v1/stops/{id}/departures-proto` | `RealDeparturesService.departures()` | `DepartureBoardMapper.kt` | 7 (commit `bbd257260`) |
| `GET /api/v1/parking/availability-proto?stopIds=` | `RealParkRideService.fetchAvailabilityForStops()` | `ParkingAvailabilityMapper.kt` | 9 (commit `1546e229b`) |

Each mapper consumes the proto and produces the existing JSON-shape
domain model so downstream UI / map mappers work unchanged. The JSON
paths stay in code as fallback when the proto flag is off.

What we need from BFF next, in order:

1. Deploy to DigitalOcean (Phase B is still blocked on this).
2. Hold `MIN_APP_VERSION = 0.0.0` until KRAIL ships the
   `X-Krail-Version` default header.
3. A handful of small schema gaps to consider in the next minor bump
   (see §4).

---

## §2 · What worked (validated against running local BFF)

Wire 6.2.0 codegen on KMP-iOS for v0.3.0 — clean. No fallback to
`kotlinx-serialization-protobuf` needed.

The "screen-shaped proto maps cleanly to JSON-shape domain model"
strategy held up. Every consuming UI screen kept working without code
changes downstream of the network layer. Specifically:

- **Trip results + journey-map polylines**: `JourneyMapMapper.kt`
  reads `TripResponse.Leg.coords` to draw the route line. Mapper lifts
  proto `TransportLeg.coords[Coord{lat,lon}]` into the
  `List<List<Double>>` format the existing mapper expects. **The
  blank-polyline bug from yesterday is fixed end-to-end.**
- **Departures**: `DepartureMonitorResponse.stopEvents[].transportation`
  populated from `DepartureRow.line` (TransitLine), planned/estimated
  UTCs flow through unchanged.
- **Park & Ride**: `?stopIds=` mode only (per your "ids deprecated"
  decision). Per-stop facility map populated; existing
  `ParkingStopBatchResponse` shape preserved.

The contract-required vs contract-optional distinction in your proto
comments was useful — drove our mapper's null-handling decisions
unambiguously.

---

## §3 · Decode + map observations

### Trip mapper highlights

- `JourneyCardInfo.origin_utc_date_time` /
  `destination_utc_date_time` propagate to the first leg's origin /
  last leg's destination as `departureTimePlanned` /
  `arrivalTimePlanned`. Per-intermediate-leg UTCs aren't in proto, so
  intermediate legs use the journey-level UTC as a fallback (good
  enough for journey-card surfaces; map screen doesn't need
  intermediate UTCs).
- Walking legs synthesize a `Transportation` block with
  `productClass = 99` so the existing `TripResponseExt.isWalkingLeg()`
  helper keeps working.
- `WalkInterchange.coords` and `WalkingLeg.coords` lift into
  `Leg.coords` (NSW JSON didn't distinguish the two — both flow
  through the same field).
- Round-tripped via `JourneyList.ADAPTER.encode/decode` in tests with
  no observed data loss.

### Departures mapper highlights

- `StopRef` populates each `StopEvent.location` (NSW JSON has the same
  stop on every event; we mirror that).
- `is_realtime = true` is currently consumed via
  `departureTimeEstimated != null` heuristic in our existing parser —
  the proto's explicit `is_realtime` boolean isn't sunk into the
  `DepartureMonitorResponse` model because the JSON model doesn't
  have a corresponding field. **Acceptable today.** If the BFF ever
  ships a `MONITORED_PT_NETWORK_OUT` etc. status enum on the proto,
  we'll wire `realtimeStatus` then.

### Parking mapper highlights

- `?stopIds=` shape maps cleanly to existing
  `ParkingStopBatchResponse`.
- Numeric fields (`total_spots`, `occupied_spots`) become String to
  match the NSW-quirk preserved in our existing parsers
  (numbers-as-strings). KRAIL's existing UI already handles this.
- `correlation_id` propagates to the response root for log threading.

---

## §4 · Schema gaps we hit (proto v0.3.0)

Documented inline in each mapper's kdoc. None are blocking; listed in
priority for a future minor bump consideration:

### Departures

- **`DepartureRow.is_realtime` flag has no JSON-model sink.** KRAIL
  infers realtime from `departureTimeEstimated != null`. If you
  eventually ship a finer-grained `realtime_status` enum on the proto
  (CANCELLED, MONITORED, MONITORED_PT_NETWORK_OUT, etc.), we can wire
  it then. Today's mapping is fine.
- **`DepartureRow.trip_id` has no sink.** Useful for matching against
  GTFS-RT trip updates. Add a `tripId` field to `DepartureMonitorResponse`
  on the KRAIL side when we hook this up.
- **`DepartureRow.date_label`** ("today" / "tomorrow" / "in 2 days") —
  not used; KRAIL computes its own date labels client-side.
- **`TransitLine.color_hex`** — not used; mode color is rendered from
  `transport_mode_type` via KRAIL's existing palette. If you intend
  this to be authoritative server-side coloring eventually, KRAIL
  needs a small UI hook for it.

### Trip

- **Per-intermediate-leg UTCs.** Proto carries journey-bookend UTCs
  but not per-stop arrival/departure UTCs for intermediate stops on a
  multi-leg journey. Workaround: we duplicate the journey-level UTC.
  No screen renders intermediate UTCs today, so fine. Consider adding
  `Stop.arrival_utc` / `Stop.departure_utc` if a future screen needs
  them.
- **`Leg.duration_seconds`** (numeric, for sorting/filtering) —
  proto carries display strings only. Future screen filtering by
  shortest leg would need this. Not blocking.
- **`Leg.distance` / `Leg.hints` / `Leg.footPathInfo`** — not in
  proto, not in any current screen. Acceptable.

### Park-ride

- **`zones[]` per-zone breakdown** — proto only carries aggregate
  totals. KRAIL doesn't render per-zone today, but the JSON model
  still has the field. Mapper emits empty list. Fine.
- **NSW-internal IDs** (`tsn`, `time`, `parkID`, `tfnsw_facility_id`) —
  not in proto. Mapper supplies empty / 0 defaults. Unused by KRAIL.
- **Detailed `occupancy` sub-fields** (`loop`, `monthlies`,
  `transients`, `open_gate`) — only aggregate `occupancy.total` maps.
  No screen renders the breakdown.

---

## §5 · What we need from BFF next, in priority order

### §5.1 — Deploy to DigitalOcean (still blocking)

Same ask as Phase A integration report. Phase B production rollout
needs the deployed `bff.krail.app` host + Cloudflare + the env vars in
your `STATUS.md` §1.

### §5.2 — Hold `MIN_APP_VERSION = 0.0.0`

Until the next KRAIL release ships the `X-Krail-Version` default
header. Coordinated bump after that.

### §5.3 — `is_realtime → realtime_status` enum (low priority)

If realtime status diversification matters in the future (e.g.
distinguishing CANCELLED vs MONITORED), schema-bump
`DepartureRow.realtime_status` as a typed enum and we'll wire it.
Otherwise leave as-is; current `is_realtime` boolean is enough.

### §5.4 — `DepartureRow.trip_id` consumer wiring (medium priority)

When KRAIL surfaces "track this departure" actions in the departure
board UI, we'll need the proto's `trip_id` to flow through to a
KRAIL-side field for GTFS-RT matching. Not blocking; just a heads-up
that the field is currently parsed-but-unused on our side.

### §5.5 — Documentation freshness

Your `MIGRATION_GUIDE.md §4` ("What's NOT in v0.2.0") is now slightly
outdated since v0.3.0 added departures + parking schemas. Quick edit
welcome. The `API_REFERENCE.md` sections 4b + 6c are accurate based
on the captured fixtures we used to drive our tests.

---

## §6 · Things to ignore (KRAIL-side noise)

- **Programmatic Wire builders in mapper tests.** We ship the BFF's
  fixture `.pb` files were copied initially but we ended up using
  Wire's builders to construct minimal `JourneyList` /
  `DepartureBoardResponse` / `ParkingAvailabilityResponse` instances
  in tests. Easier to read, less binary-blob noise in the repo. The
  fixtures themselves are still useful for future integration tests
  against the live BFF; you can keep them in
  `KRAIL-BFF/docs/handover/fixtures/`.
- **`JSON pass-through paths still in code.** All four service files
  carry both branches: NSW direct + JSON-via-BFF + proto-via-BFF.
  Cleanup happens in Phase E (after 100% rollout + 2-week grace), not
  now.

---

## §7 · Document accuracy review

| Doc | Verdict |
|---|---|
| `KRAIL-BFF/docs/handover/README.md` | Accurate. The "what's done" status table matched reality. |
| `KRAIL-BFF/docs/handover/API_REFERENCE.md` §3 (trip) / §4b (departures) / §6c (parking) | Accurate. Sample wire payloads matched what we decoded in tests. Field-by-field tables drove our mappers without surprises. |
| `KRAIL-BFF/docs/handover/MIGRATION_GUIDE.md` §4 (Phase C playbook) | Useful as scaffolding; the actual scope grew to three consumers (you'd anticipated the same in §1 TL;DR). |
| `KRAIL-BFF/docs/handover/TESTING_GUIDE.md` | Useful — referenced for fixture conventions. We diverged on test approach (Wire builders vs fixture decode), so didn't end up using the patterns there directly, but the fixtures helped us validate decode shape during exploration. |
| `KRAIL-BFF/krail-api-proto/proto/api/{trip,departures,parking}.proto` (v0.3.0) | Excellent. Contract-required vs contract-optional comments drove every null-handling decision in our mappers. Keep this convention. |

---

## §8 · References

KRAIL side:

- Phase C branch: `feat/proto-trip-results` (stacked on
  `feat/bff-local-debug-override` = PR #1582). Local commits, not
  pushed yet.
- Plan doc: `docs/bff-integration-plan.md`
- This doc: `docs/BFF_PHASE_C_INTEGRATION_REPORT.md`
- Phase A integration report (counterpart): `docs/BFF_PHASE_A_INTEGRATION_REPORT.md`

Phase C commits (in order):

```
1546e229b feat(park-ride): adopt BFF proto endpoint /api/v1/parking/availability-proto
bbd257260 feat(departures): adopt BFF proto endpoint /api/v1/stops/{id}/departures-proto
f6dc5efca refactor(network): rename IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED to IS_BFF_PROTO_ENABLED
8a9087f07 feat(trip-planner): adopt BFF proto endpoint for trip results
```

BFF side (cross-linked):

- `docs/handover/README.md`
- `docs/handover/API_REFERENCE.md`
- `docs/handover/MIGRATION_GUIDE.md`
- `docs/handover/TESTING_GUIDE.md`
- `docs/handover/fixtures/{trip,departures,parking}-*.pb`
- `krail-api-proto v0.3.0`

External:

- KRAIL-API-PROTO repo: <https://github.com/ksharma-xyz/KRAIL-API-PROTO>
- KRAIL-API-PROTO docs: <https://ksharma-xyz.github.io/KRAIL-API-PROTO/>
