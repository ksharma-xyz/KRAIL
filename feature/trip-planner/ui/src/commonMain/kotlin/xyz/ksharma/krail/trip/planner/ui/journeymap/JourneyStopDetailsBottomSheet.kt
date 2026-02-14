@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.map.StopInfoRow
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
    SharedStopDetailsBottomSheet(
        stopId = stop.stopId,
        stopName = stop.stopName,
        transportModes = emptyList<TransportMode>().toImmutableList(), // No transport modes needed
        onDismiss = onDismiss,
        modifier = modifier,
        additionalInfo = {
            // Show arrival and departure times
            val hasArrival = stop.arrivalTime != null
            val hasDeparture = stop.departureTime != null
            val areSame = stop.arrivalTime == stop.departureTime

            when {
                // If arrival and departure are the same, show just one "Time" row
                hasArrival && hasDeparture && areSame -> {
                    stop.arrivalTime?.let { time ->
                        val formattedTime = try {
                            time.utcToLocalDateTimeAEST().toHHMM()
                        } catch (_: Exception) {
                            time
                        }
                        StopInfoRow(
                            label = "Time",
                            value = formattedTime,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                // Show both if they're different
                else -> {
                    // Arrival Time
                    if (hasArrival) {
                        stop.arrivalTime?.let { time ->
                            val formattedTime = try {
                                time.utcToLocalDateTimeAEST().toHHMM()
                            } catch (_: Exception) {
                                time
                            }
                            StopInfoRow(
                                label = "Arrival",
                                value = formattedTime,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Departure Time
                    if (hasDeparture) {
                        stop.departureTime?.let { time ->
                            val formattedTime = try {
                                time.utcToLocalDateTimeAEST().toHHMM()
                            } catch (_: Exception) {
                                time
                            }
                            StopInfoRow(
                                label = "Departure",
                                value = formattedTime,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        },
    )
}

/* TODO - use in future
/**
 * Infer transport mode from line name.
 * Examples: "T1" -> Train, "333" -> Bus, "Metro" -> Metro
 */
private fun inferTransportModeFromLineName(lineName: String): TransportMode? {
    return when {
        lineName.startsWith("T", ignoreCase = true) -> TransportMode.Train()
        lineName.contains("Metro", ignoreCase = true) -> TransportMode.Metro()
        lineName.contains("L", ignoreCase = true) && lineName.length <= 3 -> TransportMode.LightRail()
        lineName.contains("F", ignoreCase = true) -> TransportMode.Ferry()
        lineName.toIntOrNull() != null -> TransportMode.Bus() // Bus routes are numbers
        else -> null
    }
}

/**
 * Get the appropriate label for line/route based on transport mode.
 * - Train, Metro, Light Rail, Ferry: "Line"
 * - Bus, Coach: "Route"
 */
private fun getLineLabel(transportMode: TransportMode?): String {
    return when (transportMode) {
        is TransportMode.Bus, is TransportMode.Coach -> "Route"
        is TransportMode.Train, is TransportMode.Metro,
        is TransportMode.LightRail, is TransportMode.Ferry,
        -> "Line"
        null -> "Line" // Default to "Line" if unknown
    }
}

*/

/**
 * Extract platform label and value from platform text.
 * The label is extracted from the text itself, not inferred from transport mode.
 * Examples:
 * - "Platform 23" -> ("Platform", "23")
 * - "Stand B" -> ("Stand", "B")
 * - "Wharf 2" -> ("Wharf", "2")
 * - "Wharf 2, Side A" -> ("Wharf", "2, Side A")
 * - "23" -> ("Platform", "23") - fallback assumes Platform for bare numbers
 *
 * Similar to getPlatformText() in TripResponseMapper but extracts label and value.
 */
/*
private fun extractPlatformLabelAndValue(platformText: String): Pair<String, String> {
    // Match patterns like "Platform 23", "Stand B", "Wharf 2"
    // This regex will match the type word and everything after it
    val regex = Regex("^(Platform|Stand|Wharf)\\s+(.+)$", RegexOption.IGNORE_CASE)
    val match = regex.find(platformText)

    return if (match != null) {
        val label = match.groupValues[1].replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
        val value = match.groupValues[2]
        label to value
    } else {
        // If it's just a number (e.g., "23", "1"), assume it's a platform number
        if (platformText.matches(Regex("^[0-9]+$"))) {
            "Platform" to platformText
        } else {
            // For anything else (including "A", "B" alone), assume platform
            "Platform" to platformText
        }
    }
}*/

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
            position = xyz.ksharma.krail.core.maps.state.LatLng(-33.8734, 151.2069),
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
