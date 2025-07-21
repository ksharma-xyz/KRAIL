package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.trip.planner.ui.components.LegView
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

@Composable
fun IntroContentRealTime(
    tagline: String,
    style: String,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    // Auto-toggle every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            expanded = !expanded
            onInteraction()
            delay(2000)
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LegView(
                routeText = "Circular Quay to Mosman Bay",
                transportModeLine = TransportModeLine(
                    transportMode = TransportMode.Ferry(),
                    lineName = "F6",
                ),
                stops = stopsList(),
                displayAllStops = expanded,
                onClick = { expanded = !expanded }, // keep in sync if user clicks
            )
        }

        TagLineWithEmoji(
            tagline = tagline,
            emoji = "\uD83D\uDE80",
            tagColor = style.hexToComposeColor()
        )
    }
}

private fun stopsList() = persistentListOf(
    TimeTableState.JourneyCardInfo.Stop(
        name = "Circular Quay, Wharf 4, Side B",
        time = "9:00 PM",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        name = "South Mosman Wharf",
        time = "9:15 PM",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        name = "Old Cremorne Wharf",
        time = "9:18 PM",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        name = "Mosman Bay Wharf",
        time = "9:20 PM",
        isWheelchairAccessible = true,
    ),
)
