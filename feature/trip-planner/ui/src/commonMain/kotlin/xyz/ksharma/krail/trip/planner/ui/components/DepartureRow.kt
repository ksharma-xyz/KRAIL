@file:Suppress("TooManyFunctions", "StringLiteralDuplication")

package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.departures.ui.state.model.DepartureTiming
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.departureboard.toTransportMode

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
    val isPrevious = departure.timing == DepartureTiming.Previous
    val lineColor = remember(departure.lineColorCode) {
        departure.lineColorCode.hexToComposeColor()
    }
    val transportMode = remember(departure.transportModeName) {
        departure.transportModeName.toTransportMode()
    }

    CompositionLocalProvider(LocalContentAlpha provides if (isPrevious) 0.5f else 1f) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Line 1: relative time + mode icon + line badge (left) | platform text (right)
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (departure.relativeTimeText.isNotBlank()) {
                        Text(
                            text = departure.relativeTimeText,
                            style = KrailTheme.typography.titleMedium,
                            color = lineColor,
                        )
                    }

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
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }

                departure.platformText?.let {
                    Text(
                        text = it,
                        textAlign = TextAlign.End,
                        style = KrailTheme.typography.bodyMedium,
                        color = KrailTheme.colors.label,
                    )
                }
            }

            // Line 2: delay indicator (if delayed/early) + departure time
            DepartureTimeRow(
                departureTimeText = departure.departureTimeText,
                scheduledTimeText = departure.scheduledTimeText,
                delayMinutes = departure.delayMinutes,
            )

            // Line 3: destination in softLabel
            Text(
                text = departure.destinationName,
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
            )
        }
    }
}

/**
 * Shows departure time with optional delay indicator.
 * - On time: just the time in label color
 * - Delayed: strikethrough scheduled time + "Delayed X min" in deviationLate + actual time
 * - Early: strikethrough scheduled time + "Early X min" in deviationEarly + actual time
 */
@Composable
private fun DepartureTimeRow(
    departureTimeText: String,
    scheduledTimeText: String?,
    delayMinutes: Int,
    modifier: Modifier = Modifier,
) {
    if (scheduledTimeText != null && delayMinutes != 0) {
        val isDelayed = delayMinutes > 0
        val deviationColor = if (isDelayed) KrailTheme.colors.deviationLate else KrailTheme.colors.deviationEarly
        val deviationLabel = if (isDelayed) "Delayed $delayMinutes min" else "Early ${-delayMinutes} min"
        Column(modifier = modifier) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(scheduledTimeText)
                        }
                    },
                    style = KrailTheme.typography.bodyMedium,
                    color = KrailTheme.colors.softLabel,
                )
                Text(
                    text = deviationLabel,
                    style = KrailTheme.typography.bodyMedium,
                    color = deviationColor,
                )
            }
            Text(
                text = departureTimeText,
                style = KrailTheme.typography.titleMedium,
                color = KrailTheme.colors.label,
            )
        }
    } else {
        Text(
            text = departureTimeText,
            style = KrailTheme.typography.bodyMedium,
            color = KrailTheme.colors.label,
            modifier = modifier,
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
    val displayDepartures = remember(departures, maxItems) {
        if (maxItems != null) departures.take(maxItems) else departures
    }
    Column(modifier = modifier) {
        var lastDateLabel = ""
        displayDepartures.forEachIndexed { index, departure ->
            if (departure.dateLabel != lastDateLabel) {
                if (index > 0) Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    text = departure.dateLabel,
                    style = KrailTheme.typography.labelLarge,
                    color = KrailTheme.colors.softLabel,
                    modifier = Modifier
                        .semantics { heading() }
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                )
                lastDateLabel = departure.dateLabel
            }

            DepartureRow(departure = departure)

            if (index < displayDepartures.lastIndex) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
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
