# TfNSW crowding (occupancy) data

What Transport for NSW publishes about how full a service is, how reliable it is, and the rules
that follow for showing it in KRAIL. Measured against the live feeds on Monday 26 October 2026.
Read this before building any crowding UI or touching an `occupancy` field.

## Summary

- Crowding comes as **three levels only**: Seats available, Few seats, Standing only
  (`MANY_SEATS_AVAILABLE`, `FEW_SEATS_AVAILABLE`, `STANDING_ROOM_ONLY`). The GTFS-R
  `occupancy_percentage` field exists but was empty on every poll. There are no headcounts.
- Trip planner and departure monitor values exist only for services leaving in roughly the
  **next 30 to 50 minutes**. Asking about a later time returns nothing, so it is a live reading,
  not a timetable forecast.
- Coverage depends on the mode. See the table below. Light rail L1 to L3 and ferries report
  nothing at all.

## Where the data is

| Source | Path | What it carries |
|---|---|---|
| Trip planner | `/v1/tp/trip` | `stopSequence[].properties.occupancy` per stop. KRAIL models it (`TripResponse.kt`, `DestinationProperties.occupancy`) but nothing reads it yet |
| Departure monitor | `/v1/tp/departure_mon` | `stopEvents[].location.properties.occupancy`, plus `properties.RealtimeTripId` and `transportation.properties.gtfsTripId` to join with GTFS-R |
| Vehicle positions | `/v2/gtfs/vehiclepos/sydneytrains`, `/v2/gtfs/vehiclepos/metro`, `/v1/gtfs/vehiclepos/buses` | `VehiclePosition.occupancy_status` (field 9) and, for trains and metro, per carriage in TfNSW extension field 1007 |
| Trip updates | `/v2/gtfs/realtime/sydneytrains` | Per stop, per carriage **forecast** of departure occupancy (StopTimeUpdate extension 1007, carriage field 7). Produced by a Sydney Trains prediction model |
| Light rail | `/v2/gtfs/vehiclepos/lightrail/innerwest`, `/v1/gtfs/vehiclepos/lightrail/cbdandsoutheast`, `/v1/gtfs/vehiclepos/lightrail/parramatta`, `/v1/gtfs/vehiclepos/lightrail/newcastle` | Note the mixed v1 and v2 paths. `/v1/.../innerwest` returns 404 |

Extension 1007 on a vehicle position is the consist: one message per carriage with
`1 name, 2 position_in_consist, 3 occupancy_status, 4 quiet_carriage, 5 toilet, 6 luggage_rack`.
The live trip screen already downloads these feeds (`RealGtfsRealtimeRepository`).

## Coverage by mode

| Mode | Vehicle level | Per carriage | Forecast at later stops |
|---|---|---|---|
| Sydney Trains | About 40% of trains live (sets with weight sensors) | Same trains | Yes, including T4 |
| Metro | Every train | Every train | No |
| Buses | Every bus | Not applicable | Not checked (feed is about 5 MB per call) |
| Light rail L1, L2, L3 | None | None | None |
| Parramatta Light Rail | Yes | No | No |
| Newcastle Light Rail | Yes | Yes | No |
| Ferries | None | None | None |

On Sydney Trains a train has carriage data on every car or on none, and T4 and the Central
Coast line had almost no live readings. The per-stop forecasts do cover T4.

## How each mode reaches the screen

No feed carries a number of seats or people for any mode. Everything is one of the three levels.

| Mode | What TfNSW sends | Where KRAIL reads it | What the rider sees |
|---|---|---|---|
| Buses | One level for the whole bus, from the moment it starts its trip | The trip and departure responses the screen already loads | The words beside the time. A bus that has not started its trip has no level, so later buses get their tag as they set off |
| Sydney Trains | A forecast level for every car at every stop still ahead, from the trip updates feed | One extra download of that feed, refreshed with the results screen every 30 seconds | Small bars and words on the closed card, boxes and a sentence on the opened card |
| Metro | A live level for every car, but no forecast for later stops | Vehicle positions, on the live trip screen only | Whole-train words in results; boxes once the rider is tracking the train |
| Parramatta Light Rail | One level per tram | Trip and departure responses | Words |
| Newcastle Light Rail | A level per car | Trip and departure responses | Words; boxes would be possible later |
| L1, L2, L3, ferries | Nothing | | Nothing |

The trip updates feed is network-wide and cannot be filtered per trip, so one download covers
every train on the screen. It changes about once a minute: a single download when the screen
opened left a train twenty minutes out several minutes stale on 27 October, and missed trains
that had no forecast yet at that moment. KRAIL now refreshes it with the results screen, keeps
the last forecast through a failed refresh and clears it after three failures in a row.

Why many trains in a list show nothing, checked on 27 October against the live feed:

- Only trains with weight sensors report. The trip id's fifth part looks like the train type
  (`A` and `B` trains reported, `H` and `T` never did), but TfNSW does not publish the letters,
  so treat that as a likely pattern, not a rule.
- A train often has no forecast until shortly before it starts its run. The 30 second refresh
  picks it up when it appears.

## Forecasts along the trip

The trip updates feed forecasts every car at every stop still ahead, not just where the rider
boards. Code can read that forecast at each stop between boarding and getting off, for any line
and any station, and name the station where it changes. Nothing is hard-coded to a place.

| Case | Example line |
|---|---|
| Seats open up later | "More seats expected from Strathfield." |
| The train fills up | "Expected to fill up from Parramatta." |
| The second train of a trip with a change | "Your next train, the T8 from Central, is expected to be standing only." |

A line is only written when all of these hold, otherwise nothing is said:

