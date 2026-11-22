package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/** The headline, a line to a thought, with the key word last and heaviest. */
internal val PARK_RIDE_HEADLINE = listOf("Driving", "to the", "station")

/** The whole headline as one phrase, which a screen reader reads as the heading. */
internal val PARK_RIDE_HEADING = introHeadlinePhrase(PARK_RIDE_HEADLINE)

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
                emphasisIndex = PARK_RIDE_HEADLINE.lastIndex,
                emoji = introEmojiFor(IntroStep.PARK_RIDE),
            )
        },
        modifier = modifier,
    ) {
        IntroParkRideCardSlot(slot = introCardSlot(state))
    }
}
