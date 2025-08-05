package xyz.ksharma.krail.core.remote_config

import xyz.ksharma.krail.core.remote_config.flag.FlagKeys

/**
 * Holds the default values for remote configuration.
 * Add new default values here. These will be used as fallbacks when the remote config is not available
 * due to network or other issues.
 */
object RemoteConfigDefaults {

    /**
     * Returns a list of default configuration key-value pairs.
     * These defaults are used as fallbacks when remote config values are not available.
     */
    fun getDefaults(): Array<Pair<String, Any?>> {
        return arrayOf(
            Pair(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key,
                """["200060", "200070", "200080", "206010", "2150106", "200017", "200039", "201016", "201039", "201080", "200066", "200030", "200046", "200050", "2155384"]""".trimMargin()
            ),
            Pair(
                FlagKeys.OUR_STORY_TEXT.key,
                """
    Welcome to KRAIL, and thank you so much for using the app. \uD83D\uDC4B

    I hope navigating around Sydney has become slightly easier for you.

    Every detail in this app, from the colors and buttons to the animations, was crafted with care, passion\uFE0F, and love over late nights and weekends. KRAIL isn't built by a company; it is built by one person, simply trying to create something calm, helpful, and free from distractions \uD83E\uDDD8.

    I'm Karan. I live in Sydney, and I originally built KRAIL for myself, just to check the next train time without scrolling past ads. I also needed the text to be larger than usual to read comfortably, something most popular apps don't handle very well. \uD83D\uDE3F So, I set out to build something more accessible and fun, an app that could support different needs while staying simple, clear, and easy to use.

    At first, it was just mine. Then I shared it with friends and family, and they shared it with others. Slowly, it started to grow, not through ads or big launches, but through people who found it helpful and passed it along. Maybe that's how it reached you, too. \uD83D\uDC95

    If KRAIL has helped you in any way, I'd really love to hear from you. Many features you enjoy in KRAIL were only possible because someone shared feedback and suggestions. I truly want KRAIL to be your personal companion; therefore, I'm always listening. Whether it's a suggestion, a bug, or just a hello \uD83D\uDC4B, feel free to email me anytime at hey@krail.app. I read every single message, and your feedback means the world to me.

    If you've found KRAIL helpful, I hope you'll share it with someone else, just like someone once shared it with you \uD83D\uDC96.

    Thanks again for being a part of this journey.
                """.trimMargin(),
            ),
            Pair(
                FlagKeys.DISCLAIMER_TEXT.key,
                """
    Important to note: Real-time data in KRAIL is provided by Transport for NSW. While I strive to ensure accuracy, I cannot guarantee it will always be correct. For the latest and most up-to-date information, please visit www.transportnsw.info
                """.trimMargin(),
            ),
            Pair(
                FlagKeys.NSW_PARK_RIDE_FACILITIES.key,
                """[{"stopId":"207210","parkRideFacilityId":"6","parkRideName":"Gordon Henry St (north)"},
                    |{"stopId":"253330","parkRideFacilityId":"7","parkRideName":"Kiama"},
                    |{"stopId":"225040","parkRideFacilityId":"8","parkRideName":"Gosford"},
                    |{"stopId":"221210","parkRideFacilityId":"9","parkRideName":"Revesby"},
                    |{"stopId":"210120","parkRideFacilityId":"10","parkRideName":"Warriewood"},
                    |{"stopId":"210115","parkRideFacilityId":"11","parkRideName":"Narrabeen"},
                    |{"stopId":"210318","parkRideFacilityId":"12","parkRideName":"Mona Vale"},
                    |{"stopId":"2103108","parkRideFacilityId":"12","parkRideName":"Mona Vale"},
                    |{"stopId":"209913","parkRideFacilityId":"13","parkRideName":"Dee Why"},
                    |{"stopId":"211420","parkRideFacilityId":"14","parkRideName":"West Ryde"},
                    |{"stopId":"223210","parkRideFacilityId":"15","parkRideName":"Sutherland"},
                    |{"stopId":"2232126","parkRideFacilityId":"15","parkRideName":"Sutherland"},
                    |{"stopId":"2232254","parkRideFacilityId":"15","parkRideName":"Sutherland"},
                    |{"stopId":"217933","parkRideFacilityId":"16","parkRideName":"Leppington"},
                    |{"stopId":"217426","parkRideFacilityId":"17","parkRideName":"Edmondson Park (south)"},
                    |{"stopId":"276010","parkRideFacilityId":"18","parkRideName":"St Marys"},
                    |{"stopId":"256020","parkRideFacilityId":"19","parkRideName":"Campbelltown Farrow Rd (north)"},
                    |{"stopId":"256020","parkRideFacilityId":"20","parkRideName":"Campbelltown Hurley St"},
                    |{"stopId":"275010","parkRideFacilityId":"21","parkRideName":"Penrith (at-grade)"},
                    |{"stopId":"275010","parkRideFacilityId":"22","parkRideName":"Penrith (multi-level)"},
                    |{"stopId":"217010","parkRideFacilityId":"23","parkRideName":"Warwick Farm"},
                    |{"stopId":"276220","parkRideFacilityId":"24","parkRideName":"Schofields"},
                    |{"stopId":"207720","parkRideFacilityId":"25","parkRideName":"Hornsby"},
                    |{"stopId":"207763","parkRideFacilityId":"25","parkRideName":"Hornsby"},
                    |{"stopId":"2155384","parkRideFacilityId":"26","parkRideName":"Tallawong P1"},
                    |{"stopId":"2155384","parkRideFacilityId":"27","parkRideName":"Tallawong P2"},
                    |{"stopId":"2155384","parkRideFacilityId":"28","parkRideName":"Tallawong P3"},
                    |{"stopId":"2155382","parkRideFacilityId":"29","parkRideName":"Kellyville (north)"},
                    |{"stopId":"2155382","parkRideFacilityId":"30","parkRideName":"Kellyville (south)"},
                    |{"stopId":"2153478","parkRideFacilityId":"31","parkRideName":"Bella Vista"},
                    |{"stopId":"2154392","parkRideFacilityId":"32","parkRideName":"Hills Showground"},
                    |{"stopId":"2126158","parkRideFacilityId":"33","parkRideName":"Cherrybrook"},
                    |{"stopId":"207010","parkRideFacilityId":"34","parkRideName":"Lindfield Village Green"},
                    |{"stopId":"220910","parkRideFacilityId":"35","parkRideName":"Beverly Hills"},
                    |{"stopId":"275020","parkRideFacilityId":"36","parkRideName":"Emu Plains"},
                    |{"stopId":"221010","parkRideFacilityId":"37","parkRideName":"Riverwood"},
                    |{"stopId":"213110","parkRideFacilityId":"486","parkRideName":"Ashfield"},
                    |{"stopId":"221710","parkRideFacilityId":"487","parkRideName":"Kogarah"},
                    |{"stopId":"214710","parkRideFacilityId":"488","parkRideName":"Seven Hills"},
                    |{"stopId":"209325","parkRideFacilityId":"489","parkRideName":"Manly Vale"},
                    |{"stopId":"209324","parkRideFacilityId":"489","parkRideName":"Manly Vale"},
                    |{"stopId":"210017","parkRideFacilityId":"490","parkRideName":"Brookvale"}]"""
                    .trimMargin()
            ),
            Pair(FlagKeys.NSW_PARK_RIDE_PEAK_TIME_COOLDOWN.key, 120),
            Pair(FlagKeys.NSW_PARK_RIDE_NON_PEAK_TIME_COOLDOWN.key, 600),
            Pair(first = FlagKeys.NSW_PARK_RIDE_BETA.key, false),
            Pair(
                first = FlagKeys.NSW_PARK_RIDE_BETA_MESSAGE_DESC.key,
                second = "🅿️\uD83D\uDE99 Park & Ride is in beta! We’ve just rolled it out and we’d love your help making it better. " +
                        "If something’s not quite right (or you just have thoughts), email us anytime at hey@krail.app or reach out via LinkedIn. 💕",
            ),
            Pair(
                first = FlagKeys.FESTIVALS.key,
                second = "[]",
            ),
            Pair(
                first = FlagKeys.DISCOVER_SYDNEY.key,
                second = """
                    [
                      {
                        "cardId": "card_1",
                        "title": "CITY 2 SURF",
                        "description": "Join the iconic City 2 Surf event in Sydney! Experience the thrill of running from the city to Bondi Beach.",
                        "startDate": "2025-12-20",
                        "endDate": "2025-12-31",
                        "imageList": [
                          "https://i.imgur.com/m9kVyqt.png"
                        ],
                        "type": "Travel",
                        "buttons": [
                          {
                            "buttonType": "Cta",
                            "label": "Click Me",
                            "url": "https://example.com/cta"
                          }
                        ]
                      },
                      {
                        "cardId": "card_2",
                        "title": "Park & Ride",
                        "description": "Your guide to Park & Ride facilities in NSW. Find the nearest facility and enjoy hassle-free travel.",
                        "disclaimer": "Image Credit: Unsplash",
                        "imageList": [
                          "https://i.imgur.com/LvuEwiP.png"
                        ],
                        "type": "Events",
                        "buttons": []
                      },
                      {
                        "cardId": "card_3",
                        "title": "Follow KRAIL",
                        "description": "Stay updated with the latest from KRAIL.",
                        "imageList": [
                          "https://i.imgur.com/xFahu5j.png"
                        ],
                        "type": "Travel",
                        "buttons": [
                          {
                            "buttonType": "AppSocial"
                          }
                        ]
                      },
                      {
                        "cardId": "card_4",
                        "title": "Opal Fare Changes 2025",
                        "description": "Opal fares are changing in 2025. Check out the latest updates and plan your travel accordingly.",
                        "imageList": [
                          "https://i.imgur.com/kpd99ri.png"
                        ],
                        "type": "Events",
                        "buttons": [
                          {
                            "buttonType": "PartnerSocial",
                            "socialPartnerName": "XYZ Place",
                            "links": [
                              {
                                "type": "Facebook",
                                "url": "https://example.com"
                              }
                            ]
                          }
                        ]
                      },
                      {
                        "cardId": "card_5",
                        "title": "New Park & Ride",
                        "description": "Park & Ride is a convenient way to travel in NSW. Find out more about the facilities available.",
                        "imageList": [
                          "https://i.imgur.com/vMbFo2W.png"
                        ],
                        "type": "Food",
                        "buttons": [
                          {
                            "buttonType": "Share",
                            "shareUrl": "https://example.com/share"
                          }
                        ]
                      },
                      {
                        "cardId": "card_6",
                        "title": "Cta + Share Card",
                        "description": "This is a sample description for the Discover Card. It can be used to display additional information.",
                        "imageList": [
                          "https://images.unsplash.com/photo-1752939124510-e444139e6404"
                        ],
                        "type": "Sports",
                        "buttons": [
                          {
                            "buttonType": "Cta",
                            "label": "Click Me",
                            "url": "https://example.com/cta"
                          },
                          {
                            "buttonType": "Share",
                            "shareUrl": "https://example.com/share"
                          }
                        ]
                      }
                    ]
                """.trimIndent(),
            ),
            Pair(
                first = FlagKeys.DISCOVER_AVAILABLE.key,
                second = true,
            ),
        )
    }
}
