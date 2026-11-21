package xyz.ksharma.krail.trip.planner.ui.timetable.business.fixtures

/**
 * Real NSW trip planner replies captured on 2026-11-21, one journey each (1:00 pm that day, except where noted), while
 * the weekend closures were live. Trimmed: `coords` arrays and each alert's `infoLinks` and `properties`
 * are removed. Used to check that a leg is a replacement bus only when NSW labels it one.
 */
internal object ReplacementBusTripFixtures {

    /** Cronulla to Central. Every leg is a replacement bus (`iconId` 14, "Temporary buses"). The legs carry no alert. */
    val CRONULLA_TO_CENTRAL = """
        {
         "version": "10.6.21.17",
         "journeys": [
          {
           "rating": 0,
           "isAdditional": false,
           "interchanges": 0,
           "legs": [
            {
             "duration": 4200,
             "origin": {
              "isGlobalId": true,
              "id": "223022",
              "name": "Cronulla Station, Cronulla St, Cronulla",
              "disassembledName": "Cronulla Station, Cronulla St",
              "type": "platform",
              "coord": [
               -34.056205,
               151.151584
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "223020",
               "name": "Cronulla Station, Cronulla St, Cronulla",
               "disassembledName": "Cronulla Station, Cronulla St",
               "type": "stop",
               "parent": {
                "id": "placeID:95357010:1",
                "name": "Cronulla",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101351"
               },
               "coord": [
                -34.055785,
                151.151498
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-21T02:10:00Z",
              "departureTimePlanned": "2026-11-21T02:10:00Z",
              "departureTimeEstimated": "2026-11-21T02:10:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "stoppingPointPlanned": "Cronulla Station, Cronulla St",
               "area": "11",
               "platform": "X1",
               "platformName": "Cronulla Station, Cronulla St",
               "plannedPlatformName": "Cronulla Station, Cronulla St",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               5,
               11
              ]
             },
             "destination": {
              "isGlobalId": true,
              "id": "201016",
              "name": "Central Station, Chalmers St, Stand G, Sydney",
              "disassembledName": "Central Station, Chalmers St, Stand G",
              "type": "platform",
              "coord": [
               -33.886259,
               151.206713
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "200060",
               "name": "Central Station, Chalmers St, Stand G, Sydney",
               "disassembledName": "Central Station, Chalmers St, Stand G",
               "type": "stop",
               "parent": {
                "id": "placeID:95301001:1",
                "name": "Sydney",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101100"
               },
               "coord": [
                -33.884024,
                151.206203
               ],
               "niveau": 0
              },
              "arrivalTimeBaseTimetable": "2026-11-21T03:20:00Z",
              "arrivalTimePlanned": "2026-11-21T03:20:00Z",
              "arrivalTimeEstimated": "2026-11-21T03:20:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "pbyb": "1",
               "stoppingPointPlanned": "Central Station, Chalmers St, Stand G",
               "area": "19",
               "platform": "G",
               "platformName": "Central Station, Chalmers St, Stand G",
               "plannedPlatformName": "Central Station, Chalmers St, Stand G",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0,
                  "stepFromStreet": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               4,
               5,
               7,
               11
              ]
             },
             "transportation": {
              "id": "nsw:1426T:4:R:sj2",
              "name": "Temporary buses 26T4",
              "disassembledName": "26T4",
              "number": "26T4",
              "description": "Cronulla, all stations to Miranda, then Central",
              "product": {
               "id": 14,
               "class": 5,
               "name": "Temporary buses",
               "iconId": 5
              },
              "operator": {
               "id": "7057",
               "name": "CDC NSW R4"
              },
              "destination": {
               "id": "10101100",
               "name": "Central Station",
               "type": "stop"
              },
              "properties": {
               "isTTB": true,
               "tripCode": 28,
               "timetablePeriod": "NSW JP",
               "lineDisplay": "LINE",
               "globalId": "3001",
               "RealtimeTripId": "2739201",
               "gtfsTripId": "3001.nsw-14-26T-4.1.TA.58.sj2",
               "attributes": [
                "PLAN_LOWFLOORVEHICLE",
                "PLAN_WHEELCHAIRACCESS"
               ]
              },
              "iconId": 14
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "223022",
               "name": "Cronulla Station, Cronulla St, Cronulla",
               "disassembledName": "Cronulla Station, Cronulla St",
               "type": "platform",
               "coord": [
                -34.056205,
                151.151584
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "223020",
                "name": "Cronulla Station, Cronulla St, Cronulla",
                "disassembledName": "Cronulla Station, Cronulla St",
                "type": "stop",
                "parent": {
                 "id": "placeID:95357010:1",
                 "name": "Cronulla",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101351"
                },
                "coord": [
                 -34.055785,
                 151.151498
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Cronulla Station, Cronulla St",
                "area": "11",
                "platform": "X1",
                "platformName": "Cronulla Station, Cronulla St",
                "plannedPlatformName": "Cronulla Station, Cronulla St",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "departureTimePlanned": "2026-11-21T02:10:00Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2230192",
               "name": "Woolooware Oval, Kingsway, Woolooware",
               "disassembledName": "Woolooware Oval, Kingsway",
               "type": "platform",
               "coord": [
                -34.048913,
                151.143142
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G2230192",
                "name": "Woolooware Oval, Kingsway, Woolooware",
                "disassembledName": "Woolooware Oval, Kingsway",
                "type": "stop",
                "parent": {
                 "id": "placeID:95357037:1",
                 "name": "Woolooware",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10141118"
                },
                "coord": [
                 -34.048914,
                 151.143121
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Woolooware Oval, Kingsway",
                "areaGid": "G2230192",
                "area": "0",
                "platform": "X1",
                "platformName": "Woolooware Oval, Kingsway",
                "plannedPlatformName": "Woolooware Oval, Kingsway"
               },
               "arrivalTimePlanned": "2026-11-21T02:15:00Z",
               "departureTimePlanned": "2026-11-21T02:15:00Z",
               "modes": [
                5
               ]
              },
              {
               "isGlobalId": true,
               "id": "2229196",
               "name": "Kingsway opp Caringbah Station, Caringbah",
               "disassembledName": "Kingsway opp Caringbah Station",
               "type": "platform",
               "coord": [
                -34.041796,
                151.121712
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "222910",
                "name": "Kingsway opp Caringbah Station, Caringbah",
                "disassembledName": "Kingsway opp Caringbah Station",
                "type": "stop",
                "parent": {
                 "id": "placeID:95357007:1",
                 "name": "Caringbah",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101349"
                },
                "coord": [
                 -34.041578,
                 151.122026
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Kingsway opp Caringbah Station",
                "area": "12",
                "platform": "X2",
                "platformName": "Kingsway opp Caringbah Station",
                "plannedPlatformName": "Kingsway opp Caringbah Station",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:22:00Z",
               "departureTimePlanned": "2026-11-21T02:22:00Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "222833",
               "name": "Miranda Interchange, Kiora Rd, Stand 6, Miranda",
               "disassembledName": "Miranda Interchange, Kiora Rd, Stand 6",
               "type": "platform",
               "coord": [
                -34.035407,
                151.101824
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G2228111",
                "name": "Miranda Interchange, Kiora Rd, Stand 6, Miranda",
                "disassembledName": "Miranda Interchange, Kiora Rd, Stand 6",
                "type": "stop",
                "parent": {
                 "id": "placeID:95357028:1",
                 "name": "Miranda",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10123752"
                },
                "coord": [
                 -34.035148,
                 151.102028
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "AREA_NIVEAU_GIS": "0.00",
                "stoppingPointPlanned": "Miranda Interchange, Kiora Rd, Stand 6",
                "area": "1",
                "platform": "X6",
                "platformName": "Miranda Interchange, Kiora Rd, Stand 6",
                "plannedPlatformName": "Miranda Interchange, Kiora Rd, Stand 6"
               },
               "arrivalTimePlanned": "2026-11-21T02:30:00Z",
               "departureTimePlanned": "2026-11-21T02:30:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "201016",
               "name": "Central Station, Chalmers St, Stand G, Sydney",
               "disassembledName": "Central Station, Chalmers St, Stand G",
               "type": "platform",
               "coord": [
                -33.886259,
                151.206713
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "200060",
                "name": "Central Station, Chalmers St, Stand G, Sydney",
                "disassembledName": "Central Station, Chalmers St, Stand G",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301001:1",
                 "name": "Sydney",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101100"
                },
                "coord": [
                 -33.884024,
                 151.206203
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "pbyb": "1",
                "stoppingPointPlanned": "Central Station, Chalmers St, Stand G",
                "area": "19",
                "platform": "G",
                "platformName": "Central Station, Chalmers St, Stand G",
                "plannedPlatformName": "Central Station, Chalmers St, Stand G",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T03:20:00Z",
               "modes": [
                1,
                2,
                4,
                5,
                7,
                11
               ]
              }
             ],
             "infos": [],
             "properties": {
              "vehicleAccess": [
               {
                "height": 0,
                "halfWidth": 0,
                "doorWidth": 2000,
                "attributes": 1073741824,
                "areaToStay": 0,
                "areaToMove": 0,
                "equipments": [
                 {
                  "vehicleEquipment": {
                   "equipmentType": 1,
                   "length": 0,
                   "width": 0,
                   "lengthAtPl": 0,
                   "widthAtPl": 0,
                   "maxWeight": 0
                  }
                 }
                ]
               }
              ],
              "PlanLowFloorVehicle": "1",
              "PlanWheelChairAccess": "1"
             }
            }
           ],
           "fare": {
            "tickets": []
           },
           "daysOfService": {
            "rvb": "003000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
           }
          }
         ]
        }
    """.trimIndent()

