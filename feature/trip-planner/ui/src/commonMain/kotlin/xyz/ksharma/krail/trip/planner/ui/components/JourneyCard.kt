package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.AlertButton
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Divider
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
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation? = null,
) {
    // Derive transport modes for styling and colors
    val transportModeList: ImmutableList<TransportMode> = remember(transportModeLineList) {
        transportModeLineList.map { it.transportMode }.toImmutableList()
    }

    val isPastJourney by remember(timeToDeparture) {
        mutableStateOf(
            timeToDeparture.contains(other = "ago", ignoreCase = true),
        )
    }
    val firstLegTransportModeColor = if (!isPastJourney) {
        transportModeList.firstOrNull()?.colorCode?.hexToComposeColor()
            ?: KrailTheme.colors.onSurface
    } else {
        KrailTheme.colors.onSurface
    }

    CompositionLocalProvider(LocalContentAlpha provides if (isPastJourney) 0.5f else 1f) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(color = KrailTheme.colors.surface)
                .animateContentSize(),
        ) {
            JourneyCardHeader(
                transportModeLineList = transportModeLineList,
                platformText = platformText,
                timeToDeparture = timeToDeparture,
                firstLegTransportModeColor = firstLegTransportModeColor,
                onClick = onClick,
            )

            when (cardState) {
                JourneyCardState.DEFAULT -> DefaultJourneyCardContent(
                    originTime = originTime,
                    destinationTime = destinationTime,
                    totalTravelTime = totalTravelTime,
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

            Divider(modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun JourneyCardHeader(
    transportModeLineList: ImmutableList<TransportModeLine>,
    platformText: String?,
    timeToDeparture: String,
    firstLegTransportModeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(
                role = Role.Button,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .padding(bottom = 4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (displayAdaptiveTransportModeList(transportModeLineList, platformText)) {
                Text(
                    text = timeToDeparture,
                    style = KrailTheme.typography.titleMedium,
                    color = firstLegTransportModeColor,
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
                        color = firstLegTransportModeColor,
                        modifier = Modifier
                            .padding(end = 8.dp),
                    )
                    TransportModesRow(
                        transportModeLineList = transportModeLineList,
                        showBadge = { it.transportMode is TransportMode.Bus },
                    )
                }
            }

            platformText?.let { text ->
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    style = KrailTheme.typography.titleMedium,
                    color = firstLegTransportModeColor,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        if (displayAdaptiveTransportModeList(transportModeLineList, platformText)) {
            // Always show badges/icons on a new line for large font scale; let FlowRow wrap
            TransportModesRow(
                transportModeLineList = transportModeLineList,
                showBadge = { it.transportMode is TransportMode.Bus },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DepartureDeviationIndicator(
    deviation: TimeTableState.JourneyCardInfo.DepartureDeviation,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    color = dotColor.copy(alpha = LocalContentAlpha.current),
                    shape = CircleShape,
                ),
        )
        Text(
            text = label,
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.onSurface,
        )
    }
}

@Composable
fun ExpandedJourneyCardContent(
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
fun DefaultJourneyCardContent(
    originTime: String,
    destinationTime: String,
    totalTravelTime: String,
    totalWalkTime: String?,
    modifier: Modifier = Modifier,
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation? = null,
) {
    Column(modifier = modifier) {
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

        // Responsive destination + group row
        ResponsiveJourneyInfoRow(
            destinationTime = destinationTime,
            totalTravelTime = totalTravelTime,
            totalWalkTime = totalWalkTime,
            departureDeviation = departureDeviation,
        )
    }
}

@Composable
private fun ResponsiveJourneyInfoRow(
    destinationTime: String,
    totalTravelTime: String,
    totalWalkTime: String?,
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation?,
) {
    // Custom layout to implement the rules:
    // 1) destination always at start of its row.
    // 2) deviation always at end of its row.
    // 3) clock and walk stay adjacent as a unit.
    // 4) Layout modes:
    //    a) One row if all fit: dest | clock+walk ... deviation
    //    b) Two rows default: Row1 dest; Row2 clock+walk | ... deviation
    //    c) Fallback: if Row2 doesn't fit clock+walk+deviation, move clock to Row1 (dest+clock);
    //                 Row2 has walk at start and deviation at end.
    SubcomposeLayout { constraints ->
        // Measure destination
        val destMeas = subcompose("dest") {
            Text(
                text = destinationTime,
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.onSurface,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        val destPl = destMeas.map { it.measure(constraints) }
        val destW = destPl.sumOf { it.width }
        val destH = destPl.maxOfOrNull { it.height } ?: 0

        // Measure clock (always present)
        val clockMeas = subcompose("clock") {
            TextWithIcon(
                painter = painterResource(Res.drawable.ic_clock),
                text = totalTravelTime,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        val clockPl = clockMeas.map { it.measure(constraints) }
        val clockW = clockPl.maxOfOrNull { it.width } ?: 0
        val clockH = clockPl.maxOfOrNull { it.height } ?: 0

        // Measure walk (optional)
        val walkMeas = subcompose("walk") {
            if (totalWalkTime != null) {
                TextWithIcon(
                    painter = painterResource(Res.drawable.ic_walk),
                    text = totalWalkTime,
                    modifier = Modifier.padding(end = 10.dp),
                )
            } else {
                Spacer(Modifier.size(0.dp))
            }
        }
        val walkPl = walkMeas.map { it.measure(constraints) }
        val walkW = walkPl.maxOfOrNull { it.width } ?: 0
        val walkH = walkPl.maxOfOrNull { it.height } ?: 0

        // Measure deviation (optional)
        val devMeas = subcompose("dev") {
            if (departureDeviation != null) {
                DepartureDeviationIndicator(deviation = departureDeviation)
            } else {
                Spacer(Modifier.size(0.dp))
            }
        }
        val devPl = devMeas.map { it.measure(constraints) }
        val devW = devPl.maxOfOrNull { it.width } ?: 0
        val devH = devPl.maxOfOrNull { it.height } ?: 0

        val maxW = constraints.maxWidth

        // Try 1: All in one row (dest + clock + walk) with deviation right-aligned
        val spaceBetween = maxW - destW - devW
        val oneRowFits = (clockW + walkW) <= spaceBetween

        // Try 2: Two rows default (Row1: dest) (Row2: clock + walk start, deviation end)
        val secondRowSpace = maxW - devW
        val twoRowDefaultFits = (clockW + walkW) <= secondRowSpace

        // Try 3: Split clock up (Row1: dest + clock) (Row2: walk start, deviation end)
        val firstRowSplitFits = (destW + clockW) <= maxW
        val secondRowSplitFits = walkW <= secondRowSpace
        val splitClockUp =
            !oneRowFits && !twoRowDefaultFits && firstRowSplitFits && secondRowSplitFits

        val layoutHeight = when {
            oneRowFits -> maxOf(destH, clockH, walkH, devH)
            splitClockUp -> destH + maxOf(walkH, devH)
            else -> destH + maxOf(clockH, walkH, devH)
        }

        layout(width = maxW, height = layoutHeight) {
            if (oneRowFits) {
                // Row 1 only: dest at start, deviation at end, clock+walk in between
                var x = 0
                destPl.forEach { p ->
                    p.placeRelative(x, 0)
                    x += p.width
                }
                val devX = maxW - devW
                // Place clock, then walk, without crossing into deviation
                var midX = x
                clockPl.forEach { p ->
                    p.placeRelative(midX, 0)
                    midX += p.width
                }
                walkPl.forEach { p ->
                    p.placeRelative(midX, 0)
                    midX += p.width
                }
                devPl.forEach { p -> p.placeRelative(devX, 0) }
            } else if (splitClockUp) {
                // Row 1: dest + clock
                var x = 0
                destPl.forEach { p ->
                    p.placeRelative(x, 0)
                    x += p.width
                }
                clockPl.forEach { p ->
                    p.placeRelative(x, 0)
                }
                // Row 2: walk start, deviation end
                val y2 = destH
                var x2 = 0
                walkPl.forEach { p ->
                    p.placeRelative(x2, y2)
                    x2 += p.width
                }
                val devX = maxW - devW
                devPl.forEach { p -> p.placeRelative(devX, y2) }
            } else {
                // Default two rows
                // Row 1: destination only
                destPl.forEach { p -> p.placeRelative(0, 0) }
                // Row 2: clock + walk at start, deviation at end
                val y2 = destH
                var x2 = 0
                clockPl.forEach { p ->
                    p.placeRelative(x2, y2)
                    x2 += p.width
                }
                walkPl.forEach { p ->
                    p.placeRelative(x2, y2)
                    x2 += p.width
                }
                val devX = maxW - devW
                devPl.forEach { p -> p.placeRelative(devX, y2) }
            }
        }
    }
}

@Composable
private fun displayAdaptiveTransportModeList(
    transportModeLineList: ImmutableList<TransportModeLine>,
    platformText: String?,
): Boolean = isLargeFontScale() || transportModeLineList.size > 2 && platformText != null

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
