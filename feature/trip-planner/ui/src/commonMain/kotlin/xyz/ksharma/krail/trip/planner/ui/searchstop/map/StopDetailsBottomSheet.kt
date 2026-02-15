@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.park.ride.ui.components.ParkAndRideIcon
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.map.StopActionButton
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.components.map.StopDetailsBottomSheet as SharedStopDetailsBottomSheet

/**
 * Stop details bottom sheet for nearby stops in SearchStopMap.
 * Uses the shared StopDetailsBottomSheet component with search-specific customizations.
 */
@Composable
fun StopDetailsBottomSheet(
    stop: NearbyStopFeature,
    onDismiss: () -> Unit,
    onSelectStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SharedStopDetailsBottomSheet(
        stopId = stop.stopId,
        stopName = stop.stopName,
        transportModes = stop.transportModes,
        onDismiss = onDismiss,
        modifier = modifier,
        additionalInfo = {
            // Parking section (if available)
            if (stop.hasParkAndRide) {
                ParkingInfoSection(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        },
        actionButton = {
            StopActionButton(
                text = "Select Stop",
                onClick = onSelectStop,
            )
        },
    )
}

@Composable
private fun ParkingInfoSection(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParkAndRideIcon()

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Park & Ride Available",
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
            )
        }
    }
}

@PreviewComponent
@Composable
private fun PreviewStopDetailsBottomSheet() {
    PreviewTheme {
        val stop = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = xyz.ksharma.krail.core.maps.state.LatLng(
                -33.8830,
                151.2070,
            ),
            transportModes = persistentListOf(
                TransportMode.Train(),
                TransportMode.Bus(),
                TransportMode.LightRail(),
            ),
            hasParkAndRide = true,
        )
        StopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
            onSelectStop = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewStopDetailsBottomSheetSingleMode() {
    PreviewTheme {
        val stop = NearbyStopFeature(
            stopId = "10101250",
            stopName = "Edgecliff Station, Stand B",
            position = xyz.ksharma.krail.core.maps.state.LatLng(
                -33.8818,
                151.2372,
            ),
            transportModes = persistentListOf(TransportMode.Bus()),
        )
        StopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
            onSelectStop = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewStopDetailsBottomSheetFerry() {
    PreviewTheme {
        val stop = NearbyStopFeature(
            stopId = "10101111",
            stopName = "Circular Quay Ferry Wharf",
            position = xyz.ksharma.krail.core.maps.state.LatLng(
                -33.8612,
                151.2107,
            ),
            transportModes = persistentListOf(TransportMode.Ferry()),
        )
        StopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
            onSelectStop = {},
        )
    }
}

// endregion
