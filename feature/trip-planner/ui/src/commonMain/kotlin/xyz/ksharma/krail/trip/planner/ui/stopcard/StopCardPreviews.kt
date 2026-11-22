package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature

// The floating stop card in every state a stop pin can open it in, over a flat stand-in for the map.
// Each is a @PreviewScreen, so it is drawn in light and dark at normal and 200 percent text, the
// 320 dp frame included.

private val SINGLE_LINE_STOP = NearbyStopFeature(
    stopId = "10101250",
    stopName = "Edgecliff Station, Stand B",
    position = LatLng(-33.8818, 151.2372),
    transportModes = persistentListOf(TransportMode.Bus),
)

private val MULTI_LINE_STOP = NearbyStopFeature(
    stopId = "200060",
    stopName = "Central Station",
    position = LatLng(-33.8830, 151.2070),
    transportModes = persistentListOf(TransportMode.Train, TransportMode.Bus, TransportMode.LightRail),
)

private fun row(line: String, mode: TransportMode, destination: String, time: String) = StopDeparture(
    lineNumber = line,
    lineColorCode = mode.colorCode,
    transportModeName = mode.name,
    destinationName = destination,
    departureTimeText = time,
    departureUtcDateTime = "2026-11-22T01:30:00Z",
    platformText = "Platform 4",
    isRealTime = true,
)

private val ONE_LINE_BOARD = DeparturesState(
    isLoading = false,
    departures = listOf(
        row("333", TransportMode.Bus, "Bondi Beach", "11:30 AM"),
        row("333", TransportMode.Bus, "Coogee", "11:42 AM"),
    ).toImmutableList(),
)

private val MANY_LINE_BOARD = DeparturesState(
    isLoading = false,
    departures = listOf(
        row("T1", TransportMode.Train, "Berowra", "11:31 AM"),
        row("T4", TransportMode.Train, "Illawarra", "11:33 AM"),
        row("333", TransportMode.Bus, "Circular Quay", "11:35 AM"),
        row("L1", TransportMode.LightRail, "Dulwich Hill", "11:36 AM"),
    ).toImmutableList(),
)

@PreviewScreen
@Composable
private fun PreviewStopCardSingleLineStop() =
    CardOverMap(SINGLE_LINE_STOP, ONE_LINE_BOARD, boardOpen = true)

@PreviewScreen
@Composable
private fun PreviewStopCardManyLinesChipOnly() =
    CardOverMap(MULTI_LINE_STOP, MANY_LINE_BOARD, boardOpen = true)

@PreviewScreen
@Composable
private fun PreviewStopCardManyLinesBadgesShown() =
    CardOverMap(MULTI_LINE_STOP, MANY_LINE_BOARD, boardOpen = true, linesOpen = true)

@PreviewScreen
@Composable
private fun PreviewStopCardBoardClosed() =
    CardOverMap(MULTI_LINE_STOP, DeparturesState(), boardOpen = false)

@PreviewScreen
@Composable
private fun PreviewStopCardLoading() =
    CardOverMap(MULTI_LINE_STOP, DeparturesState(isLoading = true), boardOpen = true)

@PreviewScreen
@Composable
private fun PreviewStopCardFailed() =
    CardOverMap(MULTI_LINE_STOP, DeparturesState(isLoading = false, isError = true), boardOpen = true)

@PreviewScreen
@Composable
private fun PreviewStopCardStaleRefreshing() =
    CardOverMap(MULTI_LINE_STOP, MANY_LINE_BOARD.copy(silentLoading = true), boardOpen = true)

@Composable
private fun CardOverMap(
    stop: NearbyStopFeature,
    departures: DeparturesState,
    boardOpen: Boolean,
    linesOpen: Boolean = false,
) {
    PreviewTheme {
        Box(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.onSurface.copy(alpha = 0.08f))) {
            StopCardPlacement(topReserve = 96.dp, onHeight = {}) {
                StopCard(
                    stop = stop,
                    departures = departures,
                    onEvent = {},
                    onClose = {},
                    boardOpenAtStart = boardOpen,
                    linesOpenAtStart = linesOpen,
                    actions = { StopCardAction(text = "Select stop", onClick = {}) },
                )
            }
        }
    }
}
