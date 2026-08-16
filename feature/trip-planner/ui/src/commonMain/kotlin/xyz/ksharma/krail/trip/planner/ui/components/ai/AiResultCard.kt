package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.components.ThemeTextFieldPlaceholderText
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.WhenChip
import xyz.ksharma.krail.trip.planner.ui.search.ai.ResolvedTripIntent

internal const val SEE_TIMETABLE_LABEL = "See timetable"

// The words the home row uses for an empty field, so the pill a rider taps to fix a missed stop
// reads the same here as it does there.
private const val STARTING_FROM_PLACEHOLDER = "Starting from"
private const val DESTINATION_PLACEHOLDER = "Destination"

/**
 * What a resolved sentence looks like, sitting above the bar that produced it.
 *
 * **It has no container of its own.** The pills and the button sit straight on the moving cloud
 * field, exactly as the input bar does. A card behind them was a grey panel laid over the one
 * thing on this screen worth looking at; the pills already carry their own surface colour, so
 * the grouping was doing nothing that the spacing between them does not.
 *
 * **A half-read sentence shows the same way**, with the field it missed left empty. That is the
 * case where seeing the result matters most: closing on a partial dropped the rider back on the
 * home row to work out for themselves which of the two fields had been filled. See times is
 * disabled until both ends are known, because there is no timetable to load without them, and
 * the way forward is the bar directly below, still holding their sentence.
 *
 * **Nothing here comes from the model.** The two names are stops from our own database and the
 * time is the one the resolver produced, which is the same rule the rest of the pipeline runs
 * under — see `AI_SEARCH_UX.md`.
 *
 * **The stops are read-only, and that is the point.** They were tappable for one build, opening
 * the ordinary stop search for that field. Doing so closed this surface and navigated away, so
 * the card was destroyed and the rider never came back to it: a one-way door dressed up as
 * editing in place. Correcting a stop belongs to the input bar below, which is still there, or
 * to the home row behind.
 *
 * Seeing times is the existing Search button, not a second route to the same place. The two
 * stops and the time are already written into the home screen's state by the time this card is
 * on screen, so pressing it does exactly what pressing Search would have done, minus the trip
 * back to find it.
 */
@Composable
internal fun AiResultCard(
    resolved: ResolvedTripIntent,
    onSeeTimes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val dateTimeText = resolved.dateTimeSelectionItem?.toDateTimeText()

    // Three gaps, three sizes, because one uniform gap groups nothing: the two stops read as a
    // pair, the button is a different job, and the bar below is a different object again. Even
    // spacing had See times touching the destination as though it were a third stop.
    Column(modifier = modifier.fillMaxWidth()) {
        // The home row's own field component, at full strength but with no click. Same pills
        // the rider knows, reporting rather than offering.
        TextFieldButton(onClick = null) {
            ThemeTextFieldPlaceholderText(
                text = resolved.fromStopItem?.stopName ?: STARTING_FROM_PLACEHOLDER,
                isActive = resolved.fromStopItem != null,
            )
        }
        Spacer(modifier = Modifier.height(dim.spacingS))

        TextFieldButton(onClick = null) {
            ThemeTextFieldPlaceholderText(
                text = resolved.toStopItem?.stopName ?: DESTINATION_PLACEHOLDER,
                isActive = resolved.toStopItem != null,
            )
        }

        // Only when a time was actually understood. A chip that always appeared would have had
        // to invent "now", and a rider seeing a departure time they never asked for cannot tell
        // an invented one from one they were misheard saying.
        if (dateTimeText != null) {
            Spacer(modifier = Modifier.height(dim.spacingM))
            Row(modifier = Modifier.padding(horizontal = dim.spacingXS)) {
                WhenChip(text = dateTimeText)
            }
        }

        Spacer(modifier = Modifier.height(dim.spacingXL))

        // Names the screen it opens. "Continue" was the first wording and promised less than
        // it delivered, which sent riders back to the row to look for the real button; "See
        // times" then said what happens without naming where they land. The timetable is a
        // place in this app with a name riders already know, so the button uses it.
        //
        // Disabled rather than hidden when a stop is missing: a button that vanishes leaves the
        // rider guessing what the card is for, where a dimmed one paired with an empty pill
        // says which of the two has to be dealt with first.
        Button(onClick = onSeeTimes, enabled = resolved.hasWholeTrip) {
            Text(text = SEE_TIMETABLE_LABEL)
        }
    }
}
