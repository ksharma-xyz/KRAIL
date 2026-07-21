@file:Suppress("MagicNumber")

package xyz.ksharma.krail.trip.planner.ui.components.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIcon
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeIconSize

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
    // Badge shown beside the stop name, so a surface with its own identity (Park & Ride)
    // carries it into the sheet. Null for a plain stop, leaving existing callers unchanged.
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val dim = KrailTheme.dimensions
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = KrailTheme.colors.bottomSheetBackground,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // Stop Name, with its badge where the surface has one.
            Row(
                modifier = Modifier
                    .padding(horizontal = dim.spacingXL)
                    .padding(bottom = dim.spacingM),
                horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.invoke()

                Text(
                    text = stopName,
                    style = KrailTheme.typography.headlineMedium,
                    color = KrailTheme.colors.onSurface,
                )
            }

            // Stop ID
            Text(
                text = "Stop ID - $stopId",
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
                modifier = Modifier.padding(horizontal = dim.spacingXL),
            )

            Spacer(modifier = Modifier.height(dim.spacingXL))

            Divider(modifier = Modifier.padding(horizontal = dim.spacingXL))

            Spacer(modifier = Modifier.height(dim.spacingXL))

            // Additional info (custom content)
            additionalInfo()

            // Transport Modes Section
            if (transportModes.isNotEmpty()) {
                Text(
                    text = "Transport Modes",
                    style = KrailTheme.typography.titleMedium,
                    color = KrailTheme.colors.onSurface,
                    modifier = Modifier.padding(horizontal = dim.spacingXL),
                )

                Spacer(modifier = Modifier.height(dim.spacingL))

                TransportModeRow(
                    transportModes = transportModes,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(dim.spacingXXXL))
            }

            // Action button (custom)
            actionButton()

            // Bottom spacer
            Spacer(modifier = Modifier.height(dim.spacingXXXL))
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
    val dim = KrailTheme.dimensions
    LazyRow(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = dim.spacingXL),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
    ) {
        items(transportModes) { mode ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dim.spacingM),
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
            .padding(horizontal = KrailTheme.dimensions.spacingXL, vertical = KrailTheme.dimensions.spacingXS),
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
            .padding(horizontal = KrailTheme.dimensions.spacingXL),
        horizontalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingL),
    ) {
        Button(
            onClick = onClick,
            dimensions = ButtonDefaults.largeButtonSize(),
        ) {
            Text(text = text)
        }
    }
}
