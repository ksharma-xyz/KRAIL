@file:Suppress("TooManyFunctions", "StringLiteralDuplication")

package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.departures.ui.state.model.DepartureTiming
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.departureboard.toTransportMode
import xyz.ksharma.krail.trip.planner.ui.pastDepartureColor

/**
 * Displays a single departure row:
 *
 * ```
 * in 5 mins  [B] [700]        Stand A   ← relative time + mode icon + line badge | platform
 * 11:30 am                              ← departure time (label color); or strikethrough + delay label
 * Schofields via ABC Road               ← destination (softLabel color)
 * ```
 */
@Composable
fun DepartureRow(
    departure: StopDeparture,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val isPast = departure.timing == DepartureTiming.Previous
    val lineColor = remember(departure.lineColorCode) {
        departure.lineColorCode.hexToComposeColor()
    }
    val transportMode = remember(departure.transportModeName) {
        departure.transportModeName.toTransportMode()
    }

    // Pre-compute deviation info so ScheduledTimeRow can be called with plain strings/colours.
    val hasDeviation = departure.scheduledTimeText != null && departure.delayMinutes != 0
    val deviationLabel: String? = if (hasDeviation) {
        if (departure.delayMinutes > 0) {
            "Delayed ${departure.delayMinutes} min"
        } else {
            "Early ${-departure.delayMinutes} min"
        }
    } else {
        null
    }
    val deviationColor: Color = if (hasDeviation) {
        if (departure.delayMinutes > 0) {
            KrailTheme.colors.deviationLate
        } else {
            KrailTheme.colors.deviationEarly
        }
    } else {
        Color.Unspecified
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isPast) {
                    Modifier.background(KrailTheme.colors.pastDepartureRowSurface)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXS),
    ) {
        // Line 1: relative time + mode icon + line badge (left) | platform text (right)
        DepartureHeaderRow(
            relativeTimeText = departure.relativeTimeText,
            isPast = isPast,
            activeTimeColor = lineColor,
            platformText = departure.platformText,
            // activePlatformColor defaults to KrailTheme.colors.label — correct for departure rows
        ) {
            transportMode?.let {
                TransportModeIcon(
                    transportMode = it,
                    size = TransportModeIconSize.XSmall,
                    displayBorder = false,
                )
            }
            TransportModeBadge(
                badgeText = departure.lineNumber,
                backgroundColor = lineColor,
                modifier = Modifier.padding(end = dim.spacingM),
            )
        }

        // Line 2: scheduled vs actual departure time (shared composable, handles deviation row)
        ScheduledTimeRow(
            timeText = departure.departureTimeText,
            isPast = isPast,
            scheduledTimeText = if (hasDeviation) departure.scheduledTimeText else null,
            deviationLabel = deviationLabel,
            deviationColor = deviationColor,
            onTimeTextStyle = KrailTheme.typography.bodyMedium,
        )

        // Line 3: destination
        Text(
            text = departure.destinationName,
            style = KrailTheme.typography.bodyMedium,
            color = pastDepartureColor(isPast, KrailTheme.colors.softLabel),
        )
    }
}

/**
 * Renders departure rows with Taj dividers between them, and a date section header
 * whenever the departure date changes (e.g. Today → Tomorrow → Wed 9 Apr).
 * Uses a plain [Column] (not LazyColumn) so it can be safely embedded
 * inside an outer vertically-scrollable container like a bottom sheet.
 */
