package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/** What a stop screen's headline says: its lines, top to bottom, and the one with the key word. */
internal data class IntroStopHeadline(val lines: List<String>, val emphasisIndex: Int)

/** The destination question, in two lines, a complete plain question. */
internal val DESTINATION_HEADLINE = listOf("Where are", "you going?")

/** The destination headline as one phrase, which a screen reader reads as the heading. */
internal val DESTINATION_HEADING = introHeadlinePhrase(DESTINATION_HEADLINE)

/** The origin question, in two lines, a complete plain question. */
internal val ORIGIN_HEADLINE = listOf("Where are you", "leaving from?")

/** The origin headline as one phrase, which a screen reader reads as the heading. */
internal val ORIGIN_HEADING = introHeadlinePhrase(ORIGIN_HEADLINE)

/** The headline of the destination or origin screen, or null for a screen that is neither. */
internal fun introStopHeadline(step: IntroStep): IntroStopHeadline? = when (step) {
    IntroStep.DESTINATION -> IntroStopHeadline(DESTINATION_HEADLINE, emphasisIndex = DESTINATION_HEADLINE.lastIndex)
    IntroStep.ORIGIN -> IntroStopHeadline(ORIGIN_HEADLINE, emphasisIndex = ORIGIN_HEADLINE.lastIndex)
    IntroStep.THEME, IntroStep.PARK_RIDE, IntroStep.CONFIRM -> null
}
