package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_clock
import krail.feature.trip_planner.ui.generated.resources.ic_share
import krail.feature.trip_planner.ui.generated.resources.ic_walk
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.share.ShareManager
import xyz.ksharma.krail.core.share.withBrandingHeader
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.components.AlertButton
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.pastDepartureTextStyle
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

/**
 * A card that displays information about a journey.
 * @param timeToDeparture The time until the journey departs.
 * @param originTime The time the journey starts.
 * @param destinationTime The time the journey ends.
 * @param totalTravelTime The total time the journey takes.
 * @param platformNumber The platform or stand number, the journey departs from.
 * @param transportModeLineList The list of transport mode lines used in the journey.
 * @param onClick The action to perform when the card is clicked.
 * @param modifier The modifier to apply to the card.
 */
@Composable
fun JourneyCard(
    timeToDeparture: String,
    platformNumber: String?,
    platformText: String?,
    originTime: String,
    destinationTime: String,
    totalTravelTime: String,
    legList: ImmutableList<TimeTableState.JourneyCardInfo.Leg>,
    transportModeLineList: ImmutableList<TransportModeLine>,
    onClick: () -> Unit,
    cardState: JourneyCardState,
    totalWalkTime: String?,
    totalUniqueServiceAlerts: Int,
    modifier: Modifier = Modifier,
    onAlertClick: () -> Unit = {},
    onLegClick: (Boolean) -> Unit = {},
    onMapClick: () -> Unit = {},
    isMapsAvailable: Boolean = false,
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation? = null,
    scheduledOriginTime: String? = null,
) {
    val isPast by remember(timeToDeparture) {
        mutableStateOf(
            timeToDeparture.contains(other = "ago", ignoreCase = true),
        )
    }

    val shareManager: ShareManager = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val cardBackground = if (isPast) KrailTheme.colors.pastDepartureRowSurface else KrailTheme.colors.surface
    // Capture these during composition — they cannot be read inside the coroutine lambda.
    val onSurfaceColor = KrailTheme.colors.onSurface
    val screenDensity = LocalDensity.current.density

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBackground)
            .drawWithContent {
                graphicsLayer.record {
                    // Draw the background explicitly inside the layer so it is included
                    // in the captured bitmap. Without this, .background() draws on the
                    // main canvas outside the layer and the shared image is transparent.
                    drawRect(color = cardBackground)
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
            .clickable(
                role = Role.Button,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp)
            .animateContentSize(),
    ) {
        JourneyCardHeader(
            transportModeLineList = transportModeLineList,
            platformText = platformText,
            timeToDeparture = timeToDeparture,
        )

        // Always visible content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { },
        ) {
            // Origin time — shows inline deviation (strikethrough + label) when late/early
            JourneyOriginTimeRow(
                originTime = originTime,
                scheduledOriginTime = scheduledOriginTime,
                departureDeviation = departureDeviation,
                isPast = isPast,
                modifier = Modifier.padding(top = 4.dp),
            )

            // Destination time + travel time + walk time
            ResponsiveJourneyInfoRow(
                destinationTime = destinationTime,
                totalTravelTime = totalTravelTime,
                totalWalkTime = totalWalkTime,
                isPast = isPast,
            )
        }

        // Expandable content - only leg details
        when (cardState) {
            JourneyCardState.DEFAULT -> {
                // No additional content in default state
            }

            JourneyCardState.EXPANDED -> ExpandedJourneyCardContent(
                legList = legList,
                totalUniqueServiceAlerts = totalUniqueServiceAlerts,
                onAlertClick = onAlertClick,
                onLegClick = onLegClick,
                onMapClick = onMapClick,
                isMapsAvailable = isMapsAvailable,
                onShareClick = {
                    coroutineScope.launch {
                        val bitmap = graphicsLayer.toImageBitmap()
                            .withBrandingHeader(
                                backgroundColor = cardBackground,
                                textColor = onSurfaceColor,
                                density = screenDensity,
                            )
                        shareManager.shareImage(bitmap)
                            .onFailure { error ->
                                logError("error while sharing image: $error")
                            }
                    }
                },
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ),
            )
        }

        Divider(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun ExpandedJourneyCardContent(
    legList: ImmutableList<TimeTableState.JourneyCardInfo.Leg>,
    totalUniqueServiceAlerts: Int,
    onAlertClick: () -> Unit,
    onLegClick: (Boolean) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMapsAvailable: Boolean = false,
    onShareClick: () -> Unit = {},
) {
    Column(modifier = modifier) {
        // Buttons row - Alert always at start, Maps always next to it
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (totalUniqueServiceAlerts > 0) {
                AlertButton(
                    dimensions = ButtonDefaults.smallButtonSize(),
                    onClick = onAlertClick,
                ) {
                    Text(
                        text = if (totalUniqueServiceAlerts > 1) {
                            "$totalUniqueServiceAlerts Alerts"
                        } else {
                            "$totalUniqueServiceAlerts Alert"
                        },
                    )
                }
            }

            // Map button
            if (isMapsAvailable) {
                Button(
                    onClick = onMapClick,
                    dimensions = ButtonDefaults.smallButtonSize(),
                    colors = ButtonDefaults.buttonColors(
                        customContainerColor = KrailTheme.colors.onSurface,
                        customContentColor = KrailTheme.colors.surface,
                    ),
                ) {
                    Text(text = "Maps")
                }
            }

            // Share button
            Button(
                onClick = onShareClick,
                dimensions = ButtonDefaults.smallButtonSize(),
                colors = ButtonDefaults.buttonColors(
                    customContainerColor = KrailTheme.colors.onSurface,
                    customContentColor = KrailTheme.colors.surface,
                ),
            ) {
                val density = LocalDensity.current
                val iconSize = with(density) { 14.sp.toDp() }
                Image(
                    painter = painterResource(Res.drawable.ic_share),
                    contentDescription = "Share journey",
                    colorFilter = ColorFilter.tint(KrailTheme.colors.surface),
                    modifier = Modifier.size(iconSize),
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
        )

        legList.forEachIndexed { index, leg ->
            when (leg) {
                is TimeTableState.JourneyCardInfo.Leg.WalkingLeg -> {
                    WalkingLeg(
                        duration = leg.duration,
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                    )
                }

                is TimeTableState.JourneyCardInfo.Leg.TransportLeg -> {
                    if (leg.walkInterchange?.position == TimeTableState.JourneyCardInfo.WalkPosition.BEFORE) {
                        leg.walkInterchange?.duration?.let { duration ->
                            WalkingLeg(
                                duration = duration,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            )
                        }
                    }

                    if (leg.walkInterchange?.position == TimeTableState.JourneyCardInfo.WalkPosition.IDEST) {
                        leg.walkInterchange?.duration?.let { duration ->
                            WalkingLeg(
                                duration = duration,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            )
                        }
                    } else {
                        var displayAllStops by rememberSaveable { mutableStateOf(false) }
                        LegView(
                            routeText = leg.displayText,
                            transportModeLine = leg.transportModeLine,
                            stops = leg.stops,
                            displayAllStops = displayAllStops,
                            modifier = Modifier.padding(
                                top = if (index > 0) {
                                    getPaddingValue(
                                        lastLeg = legList[(index - 1).coerceAtLeast(0)],
                                    )
                                } else {
                                    0.dp
                                },
                            ),
                            onClick = {
                                displayAllStops = !displayAllStops
                                onLegClick(displayAllStops)
                            },
                        )
                    }

                    if (leg.walkInterchange?.position == TimeTableState.JourneyCardInfo.WalkPosition.AFTER) {
                        leg.walkInterchange?.duration?.let { duration ->
                            WalkingLeg(
                                duration = duration,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getPaddingValue(lastLeg: TimeTableState.JourneyCardInfo.Leg): Dp {
    return if (
        lastLeg is TimeTableState.JourneyCardInfo.Leg.TransportLeg &&
        lastLeg.walkInterchange?.position != TimeTableState.JourneyCardInfo.WalkPosition.AFTER
    ) {
        16.dp
    } else {
        0.dp
    }
}

@Composable
private fun ResponsiveJourneyInfoRow(
    destinationTime: String,
    totalTravelTime: String,
    totalWalkTime: String?,
    isPast: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = destinationTime,
            style = pastDepartureTextStyle(isPast, KrailTheme.typography.titleMedium),
            color = KrailTheme.colors.onSurface,
        )

        TextWithIcon(
            painter = painterResource(Res.drawable.ic_clock),
            text = totalTravelTime,
            textStyle = KrailTheme.typography.bodyLarge,
        )

        if (totalWalkTime != null) {
            TextWithIcon(
                painter = painterResource(Res.drawable.ic_walk),
                text = totalWalkTime,
                textStyle = KrailTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun TextWithIcon(
    painter: Painter,
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = KrailTheme.typography.bodyMedium,
    color: Color = KrailTheme.colors.onSurface,
) {
    val density = LocalDensity.current
    val iconSize = with(density) { 14.sp.toDp() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = color),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(iconSize),
        )
        Text(
            text = text,
            style = textStyle,
            color = color,
        )
    }
}

/**
 * Converts JourneyCard's [TimeTableState.JourneyCardInfo.DepartureDeviation] into the
 * plain strings expected by [ScheduledTimeRow], then delegates rendering to it.
 */
@Composable
private fun JourneyOriginTimeRow(
    originTime: String,
    scheduledOriginTime: String?,
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation?,
    isPast: Boolean,
    modifier: Modifier = Modifier,
) {
    val showDeviation = scheduledOriginTime != null && (
        departureDeviation is TimeTableState.JourneyCardInfo.DepartureDeviation.Late ||
            departureDeviation is TimeTableState.JourneyCardInfo.DepartureDeviation.Early
        )

    val deviationLabel: String? = if (showDeviation) {
        when (departureDeviation) {
            is TimeTableState.JourneyCardInfo.DepartureDeviation.Late ->
                "Delayed ${departureDeviation.text.removeSuffix(" late")}"

            is TimeTableState.JourneyCardInfo.DepartureDeviation.Early ->
                "Early ${departureDeviation.text.removeSuffix(" early")}"

            else -> null
        }
    } else {
        null
    }

    val deviationColor: Color = if (showDeviation) {
        if (departureDeviation is TimeTableState.JourneyCardInfo.DepartureDeviation.Late) {
            KrailTheme.colors.deviationLate
        } else {
            KrailTheme.colors.deviationEarly
        }
    } else {
        Color.Unspecified
    }

    ScheduledTimeRow(
        timeText = originTime,
        isPast = isPast,
        scheduledTimeText = if (showDeviation) scheduledOriginTime else null,
        deviationLabel = deviationLabel,
        deviationColor = deviationColor,
        onTimeTextStyle = KrailTheme.typography.titleMedium,
        modifier = modifier,
    )
}

// region Previews

private val PREVIEW_STOPS = persistentListOf(
    TimeTableState.JourneyCardInfo.Stop(
        name = "Stop 1",
        time = "8:30am",
        isWheelchairAccessible = false,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        name = "Stop 2",
        time = "8:35am",
        isWheelchairAccessible = true,
    ),
    TimeTableState.JourneyCardInfo.Stop(
        name = "Stop 3",
        time = "8:40am",
        isWheelchairAccessible = false,
    ),
)

@PreviewComponent
@Composable
private fun Preview_Default_InlineModesAndPlatform() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 7 mins",
            originTime = "8:25am",
            destinationTime = "8:55am",
            totalTravelTime = "30 mins",
            platformNumber = "1",
            platformText = "Platform 1",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "700"),
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
                TransportModeLine(transportMode = TransportMode.Ferry, lineName = "F2"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = "10 mins",
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Late("2 mins late"),
            scheduledOriginTime = "8:23am",
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_ManyModes_Wrap() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 3 mins",
            originTime = "7:15am",
            destinationTime = "8:20am",
            totalTravelTime = "1h 5m",
            platformNumber = "3",
            platformText = "Platform 3",

            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "333"),
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "610X"),
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T8"),
                TransportModeLine(transportMode = TransportMode.Ferry, lineName = "F1"),
                TransportModeLine(transportMode = TransportMode.Metro, lineName = "M1"),
                TransportModeLine(transportMode = TransportMode.LightRail, lineName = "L2"),
                TransportModeLine(transportMode = TransportMode.Coach, lineName = "C1"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Late("4 mins late"),
            scheduledOriginTime = "7:11am",
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_JourneyCard_Expanded() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 5 mins",
            originTime = "8:25am",
            destinationTime = "8:40am",
            totalTravelTime = "15 mins",
            platformNumber = "3",
            platformText = "Platform 3",

            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "700"),
            ),
            legList = persistentListOf(
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS,
                    displayText = "towards Abc via Rainy Rd",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Train,
                        lineName = "T1",
                    ),
                    totalDuration = "20 mins",
                    tripId = "T1",
                ),
                TimeTableState.JourneyCardInfo.Leg.WalkingLeg(
                    duration = "15 mins",
                ),
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS.take(2).toImmutableList(),
                    displayText = "towards Xyz via Awesome Rd",
                    totalDuration = "10 mins",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Bus,
                        lineName = "700",
                    ),
                    tripId = "700",
                ),
            ),
            cardState = JourneyCardState.EXPANDED,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 1,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime,
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_JourneyCard_Expanded_NoAlerts() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 5 mins",
            originTime = "8:25am",
            destinationTime = "8:40am",
            totalTravelTime = "15 mins",
            platformNumber = "3",
            platformText = "Platform 3",

            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "700"),
            ),
            legList = persistentListOf(
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS,
                    displayText = "towards Abc via Rainy Rd",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Train,
                        lineName = "T1",
                    ),
                    totalDuration = "20 mins",
                    tripId = "T1",
                ),
                TimeTableState.JourneyCardInfo.Leg.WalkingLeg(
                    duration = "15 mins",
                ),
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS.take(2).toImmutableList(),
                    displayText = "towards Xyz via Awesome Rd",
                    totalDuration = "10 mins",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Bus,
                        lineName = "700",
                    ),
                    tripId = "700",
                ),
            ),
            cardState = JourneyCardState.EXPANDED,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Early("2 mins early"),
            scheduledOriginTime = "8:27am",
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_Default_WithWalkTime() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 12 mins",
            originTime = "9:15am",
            destinationTime = "9:45am",
            totalTravelTime = "30 mins",
            platformNumber = "2",
            platformText = "Platform 2",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T2"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = "5 mins",
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime,
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_Default_WithDeviation_Early() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 2 mins",
            originTime = "6:30am",
            destinationTime = "7:00am",
            totalTravelTime = "30 mins",
            platformNumber = "1",
            platformText = "Platform 1",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T4"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Early("1 min early"),
            scheduledOriginTime = "6:31am",
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_Expanded_WithWalkAndDeviation() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 8 mins",
            originTime = "10:20am",
            destinationTime = "11:05am",
            totalTravelTime = "45 mins",
            platformNumber = "5",
            platformText = "Platform 5",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "M92"),
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T3"),
            ),
            legList = persistentListOf(
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS,
                    displayText = "towards Central",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Bus,
                        lineName = "M92",
                    ),
                    totalDuration = "15 mins",
                    tripId = "M92",
                ),
                TimeTableState.JourneyCardInfo.Leg.WalkingLeg(
                    duration = "8 mins",
                ),
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS,
                    displayText = "towards North Shore",
                    totalDuration = "22 mins",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Train,
                        lineName = "T3",
                    ),
                    tripId = "T3",
                ),
            ),
            cardState = JourneyCardState.EXPANDED,
            totalWalkTime = "8 mins",
            totalUniqueServiceAlerts = 2,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Late("3 mins late"),
            scheduledOriginTime = "10:17am",
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_Expanded_MultipleAlerts() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 15 mins",
            originTime = "2:30pm",
            destinationTime = "3:15pm",
            totalTravelTime = "45 mins",
            platformNumber = null,
            platformText = null,
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Ferry, lineName = "F1"),
                TransportModeLine(transportMode = TransportMode.Bus, lineName = "380"),
            ),
            legList = persistentListOf(
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS.take(2).toImmutableList(),
                    displayText = "to Circular Quay",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Ferry,
                        lineName = "F1",
                    ),
                    totalDuration = "25 mins",
                    tripId = "F1",
                ),
                TimeTableState.JourneyCardInfo.Leg.WalkingLeg(
                    duration = "5 mins",
                ),
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS,
                    displayText = "to Bondi Junction",
                    totalDuration = "15 mins",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Bus,
                        lineName = "380",
                    ),
                    tripId = "380",
                ),
            ),
            cardState = JourneyCardState.EXPANDED,
            totalWalkTime = "5 mins",
            totalUniqueServiceAlerts = 3,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Late("5 mins late"),
            scheduledOriginTime = "2:25pm",
        )
    }
}

