package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.design.system.components.Text
import xyz.ksharma.krail.design.system.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegView(
    duration: String, // 1h 30m
    routeText: String, // AVC via XYZ
    transportModeLine: TransportModeLine,
    stops: ImmutableList<TimeTableState.JourneyCardInfo.Stop>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = transportModeLine.transportMode.colorCode
                    .hexToComposeColor()
                    .copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(vertical = 12.dp, horizontal = 12.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = routeText,
                style = KrailTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Text(
                text = duration,
                style = KrailTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProminentStopInfo(time = stops.first().time, name = stops.first().name)
            if (stops.size > 2) {
                StopsRow(
                    stops = "${stops.size - 2} stops",
                    line = transportModeLine,
                )
            } else {
                TransportModeInfo(
                    letter = transportModeLine.transportMode.name.first(),
                    backgroundColor = transportModeLine.transportMode.colorCode.hexToComposeColor(),
                    badgeText = transportModeLine.lineName,
                    badgeColor = transportModeLine.lineColorCode.hexToComposeColor(),
                )
            }
            ProminentStopInfo(time = stops.last().time, name = stops.last().name)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ProminentStopInfo(time: String, name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = time,
            style = KrailTheme.typography.titleMedium,
        )
        Text(
            text = name,
            style = KrailTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopsRow(stops: String, line: TransportModeLine, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = line.transportMode.colorCode.hexToComposeColor(),
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 20.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stops,
                style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
        TransportModeInfo(
            letter = line.transportMode.name.first(),
            backgroundColor = line.transportMode.colorCode.hexToComposeColor(),
            badgeText = line.lineName,
            badgeColor = line.lineColorCode.hexToComposeColor(),
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewLegView() {
    KrailTheme {
        LegView(
            duration = "1h 30m",
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Bus(),
                lineName = "700",
            ),
            stops = listOf(
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:00 am",
                    name = "XYZ Station, Platform 1",
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:30 am",
                    name = "ABC Station, Platform 2",
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "13:00 am",
                    name = "DEF Station, Platform 3",
                ),
            ).toImmutableList(),
            modifier = Modifier.background(KrailTheme.colors.background),
        )
    }
}

@Preview
@Composable
private fun PreviewLegViewTwoStops() {
    KrailTheme {
        LegView(
            duration = "1h 30m",
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Bus(),
                lineName = "700",
            ),
            stops = listOf(
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:00 am",
                    name = "XYZ Station, Platform 1",
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "13:00 am",
                    name = "DEF Station, Platform 3",
                ),
            ).toImmutableList(),
            modifier = Modifier.background(KrailTheme.colors.background),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewStopsRow() {
    KrailTheme {
        StopsRow(
            stops = "3 stops",
            line = TransportModeLine(
                transportMode = TransportMode.Bus(),
                lineName = "700",
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewProminentStopInfo() {
    KrailTheme {
        ProminentStopInfo(
            time = "12:00",
            name = "XYZ Station, Platform 1",
            modifier = Modifier.background(KrailTheme.colors.background),
        )
    }
}
