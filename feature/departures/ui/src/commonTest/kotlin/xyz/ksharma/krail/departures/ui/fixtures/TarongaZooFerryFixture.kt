package xyz.ksharma.krail.departures.ui.fixtures

/**
 * Minimal DepartureMonitorResponse fixture for Taronga Zoo Wharf (stop 208899).
 *
 * Source: real NSW Transport API response captured 2026-04-15.
 * Contains one representative stopEvent per unique (cls, platform, platformName) combination.
 *
 * Expected platform text mapping:
 *   F2  cls=9  platform=F1  platformName="Taronga Zoo Wharf"  → "F1"
 */
internal object TarongaZooFerryFixture {

    /**
     * Expected results ordered to match stopEvents index in [JSON].
     *
     * Triple(lineNumber, expectedPlatformText, expectedDestinationName)
     */
    val EXPECTED = listOf(
        // F2 — Sydney Ferries: raw wharf code shown as platform label
        Triple("F2", "F1", "Taronga Zoo to Circular Quay"),
    )

    val JSON = """
        {
          "stopEvents": [
            {
              "departureTimePlanned": "2026-04-15T21:14:00Z",
              "location": {
                "id": "20883",
                "name": "Taronga Zoo Wharf, Mosman",
                "disassembledName": "Taronga Zoo Wharf, Mosman",
                "parent": {
                  "id": "208899",
                  "name": "Taronga Zoo Wharf, Mosman",
                  "disassembledName": "Taronga Zoo Wharf"
                },
                "properties": {
                  "platform": "F1",
                  "platformName": "Taronga Zoo Wharf"
                }
              },
              "transportation": {
                "id": "nsw:090F2: :R:sj2",
                "disassembledName": "F2",
                "number": "F2 Taronga Zoo",
                "description": "Taronga Zoo to Circular Quay",
                "destination": {
                  "id": "10101103",
                  "name": "Circular Quay"
                },
                "product": {
                  "class": 9,
                  "name": "Sydney Ferries Network"
                },
                "operator": {
                  "id": "SF",
                  "name": "Sydney Ferries"
                }
              }
            }
          ]
        }
    """.trimIndent()
}

