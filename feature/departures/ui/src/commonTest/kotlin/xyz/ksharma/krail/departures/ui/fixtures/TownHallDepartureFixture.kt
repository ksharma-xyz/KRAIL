package xyz.ksharma.krail.departures.ui.fixtures

/**
 * Minimal DepartureMonitorResponse fixture for Town Hall Station (stop 200070).
 *
 * Source: real NSW Transport API response captured 2026-04-15.
 * Contains one representative stopEvent per unique (cls, platform, platformName) combination,
 * covering all transport modes present at Town Hall.
 *
 * Expected platform text mapping (by stopEvent index):
 *   [0]  L3  cls=4  platform=LR1  platformName="Town Hall Light Rail"     → "LR1 · Town Hall Light Rail"
 *   [1]  500X cls=5  platform=H    platformName="Town Hall, Park St, Stand H" → "Stand H"
 *   [2]  L2  cls=4  platform=null platformName="Town Hall Light Rail"     → "Town Hall Light Rail"
 *   [3]  506 cls=5  platform=K    platformName="Town Hall, Park St, Stand K" → "Stand K"
 *   [4]  T8  cls=1  platform=THL6 platformName="Platform 6"              → "Platform 6"
 *   [5]  T4  cls=1  platform=THL4 platformName="Platform 4"              → "Platform 4"
 *   [6]  T9  cls=1  platform=THL2 platformName="Platform 2"              → "Platform 2"
 *   [7]  T2  cls=1  platform=THL1 platformName="Platform 1"              → "Platform 1"
 *   [8]  T4  cls=1  platform=THL5 platformName="Platform 5"              → "Platform 5"
 *   [9]  T9  cls=1  platform=THL3 platformName="Platform 3"              → "Platform 3"
 *   [10] 311 cls=5  platform=J    platformName="Town Hall, Park St, Stand J" → "Stand J"
 *   [11] 324 cls=5  platform=G    platformName="Town Hall, Park St, Stand G" → "Stand G"
 *   [12] L3  cls=4  platform=LR2  platformName="Town Hall Light Rail"    → "LR2 · Town Hall Light Rail"
 *   [13] T2  cls=1  platform=THL6 platformName="THL6" (API echo bug)     → "Platform 6"
 */
internal object TownHallDepartureFixture {

    /**
     * Expected results ordered to match stopEvents index in [JSON].
     *
     * Triple(lineNumber, expectedPlatformText, expectedDestinationName)
     */
    val EXPECTED = listOf(
        // Light Rail — LR1 direction (code present → show code · name)
        Triple("L3",   "LR1 · Town Hall Light Rail", "Circular Quay to Juniors Kingsford"),
        // Bus — Stand H
        Triple("500X", "Stand H",                    "West Ryde to City Hyde Park via Victoria Rd (Express Service)"),
        // Light Rail — no platform code (1001_L2 product) → name only
        Triple("L2",   "Town Hall Light Rail",        "Randwick to Circular Quay"),
        // Bus — Stand K
        Triple("506",  "Stand K",                    "City Domain to Macquarie University via East Ryde"),
        // Train — THL6 normal (platformName = "Platform 6")
        Triple("T8",   "Platform 6",                 "City to Macarthur via Airport or Sydenham"),
        // Train — THL4
        Triple("T4",   "Platform 4",                 "Bondi Junction to Waterfall or Cronulla"),
        // Train — THL2
        Triple("T9",   "Platform 2",                 "Hornsby to North Shore via City"),
        // Train — THL1
        Triple("T2",   "Platform 1",                 "City to Parramatta or Leppington"),
        // Train — THL5
        Triple("T4",   "Platform 5",                 "Waterfall or Cronulla to Bondi Junction"),
        // Train — THL3
        Triple("T9",   "Platform 3",                 "North Shore to Hornsby via City"),
        // Bus — Stand J
        Triple("311",  "Stand J",                    "Central Belmore Park to City Millers Point via Darlinghurst & Potts Point"),
        // Bus — Stand G
        Triple("324",  "Stand G",                    "City Walsh Bay to Watsons Bay via Old South Head Rd"),
        // Light Rail — LR2 direction
        Triple("L3",   "LR2 · Town Hall Light Rail", "Juniors Kingsford to Circular Quay"),
        // Train — THL6 bad case: API echoes raw code as platformName → derive "Platform 6"
        Triple("T2",   "Platform 6",                 "City to Parramatta or Leppington"),
    )

