@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.components.map

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
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIcon
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIconSize
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

/**
 * Generic stop details bottom sheet for map features in trip-planner module.
 * Can be used by SearchStopMap, JourneyMap, or any other map feature.
 *
 * @param stopId The stop ID
 * @param stopName The stop name
 * @param transportModes List of transport modes serving this stop
 * @param additionalInfo Optional additional information sections to display
 * @param actionButton Optional action button at the bottom
 * @param onDismiss Callback when dismissed
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailsBottomSheet(
    stopId: String,
    stopName: String,
    transportModes: ImmutableList<TransportMode>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    additionalInfo: @Composable () -> Unit = {},
    actionButton: @Composable () -> Unit = {},
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
            // Stop Name
            Text(
                text = stopName,
                style = KrailTheme.typography.headlineMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            )

            // Stop ID
            Text(
                text = "Stop ID - $stopId",
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Additional info (custom content)
            additionalInfo()

            // Transport Modes Section
            if (transportModes.isNotEmpty()) {
                Text(
                    text = "Transport Modes",
                    style = KrailTheme.typography.titleMedium,
                    color = KrailTheme.colors.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                TransportModeRow(
                    transportModes = transportModes,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action button (custom)
            actionButton()

            // Bottom spacer
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Display row of transport mode icons with labels.
 */
@Composable
private fun TransportModeRow(
    transportModes: ImmutableList<TransportMode>,
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

/**
 * Helper component for info rows (label: value)
 */
@Composable
fun StopInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = KrailTheme.typography.titleMedium,
            color = KrailTheme.colors.onSurface,
        )
        Text(
            text = value,
            style = KrailTheme.typography.bodyLarge,
            color = KrailTheme.colors.softLabel,
        )
    }
}

/**
 * Helper component for action buttons
 */
@Composable
fun StopActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onClick,
            dimensions = ButtonDefaults.largeButtonSize(),
        ) {
            Text(text = text)
        }
    }
}
