package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.trip.planner.ui.components.LegView
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

@Composable
fun IntroContentRealTime(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LegView(
                routeText = "Manly to Circular Quay",
                transportModeLine = TransportModeLine(
                    transportMode = TransportMode.Ferry(),
                    lineName = "MFF",
                ),
                stops = stopsList(),
                onClick = { onInteraction() },
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
        name = "Manly, Wharf 2",
        time = "10:10 AM",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        name = "Circular Quay, Wharf 2",
        time = "10:30 AM",
        isWheelchairAccessible = true,
    ),
)
