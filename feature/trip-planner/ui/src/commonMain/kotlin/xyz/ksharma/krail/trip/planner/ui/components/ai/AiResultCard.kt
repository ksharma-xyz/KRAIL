package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.components.ThemeTextFieldPlaceholderText
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.trip.planner.ui.components.WhenChip
import xyz.ksharma.krail.trip.planner.ui.search.ai.ResolvedTripIntent

internal const val SEE_TIMES_LABEL = "See times"

// The words the home row uses for an empty field, so the pill a rider taps to fix a missed stop
// reads the same here as it does there.
private const val STARTING_FROM_PLACEHOLDER = "Starting from"
private const val DESTINATION_PLACEHOLDER = "Destination"

// Matches AiInputBar's own radius, because this is the same object in a later state.
private val ResultCardCornerRadius = 28.dp

// One step below `surface` in both themes, because the two fields inside the card ARE
// `surface` — that is the whole point of reusing the home row's own field component rather
// than restyling it here. A card painted `surface` in light mode would have swallowed them.
private const val CARD_DARKEN_LIGHT = 0.05f
private const val CARD_DARKEN_DARK = 0.45f

/**
 * What the input bar becomes once a whole trip resolved.
 *
 * It occupies the bar's slot, at the bar's width and corner radius, so the change reads as the
 * thing the rider typed into settling into the answer rather than as one view being replaced by
 * another.
 *
 * **A half-read sentence gets the same card**, with the field it missed left empty and waiting.
 * That is the case where seeing the answer matters most: closing on a partial dropped the rider
 * back on the home row to work out for themselves which of the two fields had been filled. See
 * times is disabled until both ends are known, because there is no timetable to load without
 * them, and the empty pill is then the only thing on the card left to press.
 *
 * **Nothing here comes from the model.** The two names are stops from our own database and the
 * time is the one the resolver produced, which is the same rule the rest of the pipeline runs
 * under — see `AI_SEARCH_UX.md`.
 *
 * Editing a stop hands the rider to the ordinary stop search for that field, and the surface
 * closes on the way: the field they are correcting lives on the screen behind it, and a
 * correction they cannot see land is not a correction.
 *
 * Seeing times is the existing Search button, not a second route to the same place. The two
 * stops and the time are already written into the home screen's state by the time this card is
 * on screen, so pressing it does exactly what pressing Search would have done, minus the trip
 * back to find it.
 */
@Composable
internal fun AiResultCard(
    resolved: ResolvedTripIntent,
    actions: AiResultActions,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val dateTimeText = resolved.dateTimeSelectionItem?.toDateTimeText()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ResultCardCornerRadius))
            .background(cardColor())
            .padding(horizontal = dim.spacingM, vertical = dim.spacingL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        // The home row's own field, unmodified. Tapping opens the stop search already aimed at
        // that field, which is the flow riders already know from the row itself.
        TextFieldButton(onClick = actions.onEditFrom) {
            ThemeTextFieldPlaceholderText(
                text = resolved.fromStopItem?.stopName ?: STARTING_FROM_PLACEHOLDER,
                isActive = resolved.fromStopItem != null,
            )
        }
        TextFieldButton(onClick = actions.onEditTo) {
            ThemeTextFieldPlaceholderText(
                text = resolved.toStopItem?.stopName ?: DESTINATION_PLACEHOLDER,
                isActive = resolved.toStopItem != null,
            )
        }

        // Only when a time was actually understood. A chip that always appeared would have had
        // to invent "now", and a rider seeing a departure time they never asked for cannot tell
        // an invented one from one they were misheard saying.
        if (dateTimeText != null) {
            Row(modifier = Modifier.padding(horizontal = dim.spacingXS)) {
                WhenChip(text = dateTimeText)
            }
        }

        // Says what happens next. "Continue" was the old wording and promised less than it
        // delivered, which is what sent riders back to the row to look for the real button.
        //
        // Disabled rather than hidden when a stop is missing: a button that vanishes leaves the
        // rider guessing what the card is for, where a dimmed one paired with an empty pill
        // says which of the two has to be dealt with first.
        Button(onClick = actions.onSeeTimes, enabled = resolved.hasWholeTrip) {
            Text(text = SEE_TIMES_LABEL)
        }
    }
}

@Composable
private fun cardColor(): Color = Color.Black
    .copy(alpha = if (isAppInDarkMode()) CARD_DARKEN_DARK else CARD_DARKEN_LIGHT)
    .compositeOver(KrailTheme.colors.surface)