    /** Parramatta to Central, searched for Sun 22 Nov 7:00 am. Normal trains. The legs carry the Richmond to Blacktown closure as a line alert, which is not this rider's stretch. */
    val PARRAMATTA_TO_CENTRAL = """
        {
         "version": "10.6.21.17",
         "journeys": [
          {
           "rating": 0,
           "isAdditional": false,
           "interchanges": 0,
           "legs": [
            {
             "duration": 1806,
             "origin": {
              "isGlobalId": true,
              "id": "2150413",
              "name": "Parramatta Station, Platform 3, Parramatta",
              "disassembledName": "Parramatta Station, Platform 3",
              "type": "platform",
              "coord": [
               -33.817458,
               151.005205
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "215020",
               "name": "Parramatta Station, Platform 3, Parramatta",
               "disassembledName": "Parramatta Station, Platform 3",
               "type": "stop",
               "parent": {
                "id": "placeID:95346013:1",
                "name": "Parramatta",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101229"
               },
               "coord": [
                -33.81749,
                151.005325
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-21T20:08:00Z",
              "departureTimePlanned": "2026-11-21T20:08:00Z",
              "departureTimeEstimated": "2026-11-21T20:08:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "NumberOfCars": "8",
               "TravelInCarsFrom": "1",
               "TravelInCarsTo": "8",
               "TravelInCarsMessage": "any",
               "stoppingPointPlanned": "Platform 3",
               "area": "2",
               "platform": "PTA3",
               "platformName": "Platform 3",
               "plannedPlatformName": "Platform 3",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0,
                  "stepFromStreet": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               5,
               7,
               11
              ]
             },
             "destination": {
              "isGlobalId": true,
              "id": "2000336",
              "name": "Central Station, Platform 16, Sydney",
              "disassembledName": "Central Station, Platform 16",
              "type": "platform",
              "coord": [
               -33.883967,
               151.206838
              ],
              "niveau": 1,
              "parent": {
               "isGlobalId": true,
               "id": "200060",
               "name": "Central Station, Platform 16, Sydney",
               "disassembledName": "Central Station, Platform 16",
               "type": "stop",
               "parent": {
                "id": "placeID:95301001:1",
                "name": "Sydney",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101100"
               },
               "coord": [
                -33.884024,
                151.206203
               ],
               "niveau": 0
              },
              "arrivalTimeBaseTimetable": "2026-11-21T20:38:06Z",
              "arrivalTimePlanned": "2026-11-21T20:38:06Z",
              "arrivalTimeEstimated": "2026-11-21T20:38:06Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "1",
               "stoppingPointPlanned": "Platform 16",
               "area": "9",
               "platform": "CE16",
               "platformName": "Platform 16",
               "plannedPlatformName": "Platform 16",
               "callType": [
                "NOBOARDING"
               ],
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0,
                  "stepFromStreet": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               4,
               5,
               7,
               11
              ]
             },
             "transportation": {
              "id": "nsw:020T1:W:R:sj2",
              "name": "Sydney Trains Network T1 North Shore & Western Line",
              "disassembledName": "T1",
              "number": "T1 North Shore & Western Line",
              "description": "Emu Plains or Richmond to City",
              "product": {
               "id": 1,
               "class": 1,
               "name": "Sydney Trains Network",
               "iconId": 1
              },
              "operator": {
               "id": "x0001",
               "name": "Sydney Trains"
              },
              "destination": {
               "id": "10101100",
               "name": "Lindfield via Central",
               "type": "stop"
              },
              "properties": {
               "isTTB": true,
               "tripCode": 295,
               "timetablePeriod": "NSW JP",
               "lineDisplay": "LINE",
               "globalId": "3001",
               "RealtimeTripId": "852C.953.147.2.A.8.92992477",
               "OperatorURL": "http://www.sydneytrains.info/",
               "gtfsTripId": "3001.nsw-2-T1-W.1.TA.783.sj2",
               "attributes": [
                "PLAN_LOWFLOORVEHICLE",
                "PLAN_WHEELCHAIRACCESS"
               ]
              },
              "iconId": 1
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "2150413",
               "name": "Parramatta Station, Platform 3, Parramatta",
               "disassembledName": "Parramatta Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.817458,
                151.005205
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "215020",
                "name": "Parramatta Station, Platform 3, Parramatta",
                "disassembledName": "Parramatta Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95346013:1",
                 "name": "Parramatta",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101229"
                },
                "coord": [
                 -33.81749,
                 151.005325
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 3",
                "area": "2",
                "platform": "PTA3",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "departureTimePlanned": "2026-11-21T20:08:00Z",
               "modes": [
                1,
                5,
                7,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2150403",
               "name": "Harris Park Station, Platform 3, Harris Park",
               "disassembledName": "Harris Park Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.823528,
                151.007529
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "215010",
                "name": "Harris Park Station, Platform 3, Harris Park",
                "disassembledName": "Harris Park Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95346008:1",
                 "name": "Harris Park",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101228"
                },
                "coord": [
                 -33.823353,
                 151.00765
                ],
                "niveau": 0
               },
               "properties": {
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 3",
                "area": "2",
                "platform": "HPK3",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3"
               },
               "modes": [
                1
               ]
              },
              {
               "isGlobalId": true,
               "id": "2142323",
               "name": "Granville Station, Platform 3, Granville",
               "disassembledName": "Granville Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.832975,
                151.012116
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "214240",
                "name": "Granville Station, Platform 3, Granville",
                "disassembledName": "Granville Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95346007:1",
                 "name": "Granville",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101227"
                },
                "coord": [
                 -33.832758,
                 151.011869
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 3",
                "area": "2",
                "platform": "GVL3",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T20:10:30Z",
               "departureTimePlanned": "2026-11-21T20:11:00Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2142314",
               "name": "Clyde Station, Platform 4, Granville",
               "disassembledName": "Clyde Station, Platform 4",
               "type": "platform",
               "coord": [
                -33.836059,
                151.017023
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "214230",
                "name": "Clyde Station, Platform 4, Granville",
                "disassembledName": "Clyde Station, Platform 4",
                "type": "stop",
                "parent": {
                 "id": "placeID:95346007:1",
                 "name": "Granville",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101220"
                },
                "coord": [
                 -33.835953,
                 151.016942
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "false",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 4",
                "area": "2",
                "platform": "CLJ4",
                "platformName": "Platform 4",
                "plannedPlatformName": "Platform 4",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 0,
                   "heightHump": 0,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T20:12:00Z",
               "departureTimePlanned": "2026-11-21T20:12:30Z",
               "modes": [
                1
               ]
              },
              {
               "isGlobalId": true,
               "id": "2144243",
               "name": "Auburn Station, Platform 3, Auburn",
               "disassembledName": "Auburn Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.849329,
                151.032818
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "214410",
                "name": "Auburn Station, Platform 3, Auburn",
                "disassembledName": "Auburn Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95304001:1",
                 "name": "Auburn",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101219"
                },
                "coord": [
                 -33.84943,
                 151.033017
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 3",
                "area": "2",
                "platform": "AUB3",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T20:15:00Z",
               "departureTimePlanned": "2026-11-21T20:15:30Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2141313",
               "name": "Lidcombe Station, Platform 3, Lidcombe",
               "disassembledName": "Lidcombe Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.863744,
                151.044946
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "214110",
                "name": "Lidcombe Station, Platform 3, Lidcombe",
                "disassembledName": "Lidcombe Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95304004:1",
                 "name": "Lidcombe",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101218"
                },
                "coord": [
                 -33.863756,
                 151.04513
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 3",
                "area": "2",
                "platform": "LCE3",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T20:18:00Z",
               "departureTimePlanned": "2026-11-21T20:18:30Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "214063",
               "name": "Flemington Station, Platform 3, Homebush West",
               "disassembledName": "Flemington Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.86492,
                151.06993
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "214020",
                "name": "Flemington Station, Platform 3, Homebush West",
                "disassembledName": "Flemington Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95356002:1",
                 "name": "Homebush West",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101217"
                },
                "coord": [
                 -33.864912,
                 151.070178
                ],
                "niveau": 0
               },
               "properties": {
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 3",
                "area": "2",
                "platform": "FLM3",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3"
               },
               "modes": [
                1,
                5
               ]
              },
              {
               "isGlobalId": true,
               "id": "214074",
               "name": "Homebush Station, Platform 4, Homebush",
               "disassembledName": "Homebush Station, Platform 4",
               "type": "platform",
               "coord": [
                -33.866792,
                151.086308
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "214010",
                "name": "Homebush Station, Platform 4, Homebush",
                "disassembledName": "Homebush Station, Platform 4",
                "type": "stop",
                "parent": {
                 "id": "placeID:95356001:1",
                 "name": "Homebush",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101216"
                },
                "coord": [
                 -33.866688,
                 151.086195
                ],
                "niveau": 0
               },
               "properties": {
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 4",
                "area": "2",
                "platform": "HOM4",
                "platformName": "Platform 4",
                "plannedPlatformName": "Platform 4"
               },
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2135234",
               "name": "Strathfield Station, Platform 4, Strathfield",
               "disassembledName": "Strathfield Station, Platform 4",
               "type": "platform",
               "coord": [
                -33.871464,
                151.094027
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "213510",
                "name": "Strathfield Station, Platform 4, Strathfield",
                "disassembledName": "Strathfield Station, Platform 4",
                "type": "stop",
                "parent": {
                 "id": "placeID:95356003:1",
                 "name": "Strathfield",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101206"
                },
                "coord": [
                 -33.871672,
                 151.094286
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 4",
                "area": "2",
                "platform": "STR4",
                "platformName": "Platform 4",
                "plannedPlatformName": "Platform 4",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T20:23:24Z",
               "departureTimePlanned": "2026-11-21T20:24:18Z",
               "modes": [
                1,
                5,
                7,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "213493",
               "name": "Burwood Station, Platform 3, Burwood",
               "disassembledName": "Burwood Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.877125,
                151.104226
               ],
               "niveau": 1,
               "parent": {
                "isGlobalId": true,
                "id": "213410",
                "name": "Burwood Station, Platform 3, Burwood",
                "disassembledName": "Burwood Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95311001:1",
                 "name": "Burwood",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101207"
                },
                "coord": [
                 -33.877102,
                 151.103857
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "1",
                "stoppingPointPlanned": "Platform 3",
                "area": "2",
                "platform": "BWD3",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T20:26:24Z",
               "departureTimePlanned": "2026-11-21T20:26:54Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "213282",
               "name": "Croydon Station, Platform 2, Croydon",
               "disassembledName": "Croydon Station, Platform 2",
               "type": "platform",
               "coord": [
                -33.88311,
                151.115489
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "213210",
                "name": "Croydon Station, Platform 2, Croydon",
                "disassembledName": "Croydon Station, Platform 2",
                "type": "stop",
                "parent": {
                 "id": "placeID:95311003:1",
                 "name": "Croydon",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101208"
                },
                "coord": [
                 -33.883075,
                 151.115206
                ],
                "niveau": 0
               },
               "properties": {
                "AREA_NIVEAU_DIVA": "-1",
                "AREA_NIVEAU_GIS": "-1.00",
                "stoppingPointPlanned": "Platform 2",
                "area": "1",
                "platform": "CYD2",
                "platformName": "Platform 2",
                "plannedPlatformName": "Platform 2"
               },
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2131121",
               "name": "Ashfield Station, Platform 1, Ashfield",
               "disassembledName": "Ashfield Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.887438,
                151.125626
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "213110",
                "name": "Ashfield Station, Platform 1, Ashfield",
                "disassembledName": "Ashfield Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95303001:1",
                 "name": "Ashfield",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101209"
                },
                "coord": [
                 -33.887638,
                 151.125851
                ],
                "niveau": 0
               },
               "properties": {
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "ASH1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1"
               },
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2015133",
               "name": "Redfern Station, Platform 3, Eveleigh",
               "disassembledName": "Redfern Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.891999,
                151.198276
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "201510",
                "name": "Redfern Station, Platform 3, Eveleigh",
                "disassembledName": "Redfern Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301014:1",
                 "name": "Eveleigh",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101421"
                },
                "coord": [
                 -33.892048,
                 151.198419
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "AREA_NIVEAU_GIS": "-1.00",
                "stoppingPointPlanned": "Platform 3",
                "area": "2",
                "platform": "RD03",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T20:35:48Z",
               "departureTimePlanned": "2026-11-21T20:36:18Z",
               "modes": [
                1,
                5
               ]
              },
              {
               "isGlobalId": true,
               "id": "2000336",
               "name": "Central Station, Platform 16, Sydney",
               "disassembledName": "Central Station, Platform 16",
               "type": "platform",
               "coord": [
                -33.883967,
                151.206838
               ],
               "niveau": 1,
               "parent": {
                "isGlobalId": true,
                "id": "200060",
                "name": "Central Station, Platform 16, Sydney",
                "disassembledName": "Central Station, Platform 16",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301001:1",
                 "name": "Sydney",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101100"
                },
                "coord": [
                 -33.884024,
                 151.206203
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "1",
                "stoppingPointPlanned": "Platform 16",
                "area": "9",
                "platform": "CE16",
                "platformName": "Platform 16",
                "plannedPlatformName": "Platform 16",
                "callType": [
                 "NOBOARDING"
                ],
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T20:38:06Z",
               "modes": [
                1,
                2,
                4,
                5,
                7,
                11
               ]
              }
             ],
             "infos": [
              {
               "timestamps": {
                "creation": "2026-11-03T04:34:00Z"
               },
               "priority": "normal",
               "id": "ems-81155",
               "version": 1,
               "type": "lineInfo",
               "urlText": "T1 Western Line: Buses replace trains between Richmond and Blacktown or Schofields",
               "url": "https://transportnsw.info/alerts/details#/ems-81155",
               "content": "<div><strong>Sunday 22 November&nbsp;</strong></div>\n<ul>\n<li><strong>From 5.30am to 9am</strong>, buses replace trains between Richmond and Blacktown.</li>\n<li><strong>From 9am to 11am</strong>, buses replace trains between Richmond and Schofields.</li>\n<li>Trains run between Blacktown or Schofields and the City.<br /><br /></li>\n<li>Some Penrith trains make an extra stop at Burwood.<br /><br /></li>\n<li><a href=\"https://transportnsw.info/trip-planner\">Plan your trip</a>&nbsp;before you travel for up-to-date real time information.&nbsp;You can also subscribe to <a href=\"https://transportnsw.info/travel-info/ways-to-get-around/train/about-trackwork#subscribe\">planned trackwork alerts</a>.</li>\n</ul>\n<div><strong>Bus routes</strong></div>\n<div>30T1 Richmond, all stations to Schofields and return</div>\n<div>50T1 Richmond, all stations to Blacktown and return</div>\n<div>57T1 Schofields, all stations to Blacktown and return</div>\n<div>&nbsp;</div>\n<div><strong>Replacement buses</strong></div>\n<ul>\n<li>Most replacement buses are Opal-enabled. Tap on and off with a valid Opal or contactless card. Fines apply without a valid ticket.</li>\n<li>Bikes aren&rsquo;t allowed on replacement buses. Use a nearby bike shed or locker.</li>\n<li>Services may be delayed or cancelled due to driver shortages. We're working to minimise disruptions and keep your journey safe.</li>\n</ul>",
               "subtitle": "T1 Western Line: Buses replace trains between Richmond and Blacktown or Schofields"
              },
              {
               "timestamps": {
                "creation": "2026-05-15T05:00:00Z"
               },
               "priority": "normal",
               "id": "4e1e1e736f20cb61817f2061d8d4b2a78a615fac",
               "version": 7154974,
               "type": "stopInfo",
               "urlText": "Station Update - Central",
               "url": "https://transportnsw.info/alerts/details#/4e1e1e736f20cb61817f2061d8d4b2a78a615fac",
               "content": "From Sunday to Thursday, between 10pm and 5am, ceiling installation works will take place in the pedestrian tunnel between platforms 4 and 11 and the lower concourse.\n - During this time, the Redfern end stairs on Platforms 4 to 11 will be closed.\n - Customer will need to use the City end of the platform to exit via the Grand Concourse.\n - If you need assistance, ask station staff or call 9379 1777.",
               "subtitle": "Station Update - Central"
              },
              {
               "timestamps": {
                "creation": "2026-11-02T01:32:00Z"
               },
               "priority": "normal",
               "id": "6f0c56fc22a5723c45151dac1866f6342eb85686",
               "version": 171751,
               "type": "stopInfo",
               "urlText": "Station Update - Central",
               "url": "https://transportnsw.info/alerts/details#/6f0c56fc22a5723c45151dac1866f6342eb85686",
               "content": "Access Restriction \nCentral Station - Entry &amp; Exit to Chalmers Street near the Dental Hospital Is closed until further notice.",
               "subtitle": "Station Update - Central"
              }
             ],
             "coupledTripsInfo": [
              {
               "positionInTrain": "-1",
               "usableForLeg": true,
               "infoOnCarLevel": true,
               "transportation": {
                "id": "nsw:020T1:W:R:sj2",
                "name": "Sydney Trains Network T1 North Shore & Western Line",
                "disassembledName": "T1",
                "number": "T1 North Shore & Western Line",
                "product": {
                 "id": 1,
                 "class": 1,
                 "name": "Sydney Trains Network",
                 "iconId": 1
                },
                "destination": {
                 "name": "Lindfield via Central",
                 "type": "stop"
                },
                "properties": {
                 "lineDisplay": "LINE",
                 "globalId": "3001"
                }
               },
               "properties": {
                "tripId": "295"
               }
              }
             ],
             "properties": {
              "vehicleAccess": [
               {
                "height": 0,
                "halfWidth": 0,
                "doorWidth": 0,
                "attributes": 1073741824,
                "areaToStay": 0,
                "areaToMove": 0,
                "equipments": [
                 {
                  "vehicleEquipment": {
                   "equipmentType": 1,
                   "length": 0,
                   "width": 0,
                   "lengthAtPl": 0,
                   "widthAtPl": 0,
                   "maxWeight": 0
                  }
                 }
                ]
               }
              ],
              "PlanLowFloorVehicle": "1",
              "PlanWheelChairAccess": "1"
             }
            }
           ],
           "fare": {
            "tickets": []
           },
           "daysOfService": {
            "rvb": "002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
           }
          }
         ]
        }
    """.trimIndent()

