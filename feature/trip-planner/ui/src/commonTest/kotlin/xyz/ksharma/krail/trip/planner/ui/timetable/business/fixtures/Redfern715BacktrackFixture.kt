package xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures

/**
 * Fixture for a real NSW Transport API trip response: Seven Hills Rd opp Monaro St (214758)
 * → Redfern/Central, captured 2026-07-03.
 *
 * Source: real API response, journey index 1 of the response captured for this query. Full
 * capture (coords/pathDescriptions stripped, everything else verbatim) committed at
 * `feature/trip-planner/network/src/commonTest/assets/trip_seven_hills_redfern_715_backtrack.json`.
 * This fixture trims it further to just the essential fields (as [OranParkToSevenHillsFixture]
 * does) and keeps only the 4-leg journey that reproduces the bug — the other 8 journeys in the
 * real response are single-interchange 715→M1 options and aren't needed to test this mapping.
 *
 * Background: see `docs/investigations/NSW_715_WALK_LEG_INVESTIGATION.md` for the full writeup.
 * In short — NSW represents a 715 bus that reverses direction one stop from the origin (to
 * board a different scheduled trip, `tripCode` 77 → 15, opposite-direction `description`) as:
 *
 *   Leg 0: 715 (outbound, tripCode 77)   Seven Hills Rd opp Monaro St → after Solander Rd
 *   Leg 1: walk (IDEST, 60s)             after Solander Rd → before Solander Rd (cross the road)
 *   Leg 2: 715 (inbound, tripCode 15)    before Solander Rd → Seven Hills Station
 *   Leg 3: T1                             Seven Hills Station → Central
 *
 * Google Maps and Opal both show this as a single 715 leg + T1, not two 715 legs split by a
 * walk. [collapseSameRouteQuickWalks] (in `TripResponseMapper.kt`) is what makes KRAIL do the
 * same. This fixture is the regression test for that specific behaviour: if a future change to
 * the collapse heuristic (or an unrelated refactor of [TripResponse.Leg] mapping) breaks it,
 * this is the test that should catch it — decoding real API JSON also catches shape drift
 * (renamed/removed fields) that a hand-built Kotlin object tree cannot.
 */
internal object Redfern715BacktrackFixture {

