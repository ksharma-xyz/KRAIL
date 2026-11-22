package xyz.ksharma.krail.trip.planner.ui.intro.v2

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
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer

internal const val PARK_RIDE_QUESTION = "Driving to the station?"
internal const val PARK_RIDE_YES = "Yes, show me free spots"
internal const val PARK_RIDE_NO = "No thanks"
private const val PARK_RIDE_EMOJI = "\uD83D\uDE97"

/**
 * Step 1: the question, the two answers one under the other, and what the answer found. The
 * answers are whole buttons that stack, so nothing is squeezed at a large font or a narrow window,
 * and the card below keeps one height from the moment it is asked for.
 */
@Composable
internal fun IntroParkRideStepContent(
    state: IntroState,
    onEvent: (IntroUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
    ) {
        IntroHeading(
            emoji = PARK_RIDE_EMOJI,
            text = PARK_RIDE_QUESTION,
            modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
        )
        Column(
            modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(dim.spacingL),
        ) {
            IntroAnswerButton(
                text = PARK_RIDE_YES,
                selected = state.parkRideAnswer == ParkRideAnswer.YES,
                onClick = { onEvent(IntroUiEvent.AnswerParkRide(yes = true)) },
            )
            IntroAnswerButton(
                text = PARK_RIDE_NO,
                selected = state.parkRideAnswer == ParkRideAnswer.NO,
                onClick = { onEvent(IntroUiEvent.AnswerParkRide(yes = false)) },
            )
        }
        IntroParkRideCardSlot(
            slot = introCardSlot(state),
            onToggle = { onEvent(IntroUiEvent.ToggleParkRide) },
        )
    }
}

/**
 * An answer is a neutral button, not one in the theme colour, which belongs to the bottom button
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
