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
            Pair(FlagKeys.LOCAL_STOPS_ENABLED.key, true),
            Pair(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key,
                """["200060", "200070", "200080", "206010", "2150106", "200017", "200039", "201016", "201039", "201080", "200066", "200030", "200046", "200050"]""".trimMargin()
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
                """[{"stopId":"207210","parkRideFacilityId":"6","parkRideName":"Park&Ride - Gordon Henry St (north)"},
                    |{"stopId":"253330","parkRideFacilityId":"7","parkRideName":"Park&Ride - Kiama"},
                    |{"stopId":"225040","parkRideFacilityId":"8","parkRideName":"Park&Ride - Gosford"},
                    |{"stopId":"221210","parkRideFacilityId":"9","parkRideName":"Park&Ride - Revesby"},
                    |{"stopId":"210120","parkRideFacilityId":"10","parkRideName":"Park&Ride - Warriewood"},
                    |{"stopId":"210115","parkRideFacilityId":"11","parkRideName":"Park&Ride - Narrabeen"},
                    |{"stopId":"210318","parkRideFacilityId":"12","parkRideName":"Park&Ride - Mona Vale"},
                    |{"stopId":"209913","parkRideFacilityId":"13","parkRideName":"Park&Ride - Dee Why"},
                    |{"stopId":"211420","parkRideFacilityId":"14","parkRideName":"Park&Ride - West Ryde"},
                    |{"stopId":"223210","parkRideFacilityId":"15","parkRideName":"Park&Ride - Sutherland"},
                    |{"stopId":"2232126","parkRideFacilityId":"15","parkRideName":"Park&Ride - Sutherland"},
                    |{"stopId":"2232254","parkRideFacilityId":"15","parkRideName":"Park&Ride - Sutherland"},
                    |{"stopId":"217933","parkRideFacilityId":"16","parkRideName":"Park&Ride - Leppington"},
                    |{"stopId":"217426","parkRideFacilityId":"17","parkRideName":"Park&Ride - Edmondson Park (south)"},
                    |{"stopId":"276010","parkRideFacilityId":"18","parkRideName":"Park&Ride - St Marys"},
                    |{"stopId":"256020","parkRideFacilityId":"19","parkRideName":"Park&Ride - Campbelltown Farrow Rd (north)"},
                    |{"stopId":"256020","parkRideFacilityId":"20","parkRideName":"Park&Ride - Campbelltown Hurley St"},
                    |{"stopId":"275010","parkRideFacilityId":"21","parkRideName":"Park&Ride - Penrith (at-grade)"},
                    |{"stopId":"275010","parkRideFacilityId":"22","parkRideName":"Park&Ride - Penrith (multi-level)"},
                    |{"stopId":"217010","parkRideFacilityId":"23","parkRideName":"Park&Ride - Warwick Farm"},
                    |{"stopId":"276220","parkRideFacilityId":"24","parkRideName":"Park&Ride - Schofields"},
                    |{"stopId":"207763","parkRideFacilityId":"25","parkRideName":"Park&Ride - Hornsby"},
                    |{"stopId":"2155384","parkRideFacilityId":"26","parkRideName":"Park&Ride - Tallawong P1"},
                    |{"stopId":"2155384","parkRideFacilityId":"27","parkRideName":"Park&Ride - Tallawong P2"},
                    |{"stopId":"2155384","parkRideFacilityId":"28","parkRideName":"Park&Ride - Tallawong P3"},
                    |{"stopId":"2155382","parkRideFacilityId":"29","parkRideName":"Park&Ride - Kellyville (north)"},
                    |{"stopId":"2155382","parkRideFacilityId":"30","parkRideName":"Park&Ride - Kellyville (south)"},
                    |{"stopId":"2153478","parkRideFacilityId":"31","parkRideName":"Park&Ride - Bella Vista"},
                    |{"stopId":"2154392","parkRideFacilityId":"32","parkRideName":"Park&Ride - Hills Showground"},
                    |{"stopId":"2126158","parkRideFacilityId":"33","parkRideName":"Park&Ride - Cherrybrook"},
                    |{"stopId":"207010","parkRideFacilityId":"34","parkRideName":"Park&Ride - Lindfield Village Green"},
                    |{"stopId":"220910","parkRideFacilityId":"35","parkRideName":"Park&Ride - Beverly Hills"},
                    |{"stopId":"275020","parkRideFacilityId":"36","parkRideName":"Park&Ride - Emu Plains"},
                    |{"stopId":"221010","parkRideFacilityId":"37","parkRideName":"Park&Ride - Riverwood"},
                    |{"stopId":"213110","parkRideFacilityId":"486","parkRideName":"Park&Ride - Ashfield"},
                    |{"stopId":"221710","parkRideFacilityId":"487","parkRideName":"Park&Ride - Kogarah"},
                    |{"stopId":"214710","parkRideFacilityId":"488","parkRideName":"Park&Ride - Seven Hills"},
                    |{"stopId":"209325","parkRideFacilityId":"489","parkRideName":"Park&Ride - Manly Vale"},
                    |{"stopId":"209324","parkRideFacilityId":"489","parkRideName":"Park&Ride - Manly Vale"},
                    |{"stopId":"210017","parkRideFacilityId":"490","parkRideName":"Park&Ride - Brookvale"}]"""
                    .trimMargin()
            ),
        )
    }
}
