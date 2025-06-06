package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_a11y
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.toAdaptiveDecorativeIconSize
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens.DisabledContentAlpha
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

@Composable
fun LegView(
    routeText: String, // AVC via XYZ
    transportModeLine: TransportModeLine,
    stops: ImmutableList<TimeTableState.JourneyCardInfo.Stop>,
    modifier: Modifier = Modifier,
    displayAllStops: Boolean = false,
    onClick: (Boolean) -> Unit = {},
) {
    val circleRadius = 8.dp
    val strokeWidth = 4.dp
    val timelineColor =
        remember(transportModeLine) { transportModeLine.lineColorCode.hexToComposeColor() }
    var showIntermediateStops by rememberSaveable { mutableStateOf(displayAllStops) }

    // Content alpha to be 100% always, as it's only visible in the expanded state.
    // If it's visible, it should be full alpha
    CompositionLocalProvider(LocalContentAlpha provides 1f) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .background(
                    color = transportModeBackgroundColor(transportMode = transportModeLine.transportMode),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        showIntermediateStops = !showIntermediateStops
                        onClick(showIntermediateStops)
                    },
                    role = Role.Button,
                )
                .padding(vertical = 12.dp, horizontal = 12.dp),
        ) {
            RouteSummary(
                routeText = routeText,
                badgeText = transportModeLine.lineName,
                badgeColor = transportModeLine.lineColorCode.hexToComposeColor(),
            )

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
            ) {
                StopInfo(
                    time = stops.first().time,
                    name = stops.first().name,
                    isProminent = true,
                    isWheelchairAccessible = stops.first().isWheelchairAccessible,
                    modifier = Modifier
                        .timeLineTop(
                            color = timelineColor,
                            strokeWidth = strokeWidth,
                            circleRadius = circleRadius,
                        )
                        .padding(start = 16.dp),
                )

                Spacer(
                    modifier = Modifier
                        .height(12.dp)
                        .timeLineCenter(
                            color = timelineColor,
                            strokeWidth = strokeWidth,
                        )
                )

                Column(
                    modifier = Modifier
                        .timeLineCenter(
                            color = timelineColor,
                            strokeWidth = strokeWidth,
                        )
                        .padding(start = 16.dp),
                ) {
                    val stopsCount by rememberSaveable(stops) { mutableIntStateOf(stops.size - 1) }
                    if (stopsCount > 1) {
                        StopsRow(
                            stops = "$stopsCount stops",
                            line = transportModeLine,
                            onClick = {
                                showIntermediateStops = !showIntermediateStops
                                onClick(showIntermediateStops)
                            },
                        )
                    } else {
                        TransportModeInfo(
                            transportMode = transportModeLine.transportMode,
                        )
                    }
                }

                if (showIntermediateStops) {
                    stops.drop(1).dropLast(1).forEach { stop ->

                        Spacer(
                            modifier = Modifier
                                .height(12.dp)
                                .timeLineCenter(
                                    color = timelineColor,
                                    strokeWidth = strokeWidth,
                                )
                        )

                        StopInfo(
                            time = stop.time,
                            name = stop.name,
                            isProminent = false,
                            isWheelchairAccessible = stop.isWheelchairAccessible,
                            modifier = Modifier
                                .timeLineCenterWithStop(
                                    color = timelineColor,
                                    strokeWidth = strokeWidth,
                                    circleRadius = circleRadius,
                                )
                                .timeLineTop(
                                    color = timelineColor,
                                    strokeWidth = strokeWidth,
                                    circleRadius = circleRadius,
                                )
                                .padding(start = 16.dp),
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .height(12.dp)
                        .timeLineCenter(
                            color = timelineColor,
                            strokeWidth = strokeWidth,
                        )
                )

                StopInfo(
                    time = stops.last().time,
                    name = stops.last().name,
                    isProminent = true,
                    isWheelchairAccessible = stops.last().isWheelchairAccessible,
                    modifier = Modifier
                        .timeLineBottom(
                            color = timelineColor,
                            strokeWidth = strokeWidth,
                            circleRadius = circleRadius,
                        )
                        .padding(start = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun RouteSummary(
    routeText: String,
    badgeText: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportModeBadge(
            backgroundColor = badgeColor,
            badgeText = badgeText,
        )

        Text(
            text = routeText,
            style = KrailTheme.typography.titleSmall,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
                .align(Alignment.CenterVertically),
        )
    }
}

@Composable
private fun StopInfo(
    time: String,
    name: String,
    isProminent: Boolean,
    isWheelchairAccessible: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = time,
            style = if (isProminent) KrailTheme.typography.bodyMedium else KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = if (isProminent) KrailTheme.typography.titleSmall else KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            if (isWheelchairAccessible) {
                Image(
                    painter = painterResource(Res.drawable.ic_a11y),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        color = if (isProminent) {
                            KrailTheme.colors.onSurface
                        } else {
                            KrailTheme.colors.onSurface.copy(
                                alpha = 0.75f,
                            )
                        },
                    ),
                    modifier = Modifier
                        .size(16.dp.toAdaptiveDecorativeIconSize())
                        .padding(end = 4.dp)
                        .align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Composable
private fun StopsRow(
    stops: String,
    line: TransportModeLine,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val buttonContainerColor by remember {
            mutableStateOf(line.transportMode.colorCode.hexToComposeColor())
        }
        Button(
            colors = ButtonColors(
                containerColor = buttonContainerColor,
                contentColor = Color.White,
                disabledContainerColor = buttonContainerColor.copy(alpha = DisabledContentAlpha),
                disabledContentColor = Color.White.copy(alpha = DisabledContentAlpha),
            ),
            onClick = onClick,
            dimensions = ButtonDefaults.smallButtonSize(),
        ) {
            Text(text = stops)
        }

        TransportModeInfo(
            transportMode = line.transportMode,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

// region Previews

@Preview
@Composable
private fun PreviewLegView() {
    KrailTheme {
        LegView(
            routeText = "towards AVC via XYZ Rd",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Bus(),
                lineName = "700",
            ),
            stops = listOf(
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:00 am",
                    name = "XYZ Station, Platform 1",
                    isWheelchairAccessible = false,
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:30 am",
                    name = "ABC Station, Platform 2",
                    isWheelchairAccessible = false,
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "01:00 am",
                    name = "DEF Station, Platform 3",
                    isWheelchairAccessible = true,
                ),
            ).toImmutableList(),
            modifier = Modifier.background(KrailTheme.colors.surface),
        )
    }
}

@Preview
@Composable
private fun PreviewLegViewTwoStops() {
    KrailTheme {
        LegView(
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Train(),
                lineName = "700",
            ),
            stops = listOf(
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:00 am",
                    name = "XYZ Station, Platform 1",
                    isWheelchairAccessible = true,
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "01:00 am",
                    name = "DEF Station, Platform 3",
                    isWheelchairAccessible = true,
                ),
            ).toImmutableList(),
            modifier = Modifier.background(KrailTheme.colors.surface),
        )
    }
}

@Preview
@Composable
private fun PreviewLegViewMetro() {
    KrailTheme {
        LegView(
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Metro(),
                lineName = "M1",
            ),
            stops = listOf(
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:00 am",
                    name = "XYZ Station, Platform 1",
                    isWheelchairAccessible = true,
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "01:00 am",
                    name = "DEF Station, Platform 3",
                    isWheelchairAccessible = true,
                ),
            ).toImmutableList(),
            modifier = Modifier.background(KrailTheme.colors.surface),
        )
    }
}

@Preview
@Composable
private fun PreviewLegViewFerry() {
    KrailTheme {
        LegView(
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Ferry(),
                lineName = "F2",
            ),
            stops = listOf(
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:00 am",
                    name = "XYZ Station, Platform 1",
                    isWheelchairAccessible = true,
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "01:00 am",
                    name = "DEF Station, Platform 3",
                    isWheelchairAccessible = true,
                ),
            ).toImmutableList(),
            modifier = Modifier.background(KrailTheme.colors.surface),
        )
    }
}