@PreviewComponent
@Composable
private fun Preview_Default_PastJourney() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "5 mins ago",
            originTime = "8:00am",
            destinationTime = "8:25am",
            totalTravelTime = "25 mins",
            platformNumber = "4",
            platformText = "Platform 4",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime,
        )
    }
}

@PreviewComponent
@Composable
private fun JourneyCardCollapsedDelayedPreview() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 5 mins",
            originTime = "8:28am",
            destinationTime = "8:55am",
            totalTravelTime = "27 mins",
            platformNumber = "4",
            platformText = "Platform 4",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T1"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Late("3 mins late"),
            scheduledOriginTime = "8:25am",
        )
    }
}

@PreviewComponent
@Composable
private fun JourneyCardCollapsedEarlyPreview() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 4 mins",
            originTime = "8:24am",
            destinationTime = "8:50am",
            totalTravelTime = "26 mins",
            platformNumber = "2",
            platformText = "Platform 2",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T2"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Early("1 min early"),
            scheduledOriginTime = "8:25am",
        )
    }
}

@PreviewComponent
@Composable
private fun JourneyCardCollapsedOnTimePreview() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE) {
        JourneyCard(
            timeToDeparture = "in 7 mins",
            originTime = "8:25am",
            destinationTime = "8:52am",
            totalTravelTime = "27 mins",
            platformNumber = "1",
            platformText = "Platform 1",
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Train, lineName = "T4"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime,
        )
    }
}

// endregion