@Composable
fun DepartureRowList(
    departures: ImmutableList<StopDeparture>,
    modifier: Modifier = Modifier,
    maxItems: Int? = null,
) {
    val dim = KrailTheme.dimensions
    val displayDepartures = remember(departures, maxItems) {
        if (maxItems != null) departures.take(maxItems) else departures
    }
    Column(modifier = modifier) {
        var lastDateLabel = ""
        displayDepartures.forEachIndexed { index, departure ->
            if (departure.dateLabel != lastDateLabel) {
                if (index > 0) Divider(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))
                Text(
                    text = departure.dateLabel,
                    style = KrailTheme.typography.titleMedium,
                    color = KrailTheme.colors.onSurface,
                    modifier = Modifier
                        .semantics { heading() }
                        .padding(horizontal = dim.pageHorizontalPadding)
                        .padding(top = dim.pageVerticalPadding, bottom = dim.spacingM),
                )
                lastDateLabel = departure.dateLabel
            }

            DepartureRow(departure = departure)

            if (index < displayDepartures.lastIndex) {
                Divider(modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding))
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private const val TRANSPORT_MODE_TRAIN = "Train"
private const val TRANSPORT_MODE_BUS = "Bus"
private const val TRANSPORT_MODE_FERRY = "Ferry"
private const val BUS_LINE_700 = "700"
private const val BUS_COLOR = "#00B5EF"
private const val BUS_DESTINATION = "Schofields via ABC Road"
private const val BUS_PLATFORM = "Stand A"
private const val BUS_DEPARTURE_TIME = "11:30 am"
private const val BUS_UTC_DATE = "2026-04-10T01:30:00Z"
private const val RELATIVE_IN_5 = "in 5 mins"

private val sampleDepartures = persistentListOf(
    // On-time bus
    StopDeparture(
        lineNumber = BUS_LINE_700,
        lineColorCode = BUS_COLOR,
        transportModeName = TRANSPORT_MODE_BUS,
        destinationName = BUS_DESTINATION,
        departureTimeText = BUS_DEPARTURE_TIME,
        departureUtcDateTime = BUS_UTC_DATE,
        relativeTimeText = "in 100005 mins",
        platformText = BUS_PLATFORM,
        isRealTime = true,
    ),
    // Delayed bus
    StopDeparture(
        lineNumber = BUS_LINE_700,
        lineColorCode = BUS_COLOR,
        transportModeName = TRANSPORT_MODE_BUS,
        destinationName = BUS_DESTINATION,
        departureTimeText = BUS_DEPARTURE_TIME,
        departureUtcDateTime = BUS_UTC_DATE,
        relativeTimeText = RELATIVE_IN_5,
        platformText = BUS_PLATFORM,
        isRealTime = true,
        scheduledTimeText = "11:28 am",
        delayMinutes = 2,
    ),
    // Early bus
    StopDeparture(
        lineNumber = BUS_LINE_700,
        lineColorCode = BUS_COLOR,
        transportModeName = TRANSPORT_MODE_BUS,
        destinationName = BUS_DESTINATION,
        departureTimeText = "11:28 am",
        departureUtcDateTime = "2026-01-7T01:28:00Z",
        relativeTimeText = RELATIVE_IN_5,
        platformText = BUS_PLATFORM,
        isRealTime = true,
        scheduledTimeText = BUS_DEPARTURE_TIME,
        delayMinutes = -2,
    ),
    StopDeparture(
        lineNumber = "T1",
        lineColorCode = "#F99D1C",
        transportModeName = TRANSPORT_MODE_TRAIN,
        destinationName = "Liverpool via Strathfield",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "2026-04-10T01:30:00Z",
        relativeTimeText = "in 5 mins",
        platformText = "Platform 4",
        isRealTime = true,
    ),
    StopDeparture(
        lineNumber = "F1",
        lineColorCode = "#00774B",
        transportModeName = TRANSPORT_MODE_FERRY,
        destinationName = "Manly Wharf",
        departureTimeText = "11:40 AM",
        departureUtcDateTime = "2026-04-10T01:40:00Z",
        relativeTimeText = "in 5 mins",
        platformText = null,
        isRealTime = false,
    ),
)

@PreviewComponent
@Composable
private fun DepartureRowBusOnTimePreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        DepartureRow(departure = sampleDepartures[0])
    }
}

@Preview
@Composable
private fun DepartureRowBusDelayedPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        DepartureRow(departure = sampleDepartures[1])
    }
}

@Preview
@Composable
private fun DepartureRowBusEarlyPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        DepartureRow(departure = sampleDepartures[2])
    }
}

@Preview
@Composable
private fun DepartureRowTrainPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureRow(departure = sampleDepartures[3])
    }
}

@Preview
@Composable
private fun DepartureRowFerryNoPlatformPreview() {
    PreviewTheme(KrailThemeStyle.Ferry) {
        DepartureRow(departure = sampleDepartures[4])
    }
}

@PreviewComponent
@Composable
private fun DepartureRowListBusPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        Column {
            DepartureRowList(departures = sampleDepartures)
        }
    }
}

@Preview
@Composable
private fun DepartureRowListMetroPreview() {
    PreviewTheme(KrailThemeStyle.Metro) {
        Column {
            DepartureRowList(departures = sampleDepartures)
        }
    }
}

@PreviewComponent
@Composable
private fun DepartureRowPreviousTrainPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        DepartureRow(
            departure = StopDeparture(
                lineNumber = "T1",
                lineColorCode = "#F99D1C",
                transportModeName = TRANSPORT_MODE_TRAIN,
                destinationName = "Liverpool via Strathfield",
                departureTimeText = "11:30 AM",
                departureUtcDateTime = "2026-04-10T01:30:00Z",
                relativeTimeText = "5 mins ago",
                platformText = "Platform 4",
                isRealTime = true,
                timing = DepartureTiming.Previous,
            ),
        )
    }
}