    /** Central to Bondi Junction. Normal trains. The legs carry the Cronulla to Central closure as a line alert, which is not this rider's stretch. */
    val CENTRAL_TO_BONDI_JUNCTION = """
        {
         "version": "10.6.21.17",
         "journeys": [
          {
           "rating": 0,
           "isAdditional": false,
           "interchanges": 0,
           "legs": [
            {
             "duration": 780,
             "origin": {
              "isGlobalId": true,
              "id": "2000344",
              "name": "Central Station, Platform 24, Sydney",
              "disassembledName": "Central Station, Platform 24",
              "type": "platform",
              "coord": [
               -33.885155,
               151.206874
              ],
              "niveau": -2,
              "parent": {
               "isGlobalId": true,
               "id": "200060",
               "name": "Central Station, Platform 24, Sydney",
               "disassembledName": "Central Station, Platform 24",
               "type": "stop",
               "parent": {
                "id": "placeID:95301001:1",
                "name": "Sydney",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101100"
               },
               "coord": [
                -33.884024,
                151.206203
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-21T02:00:00Z",
              "departureTimePlanned": "2026-11-21T02:00:00Z",
              "departureTimeEstimated": "2026-11-21T02:00:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "-2",
               "NumberOfCars": "8",
               "TravelInCarsFrom": "1",
               "TravelInCarsTo": "8",
               "TravelInCarsMessage": "any",
               "stoppingPointPlanned": "Platform 24",
               "area": "13",
               "platform": "CE24",
               "platformName": "Platform 24",
               "plannedPlatformName": "Platform 24",
               "callType": [
                "NOALIGHTING"
               ],
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0,
                  "stepFromStreet": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               4,
               5,
               7,
               11
              ]
             },
             "destination": {
              "isGlobalId": true,
              "id": "202292",
              "name": "Bondi Junction Station, Platform 2, Bondi Junction",
              "disassembledName": "Bondi Junction Station, Platform 2",
              "type": "platform",
              "coord": [
               -33.891083,
               151.248295
              ],
              "niveau": -2,
              "parent": {
               "isGlobalId": true,
               "id": "202210",
               "name": "Bondi Junction Station, Platform 2, Bondi Junction",
               "disassembledName": "Bondi Junction Station, Platform 2",
               "type": "stop",
               "parent": {
                "id": "placeID:95361003:1",
                "name": "Bondi Junction",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101109"
               },
               "coord": [
                -33.891197,
                151.248377
               ],
               "niveau": 0
              },
              "arrivalTimeBaseTimetable": "2026-11-21T02:13:00Z",
              "arrivalTimePlanned": "2026-11-21T02:13:00Z",
              "arrivalTimeEstimated": "2026-11-21T02:13:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "-2",
               "AREA_NIVEAU_GIS": "-2.00",
               "stoppingPointPlanned": "Platform 2",
               "area": "1",
               "platform": "BJN2",
               "platformName": "Platform 2",
               "plannedPlatformName": "Platform 2",
               "callType": [
                "NOBOARDING"
               ],
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               5,
               11
              ]
             },
             "transportation": {
              "id": "nsw:020T4: :R:sj2",
              "name": "Sydney Trains Network T4 Eastern Suburbs & Illawarra Line",
              "disassembledName": "T4",
              "number": "T4 Eastern Suburbs & Illawarra Line",
              "description": "Waterfall or Cronulla to Bondi Junction",
              "product": {
               "id": 1,
               "class": 1,
               "name": "Sydney Trains Network",
               "iconId": 1
              },
              "operator": {
               "id": "x0001",
               "name": "Sydney Trains"
              },
              "destination": {
               "id": "10101109",
               "name": "Bondi Junction",
               "type": "stop"
              },
              "properties": {
               "isTTB": true,
               "tripCode": 83,
               "timetablePeriod": "NSW JP",
               "lineDisplay": "LINE",
               "globalId": "3001",
               "RealtimeTripId": "657T.953.146.128.T.8.92992778",
               "OperatorURL": "http://www.sydneytrains.info/",
               "gtfsTripId": "3001.nsw-2-T4.1.TA.179.sj2",
               "attributes": [
                "PLAN_LOWFLOORVEHICLE",
                "PLAN_WHEELCHAIRACCESS"
               ]
              },
              "iconId": 1
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "2000344",
               "name": "Central Station, Platform 24, Sydney",
               "disassembledName": "Central Station, Platform 24",
               "type": "platform",
               "coord": [
                -33.885155,
                151.206874
               ],
               "niveau": -2,
               "parent": {
                "isGlobalId": true,
                "id": "200060",
                "name": "Central Station, Platform 24, Sydney",
                "disassembledName": "Central Station, Platform 24",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301001:1",
                 "name": "Sydney",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101100"
                },
                "coord": [
                 -33.884024,
                 151.206203
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-2",
                "stoppingPointPlanned": "Platform 24",
                "area": "13",
                "platform": "CE24",
                "platformName": "Platform 24",
                "plannedPlatformName": "Platform 24",
                "callType": [
                 "NOALIGHTING"
                ],
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "departureTimePlanned": "2026-11-21T02:00:00Z",
               "modes": [
                1,
                2,
                4,
                5,
                7,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2000395",
               "name": "Town Hall Station, Platform 5, Sydney",
               "disassembledName": "Town Hall Station, Platform 5",
               "type": "platform",
               "coord": [
                -33.87372,
                151.206896
               ],
               "niveau": -3,
               "parent": {
                "isGlobalId": true,
                "id": "200070",
                "name": "Town Hall Station, Platform 5, Sydney",
                "disassembledName": "Town Hall Station, Platform 5",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301001:1",
                 "name": "Sydney",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101101"
                },
                "coord": [
                 -33.873539,
                 151.20693
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-3",
                "AREA_NIVEAU_GIS": "-3.00",
                "stoppingPointPlanned": "Platform 5",
                "area": "5",
                "platform": "THL5",
                "platformName": "Platform 5",
                "plannedPlatformName": "Platform 5",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:02:00Z",
               "departureTimePlanned": "2026-11-21T02:03:00Z",
               "modes": [
                1,
                4,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2000362",
               "name": "Martin Place Station, Platform 2, Sydney",
               "disassembledName": "Martin Place Station, Platform 2",
               "type": "platform",
               "coord": [
                -33.867803,
                151.211822
               ],
               "niveau": -3,
               "parent": {
                "isGlobalId": true,
                "id": "200030",
                "name": "Martin Place Station, Platform 2, Sydney",
                "disassembledName": "Martin Place Station, Platform 2",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301001:1",
                 "name": "Sydney",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101106"
                },
                "coord": [
                 -33.867648,
                 151.210583
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-3",
                "stoppingPointPlanned": "Platform 2",
                "area": "1",
                "platform": "MPL2",
                "platformName": "Platform 2",
                "plannedPlatformName": "Platform 2",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:04:30Z",
               "departureTimePlanned": "2026-11-21T02:05:00Z",
               "modes": [
                1,
                2
               ]
              },
              {
               "isGlobalId": true,
               "id": "201172",
               "name": "Kings Cross Station, Platform 2, Potts Point",
               "disassembledName": "Kings Cross Station, Platform 2",
               "type": "platform",
               "coord": [
                -33.874254,
                151.221828
               ],
               "niveau": -2,
               "parent": {
                "isGlobalId": true,
                "id": "201110",
                "name": "Kings Cross Station, Platform 2, Potts Point",
                "disassembledName": "Kings Cross Station, Platform 2",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301022:1",
                 "name": "Potts Point",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101107"
                },
                "coord": [
                 -33.874483,
                 151.222228
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-2",
                "stoppingPointPlanned": "Platform 2",
                "area": "1",
                "platform": "KGX2",
                "platformName": "Platform 2",
                "plannedPlatformName": "Platform 2",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:06:42Z",
               "departureTimePlanned": "2026-11-21T02:07:12Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202762",
               "name": "Edgecliff Station, Platform 2, Edgecliff",
               "disassembledName": "Edgecliff Station, Platform 2",
               "type": "platform",
               "coord": [
                -33.879282,
                151.236497
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "202710",
                "name": "Edgecliff Station, Platform 2, Edgecliff",
                "disassembledName": "Edgecliff Station, Platform 2",
                "type": "stop",
                "parent": {
                 "id": "placeID:95365004:1",
                 "name": "Edgecliff",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101108"
                },
                "coord": [
                 -33.879371,
                 151.236253
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "AREA_NIVEAU_GIS": "-1.00",
                "stoppingPointPlanned": "Platform 2",
                "area": "1",
                "platform": "EDG2",
                "platformName": "Platform 2",
                "plannedPlatformName": "Platform 2",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:09:30Z",
               "departureTimePlanned": "2026-11-21T02:10:00Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202292",
               "name": "Bondi Junction Station, Platform 2, Bondi Junction",
               "disassembledName": "Bondi Junction Station, Platform 2",
               "type": "platform",
               "coord": [
                -33.891083,
                151.248295
               ],
               "niveau": -2,
               "parent": {
                "isGlobalId": true,
                "id": "202210",
                "name": "Bondi Junction Station, Platform 2, Bondi Junction",
                "disassembledName": "Bondi Junction Station, Platform 2",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361003:1",
                 "name": "Bondi Junction",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101109"
                },
                "coord": [
                 -33.891197,
                 151.248377
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-2",
                "AREA_NIVEAU_GIS": "-2.00",
                "stoppingPointPlanned": "Platform 2",
                "area": "1",
                "platform": "BJN2",
                "platformName": "Platform 2",
                "plannedPlatformName": "Platform 2",
                "callType": [
                 "NOBOARDING"
                ],
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:13:00Z",
               "modes": [
                1,
                5,
                11
               ]
              }
             ],
             "infos": [
              {
               "timestamps": {
                "creation": "2026-11-03T05:25:00Z"
               },
               "priority": "normal",
               "id": "ems-81168",
               "version": 257997,
               "type": "lineInfo",
               "urlText": "Buses replace trains between Cronulla, Waterfall and Central",
               "url": "https://transportnsw.info/alerts/details#/ems-81168",
               "content": "<div><strong>Saturday 21 and Sunday 22 November</strong></div>\n<ul>\n<li>Buses replace trains between Cronulla, Waterfall and Central.</li>\n<li>Trains run between Central and Bondi Junction, to a changed timetable.</li>\n<li>If travelling to the Airport change at Central for a T8 Airport train.<br /><br /></li>\n<li><a href=\"https://transportnsw.info/trip-planner\">Plan your trip</a>&nbsp;before you travel for up-to-date real time information.&nbsp;You can also subscribe to <a href=\"https://transportnsw.info/travel-info/ways-to-get-around/train/about-trackwork#subscribe\">planned trackwork alerts</a>.</li>\n</ul>\n<div><strong>Bus routes</strong></div>\n<div>3AT4&nbsp; Hurstville, Central and return</div>\n<div>7T4&nbsp; Penshurst, all stations to Sydenham and return</div>\n<div>13T4&nbsp; Kogarah, Rockdale, Wolli Creek and return</div>\n<div>26T4&nbsp; Cronulla, all stations to Miranda, Central and return</div>\n<div>27T4&nbsp; Waterfall, all stations to Sutherland, all stations to Miranda, Central and return</div>\n<div>28T4&nbsp; Sutherland, all stations to Hurstville, Central and return</div>\n<div>98T4&nbsp; Cronulla, all stations to Sutherland, Hurstville, all stations to Rockdale and return</div>\n<div>36T8&nbsp; Sydenham, St Peters, Erskineville, Redfern, Central and return</div>\n<div>&nbsp;</div>\n<div><strong>Replacement buses</strong></div>\n<ul>\n<li>Most replacement buses are Opal-enabled. Tap on and off with a valid Opal or contactless card. Fines apply without a valid ticket.</li>\n<li>Bikes aren&rsquo;t allowed on replacement buses. Use a nearby bike shed or locker.</li>\n<li>Services may be delayed or cancelled due to driver shortages. We're working to minimise disruptions and keep your journey safe.</li>\n</ul>",
               "subtitle": "Buses replace trains between Cronulla, Waterfall and Central"
              },
              {
               "timestamps": {
                "creation": "2026-05-15T05:00:00Z"
               },
               "priority": "normal",
               "id": "4e1e1e736f20cb61817f2061d8d4b2a78a615fac",
               "version": 7154974,
               "type": "stopInfo",
               "urlText": "Station Update - Central",
               "url": "https://transportnsw.info/alerts/details#/4e1e1e736f20cb61817f2061d8d4b2a78a615fac",
               "content": "From Sunday to Thursday, between 10pm and 5am, ceiling installation works will take place in the pedestrian tunnel between platforms 4 and 11 and the lower concourse.\n - During this time, the Redfern end stairs on Platforms 4 to 11 will be closed.\n - Customer will need to use the City end of the platform to exit via the Grand Concourse.\n - If you need assistance, ask station staff or call 9379 1777.",
               "subtitle": "Station Update - Central"
              },
              {
               "timestamps": {
                "creation": "2026-11-02T01:32:00Z"
               },
               "priority": "normal",
               "id": "6f0c56fc22a5723c45151dac1866f6342eb85686",
               "version": 171751,
               "type": "stopInfo",
               "urlText": "Station Update - Central",
               "url": "https://transportnsw.info/alerts/details#/6f0c56fc22a5723c45151dac1866f6342eb85686",
               "content": "Access Restriction \nCentral Station - Entry &amp; Exit to Chalmers Street near the Dental Hospital Is closed until further notice.",
               "subtitle": "Station Update - Central"
              }
             ],
             "coupledTripsInfo": [
              {
               "positionInTrain": "-1",
               "usableForLeg": true,
               "infoOnCarLevel": true,
               "transportation": {
                "id": "nsw:020T4: :R:sj2",
                "name": "Sydney Trains Network T4 Eastern Suburbs & Illawarra Line",
                "disassembledName": "T4",
                "number": "T4 Eastern Suburbs & Illawarra Line",
                "product": {
                 "id": 1,
                 "class": 1,
                 "name": "Sydney Trains Network",
                 "iconId": 1
                },
                "destination": {
                 "name": "Bondi Junction",
                 "type": "stop"
                },
                "properties": {
                 "lineDisplay": "LINE",
                 "globalId": "3001"
                }
               },
               "properties": {
                "tripId": "83"
               }
              }
             ],
             "properties": {
              "vehicleAccess": [
               {
                "height": 0,
                "halfWidth": 0,
                "doorWidth": 0,
                "attributes": 1073741824,
                "areaToStay": 0,
                "areaToMove": 0,
                "equipments": [
                 {
                  "vehicleEquipment": {
                   "equipmentType": 1,
                   "length": 0,
                   "width": 0,
                   "lengthAtPl": 0,
                   "widthAtPl": 0,
                   "maxWeight": 0
                  }
                 }
                ]
               }
              ],
              "PlanLowFloorVehicle": "1",
              "PlanWheelChairAccess": "1"
             }
            }
           ],
           "fare": {
            "tickets": []
           },
           "daysOfService": {
            "rvb": "003000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
           }
          }
         ]
        }
    """.trimIndent()

