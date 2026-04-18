package xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures

/**
 * Fixture for a real NSW Transport API trip response: Oran Park → Seven Hills.
 *
 * Source: real API response captured 2026-04-18 (see `/feature/trip-planner/network/src/commonTest/assets/trip_oranpark_sevenhills.json`
 * for the full response).
 *
 * This fixture captures the **null-duration bug** observed in production logs:
 * the first bus leg (Bus 858, 2 stops, ~1 min) sometimes returns `"duration": null`
 * from the API. Without a fallback, the leg — and its entire journey card — would be
 * silently dropped from the UI.
 *
 * Journeys included:
 *   [0] Normal journey   — first leg has explicit duration (60s). Expected: included ✅
 *   [1] Null-duration    — first leg has `duration` omitted.  Expected: included ✅ (bug fix)
 */
internal object OranParkToSevenHillsFixture {

    /**
     * Number of journeys that must survive [buildJourneyList] mapping.
     * Counts only journeys where every leg has enough info to build a UI card.
     */
    const val EXPECTED_JOURNEY_COUNT = 2

    /**
     * Minimal TripResponse JSON containing two representative journeys:
     *   - Journey 0: explicit `duration` on the first leg (the happy path).
     *   - Journey 1: `duration` field completely absent on the first leg (the bug case).
     *
     * Times are kept as captured from the real API (UTC).
     * Non-essential fields are stripped to keep the fixture readable.
     */
    val JSON = """
        {
          "version": "10.6.21.17",
          "journeys": [
            {
              "rating": 0,
              "isAdditional": true,
              "interchanges": 3,
              "legs": [
                {
                  "duration": 60,
                  "origin": {
                    "id": "2570303",
                    "name": "Oran Park High School, Podium Way, Oran Park",
                    "disassembledName": "Oran Park High School, Podium Way",
                    "type": "platform",
                    "departureTimePlanned": "2026-04-18T22:48:00Z",
                    "departureTimeEstimated": "2026-04-18T22:48:00Z"
                  },
                  "destination": {
                    "id": "2570339",
                    "name": "Oran Park Town Centre, Oran Park Dr, Oran Park",
                    "disassembledName": "Oran Park Town Centre, Oran Park Dr",
                    "type": "platform",
                    "arrivalTimePlanned": "2026-04-18T22:49:00Z",
                    "arrivalTimeEstimated": "2026-04-18T22:49:00Z"
                  },
                  "transportation": {
                    "id": "nsw:12858: :H:sj2",
                    "name": "Sydney Buses Network 858",
                    "disassembledName": "858",
                    "number": "858",
                    "description": "Leppington to Oran Park",
                    "product": { "id": 5, "class": 5, "name": "Sydney Buses Network", "iconId": 5 },
                    "destination": { "id": "10126161", "name": "Oran Park Town Ctr", "type": "stop" }
                  },
                  "stopSequence": [
                    {
                      "id": "2570303",
                      "name": "Oran Park High School, Podium Way, Oran Park",
                      "disassembledName": "Oran Park High School, Podium Way",
                      "type": "platform",
                      "departureTimePlanned": "2026-04-18T22:48:00Z"
                    },
                    {
                      "id": "2570339",
                      "name": "Oran Park Town Centre, Oran Park Dr, Oran Park",
                      "disassembledName": "Oran Park Town Centre, Oran Park Dr",
                      "type": "platform",
                      "arrivalTimePlanned": "2026-04-18T22:49:00Z"
                    }
                  ]
                },
                {
                  "duration": 1440,
                  "origin": {
                    "id": "2570339",
                    "name": "Oran Park Town Centre, Oran Park Dr, Oran Park",
                    "disassembledName": "Oran Park Town Centre, Oran Park Dr",
                    "type": "platform",
                    "departureTimePlanned": "2026-04-18T22:49:00Z",
                    "departureTimeEstimated": "2026-04-18T22:49:00Z"
                  },
                  "destination": {
                    "id": "203513",
                    "name": "Leppington Station, Platform 2",
                    "disassembledName": "Leppington Station, Platform 2",
                    "type": "platform",
                    "arrivalTimePlanned": "2026-04-19T00:13:00Z",
                    "arrivalTimeEstimated": "2026-04-19T00:13:00Z"
                  },
                  "transportation": {
                    "id": "nsw:12858: :R:sj2",
                    "name": "Sydney Buses Network 858",
                    "disassembledName": "858",
                    "number": "858",
                    "description": "Oran Park to Leppington",
                    "product": { "id": 5, "class": 5, "name": "Sydney Buses Network", "iconId": 5 },
                    "destination": { "id": "203513", "name": "Leppington Station", "type": "stop" }
                  },
                  "stopSequence": [
                    {
                      "id": "2570339",
                      "disassembledName": "Oran Park Town Centre, Oran Park Dr",
                      "type": "platform",
                      "departureTimePlanned": "2026-04-18T22:49:00Z"
                    },
                    {
                      "id": "203513",
                      "disassembledName": "Leppington Station, Platform 2",
                      "type": "platform",
                      "arrivalTimePlanned": "2026-04-19T00:13:00Z"
                    }
                  ]
                }
              ]
            },
            {
              "rating": 0,
              "isAdditional": true,
              "interchanges": 3,
              "legs": [
                {
                  "origin": {
                    "id": "2570303",
                    "name": "Oran Park High School, Podium Way, Oran Park",
                    "disassembledName": "Oran Park High School, Podium Way",
                    "type": "platform",
                    "departureTimePlanned": "2026-04-18T21:46:00Z",
                    "departureTimeEstimated": "2026-04-18T21:46:00Z"
                  },
                  "destination": {
                    "id": "2570339",
                    "name": "Oran Park Town Centre, Oran Park Dr, Oran Park",
                    "disassembledName": "Oran Park Town Centre, Oran Park Dr",
                    "type": "platform",
                    "arrivalTimePlanned": "2026-04-18T21:47:00Z",
                    "arrivalTimeEstimated": "2026-04-18T21:47:00Z"
                  },
                  "transportation": {
                    "id": "nsw:12858: :R:sj2",
                    "name": "Sydney Buses Network 858",
                    "disassembledName": "858",
                    "number": "858",
                    "description": "Leppington to Oran Park",
                    "product": { "id": 5, "class": 5, "name": "Sydney Buses Network", "iconId": 5 },
                    "destination": { "id": "10126161", "name": "Oran Park Town Ctr", "type": "stop" }
                  },
                  "stopSequence": [
                    {
                      "id": "2570303",
                      "disassembledName": "Oran Park High School, Podium Way",
                      "type": "platform",
                      "departureTimePlanned": "2026-04-18T21:46:00Z"
                    },
                    {
                      "id": "2570339",
                      "disassembledName": "Oran Park Town Centre, Oran Park Dr",
                      "type": "platform",
                      "arrivalTimePlanned": "2026-04-18T21:47:00Z"
                    }
                  ]
                },
                {
                  "duration": 1260,
                  "origin": {
                    "id": "2570339",
                    "name": "Oran Park Town Centre, Oran Park Dr, Oran Park",
                    "disassembledName": "Oran Park Town Centre, Oran Park Dr",
                    "type": "platform",
                    "departureTimePlanned": "2026-04-18T21:47:00Z",
                    "departureTimeEstimated": "2026-04-18T21:47:00Z"
                  },
                  "destination": {
                    "id": "203513",
                    "name": "Leppington Station, Platform 2",
                    "disassembledName": "Leppington Station, Platform 2",
                    "type": "platform",
                    "arrivalTimePlanned": "2026-04-18T23:08:00Z",
                    "arrivalTimeEstimated": "2026-04-18T23:08:00Z"
                  },
                  "transportation": {
                    "id": "nsw:12858: :R:sj2b",
                    "name": "Sydney Buses Network 858",
                    "disassembledName": "858",
                    "number": "858",
                    "description": "Oran Park to Leppington",
                    "product": { "id": 5, "class": 5, "name": "Sydney Buses Network", "iconId": 5 },
                    "destination": { "id": "203513", "name": "Leppington Station", "type": "stop" }
                  },
                  "stopSequence": [
                    {
                      "id": "2570339",
                      "disassembledName": "Oran Park Town Centre, Oran Park Dr",
                      "type": "platform",
                      "departureTimePlanned": "2026-04-18T21:47:00Z"
                    },
                    {
                      "id": "203513",
                      "disassembledName": "Leppington Station, Platform 2",
                      "type": "platform",
                      "arrivalTimePlanned": "2026-04-18T23:08:00Z"
                    }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()
}

