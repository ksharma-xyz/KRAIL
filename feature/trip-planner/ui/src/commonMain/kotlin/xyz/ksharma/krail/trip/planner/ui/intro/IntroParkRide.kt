package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentSetOf
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

@Composable
fun IntroParkRide(tagline: String, style: String, modifier: Modifier) {
    val themeColor = remember { mutableStateOf(style) }
    var isExpanded by remember { mutableStateOf(false) }

    // Auto toggle every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            isExpanded = !isExpanded
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompositionLocalProvider(LocalThemeColor provides themeColor) {
                ParkRideCard(
                    isExpanded = isExpanded,
                    parkRideUiState = ParkRideUiState(
                        stopId = "park-ride-stop-id",
                        stopName = "Bella Vista Station",
                        facilities = persistentSetOf(
                            ParkRideFacilityDetail(
                                spotsAvailable = 658,
                                totalSpots = 700,
                                facilityName = "Bella Vista",
                                percentageFull = 14,
                                stopId = "stopId1",
                                timeText = "8:00 AM",
                                facilityId = "facilityId1",
                            ),
                        ),
                    ),
                    onClick = {
                        isExpanded = !isExpanded
                    },
                )
            }
        }

        TagLineWithEmoji(
            tagline = tagline,
            emoji = "🚗",
            emojiColor = style.hexToComposeColor(),
            tagColor = style.hexToComposeColor(),
        )
    }
}
