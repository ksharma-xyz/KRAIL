package xyz.ksharma.krail.trip.planner.ui.intro

/*
 * The emoji the Intro may draw besides the app's own non festival list
 * ([xyz.ksharma.krail.core.festival.FestivalManager.commonEmojiList]), each one an operator approved exception.
 * This file is the only place in the Intro where any of them may be written out. Everything else asks
 * [introEmojiFor], and `IntroEmojiGuardTest` fails if any other emoji, or any of these anywhere but here, appears
 * in an Intro source.
 *
 * The pin and the star are the first two. The five transport emoji came later, so the theme screen can show the
 * mode the rider chose and the Park & Ride screen a car. Their code points are looked up from the Unicode names:
 * TRAIN 1F686, METRO 1F687, BUS 1F68C, FERRY 26F4 followed by the emoji variation selector FE0F, and AUTOMOBILE 1F697.
 */

/** The pin, for the destination screen. Code point 1F4CD, ROUND PUSHPIN. */
internal const val INTRO_PIN_EMOJI = "📍"

/** The star, for the confirm screen. Code point 2B50, WHITE MEDIUM STAR. */
internal const val INTRO_STAR_EMOJI = "⭐"

/** The train, for the theme screen once Train is chosen. Code point 1F686, TRAIN. */
internal const val INTRO_TRAIN_EMOJI = "🚆"

/** The metro, for the theme screen once Metro is chosen. Code point 1F687, METRO. */
internal const val INTRO_METRO_EMOJI = "🚇"

/** The bus, for the theme screen once Bus is chosen. Code point 1F68C, BUS. */
internal const val INTRO_BUS_EMOJI = "🚌"

/** The ferry, for the theme screen once Ferry is chosen. Code point 26F4, FERRY, with the variation selector FE0F. */
internal const val INTRO_FERRY_EMOJI = "⛴️"

/** The car, for the Park & Ride screen. Code point 1F697, AUTOMOBILE. */
internal const val INTRO_CAR_EMOJI = "🚗"

/** Every emoji declared here, in one list, for the guard and the table tests. */
internal val IntroDeclaredEmoji: List<String> = listOf(
    INTRO_PIN_EMOJI,
    INTRO_STAR_EMOJI,
    INTRO_TRAIN_EMOJI,
    INTRO_METRO_EMOJI,
    INTRO_BUS_EMOJI,
    INTRO_FERRY_EMOJI,
    INTRO_CAR_EMOJI,
)
