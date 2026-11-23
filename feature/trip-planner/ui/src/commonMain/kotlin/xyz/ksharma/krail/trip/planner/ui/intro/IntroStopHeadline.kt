package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/** What a stop screen's headline says: its lines, top to bottom, the hero line, and the phrase a screen reader reads. */
internal data class IntroStopHeadline(val lines: List<String>, val emphasisIndex: Int, val spoken: String)

/** The destination question, in two lines, a complete plain question. The hero is `going?`, which owns its question mark. */
internal val DESTINATION_HEADLINE = listOf("Where are you", "going?")

/** The destination question as one phrase, which a screen reader reads as the heading. */
internal const val DESTINATION_HEADING = "Where are you going?"

/** The origin question, in two lines, a complete plain question. The hero is `leaving from?`, which owns its question mark. */
internal val ORIGIN_HEADLINE = listOf("Where are you", "leaving from?")

/** The origin question as one phrase, which a screen reader reads as the heading. */
internal const val ORIGIN_HEADING = "Where are you leaving from?"

/** The line of a stop screen's headline that is the hero: the second of the two, for both. */
internal const val STOP_HERO_INDEX = 1

/** The headline of the destination or origin screen, or null for a screen that is neither. */
internal fun introStopHeadline(step: IntroStep): IntroStopHeadline? = when (step) {
    IntroStep.DESTINATION -> IntroStopHeadline(DESTINATION_HEADLINE, STOP_HERO_INDEX, DESTINATION_HEADING)
    IntroStep.ORIGIN -> IntroStopHeadline(ORIGIN_HEADLINE, STOP_HERO_INDEX, ORIGIN_HEADING)
    IntroStep.THEME, IntroStep.PARK_RIDE, IntroStep.CONFIRM -> null
}
