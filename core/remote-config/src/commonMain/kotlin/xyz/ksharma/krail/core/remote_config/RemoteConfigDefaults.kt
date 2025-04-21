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
            )
        )
    }
}