    val JSON = """
        {
          "stopEvents": [
            {
              "departureTimePlanned": "2026-04-15T06:32:00Z",
              "departureTimeEstimated": "2026-04-15T06:34:36Z",
              "location": {
                "id": "2000458",
                "name": "Town Hall Station, Town Hall Light Rail, Sydney",
                "disassembledName": "Town Hall Station, Town Hall Light Rail, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Sydney",
                  "disassembledName": "Town Hall Station"
                },
                "properties": {
                  "platform": "LR1",
                  "platformName": "Town Hall Light Rail"
                }
              },
              "transportation": {
                "id": "nsw:780L3: :H:sj2",
                "disassembledName": "L3",
                "number": "L3 Kingsford Line",
                "description": "Circular Quay to Juniors Kingsford",
                "destination": {
                  "id": "10101617",
                  "name": "Juniors Kingsford"
                },
                "product": {
                  "class": 4,
                  "name": "Sydney Light Rail Network"
                },
                "operator": {
                  "id": "SLR",
                  "name": "Sydney Light Rail"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:33:00Z",
              "departureTimeEstimated": "2026-04-15T06:36:42Z",
              "location": {
                "id": "2000249",
                "name": "Town Hall, Park St, Stand H, Sydney",
                "disassembledName": "Town Hall, Park St, Stand H, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall, Park St, Stand H, Sydney",
                  "disassembledName": "Town Hall, Park St, Stand H"
                },
                "properties": {
                  "platform": "H",
                  "platformName": "Town Hall, Park St, Stand H"
                }
              },
              "transportation": {
                "id": "nsw:17500:X:R:sj2",
                "disassembledName": "500X",
                "number": "500X",
                "description": "West Ryde to City Hyde Park via Victoria Rd (Express Service)",
                "destination": {
                  "id": "10133986",
                  "name": "City Hyde Park"
                },
                "product": {
                  "class": 5,
                  "name": "Sydney Buses Network"
                },
                "operator": {
                  "id": "2507",
                  "name": "Busways North West"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:33:00Z",
              "departureTimeEstimated": "2026-04-15T06:36:42Z",
              "location": {
                "id": "200070",
                "name": "Town Hall Station, Town Hall Light Rail, Sydney",
                "disassembledName": "Town Hall Station, Town Hall Light Rail, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Sydney",
                  "disassembledName": "Town Hall Station"
                },
                "properties": {
                  "platformName": "Town Hall Light Rail"
                }
              },
              "transportation": {
                "id": "nsw:780L2: :R:sj2",
                "disassembledName": "L2",
                "number": "L2 Randwick Line",
                "description": "Randwick to Circular Quay",
                "destination": {
                  "id": "10101103",
                  "name": "Circular Quay, Sydney"
                },
                "product": {
                  "class": 4,
                  "name": "1001_L2"
                },
                "operator": {
                  "id": "SLR",
                  "name": "Sydney Light Rail"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:34:00Z",
              "departureTimeEstimated": "2026-04-15T06:35:48Z",
              "location": {
                "id": "2000252",
                "name": "Town Hall, Park St, Stand K, Sydney",
                "disassembledName": "Town Hall, Park St, Stand K, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall, Park St, Stand K, Sydney",
                  "disassembledName": "Town Hall, Park St, Stand K"
                },
                "properties": {
                  "platform": "K",
                  "platformName": "Town Hall, Park St, Stand K"
                }
              },
              "transportation": {
                "id": "nsw:17506: :H:sj2",
                "disassembledName": "506",
                "number": "506",
                "description": "City Domain to Macquarie University via East Ryde",
                "destination": {
                  "id": "10116468",
                  "name": "East Ryde"
                },
                "product": {
                  "class": 5,
                  "name": "Sydney Buses Network"
                },
                "operator": {
                  "id": "2507",
                  "name": "Busways North West"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:35:00Z",
              "departureTimeEstimated": "2026-04-15T06:35:00Z",
              "location": {
                "id": "2000396",
                "name": "Town Hall Station, Platform 6, Sydney",
                "disassembledName": "Town Hall Station, Platform 6, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Platform 6, Sydney",
                  "disassembledName": "Town Hall Station, Platform 6"
                },
                "properties": {
                  "platform": "THL6",
                  "platformName": "Platform 6"
                }
              },
              "transportation": {
                "id": "nsw:020T8: :H:sj2",
                "disassembledName": "T8",
                "number": "T8 Airport & South Line",
                "description": "City to Macarthur via Airport or Sydenham",
                "destination": {
                  "id": "10101296",
                  "name": "Campbelltown via East Hills"
                },
                "product": {
                  "class": 1,
                  "name": "Sydney Trains Network"
                },
                "operator": {
                  "id": "x0001",
                  "name": "Sydney Trains"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:36:00Z",
              "departureTimeEstimated": "2026-04-15T06:36:00Z",
              "location": {
                "id": "2000394",
                "name": "Town Hall Station, Platform 4, Sydney",
                "disassembledName": "Town Hall Station, Platform 4, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Platform 4, Sydney",
                  "disassembledName": "Town Hall Station, Platform 4"
                },
                "properties": {
                  "platform": "THL4",
                  "platformName": "Platform 4"
                }
              },
              "transportation": {
                "id": "nsw:020T4: :H:sj2",
                "disassembledName": "T4",
                "number": "T4 Eastern Suburbs & Illawarra Line",
                "description": "Bondi Junction to Waterfall or Cronulla",
                "destination": {
                  "id": "10101355",
                  "name": "Waterfall via Wolli Creek"
                },
                "product": {
                  "class": 1,
                  "name": "Sydney Trains Network"
                },
                "operator": {
                  "id": "x0001",
                  "name": "Sydney Trains"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:36:00Z",
              "departureTimeEstimated": "2026-04-15T06:36:00Z",
              "location": {
                "id": "2000392",
                "name": "Town Hall Station, Platform 2, Sydney",
                "disassembledName": "Town Hall Station, Platform 2, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Platform 2, Sydney",
                  "disassembledName": "Town Hall Station, Platform 2"
                },
                "properties": {
                  "platform": "THL2",
                  "platformName": "Platform 2"
                }
              },
              "transportation": {
                "id": "nsw:020T9: :R:sj2",
                "disassembledName": "T9",
                "number": "T9 Northern Line",
                "description": "Hornsby to North Shore via City",
                "destination": {
                  "id": "10101100",
                  "name": "Epping via Central"
                },
                "product": {
                  "class": 1,
                  "name": "Sydney Trains Network"
                },
                "operator": {
                  "id": "x0001",
                  "name": "Sydney Trains"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:37:00Z",
              "location": {
                "id": "2000391",
                "name": "Town Hall Station, Platform 1, Sydney",
                "disassembledName": "Town Hall Station, Platform 1, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Platform 1, Sydney",
                  "disassembledName": "Town Hall Station, Platform 1"
                },
                "properties": {
                  "platform": "THL1",
                  "platformName": "Platform 1"
                }
              },
              "transportation": {
                "id": "nsw:020T2: :H:sj2",
                "disassembledName": "T2",
                "number": "T2 Leppington & Inner West Line",
                "description": "City to Parramatta or Leppington",
                "destination": {
                  "id": "10101428",
                  "name": "Leppington via Granville"
                },
                "product": {
                  "class": 1,
                  "name": "Sydney Trains Network"
                },
                "operator": {
                  "id": "x0001",
                  "name": "Sydney Trains"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:37:00Z",
              "location": {
                "id": "2000395",
                "name": "Town Hall Station, Platform 5, Sydney",
                "disassembledName": "Town Hall Station, Platform 5, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Platform 5, Sydney",
                  "disassembledName": "Town Hall Station, Platform 5"
                },
                "properties": {
                  "platform": "THL5",
                  "platformName": "Platform 5"
                }
              },
              "transportation": {
                "id": "nsw:020T4: :R:sj2",
                "disassembledName": "T4",
                "number": "T4 Eastern Suburbs & Illawarra Line",
                "description": "Waterfall or Cronulla to Bondi Junction",
                "destination": {
                  "id": "10101109",
                  "name": "Bondi Junction"
                },
                "product": {
                  "class": 1,
                  "name": "Sydney Trains Network"
                },
                "operator": {
                  "id": "x0001",
                  "name": "Sydney Trains"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:37:00Z",
              "departureTimeEstimated": "2026-04-15T06:37:00Z",
              "location": {
                "id": "2000393",
                "name": "Town Hall Station, Platform 3, Sydney",
                "disassembledName": "Town Hall Station, Platform 3, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Platform 3, Sydney",
                  "disassembledName": "Town Hall Station, Platform 3"
                },
                "properties": {
                  "platform": "THL3",
                  "platformName": "Platform 3"
                }
              },
              "transportation": {
                "id": "nsw:020T9: :H:sj2",
                "disassembledName": "T9",
                "number": "T9 Northern Line",
                "description": "North Shore to Hornsby via City",
                "destination": {
                  "id": "10101121",
                  "name": "Gordon via Lindfield"
                },
                "product": {
                  "class": 1,
                  "name": "Sydney Trains Network"
                },
                "operator": {
                  "id": "x0001",
                  "name": "Sydney Trains"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:37:00Z",
              "departureTimeEstimated": "2026-04-15T06:36:48Z",
              "location": {
                "id": "2000425",
                "name": "Town Hall, Park St, Stand J, Sydney",
                "disassembledName": "Town Hall, Park St, Stand J, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall, Park St, Stand J, Sydney",
                  "disassembledName": "Town Hall, Park St, Stand J"
                },
                "properties": {
                  "platform": "J",
                  "platformName": "Town Hall, Park St, Stand J"
                }
              },
              "transportation": {
                "id": "nsw:30311: :R:sj2",
                "disassembledName": "311",
                "number": "311",
                "description": "Central Belmore Park to City Millers Point via Darlinghurst & Potts Point",
                "destination": {
                  "id": "10111099",
                  "name": "City Millers Point"
                },
                "product": {
                  "class": 5,
                  "name": "Sydney Buses Network"
                },
                "operator": {
                  "id": "2509",
                  "name": "Transdev John Holland Buses"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:37:00Z",
              "departureTimeEstimated": "2026-04-15T06:40:06Z",
              "location": {
                "id": "2000426",
                "name": "Town Hall, Park St, Stand G, Sydney",
                "disassembledName": "Town Hall, Park St, Stand G, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall, Park St, Stand G, Sydney",
                  "disassembledName": "Town Hall, Park St, Stand G"
                },
                "properties": {
                  "platform": "G",
                  "platformName": "Town Hall, Park St, Stand G"
                }
              },
              "transportation": {
                "id": "nsw:30324: :H:sj2",
                "disassembledName": "324",
                "number": "324",
                "description": "City Walsh Bay to Watsons Bay via Old South Head Rd",
                "destination": {
                  "id": "10111999",
                  "name": "Watsons Bay"
                },
                "product": {
                  "class": 5,
                  "name": "Sydney Buses Network"
                },
                "operator": {
                  "id": "2509",
                  "name": "Transdev John Holland Buses"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:37:00Z",
              "departureTimeEstimated": "2026-04-15T06:41:06Z",
              "location": {
                "id": "2000459",
                "name": "Town Hall Station, Town Hall Light Rail, Sydney",
                "disassembledName": "Town Hall Station, Town Hall Light Rail, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, Sydney",
                  "disassembledName": "Town Hall Station"
                },
                "properties": {
                  "platform": "LR2",
                  "platformName": "Town Hall Light Rail"
                }
              },
              "transportation": {
                "id": "nsw:780L3: :R:sj2",
                "disassembledName": "L3",
                "number": "L3 Kingsford Line",
                "description": "Juniors Kingsford to Circular Quay",
                "destination": {
                  "id": "10101103",
                  "name": "Circular Quay"
                },
                "product": {
                  "class": 4,
                  "name": "Sydney Light Rail Network"
                },
                "operator": {
                  "id": "SLR",
                  "name": "Sydney Light Rail"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T06:40:00Z",
              "departureTimeEstimated": "2026-04-15T06:40:00Z",
              "location": {
                "id": "2000396",
                "name": "Town Hall Station, THL6, Sydney",
                "disassembledName": "Town Hall Station, THL6, Sydney",
                "parent": {
                  "id": "200070",
                  "name": "Town Hall Station, THL6, Sydney",
                  "disassembledName": "Town Hall Station, THL6"
                },
                "properties": {
                  "platform": "THL6",
                  "platformName": "THL6"
                }
              },
              "transportation": {
                "id": "nsw:020T2: :H:sj2",
                "disassembledName": "T2",
                "number": "T2 Leppington & Inner West Line",
                "description": "City to Parramatta or Leppington",
                "destination": {
                  "id": "10101428",
                  "name": "Leppington Station, Leppington"
                },
                "product": {
                  "class": 1,
                  "name": "Sydney Trains Network"
                },
                "operator": {
                  "id": "x0001",
                  "name": "Sydney Trains"
                }
              }
            }
          ]
        }
    """.trimIndent()
}