@Preview
@Composable
private fun DepartureRowPreviousBusDelayedPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        DepartureRow(
            departure = StopDeparture(
                lineNumber = BUS_LINE_700,
                lineColorCode = BUS_COLOR,
                transportModeName = TRANSPORT_MODE_BUS,
                destinationName = BUS_DESTINATION,
                departureTimeText = BUS_DEPARTURE_TIME,
                departureUtcDateTime = BUS_UTC_DATE,
                relativeTimeText = "12 mins ago",
                platformText = BUS_PLATFORM,
                isRealTime = true,
                scheduledTimeText = "11:28 am",
                delayMinutes = 2,
                timing = DepartureTiming.Previous,
            ),
        )
    }
}

@PreviewComponent
@Composable
private fun DepartureRowListWithPreviousPreview() {
    PreviewTheme(KrailThemeStyle.Train) {
        Column {
            DepartureRowList(
                departures = persistentListOf(
                    StopDeparture(
                        lineNumber = "T1",
                        lineColorCode = "#F99D1C",
                        transportModeName = TRANSPORT_MODE_TRAIN,
                        destinationName = "Liverpool via Strathfield",
                        departureTimeText = "11:20 AM",
                        departureUtcDateTime = "2026-04-10T01:20:00Z",
                        relativeTimeText = "10 mins ago",
                        platformText = "Platform 4",
                        isRealTime = true,
                        dateLabel = "Today",
                        timing = DepartureTiming.Previous,
                    ),
                    StopDeparture(
                        lineNumber = "T2",
                        lineColorCode = "#0098CD",
                        transportModeName = TRANSPORT_MODE_TRAIN,
                        destinationName = "Bondi Junction",
                        departureTimeText = "11:25 AM",
                        departureUtcDateTime = "2026-04-10T01:25:00Z",
                        relativeTimeText = "5 mins ago",
                        platformText = "Platform 7",
                        isRealTime = false,
                        dateLabel = "Today",
                        timing = DepartureTiming.Previous,
                    ),
                    StopDeparture(
                        lineNumber = "T1",
                        lineColorCode = "#F99D1C",
                        transportModeName = TRANSPORT_MODE_TRAIN,
                        destinationName = "Liverpool via Strathfield",
                        departureTimeText = "11:30 AM",
                        departureUtcDateTime = "2026-04-10T01:30:00Z",
                        relativeTimeText = "in 2 mins",
                        platformText = "Platform 4",
                        isRealTime = true,
                        dateLabel = "Today",
                        timing = DepartureTiming.Upcoming,
                    ),
                    StopDeparture(
                        lineNumber = "T4",
                        lineColorCode = "#005AA3",
                        transportModeName = TRANSPORT_MODE_TRAIN,
                        destinationName = "Illawarra via Sydenham",
                        departureTimeText = "11:36 AM",
                        departureUtcDateTime = "2026-04-10T01:36:00Z",
                        relativeTimeText = "in 8 mins",
                        platformText = "Platform 2",
                        isRealTime = true,
                        dateLabel = "Today",
                        timing = DepartureTiming.Upcoming,
                    ),
                ),
            )
        }
    }
}

@PreviewComponent
@Composable
private fun DepartureRowListMultiDayPreview() {
    PreviewTheme(KrailThemeStyle.Bus) {
        Column {
            DepartureRowList(
                departures = persistentListOf(
                    StopDeparture(
                        lineNumber = "700",
                        lineColorCode = "#00B5EF",
                        transportModeName = TRANSPORT_MODE_BUS,
                        destinationName = "Schofields via ABC Road",
                        departureTimeText = "11:30 pm",
                        departureUtcDateTime = "2026-04-10T13:30:00Z",
                        relativeTimeText = "in 5 mins",
                        platformText = "Stand A",
                        isRealTime = true,
                        dateLabel = "Today",
                    ),
                    StopDeparture(
                        lineNumber = "700",
                        lineColorCode = "#00B5EF",
                        transportModeName = TRANSPORT_MODE_BUS,
                        destinationName = "Schofields via ABC Road",
                        departureTimeText = "11:55 pm",
                        departureUtcDateTime = "2026-04-10T13:55:00Z",
                        relativeTimeText = "in 30 mins",
                        platformText = "Stand A",
                        isRealTime = true,
                        dateLabel = "Today",
                    ),
                    StopDeparture(
                        lineNumber = "700",
                        lineColorCode = "#00B5EF",
                        transportModeName = TRANSPORT_MODE_BUS,
                        destinationName = "Schofields via ABC Road",
                        departureTimeText = "12:10 am",
                        departureUtcDateTime = "2026-04-11T02:10:00Z",
                        relativeTimeText = "in 1 hr",
                        platformText = "Stand A",
                        isRealTime = false,
                        dateLabel = "Tomorrow",
                    ),
                ),
            )
        }
    }
}
