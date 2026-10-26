# Crowding: build plan

How to build the crowding feature designed in
[`CROWDING_DESIGN_REVIEW.md`](../investigations/CROWDING_DESIGN_REVIEW.md). Decisions are final:
**1A 2A 3A 4A 5C 6A**, with 4A withdrawn on 27 October (see the design review). Read these before starting:

1. [`TFNSW_OCCUPANCY_DATA.md`](../investigations/TFNSW_OCCUPANCY_DATA.md): what the feeds carry,
   coverage, quirks.
2. [`CROWDING_DESIGN_REVIEW.md`](../investigations/CROWDING_DESIGN_REVIEW.md) and its `.html`:
   the design, every state, the wording table. The wording table is the copy source of truth.
3. [`FEATURE_QUALITY_CHECKLIST.md`](../FEATURE_QUALITY_CHECKLIST.md),
   [`NETWORK_RELIABILITY.md`](../NETWORK_RELIABILITY.md), [`ANALYTICS_EVENTS.md`](../ANALYTICS_EVENTS.md),
   [`taj/THEME_COLOUR_ROLES.md`](../../taj/THEME_COLOUR_ROLES.md).

## Scope of version 1

| In | Out |
|---|---|
| Seat tag on trip results and the departure board | "Usually" patterns (need many weekdays of data) |
| Carriage row in trip cards and the live trip screen | "Front" and "back" wording until the platform check passes |
| Departure rows that open to the sentence and carriage row | Light rail L1 to L3 and ferries (no data) |
| Next-service line (train and bus) | Cancellations, skipped stops, short trains (see [`TRIP_UPDATES_FEED_IDEAS.md`](TRIP_UPDATES_FEED_IDEAS.md)) |
| Code-written lines about seats later in the trip (next) | |

Zone wording until the platform check passes: "both ends", "middle" and the all-along lines
only. "At the back" and "at the front" are built but gated by a constant, so turning them on is a
one-line change once the end is verified.

## Where the data comes from

| Data | Source | Already fetched? | Code today |
|---|---|---|---|
| Train level per stop on a trip | `/v1/tp/trip`, `stopSequence[].properties.occupancy` | Yes | Modelled, unused: `TripResponse.kt` `DestinationProperties.occupancy` |
| Level on a departure | `/v1/tp/departure_mon`, `stopEvents[].location.properties.occupancy` | Yes | Not modelled: add to `DepartureMonitorResponse.LocationProperties` |
| Per-car forecast per stop (Sydney Trains) | `/v2/gtfs/realtime/sydneytrains`, StopTimeUpdate extension 1007 (car field 2 = sequence, 7 = level) | Only on the live trip screen | `GtfsRealtimeService` (`TRIP_UPDATES`) in `:feature:track:network` |
| Per-car now (Metro, and live trip screen) | `/v2/gtfs/vehiclepos/{sydneytrains,metro}`, VehiclePosition extension 1007 (1 name, 2 position, 3 level) | Live trip screen | Same service, `VEHICLE_POSITIONS` |
| Join key | `RealtimeTripId` / `gtfsTripId` on trip and departure responses | Yes | Check both are modelled |

`io/gtfs/src/commonMain/proto/gtfs-realtime.proto` is a trimmed copy. It has no
`occupancy_status` and no TfNSW extension, so Wire drops those fields today. Add them as plain
fields on the messages (`VehiclePosition.occupancy_status = 9`, `StopTimeUpdate` field 6, and the
1007 carriage messages), then confirm Wire decodes a captured feed in a test fixture.

Quirks to code for: car position is 1-based on Sydney Trains and 0-based on Metro (re-sequence
from sorted order); some vehicle readings are hours old (drop anything older than one refresh);
the feed is about 285 KB for the whole network and cannot be filtered per trip.

## Branch stack

All local until the maintainer says otherwise. Each branch under 500 lines.

| # | Branch | What | Tests |
|---|---|---|---|
| 1 | `feat/crowding-model` | `SeatLevel` enum (three levels, parse from the trip/departure strings and GTFS ints), `CarriageLevels`, zone maths (busiest level per third, which zone has seats), wording builder from the design review table. Pure Kotlin, no UI | Unit tests for every row of the wording table, alternating cars, 4-car and 8-car, 0-based and 1-based |
| 2 | `feat/crowding-departure-occupancy` | Model `occupancy` on departure responses; carry `SeatLevel` into `StopDeparture` and the trip journey/leg UI state | Mapper tests with captured JSON |
| 3 | `feat/crowding-gtfs-carriages` | Proto fields above; a repository that fetches the Sydney Trains trip updates once per results screen, indexes by trip id, and exposes per-stop car levels. Behind the kill switch. Lives where both trip planner and departures can reach it without the `track:ui` / `trip-planner:ui` cycle (a small `:feature:crowding:network` + `:state` is the likely home; decide after reading the module graph) | Fixture-based decode test; repository test with the boundary fake from `:core:testing`; network failure typed per `NETWORK_RELIABILITY.md` |
| 4 | `feat/crowding-tags` | Seat tag on `JourneyCard` and `DepartureRow` | Screenshot tests, light, dark, 200% font |
| 5 | `feat/crowding-carriage-row` | Carriage row composable (one ink, fill = level, legend row, three blocks at narrow width or large font), sentence, collapse-on-failure animation; wired into the opened trip card and the live trip screen | Screenshot tests for each scenario on the review page; state-height test |
| 6 | `feat/crowding-departure-expand` | Departure rows open to sentence and carriage row; expanded state kept per trip with `rememberSaveable` | Rotation and refresh keep the row open |
| 7 | `feat/crowding-next-service` | Next-service line with the eligibility rules (one level quieter, within about 10 minutes, no tight connection later, not last service, bus listed at the same stop); tap jumps to that card | Rule tests per condition |
| 8 | `feat/crowding-tag-bars` | Small bars in the tag, no pill, no key, forecasts refreshed with the screen, smooth level changes. (The AI line branches were built and removed; see the design review) | Screenshot tests; loader refresh and failure tests |

## Flags

| Flag | Key | Default | Debug override |
|---|---|---|---|
| Carriage row in results (the one extra download) | `CROWDING_CARRIAGES_IN_RESULTS_ENABLED("crowding_carriages_in_results_enabled")` | on | Yes |

Add each to `FlagKeys.kt` and `RemoteConfigDefaults.kt`, and a row in `DebugConfigScreen.kt` the
way `alertSummaryEnabled` is done (`DebugSettingsState`, `DebugSettingsEvent`).

## Analytics

Run the `ANALYTICS_EVENTS.md` checklist before adding anything. Prefer params on existing events
(journey card expand, departure row tap) over new event names. Any change adds a ledger row in
`ANALYTICS_REGISTRY_HANDOFF.md`.

## Before shipping

1. **Platform check** (`TFNSW_OCCUPANCY_DATA.md`, "Platform check"): which end of the train the
   feed's first car is. Until it passes, "back" and "front" stay off.
2. Store data declarations: no new data type leaves the device, but re-read
   `STORE_DATA_DECLARATIONS.md` for the new request.
3. QA checklist in `CLAUDE.md` on device: rotate with the card open and loading, theme switch,
   200% font, feed failure (airplane mode after results load), Metro and bus trips, a trip with no
   crowding data at all (draws nothing).
