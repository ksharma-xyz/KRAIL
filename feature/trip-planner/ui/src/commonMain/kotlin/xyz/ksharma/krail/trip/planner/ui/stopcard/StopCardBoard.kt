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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_arrow_down
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.taj.components.AnimatedDots
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.BadgeSize
import xyz.ksharma.krail.trip.planner.ui.components.DepartureBoardBody
import xyz.ksharma.krail.trip.planner.ui.components.HostedLineFilter
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeBadge
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ReservedLine

private const val BOARD_TITLE = "Departure Board"
private const val LINES_CHIP_TEXT = "Lines"
private const val CHEVRON_OPEN_DEGREES = 180f

/**
 * The Departure Board on a stop card, in its own light card: a heading that holds the title, the one
 * Lines chip and the chevron (decision G2), then, once the chevron is open, the line badges the chip
 * shows and the existing board rows.
 *
 * The board is closed until the chevron is tapped, and only then is it fetched, so a pin tap costs
 * no call and an open board costs the one the sheet's board cost, through the same
 * [DeparturesUiEvent.LoadDepartures] and cooldown. The fetch and its 30 second poll run while this
 * is on screen and the screen is in front, because the departures state is collected with the
 * lifecycle by the caller, and they stop the moment the card closes. The open state, the chip and
 * the filter are saved with the card, so a rotation keeps them. [boardOpenAtStart] and
 * [linesOpenAtStart] only seed that state, for a preview or a test that needs the board open.
 *
 * What the board does in each phase is [stopCardBoard]. The body keeps a floor of height from the
 * moment the board opens, so loading, failed, empty and a short list all stand in the same height,
 * and the chip keeps its room while the board loads (rule 22).
 */
@Composable
internal fun StopCardBoardSection(
    stopId: String,
    stopName: String,
    state: DeparturesState,
    onEvent: (DeparturesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    boardOpenAtStart: Boolean = false,
    linesOpenAtStart: Boolean = false,
) {
    var boardOpen by rememberSaveable { mutableStateOf(boardOpenAtStart) }
    var linesOpen by rememberSaveable { mutableStateOf(linesOpenAtStart) }
    // Empty string is no filter, so it round-trips through saved state as a plain String.
    var selectedLineKey by rememberSaveable { mutableStateOf("") }
    val selectedLine = selectedLineKey.ifEmpty { null }
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
            linesOpen = linesOpen,
            onChevronClick = {
                boardOpen = !boardOpen
                onEvent(
                    DeparturesUiEvent.DepartureBoardToggle(stopId, stopName, boardOpen, DepartureBoardSource.MAP_SHEET),
                )
            },
            onLinesClick = { linesOpen = !linesOpen },
        )
        if (boardOpen) {
            BoardBody(
                stopId = stopId,
                state = state,
                board = board,
                linesOpen = linesOpen,
                selectedLine = selectedLine,
                onEvent = onEvent,
                onSelectLine = { selectedLineKey = it.orEmpty() },
            )
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
 * The heading: the title at the start, then the Lines chip and the chevron as one end cluster. They
 * are whole items in a wrapping row, so at large text the cluster goes under the title and nothing
 * is squeezed or cut.
 */
@Composable
private fun BoardHeading(
    board: StopCardBoard,
    boardOpen: Boolean,
    linesOpen: Boolean,
    onChevronClick: () -> Unit,
    onLinesClick: () -> Unit,
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
            if (board.showsLinesChip || board.reservesLinesChip) {
                ReservedLine(present = board.showsLinesChip) {
                    LinesChip(open = linesOpen, onClick = if (board.showsLinesChip) onLinesClick else ({}))
                }
            }
            Chevron(open = boardOpen, onClick = onChevronClick)
        }
    }
}

/**
 * The one Lines chip. Filled while the badges are showing and outlined while they are hidden, taj's
 * chip sized buttons as the home chips use, at least 48 dp tall. What it does and whether it is on
 * are for a screen reader to hear, not guess from the fill.
 */
@Composable
private fun LinesChip(open: Boolean, onClick: () -> Unit) {
    val modifier = Modifier
        .testTag(TripPlannerTestTags.StopCard.LINES_CHIP)
        .heightIn(min = KrailTheme.dimensions.textFieldHeight)
        .semantics {
            role = Role.Button
            stateDescription = if (open) "Showing" else "Hidden"
            onClick(label = if (open) "Hide the lines" else "Show the lines", action = null)
        }
    if (open) {
        Button(onClick = onClick, modifier = modifier, dimensions = ButtonDefaults.chipButtonSize()) {
            Text(text = LINES_CHIP_TEXT)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, dimensions = ButtonDefaults.chipButtonSize()) {
            Text(text = LINES_CHIP_TEXT)
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

@Composable
private fun BoardBody(
    stopId: String,
    state: DeparturesState,
    board: StopCardBoard,
    linesOpen: Boolean,
    selectedLine: String?,
    onEvent: (DeparturesUiEvent) -> Unit,
    onSelectLine: (String?) -> Unit,
) {
    Column {
        if (board.showsLinesChip && linesOpen) {
            LineBadges(
                badges = remember(state.departures) { lineBadges(state.departures) },
                selectedLine = selectedLine,
                onTap = { badge ->
                    val next = lineFilterAfterTap(selectedLine, badge.lineNumber)
                    onSelectLine(next)
                    onEvent(
                        DeparturesUiEvent.LineFilterChanged(
                            stopId = stopId,
                            selected = next != null,
                            lineNumber = badge.lineNumber,
                            transportMode = badge.modeName,
                        ),
                    )
                },
            )
        }
        val floor = if (board.holdsBodyFloor) boardFloorHeight(LocalDensity.current.fontScale) else 0.dp
        Box(modifier = Modifier.testTag(TripPlannerTestTags.StopCard.BOARD_BODY).heightIn(min = floor)) {
            DepartureBoardBody(
                state = state,
                onRetry = { onEvent(DeparturesUiEvent.Refresh) },
                onLoadPreviousDepartures = { onEvent(DeparturesUiEvent.LoadPreviousDepartures(stopId)) },
                onShowPreviousToggle = { show -> onEvent(DeparturesUiEvent.TogglePreviousDepartures(stopId, show)) },
                hostedLine = HostedLineFilter(selectedLine),
            )
        }
    }
}

/**
 * The stop's lines as whole badges that wrap, never a sideways row: tapping one filters the board to
 * that line and tapping it again clears the filter.
 */
@Composable
private fun LineBadges(
    badges: ImmutableList<LineBadge>,
    selectedLine: String?,
    onTap: (LineBadge) -> Unit,
) {
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = Modifier
            .testTag(TripPlannerTestTags.StopCard.LINE_BADGES)
            .fillMaxWidth()
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingM),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        badges.forEach { badge ->
            TransportModeBadge(
                badgeText = badge.lineNumber,
                backgroundColor = badge.colorHex.hexToComposeColor(),
                size = BadgeSize.Large,
                selected = selectedLine == badge.lineNumber,
                modifier = Modifier
                    .testTag(TripPlannerTestTags.StopCard.lineBadge(badge.lineNumber))
                    .heightIn(min = dim.textFieldHeight),
                onClick = { onTap(badge) },
            )
        }
    }
}
