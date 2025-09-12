package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_clock
import krail.feature.trip_planner.ui.generated.resources.ic_walk
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.AlertButton
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.SeparatorIcon
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState

/**
 * A card that displays information about a journey.
 * @param timeToDeparture The time until the journey departs.
 * @param originTime The time the journey starts.
 * @param destinationTime The time the journey ends.
 * @param totalTravelTime The total time the journey takes.
 * @param platformNumber The platform or stand number, the journey departs from.
 * @param isWheelchairAccessible Whether the journey is wheelchair accessible.
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
    isWheelchairAccessible: Boolean,
    legList: ImmutableList<TimeTableState.JourneyCardInfo.Leg>,
    transportModeLineList: ImmutableList<TransportModeLine>,
    onClick: () -> Unit,
    cardState: JourneyCardState,
    totalWalkTime: String?,
    totalUniqueServiceAlerts: Int,
    modifier: Modifier = Modifier,
    onAlertClick: () -> Unit = {},
    onLegClick: (Boolean) -> Unit = {},
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation? = null,
) {
    // Derive transport modes for styling and colors
    val transportModeList: ImmutableList<TransportMode> = remember(transportModeLineList) {
        transportModeLineList.map { it.transportMode }.toImmutableList()
    }

    val onSurface: Color = KrailTheme.colors.onSurface
    val borderColors = remember(transportModeList) { transportModeList.toColors(onSurface) }
    val isPastJourney by remember(timeToDeparture) {
        mutableStateOf(
            timeToDeparture.contains(other = "ago", ignoreCase = true),
        )
    }
    val pastJourneyColor = KrailTheme.colors.onSurface.copy(alpha = 0.5f)
    val themeColor = if (!isPastJourney) { // TODO - animate
        transportModeList.firstOrNull()?.colorCode?.hexToComposeColor()
            ?: KrailTheme.colors.onSurface
    } else {
        pastJourneyColor
    }

    val horizontalCardPadding by animateDpAsState(
        targetValue = if (cardState == JourneyCardState.DEFAULT) 12.dp else 0.dp,
        label = "cardPadding",
    )

    CompositionLocalProvider(LocalContentAlpha provides if (isPastJourney) 0.5f else 1f) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(color = KrailTheme.colors.surface)
                .then(
                    if (cardState == JourneyCardState.DEFAULT) {
                        Modifier.border(
                            width = 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            brush = if (!isPastJourney) {
                                Brush.linearGradient(colors = borderColors)
                            } else {
                                Brush.linearGradient(listOf(pastJourneyColor, pastJourneyColor))
                            },
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(
                    vertical = 8.dp,
                    horizontal = horizontalCardPadding,
                )
                .animateContentSize(),
        ) {
            when (cardState) {
                JourneyCardState.DEFAULT -> DefaultJourneyCardContent(
                    timeToDeparture = timeToDeparture,
                    originTime = originTime,
                    destinationTime = destinationTime,
                    totalTravelTime = totalTravelTime,
                    isWheelchairAccessible = isWheelchairAccessible,
                    themeColor = themeColor,
                    transportModeLineList = transportModeLineList,
                    platformText = platformText,
                    totalWalkTime = totalWalkTime,
                    departureDeviation = departureDeviation,
                    modifier = Modifier
                        .semantics(mergeDescendants = true) { }
                        .clickable(
                            role = Role.Button,
                            onClick = onClick,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
                )

                JourneyCardState.EXPANDED -> ExpandedJourneyCardContent(
                    timeToDeparture = timeToDeparture,
                    themeColor = themeColor,
                    platformText = platformText,
                    totalTravelTime = totalTravelTime,
                    legList = legList,
                    totalUniqueServiceAlerts = totalUniqueServiceAlerts,
                    onAlertClick = onAlertClick,
                    onLegClick = onLegClick,
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ),
                )
            }
        }
    }
}

@Composable
fun ExpandedJourneyCardContent(
    timeToDeparture: String,
    themeColor: Color,
    platformText: String?,
    totalTravelTime: String,
    legList: ImmutableList<TimeTableState.JourneyCardInfo.Leg>,
    totalUniqueServiceAlerts: Int,
    onAlertClick: () -> Unit,
    onLegClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = if (platformText != null) Arrangement.SpaceBetween else Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = timeToDeparture,
                style = KrailTheme.typography.titleLarge,
                color = themeColor,
            )

            platformText?.let { text ->
                Text(
                    text = text,
                    style = KrailTheme.typography.titleLarge,
                    color = themeColor,
                    modifier = Modifier,
                )
            }
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = if (totalUniqueServiceAlerts > 0) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.End
            },
            verticalArrangement = Arrangement.spacedBy(4.dp),
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

            TextWithIcon(
                painter = painterResource(Res.drawable.ic_clock),
                text = totalTravelTime,
                textStyle = KrailTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
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
                                        lastLeg = legList[(index - 1).coerceAtLeast(0)]
                                    )
                                } else {
                                    0.dp
                                }
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
fun DefaultJourneyCardContent(
    timeToDeparture: String,
    originTime: String,
    destinationTime: String,
    totalTravelTime: String,
    isWheelchairAccessible: Boolean,
    themeColor: Color,
    transportModeLineList: ImmutableList<TransportModeLine>,
    platformText: String?,
    totalWalkTime: String?,
    modifier: Modifier = Modifier,
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation? = null,
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (displayAdaptiveTransportModeList(transportModeLineList)) {
                Text(
                    text = timeToDeparture,
                    style = KrailTheme.typography.titleMedium,
                    color = themeColor,
                    modifier = Modifier
                        .padding(end = 8.dp),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = timeToDeparture,
                        style = KrailTheme.typography.titleMedium,
                        color = themeColor,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .align(Alignment.CenterVertically),
                    )
                    TransportModesRow(
                        transportModeLineList = transportModeLineList,
                        showBadge = { it.transportMode is TransportMode.Bus },
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }

            platformText?.let { text ->
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    style = KrailTheme.typography.labelLarge,
                    modifier = Modifier,
                )
            }
        }

        if (displayAdaptiveTransportModeList(transportModeLineList)) {
            // Always show badges/icons on a new line for large font scale; let FlowRow wrap
            TransportModesRow(
                transportModeLineList = transportModeLineList,
                showBadge = { it.transportMode is TransportMode.Bus },
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Origin time + deviation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                text = originTime,
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = destinationTime,
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(end = 10.dp),
            )
            TextWithIcon(
                painter = painterResource(Res.drawable.ic_clock),
                text = totalTravelTime,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 10.dp),
            )
            totalWalkTime?.let {
                TextWithIcon(
                    painter = painterResource(Res.drawable.ic_walk),
                    text = totalWalkTime,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 10.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            departureDeviation?.let { deviation ->
                val (dotColor, label) = when (deviation) {
                    is TimeTableState.JourneyCardInfo.DepartureDeviation.Late ->
                        KrailTheme.colors.deviationLate to deviation.text

                    is TimeTableState.JourneyCardInfo.DepartureDeviation.Early ->
                        KrailTheme.colors.deviationEarly to deviation.text

                    TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime ->
                        KrailTheme.colors.deviationOnTime to "On time"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                color = dotColor.copy(alpha = LocalContentAlpha.current),
                                shape = CircleShape
                            ),
                    )
                    Text(
                        text = label,
                        style = KrailTheme.typography.bodyMedium,
                        color = KrailTheme.colors.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun displayAdaptiveTransportModeList(
    transportModeLineList: ImmutableList<TransportModeLine>
): Boolean = isLargeFontScale() || transportModeLineList.size > 2

@Composable
private fun TransportModesRow(
    transportModeLineList: ImmutableList<TransportModeLine>,
    showBadge: (TransportModeLine) -> Boolean,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        transportModeLineList.forEachIndexed { index, line ->
            if (showBadge(line)) {
                TransportModeBadge(
                    backgroundColor = line.lineColorCode.hexToComposeColor(),
                    badgeText = line.lineName,
                )
            }
            TransportModeIcon(
                transportMode = line.transportMode,
                size = TransportModeIconSize.Small,
            )
            if (index != transportModeLineList.lastIndex) {
                SeparatorIcon(modifier = Modifier.align(Alignment.CenterVertically))
            }
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
    val contentAlpha = LocalContentAlpha.current
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
            colorFilter = ColorFilter.tint(color = color.copy(alpha = contentAlpha)),
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

// toColors() now accepts onSurface color as a parameter
internal fun List<TransportMode>?.toColors(onSurface: Color): List<Color> = when {
    this.isNullOrEmpty() -> listOf(onSurface, onSurface)
    size >= 2 -> map { it.colorCode.hexToComposeColor() }
    else -> {
        val color = first().colorCode.hexToComposeColor()
        listOf(color, color)
    }
}

// region Previews

private val PREVIEW_STOPS = persistentListOf(
    TimeTableState.JourneyCardInfo.Stop(
        name = "Stop 1",
        time = "8:30am",
        isWheelchairAccessible = true,
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

@Preview(name = "Default - Inline modes + platform", group = "Collapsed")
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
            isWheelchairAccessible = true,
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus(), lineName = "700"),
                TransportModeLine(transportMode = TransportMode.Train(), lineName = "T1"),
                TransportModeLine(transportMode = TransportMode.Ferry(), lineName = "F2"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = "10 mins",
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Late("2 mins late"),
        )
    }
}

@Preview(name = "Default - Inline modes + platform (Dark)", group = "Collapsed")
@Composable
private fun Preview_Default_InlineModesAndPlatform_Dark() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE, darkTheme = true) {
        JourneyCard(
            timeToDeparture = "in 7 mins",
            originTime = "8:25am",
            destinationTime = "8:55am",
            totalTravelTime = "30 mins",
            platformNumber = "1",
            platformText = "Platform 1",
            isWheelchairAccessible = true,
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus(), lineName = "700"),
                TransportModeLine(transportMode = TransportMode.Train(), lineName = "T1"),
                TransportModeLine(transportMode = TransportMode.Ferry(), lineName = "F2"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = "10 mins",
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.OnTime,
        )
    }
}

@Preview(name = "Large font - Modes on next line", group = "Collapsed")
@Composable
private fun Preview_LargeFont_ModesNextLine() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE, fontScale = 2f) {
        JourneyCard(
            timeToDeparture = "in 12 mins",
            originTime = "9:00am",
            destinationTime = "9:45am",
            totalTravelTime = "45 mins",
            platformNumber = "A",
            platformText = "Stand A",
            isWheelchairAccessible = false,
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus(), lineName = "610X"),
                TransportModeLine(transportMode = TransportMode.Train(), lineName = "T9"),
                TransportModeLine(transportMode = TransportMode.LightRail(), lineName = "L1"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
            departureDeviation = TimeTableState.JourneyCardInfo.DepartureDeviation.Early("1 min early"),
        )
    }
}

@Preview(name = "Many modes - Wrapping", group = "Collapsed")
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
            isWheelchairAccessible = true,
            transportModeLineList = persistentListOf(
                TransportModeLine(transportMode = TransportMode.Bus(), lineName = "333"),
                TransportModeLine(transportMode = TransportMode.Bus(), lineName = "610X"),
                TransportModeLine(transportMode = TransportMode.Train(), lineName = "T1"),
                TransportModeLine(transportMode = TransportMode.Train(), lineName = "T8"),
                TransportModeLine(transportMode = TransportMode.Ferry(), lineName = "F1"),
                TransportModeLine(transportMode = TransportMode.Metro(), lineName = "M1"),
                TransportModeLine(transportMode = TransportMode.LightRail(), lineName = "L2"),
                TransportModeLine(transportMode = TransportMode.Coach(), lineName = "C1"),
            ),
            legList = persistentListOf(),
            cardState = JourneyCardState.DEFAULT,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 0,
            onClick = {},
        )
    }
}

@Preview(name = "Expanded - Journey card", group = "Expanded")
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
            isWheelchairAccessible = true,
            transportModeLineList = persistentListOf(
                TransportModeLine(
                    transportMode = TransportMode.Train(),
                    lineName = "T1",
                ),
                TransportModeLine(
                    transportMode = TransportMode.Bus(),
                    lineName = "700",
                ),
            ),
            legList = persistentListOf(
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS,
                    displayText = "towards Abc via Rainy Rd",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Train(),
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
                        transportMode = TransportMode.Bus(),
                        lineName = "700",
                    ),
                    tripId = "700",
                ),
            ),
            cardState = JourneyCardState.EXPANDED,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 1,
            onClick = {},
        )
    }
}

@Preview(name = "Expanded - Journey card (Dark)", group = "Expanded")
@Composable
private fun Preview_JourneyCard_Expanded_Dark() {
    PreviewTheme(themeStyle = DEFAULT_THEME_STYLE, darkTheme = true) {
        JourneyCard(
            timeToDeparture = "in 5 mins",
            originTime = "8:25am",
            destinationTime = "8:40am",
            totalTravelTime = "15 mins",
            platformNumber = "3",
            platformText = "Platform 3",
            isWheelchairAccessible = true,
            transportModeLineList = persistentListOf(
                TransportModeLine(
                    transportMode = TransportMode.Train(),
                    lineName = "T1",
                ),
                TransportModeLine(
                    transportMode = TransportMode.Bus(),
                    lineName = "700",
                ),
            ),
            legList = persistentListOf(
                TimeTableState.JourneyCardInfo.Leg.TransportLeg(
                    stops = PREVIEW_STOPS,
                    displayText = "towards Abc via Rainy Rd",
                    transportModeLine = TransportModeLine(
                        transportMode = TransportMode.Train(),
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
                        transportMode = TransportMode.Bus(),
                        lineName = "700",
                    ),
                    tripId = "700",
                ),
            ),
            cardState = JourneyCardState.EXPANDED,
            totalWalkTime = null,
            totalUniqueServiceAlerts = 1,
            onClick = {},
        )
    }
}

// endregion
