package xyz.ksharma.krail.departures.ui.fixtures

/**
 * Minimal DepartureMonitorResponse fixture for Central Station (stop 200060).
 *
 * Source: real NSW Transport API response captured 2026-04-15.
 * Contains one representative stopEvent per unique (cls, platform, platformName) combination,
 * covering all transport modes present at Central Station.
 *
 * Expected platform text mapping (by stopEvent index):
 *   [0]  T8  cls=1  platform=CE17 platformName="Platform 17"                         → "Platform 17"
 *   [1]  320 cls=5  platform=A    platformName="Central Station, Eddy Ave, Stand A"  → "Stand A"
 *   [2]  T8  cls=1  platform=CE23 platformName="Platform 23"                         → "Platform 23"
 *   [3]  L2  cls=4  platform=null platformName="Central Chalmers Street Light Rail"  → "Central Chalmers Street Light Rail"
 *   [4]  T1  cls=1  platform=CE18 platformName="Platform 18"                         → "Platform 18"
 *   [5]  T9  cls=1  platform=CE16 platformName="Platform 16"                         → "Platform 16"
 *   [6]  320 cls=5  platform=G    platformName="Central Station, Chalmers St, Stand G" → "Stand G"
 *   [7]  T2  cls=1  platform=CE19 platformName="Platform 19"                         → "Platform 19"
 *   [8]  L1  cls=4  platform=L1   platformName="Central Grand Concourse Light Rail"  → "L1 · Central Grand Concourse Light Rail"
 *   [9]  L2  cls=4  platform=LR1  platformName="Central Chalmers Street Light Rail"  → "LR1 · Central Chalmers Street Light Rail"
 *   [10] L3  cls=4  platform=LR2  platformName="Central Chalmers Street Light Rail"  → "LR2 · Central Chalmers Street Light Rail"
 *   [11] T4  cls=1  platform=CE25 platformName="Platform 25"                         → "Platform 25"
 *   [12] T4  cls=1  platform=CE24 platformName="Platform 24"                         → "Platform 24"
 *   [13] 440 cls=5  platform=C    platformName="Central Station, Eddy Ave, Stand C"  → "Stand C"
 *   [14] T8  cls=1  platform=CE22 platformName="Platform 22"                         → "Platform 22"
 *   [15] M1  cls=2  platform=CE26 platformName="Platform 26"  (Metro)                → "Platform 26"
 *   [16] T2  cls=1  platform=CE21 platformName="Platform 21"                         → "Platform 21"
 *   [17] 343 cls=5  platform=E    platformName="Central Station, Elizabeth St, Stand E" → "Stand E"
 *   [18] M1  cls=2  platform=CE27 platformName="Platform 27"  (Metro)                → "Platform 27"
 *   [19] 621 cls=1  platform=SD02 platformName="Platform 2"  (Regional)              → "Platform 2"
 *   [20] T2  cls=1  platform=CE20 platformName="Platform 20"                         → "Platform 20"
 *   [21] 339 cls=5  platform=F    platformName="Central Station, Foveaux St, Stand F" → "Stand F"
 */
internal object CentralStationDepartureFixture {

    /**
     * Expected results ordered to match stopEvents index in [JSON].
     *
     * Triple(lineNumber, expectedPlatformText, expectedDestinationName)
     */
    val EXPECTED = listOf(
        // Trains (cls=1)
        Triple("T8",  "Platform 17", "City to Macarthur via Airport or Sydenham"),
        // Bus Stand A
        Triple("320", "Stand A",     "Central Railway Square to Green Square (Loop Service)"),
        // Trains (cls=1)
        Triple("T8",  "Platform 23", "City to Macarthur via Airport or Sydenham"),
        // Light Rail — no code (1001_L2 product) → name only
        Triple("L2",  "Central Chalmers Street Light Rail", "Randwick to Circular Quay"),
        Triple("T1",  "Platform 18", "City to Emu Plains or Richmond"),
        Triple("T9",  "Platform 16", "North Shore to Hornsby via City"),
        // Bus Stand G
        Triple("320", "Stand G",     "Central Railway Square to Green Square (Loop Service)"),
        Triple("T2",  "Platform 19", "City to Parramatta or Leppington"),
        // Light Rail L1 (Dulwich Hill)
        Triple("L1",  "L1 · Central Grand Concourse Light Rail", "Central to Dulwich Hill"),
        // Light Rail LR1 → code · name
        Triple("L2",  "LR1 · Central Chalmers Street Light Rail", "Circular Quay to Randwick"),
        // Light Rail LR2 → code · name
        Triple("L3",  "LR2 · Central Chalmers Street Light Rail", "Juniors Kingsford to Circular Quay"),
        Triple("T4",  "Platform 25", "Bondi Junction to Waterfall or Cronulla"),
        Triple("T4",  "Platform 24", "Waterfall or Cronulla to Bondi Junction"),
        // Bus Stand C
        Triple("440", "Stand C",     "Rozelle to Bondi Junction"),
        Triple("T8",  "Platform 22", "City to Macarthur via Airport or Sydenham"),
        // Metro (cls=2)
        Triple("M1",  "Platform 26", "Sydenham to Tallawong"),
        Triple("T2",  "Platform 21", "City to Parramatta or Leppington"),
        // Bus Stand E
        Triple("343", "Stand E",     "City Circular Quay to Kingsford"),
        // Metro (cls=2)
        Triple("M1",  "Platform 27", "Tallawong to Sydenham"),
        // Regional train (cls=1, SD02 prefix)
        Triple("621", "Platform 2",  "Central to Melbourne (Southern Cross)"),
        Triple("T2",  "Platform 20", "City to Parramatta or Leppington"),
        // Bus Stand F
        Triple("339", "Stand F",     "Clovelly to Central Foveaux St (Loop Service)"),
    )