    /**
     * Minimal TripResponse JSON for the single journey that reproduces the "715 / walk / 715 /
     * T1" complaint. Times, stop IDs, trip codes and route numbers are kept exactly as captured
     * from the real API. `stopSequence` for the T1 leg is trimmed to origin + destination only
     * (the real leg has 19 intermediate stations, irrelevant to this test); every other leg's
     * `stopSequence` is verbatim.
     */
    val JSON = """
        {
          "version": "10.6.21.17",
          "journeys": [
            {
              "rating": 0,
              "isAdditional": false,
              "interchanges": 1,
              "legs": [
                {
                  "duration": 60,
                  "origin": {
                    "id": "214758",
                    "name": "Seven Hills Rd opp Monaro St, Seven Hills",
                    "disassembledName": "Seven Hills Rd opp Monaro St",
                    "type": "platform",
                    "departureTimePlanned": "2026-07-03T00:03:00Z",
                    "departureTimeEstimated": "2026-07-03T00:03:00Z"
                  },
                  "destination": {
                    "id": "214759",
                    "name": "Seven Hills Rd after Solander Rd, Kings Langley",
                    "disassembledName": "Seven Hills Rd after Solander Rd",
                    "type": "platform",
                    "arrivalTimePlanned": "2026-07-03T00:04:00Z",
                    "arrivalTimeEstimated": "2026-07-03T00:04:00Z"
                  },
                  "transportation": {
                    "id": "nsw:14715: :H:sj2",
                    "name": "Sydney Buses Network 715",
                    "disassembledName": "715",
                    "number": "715",
                    "description": "Seven Hills to Rouse Hill Station via Norwest & Kellyville",
                    "product": { "id": 5, "class": 5, "name": "Sydney Buses Network", "iconId": 5 },
                    "destination": { "id": "10101501", "name": "Rouse Hill Station", "type": "stop" },
                    "properties": { "tripCode": 77, "RealtimeTripId": "2647807" }
                  },
                  "stopSequence": [
                    {
                      "id": "214758",
                      "name": "Seven Hills Rd opp Monaro St, Seven Hills",
                      "disassembledName": "Seven Hills Rd opp Monaro St",
                      "departureTimePlanned": "2026-07-03T00:03:00Z",
                      "departureTimeEstimated": "2026-07-03T00:03:00Z"
                    },
                    {
                      "id": "214759",
                      "name": "Seven Hills Rd after Solander Rd, Kings Langley",
                      "disassembledName": "Seven Hills Rd after Solander Rd",
                      "arrivalTimePlanned": "2026-07-03T00:04:00Z",
                      "arrivalTimeEstimated": "2026-07-03T00:04:00Z"
                    }
                  ]
                },
                {
                  "duration": 60,
                  "origin": {
                    "id": "214759",
                    "name": "Seven Hills Rd after Solander Rd, Kings Langley",
                    "disassembledName": "Seven Hills Rd after Solander Rd",
                    "type": "platform",
                    "departureTimePlanned": "2026-07-03T00:08:00Z",
                    "departureTimeEstimated": "2026-07-03T00:04:48Z"
                  },
                  "destination": {
                    "id": "2147213",
                    "name": "Seven Hills Rd before Solander Rd, Seven Hills",
                    "disassembledName": "Seven Hills Rd before Solander Rd",
                    "type": "platform",
                    "arrivalTimePlanned": "2026-07-03T00:09:00Z",
                    "arrivalTimeEstimated": "2026-07-03T00:05:48Z"
                  },
                  "transportation": {
                    "product": { "class": 99, "name": "footpath", "iconId": 99 },
                    "properties": { "tripCode": 0 }
                  },
                  "stopSequence": [
                    {
                      "id": "214759",
                      "name": "Seven Hills Rd after Solander Rd, Kings Langley",
                      "departureTimePlanned": "2026-07-03T00:08:00Z"
                    },
                    {
                      "id": "2147213",
                      "name": "Seven Hills Rd before Solander Rd, Seven Hills",
                      "arrivalTimePlanned": "2026-07-03T00:09:00Z"
                    }
                  ],
                  "footPathInfo": [
                    { "position": "IDEST", "duration": 60 }
                  ]
                },
                {
                  "duration": 234,
                  "origin": {
                    "id": "2147213",
                    "name": "Seven Hills Rd before Solander Rd, Seven Hills",
                    "disassembledName": "Seven Hills Rd before Solander Rd",
                    "type": "platform",
                    "departureTimePlanned": "2026-07-03T00:09:00Z",
                    "departureTimeEstimated": "2026-07-03T00:05:48Z"
                  },
                  "destination": {
                    "id": "214732",
                    "name": "Seven Hills Station, Stand A, Seven Hills",
                    "disassembledName": "Seven Hills Station, Stand A",
                    "type": "platform",
                    "arrivalTimePlanned": "2026-07-03T00:15:00Z",
                    "arrivalTimeEstimated": "2026-07-03T00:09:42Z"
                  },
                  "transportation": {
                    "id": "nsw:14715: :R:sj2",
                    "name": "Sydney Buses Network 715",
                    "disassembledName": "715",
                    "number": "715",
                    "description": "Rouse Hill Station to Seven Hills via Kellyville & Norwest",
                    "product": { "id": 5, "class": 5, "name": "Sydney Buses Network", "iconId": 5 },
                    "destination": { "id": "10101234", "name": "Seven Hills Station", "type": "stop" },
                    "properties": { "tripCode": 15, "RealtimeTripId": "2648028" }
                  },
                  "stopSequence": [
                    {
                      "id": "2147213",
                      "name": "Seven Hills Rd before Solander Rd, Seven Hills",
                      "departureTimePlanned": "2026-07-03T00:09:00Z",
                      "departureTimeEstimated": "2026-07-03T00:05:48Z"
                    },
                    {
                      "id": "2147214",
                      "name": "Seven Hills Rd at Monaro St, Seven Hills",
                      "arrivalTimePlanned": "2026-07-03T00:09:00Z",
                      "departureTimePlanned": "2026-07-03T00:09:00Z"
                    },
                    {
                      "id": "2147215",
                      "name": "Seven Hills Rd after Nattai St, Seven Hills",
                      "arrivalTimePlanned": "2026-07-03T00:10:00Z",
                      "departureTimePlanned": "2026-07-03T00:10:00Z"
                    },
                    {
                      "id": "2147216",
                      "name": "Seven Hills Rd opp Marnpar Rd, Seven Hills",
                      "arrivalTimePlanned": "2026-07-03T00:11:00Z",
                      "departureTimePlanned": "2026-07-03T00:11:00Z"
                    },
                    {
                      "id": "2147241",
                      "name": "Prospect Hwy after Station Rd, Seven Hills",
                      "arrivalTimePlanned": "2026-07-03T00:13:00Z",
                      "departureTimePlanned": "2026-07-03T00:13:00Z"
                    },
                    {
                      "id": "2147242",
                      "name": "Prospect Hwy opp Lucas Rd, Seven Hills",
                      "arrivalTimePlanned": "2026-07-03T00:13:00Z",
                      "departureTimePlanned": "2026-07-03T00:13:00Z"
                    },
                    {
                      "id": "2147243",
                      "name": "Prospect Hwy opp Hope St, Seven Hills",
                      "arrivalTimePlanned": "2026-07-03T00:14:00Z",
                      "departureTimePlanned": "2026-07-03T00:14:00Z"
                    },
                    {
                      "id": "214732",
                      "name": "Seven Hills Station, Stand A, Seven Hills",
                      "arrivalTimePlanned": "2026-07-03T00:15:00Z"
                    }
                  ],
                  "footPathInfoRedundant": true,
                  "interchange": { "desc": "Fussweg", "type": 100 }
                },
                {
                  "duration": 2130,
                  "origin": {
                    "id": "2147422",
                    "name": "Seven Hills Station, Platform 2, Seven Hills",
                    "disassembledName": "Seven Hills Station, Platform 2",
                    "type": "platform",
                    "departureTimePlanned": "2026-07-03T00:18:30Z",
                    "departureTimeEstimated": "2026-07-03T00:18:30Z"
                  },
                  "destination": {
                    "id": "2000336",
                    "name": "Central Station, Platform 16, Sydney",
                    "disassembledName": "Central Station, Platform 16",
                    "type": "platform",
                    "arrivalTimePlanned": "2026-07-03T00:54:00Z",
                    "arrivalTimeEstimated": "2026-07-03T00:54:00Z"
                  },
                  "transportation": {
                    "id": "nsw:020T1:W:R:sj2",
                    "name": "Sydney Trains Network T1 North Shore & Western Line",
                    "disassembledName": "T1",
                    "number": "T1 North Shore & Western Line",
                    "description": "Emu Plains or Richmond to City",
                    "product": { "id": 1, "class": 1, "name": "Sydney Trains Network", "iconId": 1 },
                    "destination": { "id": "10101100", "name": "Berowra via Gordon", "type": "stop" },
                    "properties": { "tripCode": 541, "RealtimeTripId": "145F.424.146.64.A.8.90146738" }
                  },
                  "stopSequence": [
                    {
                      "id": "2147422",
                      "name": "Seven Hills Station, Platform 2, Seven Hills",
                      "departureTimePlanned": "2026-07-03T00:18:30Z",
                      "departureTimeEstimated": "2026-07-03T00:18:30Z"
                    },
                    {
                      "id": "2000336",
                      "name": "Central Station, Platform 16, Sydney",
                      "arrivalTimePlanned": "2026-07-03T00:54:00Z",
                      "arrivalTimeEstimated": "2026-07-03T00:54:00Z"
                    }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()
}
