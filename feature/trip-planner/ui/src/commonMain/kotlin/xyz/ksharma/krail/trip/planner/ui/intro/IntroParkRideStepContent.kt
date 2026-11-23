package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/**
 * The question, in four short lines, a complete plain question. It offers what a Yes does, which is to show the parking spots at
 * the nearest station, and the rider answers it on the bar below. The hero is `parking spots` ([PARK_RIDE_HERO_INDEX]) and the
 * other three lines are whispers. It is drawn in capitals by the Intro's own display code.
 */
internal val PARK_RIDE_HEADLINE = listOf("Show", "parking spots", "at your", "nearest station?")

/** The line of [PARK_RIDE_HEADLINE] that is the hero. */
internal const val PARK_RIDE_HERO_INDEX = 1

/** The whole question as one phrase, which a screen reader reads as the heading. It is its own text, not the drawn lines joined. */
internal const val PARK_RIDE_HEADING = "Show parking spots at your nearest station?"

/**
 * The Park & Ride screen: the headline at the top and, in the middle, the card that the answer found.
 * The answers and the card's own action are the decision bar at the bottom of the screen, so nothing here
 * is a button, and the card keeps one height from the moment it is asked for.
 */
@Composable
internal fun IntroParkRideStepContent(state: IntroState, modifier: Modifier = Modifier) {
    IntroStepFrame(
        headline = {
            IntroHeadline(
                lines = PARK_RIDE_HEADLINE,
                emphasisIndex = PARK_RIDE_HERO_INDEX,
                emoji = introEmojiFor(IntroStep.PARK_RIDE),
                spoken = PARK_RIDE_HEADING,
                heroLayout = true,
                accent = introAccentColor(IntroStep.PARK_RIDE),
            )
        },
        modifier = modifier,
    ) {
        IntroParkRideCardSlot(slot = introCardSlot(state))
    }
}
