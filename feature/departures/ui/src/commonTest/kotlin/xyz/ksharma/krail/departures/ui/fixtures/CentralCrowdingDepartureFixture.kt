package xyz.ksharma.krail.departures.ui.fixtures

/**
 * Departure monitor events at Central (stop 200060) carrying crowding.
 *
 * Source: real NSW Transport API response captured 2026-10-26, 10:19pm, trimmed to the fields
 * the mapper reads. [0] T2 MANY_SEATS, [1] M1 FEW_SEATS, [2] T4 with no occupancy, [3] bus 343
 * MANY_SEATS.
 */
internal object CentralCrowdingDepartureFixture {

    const val JSON = """
        {
          "stopEvents": [
            {
              "departureTimePlanned": "2026-10-26T11:20:00Z",
              "departureTimeEstimated": "2026-10-26T11:20:00Z",
              "properties": {
                "RealtimeTripId": "13XX.1324.165.36.B.8.92004580"
              },
              "location": {
                "id": "2000339",
                "disassembledName": "Central Station, Platform 19, Sydney",
                "parent": {
                  "id": "200060"
                },
                "properties": {
                  "platform": "CE19",
                  "platformName": "Platform 19",
                  "occupancy": "MANY_SEATS"
                }
              },
              "transportation": {
                "disassembledName": "T2",
                "product": {
                  "class": 1
                },
                "destination": {
                  "name": "Leppington via Granville"
                }
              }
            },
            {
              "departureTimePlanned": "2026-10-26T11:29:00Z",
              "departureTimeEstimated": "2026-10-26T11:28:30Z",
              "properties": {
                "RealtimeTripId": "0273-001-036-002:1000"
              },
              "location": {
                "id": "2000467",
                "disassembledName": "Central Station, Platform 27, Sydney",
                "parent": {
                  "id": "200060"
                },
                "properties": {
                  "platform": "CE27",
                  "platformName": "Platform 27",
                  "occupancy": "FEW_SEATS"
                }
              },
              "transportation": {
                "disassembledName": "M1",
                "product": {
                  "class": 2
                },
                "destination": {
                  "name": "Sydenham"
                }
              }
            },
            {
              "departureTimePlanned": "2026-10-26T11:19:00Z",
              "departureTimeEstimated": "2026-10-26T11:19:00Z",
              "properties": {
                "RealtimeTripId": "600L.1324.165.4.T.8.92002027"
              },
              "location": {
                "id": "2000344",
                "disassembledName": "Central Station, Platform 24, Sydney",
                "parent": {
                  "id": "200060"
                },
                "properties": {
                  "platform": "CE24",
                  "platformName": "Platform 24"
                }
              },
              "transportation": {
                "disassembledName": "T4",
                "product": {
                  "class": 1
                },
                "destination": {
                  "name": "Bondi Junction"
                }
              }
            },
            {
              "departureTimePlanned": "2026-10-26T11:19:00Z",
              "departureTimeEstimated": "2026-10-26T11:21:06Z",
              "properties": {
                "RealtimeTripId": "2494492"
              },
              "location": {
                "id": "201016",
                "disassembledName": "Central Station, Chalmers St, Stand G, Sydney",
                "parent": {
                  "id": "200060"
                },
                "properties": {
                  "platform": "G",
                  "platformName": "Central Station, Chalmers St, Stand G",
                  "occupancy": "MANY_SEATS"
                }
              },
              "transportation": {
                "disassembledName": "343",
                "product": {
                  "class": 5
                },
                "destination": {
                  "name": "City Circular Quay"
                }
              }
            }
          ]
        }
    """
}