    val JSON = """
        {
          "stopEvents": [
            {
              "departureTimePlanned": "2026-04-15T10:17:00Z",
              "departureTimeEstimated": "2026-04-15T10:38:36Z",
              "location": {
                "id": "2000337",
                "name": "Central Station, Platform 17, Sydney",
                "disassembledName": "Central Station, Platform 17, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 17, Sydney",
                  "disassembledName": "Central Station, Platform 17"
                },
                "properties": {
                  "platform": "CE17",
                  "platformName": "Platform 17"
                }
              },
              "transportation": {
                "id": "nsw:020T8: :H:sj2",
                "disassembledName": "T8",
                "number": "T8 Airport & South Line",
                "description": "City to Macarthur via Airport or Sydenham",
                "destination": {
                  "id": "10101326",
                  "name": "City Circle via Town Hall"
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
              "departureTimePlanned": "2026-04-15T10:25:00Z",
              "departureTimeEstimated": "2026-04-15T10:36:00Z",
              "location": {
                "id": "200039",
                "name": "Central Station, Eddy Ave, Stand A, Sydney",
                "disassembledName": "Central Station, Eddy Ave, Stand A, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Eddy Ave, Stand A, Sydney",
                  "disassembledName": "Central Station, Eddy Ave, Stand A"
                },
                "properties": {
                  "platform": "A",
                  "platformName": "Central Station, Eddy Ave, Stand A"
                }
              },
              "transportation": {
                "id": "nsw:74320: :R:sj2",
                "disassembledName": "320",
                "number": "320",
                "description": "Central Railway Square to Green Square (Loop Service)",
                "destination": {
                  "id": "10102501",
                  "name": "Central via Green Square"
                },
                "product": {
                  "class": 5,
                  "name": "Sydney Buses Network"
                },
                "operator": {
                  "id": "2459",
                  "name": "Transit Systems"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T10:26:00Z",
              "departureTimeEstimated": "2026-04-15T10:45:24Z",
              "location": {
                "id": "2000343",
                "name": "Central Station, Platform 23, Sydney",
                "disassembledName": "Central Station, Platform 23, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 23, Sydney",
                  "disassembledName": "Central Station, Platform 23"
                },
                "properties": {
                  "platform": "CE23",
                  "platformName": "Platform 23"
                }
              },
              "transportation": {
                "id": "nsw:020T8: :H:sj2",
                "disassembledName": "T8",
                "number": "T8 Airport & South Line",
                "description": "City to Macarthur via Airport or Sydenham",
                "destination": {
                  "id": "10101297",
                  "name": "Macarthur via Airport"
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
              "departureTimePlanned": "2026-04-15T10:31:00Z",
              "departureTimeEstimated": "2026-04-15T10:32:30Z",
              "location": {
                "id": "200060",
                "name": "Central Station, Central Chalmers Street Light Rail, Sydney",
                "disassembledName": "Central Station, Central Chalmers Street Light Rail, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Sydney",
                  "disassembledName": "Central Station"
                },
                "properties": {
                  "platformName": "Central Chalmers Street Light Rail"
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
              "departureTimePlanned": "2026-04-15T10:33:00Z",
              "location": {
                "id": "2000338",
                "name": "Central Station, Platform 18, Sydney",
                "disassembledName": "Central Station, Platform 18, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 18, Sydney",
                  "disassembledName": "Central Station, Platform 18"
                },
                "properties": {
                  "platform": "CE18",
                  "platformName": "Platform 18"
                }
              },
              "transportation": {
                "id": "nsw:020T1:W:H:sj2",
                "disassembledName": "T1",
                "number": "T1 North Shore & Western Line",
                "description": "City to Emu Plains or Richmond",
                "destination": {
                  "id": "10101253",
                  "name": "Emu Plains via Parramatta"
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
              "departureTimePlanned": "2026-04-15T10:34:00Z",
              "location": {
                "id": "2000336",
                "name": "Central Station, Platform 16, Sydney",
                "disassembledName": "Central Station, Platform 16, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 16, Sydney",
                  "disassembledName": "Central Station, Platform 16"
                },
                "properties": {
                  "platform": "CE16",
                  "platformName": "Platform 16"
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
              "departureTimePlanned": "2026-04-15T10:34:00Z",
              "departureTimeEstimated": "2026-04-15T10:34:12Z",
              "location": {
                "id": "201016",
                "name": "Central Station, Chalmers St, Stand G, Sydney",
                "disassembledName": "Central Station, Chalmers St, Stand G, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Chalmers St, Stand G, Sydney",
                  "disassembledName": "Central Station, Chalmers St, Stand G"
                },
                "properties": {
                  "platform": "G",
                  "platformName": "Central Station, Chalmers St, Stand G"
                }
              },
              "transportation": {
                "id": "nsw:74320: :R:sj2",
                "disassembledName": "320",
                "number": "320",
                "description": "Central Railway Square to Green Square (Loop Service)",
                "destination": {
                  "id": "10102501",
                  "name": "Central via Green Square"
                },
                "product": {
                  "class": 5,
                  "name": "Sydney Buses Network"
                },
                "operator": {
                  "id": "2459",
                  "name": "Transit Systems"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T10:35:00Z",
              "location": {
                "id": "2000339",
                "name": "Central Station, Platform 19, Sydney",
                "disassembledName": "Central Station, Platform 19, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 19, Sydney",
                  "disassembledName": "Central Station, Platform 19"
                },
                "properties": {
                  "platform": "CE19",
                  "platformName": "Platform 19"
                }
              },
              "transportation": {
                "id": "nsw:020T2: :H:sj2",
                "disassembledName": "T2",
                "number": "T2 Leppington & Inner West Line",
                "description": "City to Parramatta or Leppington",
                "destination": {
                  "id": "10101216",
                  "name": "Homebush via Strathfield"
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
              "departureTimePlanned": "2026-04-15T10:35:00Z",
              "departureTimeEstimated": "2026-04-15T10:35:00Z",
              "location": {
                "id": "2000257",
                "name": "Central Station, Central Grand Concourse Light Rail, Sydney",
                "disassembledName": "Central Station, Central Grand Concourse Light Rail, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Sydney",
                  "disassembledName": "Central Station"
                },
                "properties": {
                  "platform": "L1",
                  "platformName": "Central Grand Concourse Light Rail"
                }
              },
              "transportation": {
                "id": "nsw:780L1: :H:sj2",
                "disassembledName": "L1",
                "number": "L1 Dulwich Hill Line",
                "description": "Central to Dulwich Hill",
                "destination": {
                  "id": "10101393",
                  "name": "Dulwich Hill"
                },
                "product": {
                  "class": 4,
                  "name": "Sydney Light Rail Network"
                },
                "operator": {
                  "id": "LR",
                  "name": "Sydney Light Rail"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T10:35:00Z",
              "departureTimeEstimated": "2026-04-15T10:38:30Z",
              "location": {
                "id": "2000447",
                "name": "Central Station, Central Chalmers Street Light Rail, Sydney",
                "disassembledName": "Central Station, Central Chalmers Street Light Rail, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Sydney",
                  "disassembledName": "Central Station"
                },
                "properties": {
                  "platform": "LR1",
                  "platformName": "Central Chalmers Street Light Rail"
                }
              },
              "transportation": {
                "id": "nsw:780L2: :H:sj2",
                "disassembledName": "L2",
                "number": "L2 Randwick Line",
                "description": "Circular Quay to Randwick",
                "destination": {
                  "id": "10101612",
                  "name": "Randwick"
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
              "departureTimePlanned": "2026-04-15T10:36:00Z",
              "departureTimeEstimated": "2026-04-15T10:37:48Z",
              "location": {
                "id": "2000448",
                "name": "Central Station, Central Chalmers Street Light Rail, Sydney",
                "disassembledName": "Central Station, Central Chalmers Street Light Rail, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Sydney",
                  "disassembledName": "Central Station"
                },
                "properties": {
                  "platform": "LR2",
                  "platformName": "Central Chalmers Street Light Rail"
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
              "departureTimePlanned": "2026-04-15T10:39:00Z",
              "location": {
                "id": "2000345",
                "name": "Central Station, Platform 25, Sydney",
                "disassembledName": "Central Station, Platform 25, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 25, Sydney",
                  "disassembledName": "Central Station, Platform 25"
                },
                "properties": {
                  "platform": "CE25",
                  "platformName": "Platform 25"
                }
              },
              "transportation": {
                "id": "nsw:020T4: :H:sj2",
                "disassembledName": "T4",
                "number": "T4 Eastern Suburbs & Illawarra Line",
                "description": "Bondi Junction to Waterfall or Cronulla",
                "destination": {
                  "id": "10101355",
                  "name": "Waterfall via Banksia"
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
              "departureTimePlanned": "2026-04-15T10:39:00Z",
              "departureTimeEstimated": "2026-04-15T10:39:00Z",
              "location": {
                "id": "2000344",
                "name": "Central Station, Platform 24, Sydney",
                "disassembledName": "Central Station, Platform 24, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 24, Sydney",
                  "disassembledName": "Central Station, Platform 24"
                },
                "properties": {
                  "platform": "CE24",
                  "platformName": "Platform 24"
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
              "departureTimePlanned": "2026-04-15T10:39:00Z",
              "departureTimeEstimated": "2026-04-15T10:38:48Z",
              "location": {
                "id": "200053",
                "name": "Central Station, Eddy Ave, Stand C, Sydney",
                "disassembledName": "Central Station, Eddy Ave, Stand C, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Eddy Ave, Stand C, Sydney",
                  "disassembledName": "Central Station, Eddy Ave, Stand C"
                },
                "properties": {
                  "platform": "C",
                  "platformName": "Central Station, Eddy Ave, Stand C"
                }
              },
              "transportation": {
                "id": "nsw:74440: :H:sj2",
                "disassembledName": "440",
                "number": "440",
                "description": "Rozelle to Bondi Junction",
                "destination": {
                  "id": "10101109",
                  "name": "Grafton Street Bondi Jct"
                },
                "product": {
                  "class": 5,
                  "name": "Sydney Buses Network"
                },
                "operator": {
                  "id": "2459",
                  "name": "Transit Systems"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T10:40:00Z",
              "departureTimeEstimated": "2026-04-15T10:55:06Z",
              "location": {
                "id": "2000342",
                "name": "Central Station, Platform 22, Sydney",
                "disassembledName": "Central Station, Platform 22, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 22, Sydney",
                  "disassembledName": "Central Station, Platform 22"
                },
                "properties": {
                  "platform": "CE22",
                  "platformName": "Platform 22"
                }
              },
              "transportation": {
                "id": "nsw:020T8: :H:sj2",
                "disassembledName": "T8",
                "number": "T8 Airport & South Line",
                "description": "City to Macarthur via Airport or Sydenham",
                "destination": {
                  "id": "10101326",
                  "name": "Sydenham"
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
              "departureTimePlanned": "2026-04-15T10:40:00Z",
              "departureTimeEstimated": "2026-04-15T10:40:00Z",
              "location": {
                "id": "2000466",
                "name": "Central Station, Platform 26, Sydney",
                "disassembledName": "Central Station, Platform 26, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 26, Sydney",
                  "disassembledName": "Central Station, Platform 26"
                },
                "properties": {
                  "platform": "CE26",
                  "platformName": "Platform 26"
                }
              },
              "transportation": {
                "id": "nsw:030M1: :H:sj2",
                "disassembledName": "M1",
                "number": "M1 Metro North West & Bankstown Line",
                "description": "Sydenham to Tallawong",
                "destination": {
                  "id": "10101500",
                  "name": "Tallawong"
                },
                "product": {
                  "class": 2,
                  "name": "Sydney Metro Network"
                },
                "operator": {
                  "id": "SMNW",
                  "name": "Sydney Metro"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T10:41:00Z",
              "location": {
                "id": "2000341",
                "name": "Central Station, Platform 21, Sydney",
                "disassembledName": "Central Station, Platform 21, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 21, Sydney",
                  "disassembledName": "Central Station, Platform 21"
                },
                "properties": {
                  "platform": "CE21",
                  "platformName": "Platform 21"
                }
              },
              "transportation": {
                "id": "nsw:020T2: :H:sj2",
                "disassembledName": "T2",
                "number": "T2 Leppington & Inner West Line",
                "description": "City to Parramatta or Leppington",
                "destination": {
                  "id": "10101428",
                  "name": "City Circle via Museum"
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
              "departureTimePlanned": "2026-04-15T10:41:00Z",
              "departureTimeEstimated": "2026-04-15T10:42:42Z",
              "location": {
                "id": "201080",
                "name": "Central Station, Elizabeth St, Stand E, Sydney",
                "disassembledName": "Central Station, Elizabeth St, Stand E, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Elizabeth St, Stand E, Sydney",
                  "disassembledName": "Central Station, Elizabeth St, Stand E"
                },
                "properties": {
                  "platform": "E",
                  "platformName": "Central Station, Elizabeth St, Stand E"
                }
              },
              "transportation": {
                "id": "nsw:30343: :H:sj2",
                "disassembledName": "343",
                "number": "343",
                "description": "City Circular Quay to Kingsford",
                "destination": {
                  "id": "10112155",
                  "name": "Kingsford"
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
              "departureTimePlanned": "2026-04-15T10:42:00Z",
              "departureTimeEstimated": "2026-04-15T10:42:00Z",
              "location": {
                "id": "2000467",
                "name": "Central Station, Platform 27, Sydney",
                "disassembledName": "Central Station, Platform 27, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 27, Sydney",
                  "disassembledName": "Central Station, Platform 27"
                },
                "properties": {
                  "platform": "CE27",
                  "platformName": "Platform 27"
                }
              },
              "transportation": {
                "id": "nsw:030M1: :R:sj2",
                "disassembledName": "M1",
                "number": "M1 Metro North West & Bankstown Line",
                "description": "Tallawong to Sydenham",
                "destination": {
                  "id": "10101326",
                  "name": "Sydenham"
                },
                "product": {
                  "class": 2,
                  "name": "Sydney Metro Network"
                },
                "operator": {
                  "id": "SMNW",
                  "name": "Sydney Metro"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T10:42:00Z",
              "departureTimeEstimated": "2026-04-15T10:42:00Z",
              "location": {
                "id": "2000322",
                "name": "Central Station, Platform 2, Sydney",
                "disassembledName": "Central Station, Platform 2, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 2, Sydney",
                  "disassembledName": "Central Station, Platform 2"
                },
                "properties": {
                  "platform": "SD02",
                  "platformName": "Platform 2"
                }
              },
              "transportation": {
                "id": "nsw:76621: :H:sj2",
                "disassembledName": "621",
                "number": "621",
                "description": "Central to Melbourne (Southern Cross)",
                "destination": {
                  "id": "10101844",
                  "name": "Southern Cross (Melbourne)"
                },
                "product": {
                  "class": 1,
                  "name": "Regional Trains and Coaches Network"
                },
                "operator": {
                  "id": "711",
                  "name": "NSW TrainLink"
                }
              }
            },
            {
              "departureTimePlanned": "2026-04-15T10:44:00Z",
              "location": {
                "id": "2000340",
                "name": "Central Station, Platform 20, Sydney",
                "disassembledName": "Central Station, Platform 20, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Platform 20, Sydney",
                  "disassembledName": "Central Station, Platform 20"
                },
                "properties": {
                  "platform": "CE20",
                  "platformName": "Platform 20"
                }
              },
              "transportation": {
                "id": "nsw:020T2: :H:sj2",
                "disassembledName": "T2",
                "number": "T2 Leppington & Inner West Line",
                "description": "City to Parramatta or Leppington",
                "destination": {
                  "id": "10101421",
                  "name": "City Circle via Museum"
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
              "departureTimePlanned": "2026-04-15T10:45:00Z",
              "location": {
                "id": "201039",
                "name": "Central Station, Foveaux St, Stand F, Sydney",
                "disassembledName": "Central Station, Foveaux St, Stand F, Sydney",
                "parent": {
                  "id": "200060",
                  "name": "Central Station, Foveaux St, Stand F, Sydney",
                  "disassembledName": "Central Station, Foveaux St, Stand F"
                },
                "properties": {
                  "platform": "F",
                  "platformName": "Central Station, Foveaux St, Stand F"
                }
              },
              "transportation": {
                "id": "nsw:30339: :R:sj2",
                "disassembledName": "339",
                "number": "339",
                "description": "Clovelly to Central Foveaux St (Loop Service)",
                "destination": {
                  "id": "10112139",
                  "name": "Clovelly via Central Foveaux St"
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
            }
          ]
        }
    """.trimIndent()
}

