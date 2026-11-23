package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingContent
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ClosedTileFigures
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.savedtrips.tileShell

/**
 * The one-time Park & Ride suggestion card on the map home, `docs/MAP_HOME_UX.md` rule 47. The card shell is
 * the Park & Ride row's own and its three lines are the closed tile's own ([ClosedTileFigures]), so a
 * station reads the same here as the tile it becomes. Under them are exactly two buttons, both taj's: the
 * outlined Not now at the left and the primary Add to home at the right, which stack with the primary on top
 * when two whole buttons do not fit ([SuggestionActions]).
 *
 * The card is the same height whether its numbers are loading, loaded or failed, because the number and the
 * time keep their line boxes (rule 22). It is one region for a screen reader: the figures are read as one
 * line and the two buttons are the only other stops. There is no heading, tip or caption on it.
 */
@Composable
internal fun ParkRideSuggestionCard(
    tile: ParkRideTile,
    onAddToHome: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier
            .testTag(TripPlannerTestTags.ParkRideSuggestion.CARD)
            .fillMaxWidth()
            .tileShell()
            .semantics { isTraversalGroup = true }
            .padding(dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
    ) {
        FloatingContent {
            Column(
                modifier = Modifier
                    .testTag(TripPlannerTestTags.ParkRideSuggestion.FIGURES)
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = suggestionSpokenLabel(tile) },
                verticalArrangement = Arrangement.spacedBy(dim.spacingM),
            ) {
                ClosedTileFigures(tile)
            }
            SuggestionActions(
                notNow = {
                    OutlinedButton(
                        onClick = onNotNow,
                        colors = FloatingDark.outlinedButtonColors(),
                        modifier = Modifier
                            .testTag(TripPlannerTestTags.ParkRideSuggestion.NOT_NOW)
                            .heightIn(min = dim.textFieldHeight),
                        dimensions = ButtonDefaults.mediumButtonSize(),
                    ) { Text(text = NOT_NOW_TEXT) }
                },
                addToHome = {
                    Button(
                        onClick = onAddToHome,
                        modifier = Modifier
                            .testTag(TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME)
                            .heightIn(min = dim.textFieldHeight)
                            .semantics { onClick(label = addToHomeSpokenLabel(tile), action = null) },
                        dimensions = ButtonDefaults.mediumButtonSize(),
                    ) { Text(text = ADD_TO_HOME_TEXT) }
                },
            )
        }
    }
}

/**
 * What the home stack needs to draw the card and to tell the ViewModel what happened to it, so the stack takes
 * one thing and not four. [onShown] is raised each time the card is drawn and the ViewModel counts only the first.
 */
@Immutable
internal class ParkRideSuggestionSlot(
    val tile: ParkRideTile,
    val onShown: () -> Unit,
    val onAddToHome: () -> Unit,
    val onNotNow: () -> Unit,
)

/**
 * The card as an item of the home stack, in the row's place and on the cards' edges, with the same gap under
 * it the trip cards have. Drawing it is what marks the suggestion as shown, so a card scrolled out of reach
 * and never composed is not counted.
 */
@Composable
internal fun ParkRideSuggestionItem(slot: ParkRideSuggestionSlot) {
    val dim = KrailTheme.dimensions
    val onShown by rememberUpdatedState(slot.onShown)
    LaunchedEffect(slot.tile.stationId) { onShown() }
    Column {
        Box(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding)) {
            ParkRideSuggestionCard(
                tile = slot.tile,
                onAddToHome = slot.onAddToHome,
                onNotNow = slot.onNotNow,
            )
        }
        Spacer(modifier = Modifier.height(dim.spacingXL))
    }
}

/**
 * The slot for the state the ViewModel holds, or null while there is no card. Remembered on the tile, so a state that
 * repeats builds no new lambdas. Both the home entry and the tests that stand in for it build the slot here, so the
 * events a tap sends cannot differ between them.
 */
@Composable
internal fun rememberParkRideSuggestionSlot(
    viewModel: ParkRideSuggestionViewModel,
    state: ParkRideSuggestionState,
): ParkRideSuggestionSlot? = remember(viewModel, state.tile) {
    state.tile?.let { tile ->
        ParkRideSuggestionSlot(
            tile = tile,
            onShown = { viewModel.onEvent(ParkRideSuggestionEvent.Shown(tile.stationId)) },
            onAddToHome = { viewModel.onEvent(ParkRideSuggestionEvent.AddToHome) },
            onNotNow = { viewModel.onEvent(ParkRideSuggestionEvent.NotNow) },
        )
    }
}
