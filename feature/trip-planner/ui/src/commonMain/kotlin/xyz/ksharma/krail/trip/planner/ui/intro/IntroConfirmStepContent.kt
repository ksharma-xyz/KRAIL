package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay

/** The confirm screen's question, in two lines, a complete plain question. The hero is `save` ([CONFIRM_HERO_INDEX]). */
internal val CONFIRM_HEADLINE = listOf("Save", "this trip?")

/** The line of [CONFIRM_HEADLINE] that is the hero. */
internal const val CONFIRM_HERO_INDEX = 0

/** The confirm question as one phrase, which a screen reader reads as the heading. */
internal const val CONFIRM_HEADING = "Save this trip?"

/** What a screen reader says for the trip tile, which shows the two names stacked. */
internal fun confirmTileDescription(startName: String, endName: String): String = "From $startName to $endName"

/**
 * The confirm screen: the headline with the star, which celebrates once, and the trip the rider chose as the real
 * saved trip tile, `SavedTripCard`, with its two stop names stacked and no badges. The tile is shown as the card
 * on Saved Trips is and is not a button, because the bar below holds the two answers. It sits in the room the
 * middle zone leaves around it, so the screen is mostly empty space. A state without both stops shows no tile,
 * which no flow reaches: any two chosen stops reach this screen.
 */
@Composable
internal fun IntroConfirmStepContent(state: IntroState, modifier: Modifier = Modifier) {
    IntroStepFrame(
        headline = {
            IntroHeadline(
                lines = CONFIRM_HEADLINE,
                emphasisIndex = CONFIRM_HERO_INDEX,
                emoji = introEmojiFor(IntroStep.CONFIRM),
                spoken = CONFIRM_HEADING,
                celebrate = true,
                heroLayout = true,
                accent = introAccentColor(IntroStep.CONFIRM),
            )
        },
        modifier = modifier,
    ) {
        val start = state.startStop()
        val end = state.endStop()
        if (start != null && end != null) {
            SavedTripCard(
                fromDisplay = StopDisplay(stopId = start.stopId, name = start.name),
                toDisplay = StopDisplay(stopId = end.stopId, name = end.name),
                onCardClick = {},
                editing = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KrailTheme.dimensions.pageHorizontalPadding)
                    .testTag(TripPlannerTestTags.INTRO_CONFIRM_TILE)
                    .clearAndSetSemantics { contentDescription = confirmTileDescription(start.name, end.name) },
            )
        }
    }
}
