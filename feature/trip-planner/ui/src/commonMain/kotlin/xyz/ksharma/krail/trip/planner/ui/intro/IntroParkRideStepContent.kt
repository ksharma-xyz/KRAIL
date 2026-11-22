package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

internal const val PARK_RIDE_HEADING = "Driving to the station"

/**
 * The Park & Ride screen: the headline, and under it the card that the answer found. The answers
 * and the card's own action are the decision bar at the bottom of the screen, so nothing here is a
 * button, and the card keeps one height from the moment it is asked for.
 */
@Composable
internal fun IntroParkRideStepContent(state: IntroState, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
    ) {
        IntroHeading(
            emoji = introEmojiFor(IntroStep.PARK_RIDE),
            text = PARK_RIDE_HEADING,
            modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
        )
        IntroParkRideCardSlot(slot = introCardSlot(state))
    }
}

/**
 * A suggestion is a neutral button, not one in the theme colour, which belongs to the bottom bar
 * alone. The chosen one is filled, and every taj button colours its own label. Step 2 uses the same
 * button for its suggestions, with [description] for a screen reader when the words on screen are
 * only a stop name.
 */
@Composable
internal fun IntroAnswerButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = null,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = KrailTheme.dimensions.iconXXXL)
        .semantics {
            this.selected = selected
            description?.let { contentDescription = it }
        }
    if (selected) {
        Button(onClick = onClick, colors = ButtonDefaults.monochromeButtonColors(), modifier = modifier) {
            Text(text = text, textAlign = TextAlign.Center)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            dimensions = ButtonDefaults.largeButtonSize(),
            modifier = modifier,
        ) {
            Text(text = text, textAlign = TextAlign.Center)
        }
    }
}
