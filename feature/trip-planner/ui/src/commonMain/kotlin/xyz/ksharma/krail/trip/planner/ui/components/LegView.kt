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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_a11y
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.ContentAlphaTokens.DisabledContentAlpha
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

@Composable
fun LegView(
    routeText: String?, // AVC via XYZ
    transportModeLine: TransportModeLine,
    stops: ImmutableList<TimeTableState.JourneyCardInfo.Stop>,
    modifier: Modifier = Modifier,
    displayAllStops: Boolean = false,
    isSchoolBus: Boolean = false,
    onClick: () -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    val circleRadius = dim.spacingM
    val strokeWidth = dim.journeyLegStrokeWidth
    val timelineColor =
        remember(transportModeLine) { transportModeLine.lineColorCode.hexToComposeColor() }

    // Content alpha to be 100% always, as it's only visible in the expanded state.
    // If it's visible, it should be full alpha
    CompositionLocalProvider(LocalContentAlpha provides 1f) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .background(
                    color = transportModeBackgroundColor(transportMode = transportModeLine.transportMode),
                    shape = RoundedCornerShape(dim.cardCornerRadius),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        onClick()
                    },
                    role = Role.Button,
                )
                .padding(dim.spacingL),
        ) {
            RouteSummary(
                routeText = routeText,
                badgeText = transportModeLine.lineName,
                badgeColor = transportModeLine.lineColorCode.hexToComposeColor(),
            )

            Spacer(modifier = Modifier.height(dim.spacingL))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = dim.spacingXS),
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
                        .padding(start = dim.spacingXL),
                )

                Spacer(
                    modifier = Modifier
                        .height(dim.spacingL)
                        .timeLineCenter(
                            color = timelineColor,
                            strokeWidth = strokeWidth,
                        ),
                )

                Column(
                    modifier = Modifier
                        .timeLineCenter(
                            color = timelineColor,
                            strokeWidth = strokeWidth,
                        )
                        .padding(start = dim.spacingXL),
                ) {
                    val stopsCount by rememberSaveable(stops) { mutableIntStateOf(stops.size - 1) }
                    if (stopsCount > 1) {
                        StopsRow(
                            stops = "$stopsCount stops",
                            line = transportModeLine,
                            isSchoolBus = isSchoolBus,
                            onClick = {
                                onClick()
                            },
                        )
                    } else {
                        TransportModeInfo(
                            transportMode = transportModeLine.transportMode,
                        )
                    }
                }

                if (displayAllStops) {
                    stops.drop(1).dropLast(1).forEach { stop ->

                        Spacer(
                            modifier = Modifier
                                .height(dim.spacingL)
                                .timeLineCenter(
                                    color = timelineColor,
                                    strokeWidth = strokeWidth,
                                ),
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
                                .padding(start = dim.spacingXL),
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .height(dim.spacingL)
                        .timeLineCenter(
                            color = timelineColor,
                            strokeWidth = strokeWidth,
                        ),
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
                        .padding(start = dim.spacingXL),
                )
            }
        }
    }
}

@Composable
private fun RouteSummary(
    routeText: String?,
    badgeText: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportModeBadge(
                backgroundColor = badgeColor,
                badgeText = badgeText,
                modifier = Modifier.padding(end = dim.spacingML),
            )

            routeText?.let {
                Text(
                    text = routeText,
                    style = KrailTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun StopInfo(
    time: String,
    name: String,
    isProminent: Boolean,
    isWheelchairAccessible: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(modifier = modifier) {
        Text(
            text = time,
            style = if (isProminent) KrailTheme.typography.bodyMedium else KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXXS),
        ) {
            val textStyle =
                if (isProminent) KrailTheme.typography.titleSmall else KrailTheme.typography.bodySmall
            val iconTint =
                if (isProminent) {
                    KrailTheme.colors.onSurface
                } else {
                    KrailTheme.colors.onSurface.copy(
                        alpha = 0.75f,
                    )
                }

            val annotated = remember(name, isWheelchairAccessible) {
                buildAnnotatedString {
                    append(name)
                    if (isWheelchairAccessible) {
                        append(" ")
                        appendInlineContent("a11y")
                    }
                }
            }
            val inline = if (isWheelchairAccessible) {
                val sizeSp = textStyle.fontSize
                mapOf(
                    "a11y" to InlineTextContent(
                        Placeholder(
                            width = sizeSp,
                            height = sizeSp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_a11y),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconTint),
                        )
                    },
                )
            } else {
                emptyMap()
            }

            Text(
                text = annotated,
                style = textStyle,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.align(Alignment.CenterVertically),
                inlineContent = inline,
            )
        }
    }
}

@Composable
private fun StopsRow(
    stops: String,
    line: TransportModeLine,
    modifier: Modifier = Modifier,
    isSchoolBus: Boolean = false,
    onClick: () -> Unit,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buttonContainerColor by remember {
            mutableStateOf(NswTransportConfig.colorFor(line.transportMode).hexToComposeColor())
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

        if (isSchoolBus) {
            Text(
                text = "School Bus",
                style = KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = KrailTheme.colors.onSurface,
            )
        }
    }
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewLegView() {
    PreviewTheme {
        LegView(
            routeText = "towards AVC via XYZ Rd",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Bus,
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

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewLegViewTwoStops() {
    PreviewTheme {
        LegView(
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Train,
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

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewLegViewMetro() {
    PreviewTheme {
        LegView(
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Metro,
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

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewLegViewFerry() {
    PreviewTheme {
        LegView(
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Ferry,
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

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewLegViewLightRail() {
    PreviewTheme {
        LegView(
            routeText = "towards AVC via XYZ",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.LightRail,
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

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewLegViewSchoolBus() {
    PreviewTheme {
        LegView(
            routeText = "towards Parramatta via Great Western Hwy",
            transportModeLine = TransportModeLine(
                transportMode = TransportMode.Bus,
                lineName = "8550",
            ),
            isSchoolBus = true,
            stops = listOf(
                TimeTableState.JourneyCardInfo.Stop(
                    time = "8:05 am",
                    name = "Penrith Station, Stop A",
                    isWheelchairAccessible = false,
                ),
                TimeTableState.JourneyCardInfo.Stop(
                    time = "8:35 am",
                    name = "Parramatta Station, Stop B",
                    isWheelchairAccessible = false,
                ),
            ).toImmutableList(),
            modifier = Modifier.background(KrailTheme.colors.surface),
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewStopsRow() {
    PreviewTheme {
        StopsRow(
            stops = "3 stops",
            line = TransportModeLine(
                transportMode = TransportMode.Bus,
                lineName = "700",
            ),
            onClick = {},
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewProminentStopInfo() {
    PreviewTheme {
        StopInfo(
            time = "12:00",
            name = "XYZ Station, Platform 1",
            isProminent = true,
            isWheelchairAccessible = true,
            modifier = Modifier.background(KrailTheme.colors.surface),
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewRouteSummary() {
    PreviewTheme {
        RouteSummary(
            routeText = "towards AVC via XYZ Rd",
            modifier = Modifier.background(KrailTheme.colors.surface),
            badgeText = "700",
            badgeColor = Color.Red,
        )
    }
}

// endregion
