package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.taj.components.AnimatedDots
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.DepartureBoardBody

private const val BOARD_TITLE = "Departure Board"
private const val CHEVRON_OPEN_DEGREES = 180f

/**
 * The Departure Board on a stop card, in its own light card: a heading that holds the title and the
 * chevron, then, once the chevron is open, the board's own rows for every line the stop serves.
 *
 * The board is closed until the chevron is tapped, and only then is it fetched, so a pin tap costs
 * no call and an open board costs the one the sheet's board cost, through the same
 * [DeparturesUiEvent.LoadDepartures] and cooldown. The fetch and its 30 second poll run while this
 * is on screen and the screen is in front, because the departures state is collected with the
 * lifecycle by the caller, and they stop the moment the card closes. The open state is saved with
 * the card, so a rotation keeps it. [boardOpenAtStart] only seeds that state, for a preview or a test
 * that needs the board open.
 *
 * The rider narrows the list with the board's own filter, the pill and the per mode line groups
 * that sit above the rows, exactly as the board has always had them. What the board does in each
 * phase is [stopCardBoard]. The body keeps a floor of height from the moment the board opens, so
 * loading, failed, empty and a short list all stand in the same height (rule 22).
 */
@Composable
internal fun StopCardBoardSection(
    stopId: String,
    stopName: String,
    state: DeparturesState,
    onEvent: (DeparturesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    boardOpenAtStart: Boolean = false,
) {
    var boardOpen by rememberSaveable { mutableStateOf(boardOpenAtStart) }
    val board = stopCardBoard(state, boardOpen)

    BoardPollingEffect(stopId = stopId, stopName = stopName, boardOpen = boardOpen, onEvent = onEvent)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(KrailTheme.colors.surface, CardShape),
    ) {
        BoardHeading(
            board = board,
            boardOpen = boardOpen,
            onChevronClick = {
                boardOpen = !boardOpen
                onEvent(
                    DeparturesUiEvent.DepartureBoardToggle(stopId, stopName, boardOpen, DepartureBoardSource.MAP_SHEET),
                )
            },
        )
        if (boardOpen) {
            BoardBody(stopId = stopId, state = state, board = board, onEvent = onEvent)
        }
    }
}

/**
 * Starts the board's fetch and poll while it is open and stops them when it is closed or the card
 * leaves the screen. Closing the card stops the poll at once and does not wait out the view model's
 * grace, so a card that is gone never keeps asking.
 */
@Composable
private fun BoardPollingEffect(
    stopId: String,
    stopName: String,
    boardOpen: Boolean,
    onEvent: (DeparturesUiEvent) -> Unit,
) {
    LaunchedEffect(boardOpen, stopId) {
        if (boardOpen) {
            onEvent(DeparturesUiEvent.LoadDepartures(stopId, stopName, DepartureBoardSource.MAP_SHEET))
        } else {
            onEvent(DeparturesUiEvent.StopPolling)
        }
    }
    DisposableEffect(stopId) {
        onDispose { onEvent(DeparturesUiEvent.StopPolling) }
    }
}

/**
 * The heading: the title at the start, then the refresh dots and the chevron as one end cluster.
 * They are whole items in a wrapping row, so at large text the cluster goes under the title and
 * nothing is squeezed or cut.
 */
@Composable
private fun BoardHeading(
    board: StopCardBoard,
    boardOpen: Boolean,
    onChevronClick: () -> Unit,
) {
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(start = dim.pageHorizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = BOARD_TITLE, style = KrailTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (board.refreshing) {
                AnimatedDots(
                    modifier = Modifier.size(width = dim.spacingXXXXL, height = dim.spacingXL),
                    color = KrailTheme.colors.softLabel,
                )
            }
            Chevron(open = boardOpen, onClick = onChevronClick)
        }
    }
}

/** The chevron, taj's round icon button with no fill, 48 dp, turned over while the board is open. */
@Composable
private fun Chevron(open: Boolean, onClick: () -> Unit) {
    val label = if (open) "Collapse the departure board" else "Expand the departure board"
    RoundIconButton(
        onClick = onClick,
        color = Color.Transparent,
        onClickLabel = label,
        modifier = Modifier.testTag(TripPlannerTestTags.StopCard.BOARD_CHEVRON),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_arrow_down),
            contentDescription = label,
            colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
            modifier = Modifier
                .size(KrailTheme.dimensions.spacingXL)
                .rotate(if (open) CHEVRON_OPEN_DEGREES else 0f),
        )
    }
}

/**
 * The rows: the board's own body, with its own line filter and its Show previous departures, as the
 * departure board has everywhere else. The rider's filter changes are reported through the same
 * [DeparturesUiEvent.LineFilterChanged] the sheet's board sent.
 */
@Composable
private fun BoardBody(
    stopId: String,
    state: DeparturesState,
    board: StopCardBoard,
    onEvent: (DeparturesUiEvent) -> Unit,
) {
    val floor = if (board.holdsBodyFloor) boardFloorHeight(LocalDensity.current.fontScale) else 0.dp
    Box(modifier = Modifier.testTag(TripPlannerTestTags.StopCard.BOARD_BODY).heightIn(min = floor)) {
        DepartureBoardBody(
            state = state,
            onRetry = { onEvent(DeparturesUiEvent.Refresh) },
            onLoadPreviousDepartures = { onEvent(DeparturesUiEvent.LoadPreviousDepartures(stopId)) },
            onLineFilterChange = { lineNumber, transportMode, selected ->
                onEvent(
                    DeparturesUiEvent.LineFilterChanged(
                        stopId = stopId,
                        selected = selected,
                        lineNumber = lineNumber,
                        transportMode = transportMode,
                    ),
                )
            },
            onShowPreviousToggle = { show -> onEvent(DeparturesUiEvent.TogglePreviousDepartures(stopId, show)) },
        )
    }
}
