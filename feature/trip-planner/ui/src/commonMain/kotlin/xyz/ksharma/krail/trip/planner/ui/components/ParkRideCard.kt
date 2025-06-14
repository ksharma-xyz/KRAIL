/*
package xyz.ksharma.krail.trip.planner.ui.components // Or your preferred UI package

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideLoadedContent
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideStopsUiState


@Composable
fun ParkRideCard(
//    item: ParkRideStopItem,
    onToggleExpansion: (stopId: String) -> Unit,
    onNavigateToDetails: (stopId: String) -> Unit, // For the top-right navigate button
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(themeBackgroundColor())
            .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ParkRideIcon()

            Column(modifier = Modifier.animateContentSize()) {
                when (item.expansionState) {
                    ExpansionState.Collapsed -> {
                        Text(
                            text = item.stopName,
                            style = KrailTheme.typography.headlineMedium,
                            color = getForegroundColor(themeBackgroundColor()),
                        )
                    }

                    is ExpansionState.ErrorLoadingFacilities -> {
                        Text("Error loading data")
                        Text("Refresh")
                    }

                    is ExpansionState.FacilitiesLoaded -> {
                        (item.expansionState as? ExpansionState.FacilitiesLoaded)?.facilities?.forEach { parkRideState ->
                            ParkRideLoadedContent(parkRideState = parkRideState)
                        }
                    }

                    ExpansionState.Loading -> {
                        Text("Loading")
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
*/
