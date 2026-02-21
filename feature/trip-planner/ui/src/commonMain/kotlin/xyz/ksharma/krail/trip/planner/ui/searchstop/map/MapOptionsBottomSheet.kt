package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeSortOrder
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapOptionsBottomSheet(
    searchRadiusKm: Double,
    selectedTransportModes: ImmutableSet<Int>,
    showDistanceScale: Boolean,
    showCompass: Boolean,
    onDismiss: () -> Unit,
    onEvent: (SearchStopUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local state to track pending changes
    var pendingRadiusKm by remember { mutableDoubleStateOf(searchRadiusKm) }
    var pendingTransportModes by remember {
        mutableStateOf(selectedTransportModes.toMutableSet())
    }
    var pendingShowDistanceScale by remember { mutableStateOf(showDistanceScale) }
    var pendingShowCompass by remember { mutableStateOf(showCompass) }

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
            // Header: "Options" and "Done"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Map Options",
                    style = KrailTheme.typography.headlineMedium,
                    color = KrailTheme.colors.onSurface,
                    modifier = Modifier.alignByBaseline(),
                )

                TextButton(
                    dimensions = ButtonDefaults.largeButtonSize(),
                    modifier = Modifier.alignByBaseline(),
                    onClick = {
                        // Apply all pending changes
                        if (pendingRadiusKm != searchRadiusKm) {
                            onEvent(SearchStopUiEvent.SearchRadiusChanged(pendingRadiusKm))
                        }
                        if (pendingShowDistanceScale != showDistanceScale) {
                            onEvent(
                                SearchStopUiEvent.ShowDistanceScaleToggled(
                                    pendingShowDistanceScale,
                                ),
                            )
                        }
                        if (pendingShowCompass != showCompass) {
                            onEvent(SearchStopUiEvent.ShowCompassToggled(pendingShowCompass))
                        }
                        // Apply transport mode changes
                        val modesChanged = pendingTransportModes != selectedTransportModes
                        if (modesChanged) {
                            // Calculate which modes were toggled
                            val added = pendingTransportModes - selectedTransportModes
                            val removed = selectedTransportModes - pendingTransportModes
                            (added + removed).forEach { productClass ->
                                TransportMode.toTransportModeType(productClass)?.let { mode ->
                                    onEvent(SearchStopUiEvent.TransportModeFilterToggled(mode))
                                }
                            }
                        }
                        // Single analytics snapshot of the full saved config
                        onEvent(
                            SearchStopUiEvent.MapOptionsSaved(
                                radiusKm = pendingRadiusKm,
                                transportModes = pendingTransportModes.sorted().joinToString(","),
                                showDistanceScale = pendingShowDistanceScale,
                                showCompass = pendingShowCompass,
                                radiusChanged = pendingRadiusKm != searchRadiusKm,
                                modesChanged = modesChanged,
                            ),
                        )
                        onDismiss()
                    },
                ) {
                    Text(
                        text = "Save",
                        color = LocalThemeColor.current.value.hexToComposeColor(),
                    )
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Search Radius Section
            Text(
                text = "Search Radius",
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            SearchRadiusChips(
                selectedRadiusKm = pendingRadiusKm,
                onRadiusSelect = { radius ->
                    pendingRadiusKm = radius
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Transport Mode Section
            Text(
                text = "Transport Mode",
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            TransportModeFilterRow(
                selectedModes = pendingTransportModes,
                onModeToggle = { mode ->
                    val newModes = pendingTransportModes.toMutableSet()
                    if (newModes.contains(mode.productClass)) {
                        newModes.remove(mode.productClass)
                    } else {
                        newModes.add(mode.productClass)
                    }
                    pendingTransportModes = newModes
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Map Controls Section
            Text(
                text = "Map Controls",
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            MapControlToggle(
                label = "Show distance scale",
                checked = pendingShowDistanceScale,
                onCheckedChange = { enabled ->
                    pendingShowDistanceScale = enabled
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            MapControlToggle(
                label = "Show compass",
                checked = pendingShowCompass,
                onCheckedChange = { enabled ->
                    pendingShowCompass = enabled
                },
            )

            // Bottom spacer for better UX
            Spacer(modifier = Modifier.height(108.dp))
        }
    }
}

@Composable
private fun SearchRadiusChips(
    selectedRadiusKm: Double,
    onRadiusSelect: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val radiusOptions = remember { listOf(1.0, 3.0, 5.0) }
    val themeColor = LocalThemeColor.current.value.hexToComposeColor()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        radiusOptions.forEach { radius ->
            FilterChip(
                selected = selectedRadiusKm == radius,
                onClick = { onRadiusSelect(radius) },
                label = {
                    Text(
                        text = "${radius.toInt()}km",
                        color = if (selectedRadiusKm == radius) {
                            KrailTheme.colors.surface
                        } else {
                            KrailTheme.colors.onSurface
                        },
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = themeColor,
                    selectedLabelColor = KrailTheme.colors.surface,
                    containerColor = KrailTheme.colors.surface,
                    labelColor = KrailTheme.colors.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun TransportModeFilterRow(
    selectedModes: Set<Int>,
    onModeToggle: (TransportMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allModes = remember {
        TransportMode.sortedValues(TransportModeSortOrder.PRIORITY)
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(allModes) { mode ->
            TransportModeChip(
                transportMode = mode,
                selected = selectedModes.contains(mode.productClass),
                onClick = { onModeToggle(mode) },
            )
        }
    }
}

@Composable
private fun MapControlToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeColor = LocalThemeColor.current.value.hexToComposeColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = KrailTheme.typography.bodyLarge,
            color = KrailTheme.colors.onSurface,
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KrailTheme.colors.surface,
                checkedTrackColor = themeColor,
                uncheckedThumbColor = KrailTheme.colors.surface,
                uncheckedTrackColor = KrailTheme.colors.onSurface.copy(alpha = 0.3f),
            ),
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewMapOptionsBottomSheet() {
    PreviewTheme {
        MapOptionsBottomSheet(
            searchRadiusKm = NearbyStopsConfig.DEFAULT_RADIUS_KM,
            selectedTransportModes = TransportMode.allProductClasses().toPersistentSet(),
            showDistanceScale = false,
            showCompass = true,
            onDismiss = {},
            onEvent = {},
        )
    }
}
