package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

private const val TRANSPORT_MODE_TRAIN = "Train"
private const val TRANSPORT_MODE_BUS = "Bus"
private const val TRANSPORT_MODE_FERRY = "Ferry"
private const val SAMPLE_DATE_PREFIX = "2026-04-08T01:"

internal val previewTrainDepartures = persistentListOf(
    StopDeparture(
        lineNumber = "T1",
        lineColorCode = "#F99D1C",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Liverpool via Strathfield",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}30:00Z",
        platformText = "Platform 4",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "T2",
        lineColorCode = "#0098CD",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Bondi Junction",
        departureTimeText = "11:33 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}33:00Z",
        platformText = "Platform 7",
        isRealTime = false,
    ),
    StopDeparture(
        lineNumber = "T4",
        lineColorCode = "#005AA3",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Illawarra via Sydenham",
        departureTimeText = "11:36 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}36:00Z",
        platformText = "Platform 2",
        isRealTime = true,
    ),
)

internal val previewBusDepartures = persistentListOf(
    StopDeparture(
        lineNumber = "333",
        lineColorCode = "#00B5EF",
        transportModeName = TRANSPORT_MODE_BUS,
        destinationName = "Parramatta Interchange",
        departureTimeText = "11:40 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}40:00Z",
        platformText = "Stand B",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "370",
        lineColorCode = "#00B5EF",
        transportModeName = TRANSPORT_MODE_BUS,
        destinationName = "Coogee Beach",
        departureTimeText = "11:45 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}45:00Z",
        platformText = null,
        isRealTime = false,
    ),
)

internal val previewFerryDepartures = persistentListOf(
    StopDeparture(
        lineNumber = "F1",
        lineColorCode = "#00774B",
        transportModeName = TRANSPORT_MODE_FERRY,
        destinationName = "Manly Wharf",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}30:00Z",
        platformText = "Wharf 1",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "F2",
        lineColorCode = "#144734",
        transportModeName = TRANSPORT_MODE_FERRY,
        destinationName = "Parramatta River",
        departureTimeText = "11:45 AM",
        departureUtcDateTime = "${SAMPLE_DATE_PREFIX}45:00Z",
        platformText = "Wharf 3",
        isRealTime = false,
    ),
)

@Preview(name = "Collapsed", showBackground = true)
@Composable
private fun DepartureBoardStopCardCollapsedPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureBoardStopCard(
            stopId = "10101010",
            state = DeparturesState(),
            onEvent = {},
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun DepartureBoardStopCardLoadingPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        Column { LoadingContent() }
    }
}

@PreviewComponent
@Composable
private fun DepartureBoardStopCardLoadedTrainPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        Column {
            LinesServedRow(
                departures = previewTrainDepartures,
                selectedLine = "T2",
                onLineSelect = {},
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            DepartureRowList(departures = previewTrainDepartures)
        }
    }
}

@Preview(name = "Loaded — Bus theme", showBackground = true)
@Composable
private fun DepartureBoardStopCardLoadedBusPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        Column {
            LinesServedRow(
                departures = previewBusDepartures,
                selectedLine = null,
                onLineSelect = {},
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            DepartureRowList(departures = previewBusDepartures)
        }
    }
}

@Preview(name = "Error state", showBackground = true)
@Composable
private fun DepartureBoardStopCardErrorPreview() {
    PreviewTheme(KrailThemeStyle.Metro) {
        Column { DeparturesErrorContent(onRetry = {}) }
    }
}

@Preview(name = "Ferry theme", showBackground = true)
@Composable
private fun DepartureBoardStopCardFerryPreview() {
    PreviewTheme(KrailThemeStyle.Ferry) {
        Column {
            LinesServedRow(
                departures = previewFerryDepartures,
                selectedLine = null,
                onLineSelect = {},
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            DepartureRowList(departures = previewFerryDepartures)
        }
    }
}