- The change is at least one whole level.
- It lasts until the rider gets off, not one stop and back.
- That stop is within about 30 minutes; forecasts are within one level more than nine times in
  ten but exactly right only about two times in three, and worse further out.
- Every stop in between has a forecast.
- The wording says "expected", never a promise.

Before shipping, replay collected peaks: for every trip where the forecast said seats open up at
a station, check the live reading when the train got there. Ship a case only if it held on most
trips, and record the result here in words, not counts.

## Reliability

- 40 polls of trains, metro and buses over 40 minutes of the morning peak: every request
  answered, coverage steady from poll to poll.
- Levels change slowly: about 1 to 5% of vehicles change level per minute; jumps straight from
  Seats available to Standing only are rare.
- Some train positions in the feed are hours old. Ignore readings whose vehicle timestamp is
  older than one refresh cycle.
- **Forecast accuracy, measured**: comparing each per-stop forecast with the train's live
  reading when it reached that stop, forecasts were exactly right about three times in four and
  within one level more than nine times in ten (5 to 30 minutes ahead, Monday morning).

## Quirks

- Carriage position is 1-based on Sydney Trains and 0-based on Metro. Re-sequence from sorted
  order.
- Trip update stop times carry an arrival or departure delay on most stops but a clock time on
  only some. Order comes from the feed order.
- Trip update stop ids are platform ids, which are not in `NSW_STOPS.pb`. Resolve them with
  `/v1/tp/stop_finder` (`disassembledName` gives the station; `parent.name` is the suburb).
- `tripMessage` alerts and some vehicles linger for hours; always check timestamps.
- Route numbers repeat across NSW. Match mode and region, not the number alone.

## Field study plan

Rider suggestions beyond a live reading ("usually eases off after Strathfield") need evidence
that a pattern repeats. The plan:

1. Poll every feed above through both peaks (7:30 to 9:00am, 4:30 to 6:30pm Sydney time), every
   minute for rail, every three minutes otherwise, across every line and mode.
2. Test these themes per line and direction: ends vs middle of the train, where load eases or
   builds, whether the next service after a Standing-only one is quieter, bus bunching, and
   forecast accuracy by lead time.
3. Evidence tiers: a pattern seen in one peak is a lead only and never shown to riders. A pattern
   holding on at least 8 of 10 weekdays may use "usually". Forecast accuracy is measured, not
   assumed. A live reading can always be shown as it is.
4. Published research supports two kinds of advice best: choosing a carriage, and taking a later
   service with seats instead of an overcrowded one. Most of it is simulation or survey work, so
   KRAIL's own measurement of what riders do is the missing piece.

## What else the Sydney Trains trip updates feed carries

Checked on Monday 26 October 2026, evening (619 trips in one fetch). The same download that
brings the carriage forecasts also has:

| Field | What it says | Seen |
|---|---|---|
| `TripDescriptor.schedule_relationship` | Scheduled, added, cancelled (3) or replacement (5) | 97 cancelled and 139 replacement trips in one evening fetch |
| `StopTimeUpdate.schedule_relationship` | A stop the train will skip (1) | 30 skipped stops |
| `StopTimeUpdate.departure.delay` | Seconds late at each stop | On most stops; 65 stops 5 minutes or more late |
| `StopTimeUpdate.departure_occupancy_status` | Whole-train level per stop | About a third of stops |
| Carriages per stop (extension 1007) | 8 or 4 cars | Tells a short train apart, which stops mid-platform |
| Vehicle extension 1007 field 3 | One letter per train type (A, B, D, T, H, M and others) | Every trip. The type implies facilities such as toilets; the letter mapping is not documented, so check it before use |

The trip planner already gives KRAIL delays, so only cancellations, skipped stops, short trains
and train type would be new. None of these are in the crowding build; they are noted here so a
later feature does not need a second download. The ideas are parked in
[`docs/plans/TRIP_UPDATES_FEED_IDEAS.md`](../plans/TRIP_UPDATES_FEED_IDEAS.md).

## Platform check: which end is the front

Checked again on 27 October: the car ids in the vehicle feed are anonymised per trip (for
example `5901.5212.6615...`) and do not match the numbers painted on the carriages, so the feed's
first car cannot be tied to a real end from data alone.

Zone advice ("Head to the back for more room") is worse than nothing if the ends are swapped,
and carriage order differs between feeds and may flip when a train reverses at a terminus.
Before shipping any zone advice, check on a real platform:

1. Pick a Waratah service on a line with live carriage data (T1 Western or T8 Airport).
2. Before it arrives, note the per-carriage levels the feed reports for that trip, in feed order.
3. As it arrives, note which end leads and the guard's position (middle of the train).
4. Walk the train and note which cars are visibly fuller. Note the car number painted inside.
5. Record whether feed position 1 is the leading car, the trailing car, or varies.
6. Repeat with a train that has just reversed at a terminus (for example at Central).
7. Repeat once on Metro.

Record the answer next to the mapper that orders carriages, with the date checked.

## Design rules that follow from the data

- Show levels as words. Never invent a number or a percentage.
- No car numbers in the UI. Riders cannot count carriages on a platform; use Front, Middle
  and Back.
- Advice informs and never instructs. Name another service the way the station board does:
  line, clock time with am or pm, platform.
- Match certainty in the wording: live "has", forecast "should", a pattern proven over many days
  "usually", no data says nothing.
- At night, do not point riders to the emptiest carriage; NSW Police advise the blue Nightsafe
  area near the guard.
- Data that arrives with a response the screen already loads has no loading state of its own.
  Data from a second feed is fetched once per screen and drawn only if it is ready.
