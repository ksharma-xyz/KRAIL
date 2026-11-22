package xyz.ksharma.krail.trip.planner.ui.intro

/**
 * The pin, for the destination screen. An operator approved exception: the Intro draws emoji from the
 * app's own non festival list ([xyz.ksharma.krail.core.festival.FestivalManager.commonEmojiList]) and
 * from nowhere else, except for this and [INTRO_STAR_EMOJI].
 *
 * This file is the only place in the Intro where either may be written out. Everything else asks
 * [introEmojiFor], and `IntroEmojiGuardTest` fails if any other emoji, or either of these anywhere but
 * here, appears in an Intro source. Written as the escape of its two UTF-16 units, code point 1F4CD.
 */
internal const val INTRO_PIN_EMOJI = "📍"

/**
 * The star, for the confirm screen. An operator approved exception, with [INTRO_PIN_EMOJI]; see there
 * for the rule. Code point 2B50.
 */
internal const val INTRO_STAR_EMOJI = "⭐"
