@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.park.ride.ui.components.ParkAndRideIcon
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIcon
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIconSize
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailsBottomSheet(
    stop: NearbyStopFeature,
    onDismiss: () -> Unit,
    onSelectStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            // Header: Stop Name and Done Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                ) {
                    Text(
                        text = stop.stopName,
                        style = KrailTheme.typography.headlineMedium,
                    )
                }
            }

            // Stop ID below name
            Text(
                text = "Stop Id - " + stop.stopId,
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Parking Section
            if (stop.hasParkAndRide) {
                ParkingInfoSection(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Transport Modes Section
            if (stop.transportModes.isNotEmpty()) {
                Text(
                    text = "Transport Modes",
                    style = KrailTheme.typography.titleMedium,
                    color = KrailTheme.colors.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                TransportModeRow(
                    transportModes = stop.transportModes,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Select Stop Button (Primary)
                Button(
                    onClick = onSelectStop,
                    dimensions = ButtonDefaults.largeButtonSize(),
                ) {
                    Text(text = "Select Stop")
                }
            }

            // Bottom spacer for better UX
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TransportModeRow(
    transportModes: List<TransportMode>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(transportModes) { mode ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TransportModeIcon(
                    transportMode = mode,
                    size = TransportModeIconSize.Large,
                    displayBorder = false,
                )
                Text(
                    text = mode.name,
                    style = KrailTheme.typography.bodySmall,
                    color = KrailTheme.colors.onSurface,
                )
            }
        }
    }
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
        // Park and Ride Icon
        ParkAndRideIcon()

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Park & Ride Available",
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
            )

            // show live parking details.
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
            position = xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng(
                -33.8830,
                151.2070,
            ),
            distanceKm = 0.5,
            transportModes = listOf(
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
            position = xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng(
                -33.8818,
                151.2372,
            ),
            distanceKm = 1.2,
            transportModes = listOf(TransportMode.Bus()),
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
            position = xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng(
                -33.8612,
                151.2107,
            ),
            distanceKm = 0.05,
            transportModes = listOf(TransportMode.Ferry()),
        )
        StopDetailsBottomSheet(
            stop = stop,
            onDismiss = {},
            onSelectStop = {},
        )
    }
}
