package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.DeparturesViewModel
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.DeparturesRelativeTimeTicker
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIcon
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIconSize
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature

/**
 * The stop card for a tapped stop pin, with the departures it needs from the departures view model
 * and the clock that keeps its "in x mins" counting down. A view model is obtained here, as the sheet
 * it replaces did, and handed down as state and events, so [StopCard] itself is plain.
 *
 * The departures state is collected with the lifecycle, so the board's poll runs only while the card
 * is open and the screen is in front (`docs/POLLING_LIFECYCLE.md`).
 */
@Composable
internal fun StopCardWithDepartures(
    stop: NearbyStopFeature,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val departuresViewModel = koinViewModel<DeparturesViewModel>()
    val departures by departuresViewModel.uiState.collectAsStateWithLifecycle()

    // Without this the "in x mins" on every row freezes at whatever the last fetch computed.
    DeparturesRelativeTimeTicker(departuresViewModel)

    StopCard(
        stop = stop,
        departures = departures,
        onEvent = departuresViewModel::onEvent,
        onClose = onClose,
        modifier = modifier,
        actions = actions,
    )
}

/**
 * The floating card a stop pin opens, `docs/MAP_HOME_UX.md` (Stop card): the stop's name with the
 * close button, its mode badges, the Departure Board in its own light card, then the actions. Padding
 * 16 all round and 8 between the blocks.
 *
 * [actions] is the slot at the bottom, for the stop's one pick action today and for Save as, Go here
 * and Start here later, so filling it does not reshape the card. The card is keyed on its stop, so
 * the board's state belongs to the stop and a rotation keeps it.
 */
@Composable
internal fun StopCard(
    stop: NearbyStopFeature,
    departures: DeparturesState,
    onEvent: (DeparturesUiEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    boardOpenAtStart: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    val layout = stopCardLayout(StopCardKind.Stop, modeCount = stop.transportModes.size, carParkCount = 0)
    key(stopCardStateKey(StopCardKind.Stop, stop.stopId)) {
        StopCardFrame(
            announcement = stop.stopName,
            closeLabel = "Close ${stop.stopName}",
            onClose = onClose,
            modifier = modifier,
            heading = {
                Text(
                    text = stop.stopName,
                    style = KrailTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            },
        ) {
            if (layout.showsModes) ModeBadges(modes = stop.transportModes)
            if (layout.showsBoard) {
                StopCardBoardSection(
                    stopId = stop.stopId,
                    stopName = stop.stopName,
                    state = departures,
                    onEvent = onEvent,
                    boardOpenAtStart = boardOpenAtStart,
                )
            }
            if (layout.showsActionSlot) ActionSlot(actions)
        }
    }
}

/**
 * The stop's modes as the app's own mode icons, whole items that wrap. A screen reader hears them as
 * one line, the modes by name, and not as a run of unlabelled circles.
 */
@Composable
private fun ModeBadges(modes: ImmutableList<TransportMode>) {
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = Modifier
            .testTag(TripPlannerTestTags.StopCard.MODES)
            .clearAndSetSemantics { contentDescription = modes.joinToString(separator = ", ") { it.name } },
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        modes.forEach { mode ->
            TransportModeIcon(transportMode = mode, size = TransportModeIconSize.Small)
        }
    }
}

/** The room at the bottom of every card for the actions it hosts. */
@Composable
internal fun ActionSlot(actions: @Composable () -> Unit) {
    Box(modifier = Modifier.testTag(TripPlannerTestTags.StopCard.ACTIONS).fillMaxWidth()) { actions() }
}

/**
 * One action of the slot: taj's [Button] at its large size, a verb or a place and nothing else, at least as
 * tall as the search bar, 48 dp, which is the touch target (rule 21). The words are the caller's, so the
 * slot reads Go here on a stop and Add to home on a station.
 */
@Composable
internal fun StopCardAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = KrailTheme.dimensions.textFieldHeight),
        dimensions = ButtonDefaults.largeButtonSize(),
    ) {
        Text(text = text)
    }
}