    /** Sydenham to Central. Metro only, no train, while the train closure is live. */
    val SYDENHAM_TO_CENTRAL = """
        {
         "version": "10.6.21.17",
         "journeys": [
          {
           "rating": 0,
           "isAdditional": false,
           "interchanges": 0,
           "legs": [
            {
             "duration": 318,
             "origin": {
              "isGlobalId": true,
              "id": "204471",
              "name": "Sydenham Station, Platform 1, Sydenham",
              "disassembledName": "Sydenham Station, Platform 1",
              "type": "platform",
              "coord": [
               -33.913707,
               151.167192
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "204420",
               "name": "Sydenham Station, Platform 1, Sydenham",
               "disassembledName": "Sydenham Station, Platform 1",
               "type": "stop",
               "parent": {
                "id": "placeID:95338008:1",
                "name": "Sydenham",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101326"
               },
               "coord": [
                -33.914459,
                151.166526
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-21T02:02:36Z",
              "departureTimePlanned": "2026-11-21T02:02:36Z",
              "departureTimeEstimated": "2026-11-21T02:02:36Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "AREA_NIVEAU_GIS": "0.00",
               "stoppingPointPlanned": "Platform 1",
               "area": "1",
               "platform": "SYD1",
               "platformName": "Platform 1",
               "plannedPlatformName": "Platform 1",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               5
              ]
             },
             "destination": {
              "isGlobalId": true,
              "id": "2000466",
              "name": "Central Station, Platform 26, Sydney",
              "disassembledName": "Central Station, Platform 26",
              "type": "platform",
              "coord": [
               -33.884117,
               151.206381
              ],
              "niveau": -2,
              "parent": {
               "isGlobalId": true,
               "id": "200060",
               "name": "Central Station, Platform 26, Sydney",
               "disassembledName": "Central Station, Platform 26",
               "type": "stop",
               "parent": {
                "id": "placeID:95301001:1",
                "name": "Sydney",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101100"
               },
               "coord": [
                -33.884024,
                151.206203
               ],
               "niveau": 0
              },
              "arrivalTimeBaseTimetable": "2026-11-21T02:07:54Z",
              "arrivalTimePlanned": "2026-11-21T02:07:54Z",
              "arrivalTimeEstimated": "2026-11-21T02:07:54Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "-3",
               "AREA_NIVEAU_GIS": "-2.00",
               "stoppingPointPlanned": "Platform 26",
               "area": "14",
               "platform": "CE26",
               "platformName": "Platform 26",
               "plannedPlatformName": "Platform 26",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               4,
               5,
               7,
               11
              ]
             },
             "transportation": {
              "id": "nsw:030M1: :H:sj2",
              "name": "Sydney Metro Network M1 Metro North West & Bankstown Line",
              "disassembledName": "M1",
              "number": "M1 Metro North West & Bankstown Line",
              "description": "Bankstown to Tallawong",
              "product": {
               "id": 24,
               "class": 2,
               "name": "Sydney Metro Network",
               "iconId": 2
              },
              "operator": {
               "id": "SMNW",
               "name": "Sydney Metro"
              },
              "destination": {
               "id": "10101500",
               "name": "Tallawong",
               "type": "stop"
              },
              "properties": {
               "isTTB": true,
               "tripCode": 826,
               "timetablePeriod": "NSW JP",
               "lineDisplay": "LINE",
               "globalId": "3001",
               "RealtimeTripId": "0275-001-110-007:1000",
               "OperatorURL": "http://transportnsw.info",
               "FrequencyLine": "1",
               "gtfsTripId": "3001.nsw-3-M1.1.TA.3817.sj2",
               "attributes": [
                "PLAN_LOWFLOORVEHICLE",
                "PLAN_WHEELCHAIRACCESS"
               ]
              },
              "iconId": 24
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "204471",
               "name": "Sydenham Station, Platform 1, Sydenham",
               "disassembledName": "Sydenham Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.913707,
                151.167192
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "204420",
                "name": "Sydenham Station, Platform 1, Sydenham",
                "disassembledName": "Sydenham Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95338008:1",
                 "name": "Sydenham",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101326"
                },
                "coord": [
                 -33.914459,
                 151.166526
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "AREA_NIVEAU_GIS": "0.00",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "SYD1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "departureTimePlanned": "2026-11-21T02:02:36Z",
               "modes": [
                1,
                2,
                5
               ]
              },
              {
               "isGlobalId": true,
               "id": "2017077",
               "name": "Waterloo Station, Platform 1, Waterloo",
               "disassembledName": "Waterloo Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.897528,
                151.200212
               ],
               "niveau": -5,
               "parent": {
                "isGlobalId": true,
                "id": "201721",
                "name": "Waterloo Station, Platform 1, Waterloo",
                "disassembledName": "Waterloo Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301031:1",
                 "name": "Waterloo",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101515"
                },
                "coord": [
                 -33.897445,
                 151.20024
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-5",
                "AREA_NIVEAU_GIS": "-5.00",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "WLO1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:05:48Z",
               "departureTimePlanned": "2026-11-21T02:06:18Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2000466",
               "name": "Central Station, Platform 26, Sydney",
               "disassembledName": "Central Station, Platform 26",
               "type": "platform",
               "coord": [
                -33.884117,
                151.206381
               ],
               "niveau": -2,
               "parent": {
                "isGlobalId": true,
                "id": "200060",
                "name": "Central Station, Platform 26, Sydney",
                "disassembledName": "Central Station, Platform 26",
                "type": "stop",
                "parent": {
                 "id": "placeID:95301001:1",
                 "name": "Sydney",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101100"
                },
                "coord": [
                 -33.884024,
                 151.206203
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-3",
                "AREA_NIVEAU_GIS": "-2.00",
                "stoppingPointPlanned": "Platform 26",
                "area": "14",
                "platform": "CE26",
                "platformName": "Platform 26",
                "plannedPlatformName": "Platform 26",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:07:54Z",
               "modes": [
                1,
                2,
                4,
                5,
                7,
                11
               ]
              }
             ],
             "infos": [
              {
               "timestamps": {
                "creation": "2026-05-15T05:00:00Z"
               },
               "priority": "normal",
               "id": "4e1e1e736f20cb61817f2061d8d4b2a78a615fac",
               "version": 7154974,
               "type": "stopInfo",
               "urlText": "Station Update - Central",
               "url": "https://transportnsw.info/alerts/details#/4e1e1e736f20cb61817f2061d8d4b2a78a615fac",
               "content": "From Sunday to Thursday, between 10pm and 5am, ceiling installation works will take place in the pedestrian tunnel between platforms 4 and 11 and the lower concourse.\n - During this time, the Redfern end stairs on Platforms 4 to 11 will be closed.\n - Customer will need to use the City end of the platform to exit via the Grand Concourse.\n - If you need assistance, ask station staff or call 9379 1777.",
               "subtitle": "Station Update - Central"
              },
              {
               "timestamps": {
                "creation": "2026-11-02T01:32:00Z"
               },
               "priority": "normal",
               "id": "6f0c56fc22a5723c45151dac1866f6342eb85686",
               "version": 171751,
               "type": "stopInfo",
               "urlText": "Station Update - Central",
               "url": "https://transportnsw.info/alerts/details#/6f0c56fc22a5723c45151dac1866f6342eb85686",
               "content": "Access Restriction \nCentral Station - Entry &amp; Exit to Chalmers Street near the Dental Hospital Is closed until further notice.",
               "subtitle": "Station Update - Central"
              }
             ],
             "properties": {
              "vehicleAccess": [
               {
                "height": 0,
                "halfWidth": 0,
                "doorWidth": 0,
                "attributes": 1073741824,
                "areaToStay": 0,
                "areaToMove": 0,
                "equipments": [
                 {
                  "vehicleEquipment": {
                   "equipmentType": 1,
                   "length": 0,
                   "width": 0,
                   "lengthAtPl": 0,
                   "widthAtPl": 0,
                   "maxWeight": 0
                  }
                 }
                ]
               }
              ],
              "PlanLowFloorVehicle": "1",
              "PlanWheelChairAccess": "1"
             }
            }
           ],
           "fare": {
            "tickets": []
           },
           "daysOfService": {
            "rvb": "183000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
           }
          }
         ]
        }
    """.trimIndent()

