@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.map.StopInfoRow
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyStopUiMapper.getTimeInfo
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import xyz.ksharma.krail.trip.planner.ui.components.map.StopDetailsBottomSheet as SharedStopDetailsBottomSheet

/**
 * Stop details bottom sheet for journey map stops.
 * Shows arrival/departure times and platform/stand/wharf information.
 */
@Composable
fun JourneyStopDetailsBottomSheet(
    stop: JourneyStopFeature,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Extract time info outside composable - this is pure computation
    val timeInfo = remember(stop.arrivalTime, stop.departureTime) {
        stop.getTimeInfo()
    }

    SharedStopDetailsBottomSheet(
        stopId = stop.stopId,
        stopName = stop.stopName,
        transportModes = emptyList<TransportMode>().toImmutableList(),
        onDismiss = onDismiss,
        modifier = modifier,
        additionalInfo = {
            // Display time info rows
            timeInfo.forEach { info ->
                StopInfoRow(
                    label = info.label,
                    value = info.value,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (timeInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
    )
}

// region Previews

@PreviewComponent
@Composable
private fun PreviewJourneyStopDetailsOrigin() {
    PreviewTheme {
        val stop = JourneyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = xyz.ksharma.krail.core.maps.state.LatLng(-33.8830, 151.2070),
            stopType = StopType.ORIGIN,
            platform = "Platform 23",
            lineName = "T1",
            lineColor = "#F99D1C",
            arrivalTime = null, // Origin doesn't have arrival
            departureTime = "2025-06-13T00:15:00Z", // UTC time - will be converted to AEST
        )
        JourneyStopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewJourneyStopDetailsInterchange() {
    PreviewTheme {
        val stop = JourneyStopFeature(
            stopId = "10101250",
            stopName = "Town Hall Station",
            position = LatLng(-33.8734, 151.2069),
            stopType = StopType.INTERCHANGE,
            platform = "Platform 1",
            lineName = "Metro",
            lineColor = "#009B77",
            arrivalTime = "2025-06-13T00:20:00Z", // UTC time
            departureTime = "2025-06-13T00:22:00Z", // UTC time
        )
        JourneyStopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewJourneyStopDetailsDestination() {
    PreviewTheme {
        val stop = JourneyStopFeature(
            stopId = "10101111",
            stopName = "Circular Quay",
            position = xyz.ksharma.krail.core.maps.state.LatLng(-33.8612, 151.2107),
            stopType = StopType.DESTINATION,
            platform = null,
            lineName = "333",
            lineColor = "#00B5EF",
            arrivalTime = "2025-06-13T00:35:00Z", // UTC time
            departureTime = null, // Destination doesn't have departure
        )
        JourneyStopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewJourneyStopDetailsBusStand() {
    PreviewTheme {
        val stop = JourneyStopFeature(
            stopId = "2131125",
            stopName = "Edgecliff Station",
            position = xyz.ksharma.krail.core.maps.state.LatLng(-33.8818, 151.2372),
            stopType = StopType.ORIGIN,
            platform = "Stand B",
            lineName = "389",
            lineColor = "#00B5EF",
            arrivalTime = null,
            departureTime = "2025-06-13T01:45:00Z", // UTC time
        )
        JourneyStopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewJourneyStopDetailsFerryWharf() {
    PreviewTheme {
        val stop = JourneyStopFeature(
            stopId = "10101331",
            stopName = "Circular Quay",
            position = xyz.ksharma.krail.core.maps.state.LatLng(-33.8612, 151.2107),
            stopType = StopType.INTERCHANGE,
            platform = "Wharf 2",
            lineName = "F1",
            lineColor = "#5AB031",
            arrivalTime = "2025-06-13T02:08:00Z", // UTC time
            departureTime = "2025-06-13T02:10:00Z", // UTC time
        )
        JourneyStopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
        )
    }
}

// endregion
