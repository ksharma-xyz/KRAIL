package xyz.ksharma.krail.trip.planner.ui.components // Or your preferred UI package

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.car_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideLoadedContent
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideStopItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideStopItem.ExpansionState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideStopsUiState


@Composable
fun ParkRideCard(
    item: ParkRideStopItem,
    onToggleExpansion: (stopId: String) -> Unit,
    onNavigateToDetails: (stopId: String) -> Unit, // For the top-right navigate button
    modifier: Modifier = Modifier
) {
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val contentColorOnPrimary = getForegroundColor(themeColor()) // For P icon on dark green

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = cardBackgroundColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            // Header Row (Always Visible: Icon, Stop Name, Expand/Collapse/Navigate Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion(item.stopId) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ParkRideIcon()

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = item.stopName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Action Icon: Expand/Collapse or Navigate
                val isExpanded =
                    item.expansionState is ExpansionState.FacilitiesLoaded ||
                            item.expansionState is ExpansionState.Loading ||
                            item.expansionState is ExpansionState.ErrorLoadingFacilities

                if (item.expansionState is ExpansionState.FacilitiesLoaded && (item.expansionState as ExpansionState.FacilitiesLoaded).facilities.isNotEmpty()) {
                    Image(
                        painter = painterResource(Res.drawable.car_icon),
                        colorFilter = ColorFilter.tint(color = Color.White),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp, bottom = 3.dp),
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.car_icon),
                        colorFilter = ColorFilter.tint(color = Color.White),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp, bottom = 3.dp),
                    )
                }
            }

            // Expanded Content Area
            AnimatedVisibility(
                visible = item.expansionState is ExpansionState.Loading ||
                        item.expansionState is ExpansionState.FacilitiesLoaded ||
                        item.expansionState is ExpansionState.ErrorLoadingFacilities
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Divider(
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    when (val state = item.expansionState) {
                        is ExpansionState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is ExpansionState.FacilitiesLoaded -> {
                            if (state.facilities.isEmpty()) {
                                Text(
                                    text = "No Park & Ride facilities found for this stop.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.facilities.forEachIndexed { index, facility ->
                                        ParkRideLoadedContent(parkRideState = facility)
                                        if (index < state.facilities.size - 1) {
                                            // Optional: Divider between facilities if needed
                                            // Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                        }

                        is ExpansionState.ErrorLoadingFacilities -> {
                            Text(
                                text = "Error: ${state.message}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        ExpansionState.Collapsed -> {
                            // This case should not be reached here due to AnimatedVisibility condition
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandCollapseIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(themeColor().copy(alpha = 0.8f)) // Match the P icon background
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(color = getForegroundColor(themeColor())),
        )
    }
}


// --- Previews ---

@Preview
@Composable
private fun ParkRideCardCollapsedPreview() {
    KrailTheme {
        CompositionLocalProvider(LocalThemeColor provides remember { mutableStateOf(KrailThemeStyle.BarbiePink.hexColorCode) }) {
            ParkRideCard(
                item = ParkRideStopItem(
                    stopId = "schofields",
                    stopName = "Schofields",
                    expansionState = ExpansionState.Collapsed
                ),
                onToggleExpansion = {},
                onNavigateToDetails = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ParkRideCardExpandedLoadingPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        ParkRideCard(
            item = ParkRideStopItem(
                stopId = "tallawong",
                stopName = "Tallawong Station",
                expansionState = ExpansionState.Loading
            ),
            onToggleExpansion = {},
            onNavigateToDetails = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun ParkRideCardExpandedLoadedPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        ParkRideCard(
            item = ParkRideStopItem(
                stopId = "tallawong",
                stopName = "Tallawong Station",
                expansionState = ExpansionState.FacilitiesLoaded(facilities)
            ),
            onToggleExpansion = {},
            onNavigateToDetails = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun ParkRideCardExpandedEmptyPreview() {
    PreviewTheme(KrailThemeStyle.Ferry) {
        ParkRideCard(
            item = ParkRideStopItem(
                stopId = "empty",
                stopName = "Empty Stop",
                expansionState = ExpansionState.FacilitiesLoaded(persistentListOf())
            ),
            onToggleExpansion = {},
            onNavigateToDetails = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun ParkRideCardExpandedErrorPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        ParkRideCard(
            item = ParkRideStopItem(
                stopId = "errorstop",
                stopName = "Error Stop",
                expansionState = ExpansionState.ErrorLoadingFacilities("Network connection failed.")
            ),
            onToggleExpansion = {},
            onNavigateToDetails = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

// Full UI Preview with a list
@Preview
@Composable
private fun ParkRideStopsUiPreview() {
    val uiState = ParkRideStopsUiState(
        items = persistentListOf(
            ParkRideStopItem(
                stopId = "tallawong",
                stopName = "Tallawong Station",
                expansionState = ExpansionState.FacilitiesLoaded(facilities),
            ),
            ParkRideStopItem(
                stopId = "schofields",
                stopName = "Schofields",
                expansionState = ExpansionState.Collapsed
            ),
            ParkRideStopItem(
                stopId = "loadingstop",
                stopName = "Loading Another Stop",
                expansionState = ExpansionState.Loading
            )
        )
    )
    PreviewTheme(KrailThemeStyle.Metro) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.items.forEach { item ->
                ParkRideCard(
                    item = item,
                    onToggleExpansion = {},
                    onNavigateToDetails = {}
                )
            }
        }
    }
}

val facilities = persistentListOf(
    ParkRideState(
        spotsAvailable = 100,
        totalSpots = 200,
        facilityName = "Tallawong P1",
        percentageFull = 10,
        timeText = "7:00 AM",
        stopId = "1111",
        displayParkRideIcon = true,
    ),
    ParkRideState(
        spotsAvailable = 130,
        totalSpots = 240,
        facilityName = "Tallawong P2",
        percentageFull = 30,
        timeText = "7:00 AM",
        stopId = "1111",
        displayParkRideIcon = false,
    ),
)