    /** Bondi Junction to Bondi Beach. Normal Sydney buses (`iconId` 5), the false positive the old bus class check allowed. */
    val BONDI_JUNCTION_TO_BONDI_BEACH = """
        {
         "version": "10.6.21.17",
         "journeys": [
          {
           "rating": 0,
           "isAdditional": true,
           "interchanges": 0,
           "legs": [
            {
             "duration": 900,
             "origin": {
              "isGlobalId": true,
              "id": "202269",
              "name": "Bondi Junction Station, Stand A, Bondi Junction",
              "disassembledName": "Bondi Junction Station, Stand A",
              "type": "platform",
              "coord": [
               -33.891426,
               151.249566
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "202210",
               "name": "Bondi Junction Station, Stand A, Bondi Junction",
               "disassembledName": "Bondi Junction Station, Stand A",
               "type": "stop",
               "parent": {
                "id": "placeID:95361003:1",
                "name": "Bondi Junction",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101109"
               },
               "coord": [
                -33.891197,
                151.248377
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-21T01:59:00Z",
              "departureTimePlanned": "2026-11-21T01:59:00Z",
              "departureTimeEstimated": "2026-11-21T01:59:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "AREA_NIVEAU_GIS": "0.00",
               "pbyb": "1",
               "stoppingPointPlanned": "Bondi Junction Station, Stand A",
               "area": "11",
               "platform": "A",
               "platformName": "Bondi Junction Station, Stand A",
               "plannedPlatformName": "Bondi Junction Station, Stand A",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               5,
               11
              ]
             },
             "destination": {
              "isGlobalId": true,
              "id": "202622",
              "name": "Bondi Beach, Campbell Pde, Stand C, Bondi Beach",
              "disassembledName": "Bondi Beach, Campbell Pde, Stand C",
              "type": "platform",
              "coord": [
               -33.890566,
               151.274652
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "G202653",
               "name": "Bondi Beach, Campbell Pde, Stand C, Bondi Beach",
               "disassembledName": "Bondi Beach, Campbell Pde, Stand C",
               "type": "stop",
               "parent": {
                "id": "placeID:95361002:1",
                "name": "Bondi Beach",
                "type": "locality"
               },
               "properties": {
                "stopId": "10111789"
               },
               "coord": [
                -33.891041,
                151.274459
               ],
               "niveau": 0
              },
              "arrivalTimeBaseTimetable": "2026-11-21T02:14:00Z",
              "arrivalTimePlanned": "2026-11-21T02:14:00Z",
              "arrivalTimeEstimated": "2026-11-21T02:14:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "stoppingPointPlanned": "Bondi Beach, Campbell Pde, Stand C",
               "areaGid": "G202653",
               "area": "0",
               "platform": "C",
               "platformName": "Bondi Beach, Campbell Pde, Stand C",
               "plannedPlatformName": "Bondi Beach, Campbell Pde, Stand C"
              },
              "modes": [
               5,
               11
              ]
             },
             "transportation": {
              "id": "nsw:30333: :H:sj2",
              "name": "Sydney Buses Network 333",
              "disassembledName": "333",
              "number": "333",
              "description": "City Circular Quay to North Bondi via Bondi Junction",
              "product": {
               "id": 5,
               "class": 5,
               "name": "Sydney Buses Network",
               "iconId": 5
              },
              "operator": {
               "id": "2509",
               "name": "Transdev John Holland Buses"
              },
              "destination": {
               "id": "10111776",
               "name": "North Bondi",
               "type": "stop"
              },
              "properties": {
               "isTTB": true,
               "tripCode": 1371,
               "timetablePeriod": "NSW JP",
               "lineDisplay": "LINE",
               "globalId": "3002",
               "RealtimeTripId": "2531322",
               "OperatorURL": "http://transportnsw.info",
               "FrequencyLine": "1",
               "gtfsTripId": "3002.nsw-30-333.1.TA.1829.sj2",
               "attributes": [
                "PLAN_LOWFLOORVEHICLE",
                "PLAN_WHEELCHAIRACCESS"
               ]
              },
              "iconId": 5
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "202269",
               "name": "Bondi Junction Station, Stand A, Bondi Junction",
               "disassembledName": "Bondi Junction Station, Stand A",
               "type": "platform",
               "coord": [
                -33.891426,
                151.249566
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "202210",
                "name": "Bondi Junction Station, Stand A, Bondi Junction",
                "disassembledName": "Bondi Junction Station, Stand A",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361003:1",
                 "name": "Bondi Junction",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101109"
                },
                "coord": [
                 -33.891197,
                 151.248377
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "AREA_NIVEAU_GIS": "0.00",
                "pbyb": "1",
                "stoppingPointPlanned": "Bondi Junction Station, Stand A",
                "area": "11",
                "platform": "A",
                "platformName": "Bondi Junction Station, Stand A",
                "plannedPlatformName": "Bondi Junction Station, Stand A",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "departureTimePlanned": "2026-11-21T01:59:00Z",
               "modes": [
                1,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202251",
               "name": "Westfield Bondi Junction, Oxford St, Stand B, Bondi Junction",
               "disassembledName": "Westfield Bondi Junction, Oxford St, Stand B",
               "type": "platform",
               "coord": [
                -33.891707,
                151.251622
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202251",
                "name": "Westfield Bondi Junction, Oxford St, Stand B, Bondi Junction",
                "disassembledName": "Westfield Bondi Junction, Oxford St, Stand B",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361003:1",
                 "name": "Bondi Junction",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111580"
                },
                "coord": [
                 -33.891911,
                 151.250908
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "pbyb": "1",
                "stoppingPointPlanned": "Westfield Bondi Junction, Oxford St, Stand B",
                "area": "1",
                "platform": "B",
                "platformName": "Westfield Bondi Junction, Oxford St, Stand B",
                "plannedPlatformName": "Westfield Bondi Junction, Oxford St, Stand B",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-21T02:00:00Z",
               "departureTimePlanned": "2026-11-21T02:00:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202253",
               "name": "Bondi Rd opp Waverley Council Chambers, Bondi Junction",
               "disassembledName": "Bondi Rd opp Waverley Council Chambers",
               "type": "platform",
               "coord": [
                -33.892791,
                151.256224
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202255",
                "name": "Bondi Rd opp Waverley Council Chambers, Bondi Junction",
                "disassembledName": "Bondi Rd opp Waverley Council Chambers",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361003:1",
                 "name": "Bondi Junction",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111595"
                },
                "coord": [
                 -33.892841,
                 151.256075
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bondi Rd opp Waverley Council Chambers",
                "areaGid": "G202255",
                "area": "0",
                "platform": "X2",
                "platformName": "Bondi Rd opp Waverley Council Chambers",
                "plannedPlatformName": "Bondi Rd opp Waverley Council Chambers"
               },
               "arrivalTimePlanned": "2026-11-21T02:03:00Z",
               "departureTimePlanned": "2026-11-21T02:03:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202612",
               "name": "Bondi Rd at Penkivil St, Bondi",
               "disassembledName": "Bondi Rd at Penkivil St",
               "type": "platform",
               "coord": [
                -33.893518,
                151.2615
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202612",
                "name": "Bondi Rd at Penkivil St, Bondi",
                "disassembledName": "Bondi Rd at Penkivil St",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361001:1",
                 "name": "Bondi",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111762"
                },
                "coord": [
                 -33.893518,
                 151.2615
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bondi Rd at Penkivil St",
                "areaGid": "G202612",
                "area": "0",
                "platform": "X1",
                "platformName": "Bondi Rd at Penkivil St",
                "plannedPlatformName": "Bondi Rd at Penkivil St"
               },
               "arrivalTimePlanned": "2026-11-21T02:06:00Z",
               "departureTimePlanned": "2026-11-21T02:06:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202614",
               "name": "Bondi Rd at Wellington St, Bondi",
               "disassembledName": "Bondi Rd at Wellington St",
               "type": "platform",
               "coord": [
                -33.893929,
                151.264448
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202614",
                "name": "Bondi Rd at Wellington St, Bondi",
                "disassembledName": "Bondi Rd at Wellington St",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361001:1",
                 "name": "Bondi",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111764"
                },
                "coord": [
                 -33.89399,
                 151.264257
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bondi Rd at Wellington St",
                "areaGid": "G202614",
                "area": "0",
                "platform": "X1",
                "platformName": "Bondi Rd at Wellington St",
                "plannedPlatformName": "Bondi Rd at Wellington St"
               },
               "arrivalTimePlanned": "2026-11-21T02:07:00Z",
               "departureTimePlanned": "2026-11-21T02:07:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202616",
               "name": "Bondi Rd at Castlefield St, Bondi",
               "disassembledName": "Bondi Rd at Castlefield St",
               "type": "platform",
               "coord": [
                -33.894372,
                151.267268
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202616",
                "name": "Bondi Rd at Castlefield St, Bondi",
                "disassembledName": "Bondi Rd at Castlefield St",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361001:1",
                 "name": "Bondi",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111766"
                },
                "coord": [
                 -33.894507,
                 151.267524
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bondi Rd at Castlefield St",
                "areaGid": "G202616",
                "area": "0",
                "platform": "X1",
                "platformName": "Bondi Rd at Castlefield St",
                "plannedPlatformName": "Bondi Rd at Castlefield St"
               },
               "arrivalTimePlanned": "2026-11-21T02:08:00Z",
               "departureTimePlanned": "2026-11-21T02:08:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202619",
               "name": "Bondi Rd opp Dudley St, Bondi Beach",
               "disassembledName": "Bondi Rd opp Dudley St",
               "type": "platform",
               "coord": [
                -33.895239,
                151.271633
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202619",
                "name": "Bondi Rd opp Dudley St, Bondi Beach",
                "disassembledName": "Bondi Rd opp Dudley St",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361002:1",
                 "name": "Bondi Beach",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111769"
                },
                "coord": [
                 -33.895239,
                 151.271633
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bondi Rd opp Dudley St",
                "areaGid": "G202619",
                "area": "0",
                "platform": "X1",
                "platformName": "Bondi Rd opp Dudley St",
                "plannedPlatformName": "Bondi Rd opp Dudley St"
               },
               "arrivalTimePlanned": "2026-11-21T02:10:00Z",
               "departureTimePlanned": "2026-11-21T02:10:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202620",
               "name": "Campbell Pde opp South Bondi Beach, Bondi Beach",
               "disassembledName": "Campbell Pde opp South Bondi Beach",
               "type": "platform",
               "coord": [
                -33.894378,
                151.27307
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202654",
                "name": "Campbell Pde opp South Bondi Beach, Bondi Beach",
                "disassembledName": "Campbell Pde opp South Bondi Beach",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361002:1",
                 "name": "Bondi Beach",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111790"
                },
                "coord": [
                 -33.894355,
                 151.273199
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Campbell Pde opp South Bondi Beach",
                "areaGid": "G202654",
                "area": "0",
                "platform": "X2",
                "platformName": "Campbell Pde opp South Bondi Beach",
                "plannedPlatformName": "Campbell Pde opp South Bondi Beach"
               },
               "arrivalTimePlanned": "2026-11-21T02:12:00Z",
               "departureTimePlanned": "2026-11-21T02:12:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "202622",
               "name": "Bondi Beach, Campbell Pde, Stand C, Bondi Beach",
               "disassembledName": "Bondi Beach, Campbell Pde, Stand C",
               "type": "platform",
               "coord": [
                -33.890566,
                151.274652
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202653",
                "name": "Bondi Beach, Campbell Pde, Stand C, Bondi Beach",
                "disassembledName": "Bondi Beach, Campbell Pde, Stand C",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361002:1",
                 "name": "Bondi Beach",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111789"
                },
                "coord": [
                 -33.891041,
                 151.274459
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bondi Beach, Campbell Pde, Stand C",
                "areaGid": "G202653",
                "area": "0",
                "platform": "C",
                "platformName": "Bondi Beach, Campbell Pde, Stand C",
                "plannedPlatformName": "Bondi Beach, Campbell Pde, Stand C"
               },
               "arrivalTimePlanned": "2026-11-21T02:14:00Z",
               "modes": [
                5,
                11
               ]
              }
             ],
             "infos": [
              {
               "timestamps": {
                "creation": "2026-11-04T01:46:00Z"
               },
               "priority": "normal",
               "id": "ems-81262",
               "version": 2959,
               "type": "lineInfo",
               "urlText": "Bus service changes from 15 November",
               "url": "https://transportnsw.info/alerts/details#/ems-81262",
               "content": "<div><strong>From Sunday 15 November</strong>, bus service changes including timetable and route changes and new routes will better connect communities with employment hubs, transport and essential services. They will also provide more frequent, reliable and sustainable transport options.</div>\n<ul>\n<li>Read the <a href=\"https://transportnsw.info/news/2026/2026-bus-service-changes\">news article at transportnsw.info</a> for route-specific information and to learn more about the changes.</li>\n<li>Plan ahead using the <a href=\"https://transportnsw.info/trip-planner\">Trip Planner</a>, check signage at stops and allow extra time for your journeys during the service changes.</li>\n</ul>",
               "subtitle": "Bus service changes from 15 November"
              }
             ],
             "properties": {
              "vehicleAccess": [
               {
                "height": 0,
                "halfWidth": 0,
                "doorWidth": 2000,
                "attributes": 1073741824,
                "areaToStay": 0,
                "areaToMove": 0,
                "equipments": [
                 {
                  "vehicleEquipment": {
                   "equipmentType": 1,
                   "length": 0,
                   "width": 0,
                   "lengthAtPl": 0,
                   "widthAtPl": 0,
                   "maxWeight": 0
                  }
                 }
                ]
               }
              ],
              "PlanLowFloorVehicle": "1",
              "PlanWheelChairAccess": "1"
             }
            },
            {
             "duration": 240,
             "distance": 239,
             "origin": {
              "isGlobalId": true,
              "id": "202622",
              "name": "Bondi Beach, Bondi Beach",
              "disassembledName": "Bondi Beach",
              "type": "platform",
              "coord": [
               -33.890566,
               151.274652
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "G202653",
               "name": "Bondi Beach, Bondi Beach",
               "disassembledName": "Bondi Beach",
               "type": "stop",
               "parent": {
                "id": "placeID:95361002:1",
                "name": "Bondi Beach",
                "type": "locality"
               },
               "properties": {
                "stopId": "10111789"
               },
               "coord": [
                -33.891041,
                151.274459
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-21T02:14:00Z",
              "departureTimePlanned": "2026-11-21T02:14:00Z",
              "departureTimeEstimated": "2026-11-21T02:14:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "stoppingPointPlanned": "Bondi Beach, Campbell Pde, Stand C",
               "areaGid": "G202653",
               "area": "0",
               "platform": "C",
               "platformName": "Bondi Beach, Campbell Pde, Stand C",
               "plannedPlatformName": "Bondi Beach, Campbell Pde, Stand C"
              },
              "modes": [
               5,
               11
              ]
             },
             "destination": {
              "id": "suburbID:95361002:1:Bondi Beach:4895254:3758264:GDAV",
              "name": "Bondi Beach",
              "type": "locality",
              "coord": [
               -33.890693,
               151.273081
              ],
              "parent": {
               "id": "placeID:95361002:-1",
               "name": "Bondi Beach",
               "type": "locality",
               "disassembledName": ""
              },
              "arrivalTimeBaseTimetable": "2026-11-21T02:18:00Z",
              "arrivalTimePlanned": "2026-11-21T02:18:00Z",
              "arrivalTimeEstimated": "2026-11-21T02:18:00Z",
              "properties": {
               "area": "0"
              },
              "disassembledName": ""
             },
             "transportation": {
              "product": {
               "class": 100,
               "name": "footpath",
               "iconId": 100
              },
              "properties": {
               "tripCode": 0,
               "lineDisplay": "LINE"
              }
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "202622",
               "name": "Bondi Beach, Bondi Beach, Campbell Pde, Stand C, Bondi Beach",
               "disassembledName": "Bondi Beach, Campbell Pde, Stand C",
               "type": "platform",
               "coord": [
                -33.890566,
                151.274652
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G202653",
                "name": "Bondi Beach, Bondi Beach",
                "disassembledName": "Bondi Beach",
                "type": "stop",
                "parent": {
                 "id": "placeID:95361002:1",
                 "name": "Bondi Beach",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10111789"
                },
                "coord": [
                 -33.891041,
                 151.274459
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bondi Beach, Campbell Pde, Stand C",
                "areaGid": "G202653",
                "area": "0",
                "platform": "C",
                "platformName": "Bondi Beach, Campbell Pde, Stand C",
                "plannedPlatformName": "Bondi Beach, Campbell Pde, Stand C"
               },
               "modes": [
                5,
                11
               ]
              }
             ],
             "infos": [],
             "pathDescriptions": [
              {
               "turnDirection": "UNKNOWN",
               "manoeuvre": "LEAVE",
               "name": "Leave Low platform bus 333",
               "coord": [
                -33.890502,
                151.27467
               ],
               "duration": 0,
               "cumDuration": 0,
               "distance": 15,
               "cumDistance": 15,
               "fromCoordsIndex": 0,
               "toCoordsIndex": 0
              },
              {
               "turnDirection": "STRAIGHT",
               "manoeuvre": "KEEP",
               "name": "footpath",
               "coord": [
                -33.890502,
                151.27467
               ],
               "skyDirection": 42,
               "duration": 7,
               "cumDuration": 7,
               "distance": 7,
               "cumDistance": 22,
               "fromCoordsIndex": 0,
               "toCoordsIndex": 1,
               "properties": {
                "TRACK_SURFACE": "COBBLE"
               }
              },
              {
               "turnDirection": "U_TURN",
               "manoeuvre": "U_TURN",
               "name": "Roscoe Street",
               "coord": [
                -33.890455,
                151.274722
               ],
               "skyDirection": 306,
               "duration": 41,
               "cumDuration": 48,
               "distance": 46,
               "cumDistance": 69,
               "fromCoordsIndex": 1,
               "toCoordsIndex": 3,
               "properties": {
                "TRACK_ILLUMINATED": "true",
                "TRACK_SURFACE": "COBBLE"
               }
              },
              {
               "turnDirection": "LEFT",
               "manoeuvre": "TURN",
               "name": "Gould Lane",
               "coord": [
                -33.890199,
                151.27432
               ],
               "skyDirection": 218,
               "duration": 94,
               "cumDuration": 143,
               "distance": 104,
               "cumDistance": 174,
               "fromCoordsIndex": 3,
               "toCoordsIndex": 6,
               "properties": {
                "TRACK_SURFACE": "ASPHALTED"
               }
              },
              {
               "turnDirection": "RIGHT",
               "manoeuvre": "TURN",
               "name": "",
               "coord": [
                -33.890944,
                151.273623
               ],
               "skyDirection": 308,
               "duration": 2,
               "cumDuration": 145,
               "distance": 2,
               "cumDistance": 176,
               "fromCoordsIndex": 6,
               "toCoordsIndex": 7,
               "properties": {
                "OUTDOOR_TYPE": "HIKING_TRAIL",
                "TRACK_ILLUMINATED": "true",
                "TRACK_SURFACE": "ASPHALTED"
               }
              },
              {
               "turnDirection": "STRAIGHT",
               "manoeuvre": "CONTINUE",
               "name": "",
               "coord": [
                -33.890936,
                151.273601
               ],
               "skyDirection": 306,
               "duration": 12,
               "cumDuration": 158,
               "distance": 14,
               "cumDistance": 191,
               "fromCoordsIndex": 7,
               "toCoordsIndex": 8,
               "properties": {
                "OUTDOOR_TYPE": "HIKING_TRAIL",
                "TRACK_ILLUMINATED": "true",
                "TRACK_SURFACE": "COBBLE"
               }
              },
              {
               "turnDirection": "STRAIGHT",
               "manoeuvre": "CONTINUE",
               "name": "",
               "coord": [
                -33.890859,
                151.273467
               ],
               "skyDirection": 305,
               "duration": 2,
               "cumDuration": 160,
               "distance": 3,
               "cumDistance": 194,
               "fromCoordsIndex": 8,
               "toCoordsIndex": 9,
               "properties": {
                "OUTDOOR_TYPE": "HIKING_TRAIL",
                "TRACK_ILLUMINATED": "true",
                "TRACK_SURFACE": "ASPHALTED"
               }
              },
              {
               "turnDirection": "LEFT",
               "manoeuvre": "TURN",
               "name": "Gould Street",
               "coord": [
                -33.890842,
                151.273445
               ],
               "skyDirection": 218,
               "duration": 7,
               "cumDuration": 168,
               "distance": 8,
               "cumDistance": 202,
               "fromCoordsIndex": 9,
               "toCoordsIndex": 10,
               "properties": {
                "TRACK_ILLUMINATED": "true",
                "TRACK_SURFACE": "ASPHALTED"
               }
              },
              {
               "turnDirection": "RIGHT",
               "manoeuvre": "TURN",
               "name": "Hall Street",
               "coord": [
                -33.890898,
                151.273393
               ],
               "skyDirection": 308,
               "duration": 32,
               "cumDuration": 201,
               "distance": 36,
               "cumDistance": 239,
               "fromCoordsIndex": 10,
               "toCoordsIndex": 12
              }
             ]
            }
           ],
           "fare": {
            "tickets": []
           },
           "daysOfService": {
            "rvb": "081000000004081020408102040810200000000000000000000000000000000000000000000000000000000000000000"
           }
          }
         ]
        }
    """.trimIndent()

