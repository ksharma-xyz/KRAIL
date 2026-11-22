package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.taj.components.AnimatedDots
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.DepartureBoardBody
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDarkTheme

private const val BOARD_TITLE = "Departure Board"
private const val CHEVRON_OPEN_DEGREES = 180f

/**
 * The Departure Board on a stop card: a heading that holds the title and the chevron, then, once the
 * heading is tapped open, the board's own rows for every line the stop serves. It draws no card, no
 * background and no inset of its own, so the heading and the rows sit on the stop card's one surface
 * with their dividers, and the rows' own 16 dp is the page edge inset.
 *
 * The board is closed until the heading is tapped, and only then is it fetched, so a pin tap costs
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

    Column(modifier = modifier.fillMaxWidth()) {
        BoardHeading(
            board = board,
            boardOpen = boardOpen,
            onClick = {
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
 * The heading, one button: the title at the start, then the refresh dots and the chevron as one end
 * cluster. The whole row opens and closes the board, not only the arrow, and is at least 48 dp tall.
 * The chevron is the visual cue only. A screen reader hears one expandable button, the title, and
 * whether it is expanded or collapsed. The title and the cluster are whole items in a wrapping row,
 * so at large text the cluster goes under the title and nothing is squeezed or cut.
 */
@Composable
private fun BoardHeading(
    board: StopCardBoard,
    boardOpen: Boolean,
    onClick: () -> Unit,
) {
    val dim = KrailTheme.dimensions
    val label = if (boardOpen) "Collapse the departure board" else "Expand the departure board"
    FlowRow(
        modifier = Modifier
            .testTag(TripPlannerTestTags.StopCard.BOARD_HEADING)
            .fillMaxWidth()
            .heightIn(min = dim.textFieldHeight)
            .clip(RoundedCornerShape(dim.radiusM))
            .klickable(role = Role.Button, onClick = onClick)
            .semantics {
                stateDescription = if (boardOpen) "Expanded" else "Collapsed"
                onClick(label = label, action = null)
            }
            .padding(horizontal = dim.pageHorizontalPadding),
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
                    color = FloatingDark.softContent,
                )
            }
            Chevron(open = boardOpen)
        }
    }
}

/** The chevron, the existing arrow icon in a 48 dp box, turned over while the board is open. A cue, not a control. */
@Composable
private fun Chevron(open: Boolean) {
    val dim = KrailTheme.dimensions
    Box(modifier = Modifier.size(dim.textFieldHeight), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(Res.drawable.ic_arrow_down),
            contentDescription = null,
            colorFilter = ColorFilter.tint(FloatingDark.content),
            modifier = Modifier.size(dim.spacingXL).rotate(if (open) CHEVRON_OPEN_DEGREES else 0f),
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
        // The board's rows are the app's own and read mode adaptive tokens inside, with no colours to pass, so
        // they are drawn in the dark scheme the card is.
        FloatingDarkTheme {
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
}