@Preview
@Composable
private fun PreviewLegViewLightRail() {
    KrailTheme {
        LegView(
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.LightRail(),
                lineName = "L1",
            ),
            stops = listOf(
                TimeTableState.JourneyCardInfo.Stop(
                    time = "12:00 am",
                    name = "XYZ Station, Platform 1",
                    isWheelchairAccessible = true,
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "01:00 am",
                    name = "DEF Station, Platform 3",
                    isWheelchairAccessible = false,
                ),
            ).toImmutableList(),
            modifier = Modifier.background(KrailTheme.colors.surface),
        )
    }
}

@Preview
@Composable
private fun PreviewStopsRow() {
    KrailTheme {
        StopsRow(
            stops = "3 stops",
            line = TransportModeLine(
                transportMode = TransportMode.Bus(),
                lineName = "700",
            ),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewProminentStopInfo() {
    KrailTheme {
        StopInfo(
            time = "12:00",
            name = "XYZ Station, Platform 1",
            isProminent = true,
            isWheelchairAccessible = true,
            modifier = Modifier.background(KrailTheme.colors.surface),
        )
    }
}

@Preview
@Composable
private fun PreviewRouteSummary() {
    KrailTheme {
        RouteSummary(
            routeText = "towards AVC via XYZ Rd",
            modifier = Modifier.background(KrailTheme.colors.surface),
            badgeText = "700",
            badgeColor = "00B5EF".hexToComposeColor(),
        )
    }
}

// endregion