    /** Bankstown to Sydenham, searched for Mon 23 Nov 11:30 pm, the night metro is replaced by buses. One last metro, then replacement buses (`iconId` 14). */
    val BANKSTOWN_TO_SYDENHAM_NIGHT = """
        {
         "version": "10.6.21.17",
         "journeys": [
          {
           "rating": 0,
           "isAdditional": false,
           "interchanges": 0,
           "legs": [
            {
             "duration": 1308,
             "origin": {
              "isGlobalId": true,
              "id": "2200513",
              "name": "Bankstown Station, Platform 3, Bankstown",
              "disassembledName": "Bankstown Station, Platform 3",
              "type": "platform",
              "coord": [
               -33.917666,
               151.036061
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "220010",
               "name": "Bankstown Station, Platform 3, Bankstown",
               "disassembledName": "Bankstown Station, Platform 3",
               "type": "stop",
               "parent": {
                "id": "placeID:95305001:1",
                "name": "Bankstown",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101401"
               },
               "coord": [
                -33.917848,
                151.034654
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-23T12:33:00Z",
              "departureTimePlanned": "2026-11-23T12:33:00Z",
              "departureTimeEstimated": "2026-11-23T12:33:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "stoppingPointPlanned": "Platform 3",
               "area": "3",
               "platform": "BNK3",
               "platformName": "Platform 3",
               "plannedPlatformName": "Platform 3",
               "callType": [
                "NOALIGHTING"
               ],
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               5,
               11
              ]
             },
             "destination": {
              "isGlobalId": true,
              "id": "204471",
              "name": "Sydenham Station, Platform 1, Sydenham",
              "disassembledName": "Sydenham Station, Platform 1",
              "type": "platform",
              "coord": [
               -33.913707,
               151.167192
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "204420",
               "name": "Sydenham Station, Platform 1, Sydenham",
               "disassembledName": "Sydenham Station, Platform 1",
               "type": "stop",
               "parent": {
                "id": "placeID:95338008:1",
                "name": "Sydenham",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101326"
               },
               "coord": [
                -33.914459,
                151.166526
               ],
               "niveau": 0
              },
              "arrivalTimeBaseTimetable": "2026-11-23T12:54:48Z",
              "arrivalTimePlanned": "2026-11-23T12:54:48Z",
              "arrivalTimeEstimated": "2026-11-23T12:54:48Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "AREA_NIVEAU_GIS": "0.00",
               "stoppingPointPlanned": "Platform 1",
               "area": "1",
               "platform": "SYD1",
               "platformName": "Platform 1",
               "plannedPlatformName": "Platform 1",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               5
              ]
             },
             "transportation": {
              "id": "nsw:030M1: :H:sj2",
              "name": "Sydney Metro Network M1 Metro North West & Bankstown Line",
              "disassembledName": "M1",
              "number": "M1 Metro North West & Bankstown Line",
              "description": "Bankstown to Tallawong",
              "product": {
               "id": 24,
               "class": 2,
               "name": "Sydney Metro Network",
               "iconId": 2
              },
              "operator": {
               "id": "SMNW",
               "name": "Sydney Metro"
              },
              "destination": {
               "id": "10101500",
               "name": "Tallawong",
               "type": "stop"
              },
              "properties": {
               "isTTB": true,
               "tripCode": 693,
               "timetablePeriod": "NSW JP",
               "lineDisplay": "LINE",
               "globalId": "3001",
               "RealtimeTripId": "0273-001-110-015:1000",
               "OperatorURL": "http://transportnsw.info",
               "FrequencyLine": "1",
               "gtfsTripId": "3001.nsw-3-M1.1.TA.3134.sj2",
               "attributes": [
                "PLAN_LOWFLOORVEHICLE",
                "PLAN_WHEELCHAIRACCESS"
               ]
              },
              "iconId": 24
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "2200513",
               "name": "Bankstown Station, Platform 3, Bankstown",
               "disassembledName": "Bankstown Station, Platform 3",
               "type": "platform",
               "coord": [
                -33.917666,
                151.036061
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "220010",
                "name": "Bankstown Station, Platform 3, Bankstown",
                "disassembledName": "Bankstown Station, Platform 3",
                "type": "stop",
                "parent": {
                 "id": "placeID:95305001:1",
                 "name": "Bankstown",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101401"
                },
                "coord": [
                 -33.917848,
                 151.034654
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 3",
                "area": "3",
                "platform": "BNK3",
                "platformName": "Platform 3",
                "plannedPlatformName": "Platform 3",
                "callType": [
                 "NOALIGHTING"
                ],
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "departureTimePlanned": "2026-11-23T12:33:00Z",
               "modes": [
                1,
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2196291",
               "name": "Punchbowl Station, Platform 1, Punchbowl",
               "disassembledName": "Punchbowl Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.925366,
                151.055632
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "219610",
                "name": "Punchbowl Station, Platform 1, Punchbowl",
                "disassembledName": "Punchbowl Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315021:1",
                 "name": "Punchbowl",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101400"
                },
                "coord": [
                 -33.925405,
                 151.055548
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "PUN1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:35:12Z",
               "departureTimePlanned": "2026-11-23T12:35:42Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2195141",
               "name": "Wiley Park Station, Platform 1, Wiley Park",
               "disassembledName": "Wiley Park Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.922827,
                151.067585
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "219520",
                "name": "Wiley Park Station, Platform 1, Wiley Park",
                "disassembledName": "Wiley Park Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315014:1",
                 "name": "Wiley Park",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101399"
                },
                "coord": [
                 -33.922762,
                 151.067917
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "WLY1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:37:06Z",
               "departureTimePlanned": "2026-11-23T12:37:36Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2195131",
               "name": "Lakemba Station, Platform 1, Lakemba",
               "disassembledName": "Lakemba Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.920044,
                151.07598
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "219510",
                "name": "Lakemba Station, Platform 1, Lakemba",
                "disassembledName": "Lakemba Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315011:1",
                 "name": "Lakemba",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101398"
                },
                "coord": [
                 -33.919961,
                 151.076311
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "LAK1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:38:54Z",
               "departureTimePlanned": "2026-11-23T12:39:24Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "219281",
               "name": "Belmore Station, Platform 1, Belmore",
               "disassembledName": "Belmore Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.917195,
                151.0885
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "219210",
                "name": "Belmore Station, Platform 1, Belmore",
                "disassembledName": "Belmore Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315003:1",
                 "name": "Belmore",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101397"
                },
                "coord": [
                 -33.917225,
                 151.088415
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "AREA_NIVEAU_GIS": "-1.00",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "BMO1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:41:06Z",
               "departureTimePlanned": "2026-11-23T12:41:36Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2194111",
               "name": "Campsie Station, Platform 1, Campsie",
               "disassembledName": "Campsie Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.910438,
                151.103101
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "219410",
                "name": "Campsie Station, Platform 1, Campsie",
                "disassembledName": "Campsie Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315005:1",
                 "name": "Campsie",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101396"
                },
                "coord": [
                 -33.910435,
                 151.103458
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "CMP1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:43:30Z",
               "departureTimePlanned": "2026-11-23T12:44:06Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2193111",
               "name": "Canterbury Station, Platform 1, Canterbury",
               "disassembledName": "Canterbury Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.911974,
                151.118607
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "219320",
                "name": "Canterbury Station, Platform 1, Canterbury",
                "disassembledName": "Canterbury Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315006:1",
                 "name": "Canterbury",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101395"
                },
                "coord": [
                 -33.912087,
                 151.118731
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "AREA_NIVEAU_GIS": "-1.00",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "CBY1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:46:00Z",
               "departureTimePlanned": "2026-11-23T12:46:30Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2193121",
               "name": "Hurlstone Park Station, Platform 1, Hurlstone Park",
               "disassembledName": "Hurlstone Park Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.910291,
                151.13225
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "219310",
                "name": "Hurlstone Park Station, Platform 1, Hurlstone Park",
                "disassembledName": "Hurlstone Park Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95338003:1",
                 "name": "Hurlstone Park",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101394"
                },
                "coord": [
                 -33.910202,
                 151.132484
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "AREA_NIVEAU_GIS": "-1.00",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "HRL1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:48:06Z",
               "departureTimePlanned": "2026-11-23T12:48:36Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "220361",
               "name": "Dulwich Hill Station, Platform 1, Dulwich Hill",
               "disassembledName": "Dulwich Hill Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.910937,
                151.140884
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "220310",
                "name": "Dulwich Hill Station, Platform 1, Dulwich Hill",
                "disassembledName": "Dulwich Hill Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95338001:1",
                 "name": "Dulwich Hill",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101393"
                },
                "coord": [
                 -33.910943,
                 151.140722
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "AREA_NIVEAU_GIS": "-1.00",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "DHL1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:50:06Z",
               "departureTimePlanned": "2026-11-23T12:50:36Z",
               "modes": [
                2,
                4,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2204121",
               "name": "Marrickville Station, Platform 1, Marrickville",
               "disassembledName": "Marrickville Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.913856,
                151.153442
               ],
               "niveau": -1,
               "parent": {
                "isGlobalId": true,
                "id": "220410",
                "name": "Marrickville Station, Platform 1, Marrickville",
                "disassembledName": "Marrickville Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95338005:1",
                 "name": "Marrickville",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101392"
                },
                "coord": [
                 -33.913868,
                 151.153356
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "-1",
                "AREA_NIVEAU_GIS": "-1.00",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "MKV1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:52:30Z",
               "departureTimePlanned": "2026-11-23T12:53:00Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "204471",
               "name": "Sydenham Station, Platform 1, Sydenham",
               "disassembledName": "Sydenham Station, Platform 1",
               "type": "platform",
               "coord": [
                -33.913707,
                151.167192
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "204420",
                "name": "Sydenham Station, Platform 1, Sydenham",
                "disassembledName": "Sydenham Station, Platform 1",
                "type": "stop",
                "parent": {
                 "id": "placeID:95338008:1",
                 "name": "Sydenham",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101326"
                },
                "coord": [
                 -33.914459,
                 151.166526
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "AREA_NIVEAU_GIS": "0.00",
                "stoppingPointPlanned": "Platform 1",
                "area": "1",
                "platform": "SYD1",
                "platformName": "Platform 1",
                "plannedPlatformName": "Platform 1",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:54:48Z",
               "modes": [
                1,
                2,
                5
               ]
              }
             ],
             "infos": [
              {
               "timestamps": {
                "creation": "2026-11-09T00:45:00Z"
               },
               "priority": "normal",
               "id": "ems-81504",
               "version": 164790,
               "type": "lineInfo",
               "urlText": "Buses replace metro services between Bankstown and Sydenham",
               "url": "https://transportnsw.info/alerts/details#/ems-81504",
               "content": "<div><strong>Monday 23 November</strong></div>\n<ul>\n<li><strong>From 11.20pm</strong>, buses replace metro services from Sydenham to Bankstown.</li>\n<li><strong>From 11.40pm</strong>, buses replace metro services from Bankstown to Sydenham.</li>\n<li>Services run between Sydenham and Tallawong, to the normal frequency.</li>\n<li>The last metro services leave:<br />-&nbsp;Sydenham at 23:15, arriving Bankstown at 23:37<br />- Bankstown at 23:33, arriving Sydenham at 23:54<br /><br /></li>\n<li><a href=\"https://transportnsw.info/trip-planner\" target=\"_blank\" rel=\"noopener\">Plan your trip</a> before you travel for up-to-date real time information. You can also subscribe to <a href=\"https://transportnsw.info/travel-info/ways-to-get-around/train/about-trackwork#subscribe\" target=\"_blank\" rel=\"noopener\">planned trackwork alerts</a>.</li>\n</ul>\n<div>&nbsp;</div>\n<div><strong>Bus routes</strong><br />1M&nbsp; &nbsp;Bankstown, all stations to Sydenham and return<br />2M&nbsp; &nbsp;Bankstown, all stations to Belmore, express to Sydenham and return<br />3M&nbsp; &nbsp;Campsie, Canterbury, Sydenham and return</div>\n<div>&nbsp;</div>\n<div><br /><strong>Replacement buses</strong></div>\n<ul>\n<li>Most replacement buses are Opal-enabled. Tap on and off with a valid Opal or contactless card. Fines apply without a valid ticket.</li>\n<li>Bikes aren&rsquo;t allowed on replacement buses. Use a nearby bike shed or locker.</li>\n<li>Services may be delayed or cancelled due to driver shortages. We're working to minimise disruptions and keep your journey safe.<br /><br /></li>\n</ul>",
               "subtitle": "Buses replace metro services between Bankstown and Sydenham"
              }
             ],
             "properties": {
              "vehicleAccess": [
               {
                "height": 0,
                "halfWidth": 0,
                "doorWidth": 0,
                "attributes": 1073741824,
                "areaToStay": 0,
                "areaToMove": 0,
                "equipments": [
                 {
                  "vehicleEquipment": {
                   "equipmentType": 1,
                   "length": 0,
                   "width": 0,
                   "lengthAtPl": 0,
                   "widthAtPl": 0,
                   "maxWeight": 0
                  }
                 }
                ]
               }
              ],
              "PlanLowFloorVehicle": "1",
              "PlanWheelChairAccess": "1"
             }
            }
           ],
           "fare": {
            "tickets": []
           },
           "daysOfService": {
            "rvb": "27C8000079F3E7CF1F3E7CF903E7CF9F73E7CF9F3E7CF9F300013E7C0000000000000000000000000000000000000000"
           }
          },
          {
           "rating": 0,
           "isAdditional": false,
           "interchanges": 0,
           "legs": [
            {
             "duration": 1920,
             "origin": {
              "isGlobalId": true,
              "id": "220080",
              "name": "Bankstown Station, Bankstown",
              "disassembledName": "Bankstown Station",
              "type": "platform",
              "coord": [
               -33.918046,
               151.035744
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "220010",
               "name": "Bankstown Station, Bankstown",
               "disassembledName": "Bankstown Station",
               "type": "stop",
               "parent": {
                "id": "placeID:95305001:1",
                "name": "Bankstown",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101401"
               },
               "coord": [
                -33.917848,
                151.034654
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-23T12:41:00Z",
              "departureTimePlanned": "2026-11-23T12:41:00Z",
              "departureTimeEstimated": "2026-11-23T12:41:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "stoppingPointPlanned": "Bankstown Station",
               "area": "13",
               "platform": "X2",
               "platformName": "Bankstown Station",
               "plannedPlatformName": "Bankstown Station",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0,
                  "stepFromStreet": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               5,
               11
              ]
             },
             "destination": {
              "isGlobalId": true,
              "id": "220450",
              "name": "Sydenham Station, Railway Pde, Sydenham",
              "disassembledName": "Sydenham Station, Railway Pde",
              "type": "platform",
              "coord": [
               -33.913912,
               151.166716
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "204420",
               "name": "Sydenham Station, Railway Pde, Sydenham",
               "disassembledName": "Sydenham Station, Railway Pde",
               "type": "stop",
               "parent": {
                "id": "placeID:95338008:1",
                "name": "Sydenham",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101326"
               },
               "coord": [
                -33.914459,
                151.166526
               ],
               "niveau": 0
              },
              "arrivalTimeBaseTimetable": "2026-11-23T13:13:00Z",
              "arrivalTimePlanned": "2026-11-23T13:13:00Z",
              "arrivalTimeEstimated": "2026-11-23T13:13:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "AREA_NIVEAU_GIS": "0.00",
               "stoppingPointPlanned": "Sydenham Station, Railway Pde",
               "area": "12",
               "platform": "X3",
               "platformName": "Sydenham Station, Railway Pde",
               "plannedPlatformName": "Sydenham Station, Railway Pde",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               5
              ]
             },
             "transportation": {
              "id": "nsw:3102M: :R:sj2",
              "name": "Temporary buses 2M",
              "disassembledName": "2M",
              "number": "2M",
              "description": "Unknown",
              "product": {
               "id": 14,
               "class": 5,
               "name": "Temporary buses",
               "iconId": 5
              },
              "operator": {
               "id": "7066",
               "name": "U-Go Mobility"
              },
              "destination": {
               "id": "10101326",
               "name": "Sydenham Station: Stand E",
               "type": "stop"
              },
              "properties": {
               "isTTB": true,
               "tripCode": 3,
               "timetablePeriod": "NSW JP",
               "lineDisplay": "LINE",
               "globalId": "3001",
               "RealtimeTripId": "2790415",
               "OperatorURL": "http://transportnsw.info",
               "gtfsTripId": "3001.nsw-31-2M.1.TA.3.sj2",
               "attributes": [
                "PLAN_LOWFLOORVEHICLE",
                "PLAN_WHEELCHAIRACCESS"
               ]
              },
              "iconId": 14
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "220080",
               "name": "Bankstown Station, Bankstown",
               "disassembledName": "Bankstown Station",
               "type": "platform",
               "coord": [
                -33.918046,
                151.035744
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "220010",
                "name": "Bankstown Station, Bankstown",
                "disassembledName": "Bankstown Station",
                "type": "stop",
                "parent": {
                 "id": "placeID:95305001:1",
                 "name": "Bankstown",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101401"
                },
                "coord": [
                 -33.917848,
                 151.034654
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bankstown Station",
                "area": "13",
                "platform": "X2",
                "platformName": "Bankstown Station",
                "plannedPlatformName": "Bankstown Station",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "departureTimePlanned": "2026-11-23T12:41:00Z",
               "modes": [
                1,
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2196295",
               "name": "The Boulevarde opp The Broadway Plaza, Punchbowl",
               "disassembledName": "The Boulevarde opp The Broadway Plaza",
               "type": "platform",
               "coord": [
                -33.925557,
                151.05776
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G2196295",
                "name": "The Boulevarde opp The Broadway Plaza, Punchbowl",
                "disassembledName": "The Boulevarde opp The Broadway Plaza",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315021:1",
                 "name": "Punchbowl",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10144868"
                },
                "coord": [
                 -33.925557,
                 151.05776
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "The Boulevarde opp The Broadway Plaza",
                "areaGid": "G2196295",
                "area": "0",
                "platform": "X2",
                "platformName": "The Boulevarde opp The Broadway Plaza",
                "plannedPlatformName": "The Boulevarde opp The Broadway Plaza"
               },
               "arrivalTimePlanned": "2026-11-23T12:45:00Z",
               "departureTimePlanned": "2026-11-23T12:45:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2195110",
               "name": "Wiley Park Station, The Boulevarde, Wiley Park",
               "disassembledName": "Wiley Park Station, The Boulevarde",
               "type": "platform",
               "coord": [
                -33.922936,
                151.068098
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "219520",
                "name": "Wiley Park Station, The Boulevarde, Wiley Park",
                "disassembledName": "Wiley Park Station, The Boulevarde",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315014:1",
                 "name": "Wiley Park",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101399"
                },
                "coord": [
                 -33.922762,
                 151.067917
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Wiley Park Station, The Boulevarde",
                "area": "11",
                "platform": "X1",
                "platformName": "Wiley Park Station, The Boulevarde",
                "plannedPlatformName": "Wiley Park Station, The Boulevarde",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:48:00Z",
               "departureTimePlanned": "2026-11-23T12:48:00Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2195118",
               "name": "Lakemba Station, The Boulevarde, Stand D, Lakemba",
               "disassembledName": "Lakemba Station, The Boulevarde, Stand D",
               "type": "platform",
               "coord": [
                -33.92016,
                151.076559
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "219510",
                "name": "Lakemba Station, The Boulevarde, Stand D, Lakemba",
                "disassembledName": "Lakemba Station, The Boulevarde, Stand D",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315011:1",
                 "name": "Lakemba",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101398"
                },
                "coord": [
                 -33.919961,
                 151.076311
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Lakemba Station, The Boulevarde, Stand D",
                "area": "14",
                "platform": "D",
                "platformName": "Lakemba Station, The Boulevarde, Stand D",
                "plannedPlatformName": "Lakemba Station, The Boulevarde, Stand D",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:51:00Z",
               "departureTimePlanned": "2026-11-23T12:51:00Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "219226",
               "name": "Belmore Station, Bridge Rd, Belmore",
               "disassembledName": "Belmore Station, Bridge Rd",
               "type": "platform",
               "coord": [
                -33.917674,
                151.087929
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "219210",
                "name": "Belmore Station, Bridge Rd, Belmore",
                "disassembledName": "Belmore Station, Bridge Rd",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315003:1",
                 "name": "Belmore",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101397"
                },
                "coord": [
                 -33.917225,
                 151.088415
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Belmore Station, Bridge Rd",
                "area": "13",
                "platform": "X4",
                "platformName": "Belmore Station, Bridge Rd",
                "plannedPlatformName": "Belmore Station, Bridge Rd",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T12:54:00Z",
               "departureTimePlanned": "2026-11-23T12:54:00Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "220450",
               "name": "Sydenham Station, Railway Pde, Sydenham",
               "disassembledName": "Sydenham Station, Railway Pde",
               "type": "platform",
               "coord": [
                -33.913912,
                151.166716
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "204420",
                "name": "Sydenham Station, Railway Pde, Sydenham",
                "disassembledName": "Sydenham Station, Railway Pde",
                "type": "stop",
                "parent": {
                 "id": "placeID:95338008:1",
                 "name": "Sydenham",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101326"
                },
                "coord": [
                 -33.914459,
                 151.166526
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "AREA_NIVEAU_GIS": "0.00",
                "stoppingPointPlanned": "Sydenham Station, Railway Pde",
                "area": "12",
                "platform": "X3",
                "platformName": "Sydenham Station, Railway Pde",
                "plannedPlatformName": "Sydenham Station, Railway Pde",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T13:13:00Z",
               "modes": [
                1,
                2,
                5
               ]
              }
             ],
             "infos": [],
             "properties": {
              "vehicleAccess": [
               {
                "height": 0,
                "halfWidth": 0,
                "doorWidth": 2000,
                "attributes": 1073741824,
                "areaToStay": 0,
                "areaToMove": 0,
                "equipments": [
                 {
                  "vehicleEquipment": {
                   "equipmentType": 1,
                   "length": 0,
                   "width": 0,
                   "lengthAtPl": 0,
                   "widthAtPl": 0,
                   "maxWeight": 0
                  }
                 }
                ]
               }
              ],
              "PlanLowFloorVehicle": "1",
              "PlanWheelChairAccess": "1"
             }
            }
           ],
           "fare": {
            "tickets": []
           },
           "daysOfService": {
            "rvb": "204000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
           }
          },
          {
           "rating": 0,
           "isAdditional": false,
           "interchanges": 0,
           "legs": [
            {
             "duration": 1920,
             "origin": {
              "isGlobalId": true,
              "id": "220080",
              "name": "Bankstown Station, Bankstown",
              "disassembledName": "Bankstown Station",
              "type": "platform",
              "coord": [
               -33.918046,
               151.035744
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "220010",
               "name": "Bankstown Station, Bankstown",
               "disassembledName": "Bankstown Station",
               "type": "stop",
               "parent": {
                "id": "placeID:95305001:1",
                "name": "Bankstown",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101401"
               },
               "coord": [
                -33.917848,
                151.034654
               ],
               "niveau": 0
              },
              "departureTimeBaseTimetable": "2026-11-23T12:53:00Z",
              "departureTimePlanned": "2026-11-23T12:53:00Z",
              "departureTimeEstimated": "2026-11-23T12:53:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "stoppingPointPlanned": "Bankstown Station",
               "area": "13",
               "platform": "X2",
               "platformName": "Bankstown Station",
               "plannedPlatformName": "Bankstown Station",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0,
                  "stepFromStreet": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               5,
               11
              ]
             },
             "destination": {
              "isGlobalId": true,
              "id": "220450",
              "name": "Sydenham Station, Railway Pde, Sydenham",
              "disassembledName": "Sydenham Station, Railway Pde",
              "type": "platform",
              "coord": [
               -33.913912,
               151.166716
              ],
              "niveau": 0,
              "parent": {
               "isGlobalId": true,
               "id": "204420",
               "name": "Sydenham Station, Railway Pde, Sydenham",
               "disassembledName": "Sydenham Station, Railway Pde",
               "type": "stop",
               "parent": {
                "id": "placeID:95338008:1",
                "name": "Sydenham",
                "type": "locality"
               },
               "properties": {
                "stopId": "10101326"
               },
               "coord": [
                -33.914459,
                151.166526
               ],
               "niveau": 0
              },
              "arrivalTimeBaseTimetable": "2026-11-23T13:25:00Z",
              "arrivalTimePlanned": "2026-11-23T13:25:00Z",
              "arrivalTimeEstimated": "2026-11-23T13:25:00Z",
              "properties": {
               "WheelchairAccess": "true",
               "AREA_NIVEAU_DIVA": "0",
               "AREA_NIVEAU_GIS": "0.00",
               "stoppingPointPlanned": "Sydenham Station, Railway Pde",
               "area": "12",
               "platform": "X3",
               "platformName": "Sydenham Station, Railway Pde",
               "plannedPlatformName": "Sydenham Station, Railway Pde",
               "accessArray": [
                {
                 "stoppointAccess": {
                  "height": 780,
                  "equipment": 0,
                  "maxWeight": 0,
                  "distanceTrack": 0,
                  "lengthAvail": 0,
                  "widthAvail": 0,
                  "attributes": 0
                 }
                }
               ]
              },
              "modes": [
               1,
               2,
               5
              ]
             },
             "transportation": {
              "id": "nsw:3102M: :R:sj2",
              "name": "Temporary buses 2M",
              "disassembledName": "2M",
              "number": "2M",
              "description": "Unknown",
              "product": {
               "id": 14,
               "class": 5,
               "name": "Temporary buses",
               "iconId": 5
              },
              "operator": {
               "id": "7066",
               "name": "U-Go Mobility"
              },
              "destination": {
               "id": "10101326",
               "name": "Sydenham Station: Stand E",
               "type": "stop"
              },
              "properties": {
               "isTTB": true,
               "tripCode": 4,
               "timetablePeriod": "NSW JP",
               "lineDisplay": "LINE",
               "globalId": "3001",
               "RealtimeTripId": "2790438",
               "OperatorURL": "http://transportnsw.info",
               "gtfsTripId": "3001.nsw-31-2M.1.TA.4.sj2",
               "attributes": [
                "PLAN_LOWFLOORVEHICLE",
                "PLAN_WHEELCHAIRACCESS"
               ]
              },
              "iconId": 14
             },
             "stopSequence": [
              {
               "isGlobalId": true,
               "id": "220080",
               "name": "Bankstown Station, Bankstown",
               "disassembledName": "Bankstown Station",
               "type": "platform",
               "coord": [
                -33.918046,
                151.035744
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "220010",
                "name": "Bankstown Station, Bankstown",
                "disassembledName": "Bankstown Station",
                "type": "stop",
                "parent": {
                 "id": "placeID:95305001:1",
                 "name": "Bankstown",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101401"
                },
                "coord": [
                 -33.917848,
                 151.034654
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Bankstown Station",
                "area": "13",
                "platform": "X2",
                "platformName": "Bankstown Station",
                "plannedPlatformName": "Bankstown Station",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0,
                   "stepFromStreet": 0
                  }
                 }
                ]
               },
               "departureTimePlanned": "2026-11-23T12:53:00Z",
               "modes": [
                1,
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2196295",
               "name": "The Boulevarde opp The Broadway Plaza, Punchbowl",
               "disassembledName": "The Boulevarde opp The Broadway Plaza",
               "type": "platform",
               "coord": [
                -33.925557,
                151.05776
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "G2196295",
                "name": "The Boulevarde opp The Broadway Plaza, Punchbowl",
                "disassembledName": "The Boulevarde opp The Broadway Plaza",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315021:1",
                 "name": "Punchbowl",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10144868"
                },
                "coord": [
                 -33.925557,
                 151.05776
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "The Boulevarde opp The Broadway Plaza",
                "areaGid": "G2196295",
                "area": "0",
                "platform": "X2",
                "platformName": "The Boulevarde opp The Broadway Plaza",
                "plannedPlatformName": "The Boulevarde opp The Broadway Plaza"
               },
               "arrivalTimePlanned": "2026-11-23T12:57:00Z",
               "departureTimePlanned": "2026-11-23T12:57:00Z",
               "modes": [
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2195110",
               "name": "Wiley Park Station, The Boulevarde, Wiley Park",
               "disassembledName": "Wiley Park Station, The Boulevarde",
               "type": "platform",
               "coord": [
                -33.922936,
                151.068098
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "219520",
                "name": "Wiley Park Station, The Boulevarde, Wiley Park",
                "disassembledName": "Wiley Park Station, The Boulevarde",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315014:1",
                 "name": "Wiley Park",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101399"
                },
                "coord": [
                 -33.922762,
                 151.067917
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Wiley Park Station, The Boulevarde",
                "area": "11",
                "platform": "X1",
                "platformName": "Wiley Park Station, The Boulevarde",
                "plannedPlatformName": "Wiley Park Station, The Boulevarde",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T13:00:00Z",
               "departureTimePlanned": "2026-11-23T13:00:00Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "2195118",
               "name": "Lakemba Station, The Boulevarde, Stand D, Lakemba",
               "disassembledName": "Lakemba Station, The Boulevarde, Stand D",
               "type": "platform",
               "coord": [
                -33.92016,
                151.076559
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "219510",
                "name": "Lakemba Station, The Boulevarde, Stand D, Lakemba",
                "disassembledName": "Lakemba Station, The Boulevarde, Stand D",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315011:1",
                 "name": "Lakemba",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101398"
                },
                "coord": [
                 -33.919961,
                 151.076311
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Lakemba Station, The Boulevarde, Stand D",
                "area": "14",
                "platform": "D",
                "platformName": "Lakemba Station, The Boulevarde, Stand D",
                "plannedPlatformName": "Lakemba Station, The Boulevarde, Stand D",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T13:03:00Z",
               "departureTimePlanned": "2026-11-23T13:03:00Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "219226",
               "name": "Belmore Station, Bridge Rd, Belmore",
               "disassembledName": "Belmore Station, Bridge Rd",
               "type": "platform",
               "coord": [
                -33.917674,
                151.087929
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "219210",
                "name": "Belmore Station, Bridge Rd, Belmore",
                "disassembledName": "Belmore Station, Bridge Rd",
                "type": "stop",
                "parent": {
                 "id": "placeID:95315003:1",
                 "name": "Belmore",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101397"
                },
                "coord": [
                 -33.917225,
                 151.088415
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "stoppingPointPlanned": "Belmore Station, Bridge Rd",
                "area": "13",
                "platform": "X4",
                "platformName": "Belmore Station, Bridge Rd",
                "plannedPlatformName": "Belmore Station, Bridge Rd",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T13:06:00Z",
               "departureTimePlanned": "2026-11-23T13:06:00Z",
               "modes": [
                2,
                5,
                11
               ]
              },
              {
               "isGlobalId": true,
               "id": "220450",
               "name": "Sydenham Station, Railway Pde, Sydenham",
               "disassembledName": "Sydenham Station, Railway Pde",
               "type": "platform",
               "coord": [
                -33.913912,
                151.166716
               ],
               "niveau": 0,
               "parent": {
                "isGlobalId": true,
                "id": "204420",
                "name": "Sydenham Station, Railway Pde, Sydenham",
                "disassembledName": "Sydenham Station, Railway Pde",
                "type": "stop",
                "parent": {
                 "id": "placeID:95338008:1",
                 "name": "Sydenham",
                 "type": "locality"
                },
                "properties": {
                 "stopId": "10101326"
                },
                "coord": [
                 -33.914459,
                 151.166526
                ],
                "niveau": 0
               },
               "properties": {
                "WheelchairAccess": "true",
                "AREA_NIVEAU_DIVA": "0",
                "AREA_NIVEAU_GIS": "0.00",
                "stoppingPointPlanned": "Sydenham Station, Railway Pde",
                "area": "12",
                "platform": "X3",
                "platformName": "Sydenham Station, Railway Pde",
                "plannedPlatformName": "Sydenham Station, Railway Pde",
                "accessArray": [
                 {
                  "stoppointAccess": {
                   "height": 780,
                   "equipment": 0,
                   "maxWeight": 0,
                   "distanceTrack": 0,
                   "lengthAvail": 0,
                   "widthAvail": 0,
                   "attributes": 0
                  }
                 }
                ]
               },
               "arrivalTimePlanned": "2026-11-23T13:25:00Z",
               "modes": [
                1,
                2,
                5
               ]
              }
             ],
             "infos": [],
             "properties": {
              "vehicleAccess": [
               {
                "height": 0,
                "halfWidth": 0,
                "doorWidth": 2000,
                "attributes": 1073741824,
                "areaToStay": 0,
                "areaToMove": 0,
                "equipments": [
                 {
                  "vehicleEquipment": {
                   "equipmentType": 1,
                   "length": 0,
                   "width": 0,
                   "lengthAtPl": 0,
                   "widthAtPl": 0,
                   "maxWeight": 0
                  }
                 }
                ]
               }
              ],
              "PlanLowFloorVehicle": "1",
              "PlanWheelChairAccess": "1"
             }
            }
           ],
           "fare": {
            "tickets": []
           },
           "daysOfService": {
            "rvb": "204000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
           }
          }
         ]
        }
    """.trimIndent()
}
