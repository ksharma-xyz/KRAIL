package xyz.ksharma.krail.trip.planner.ui.timetable

import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val AEST_TIMEZONE = "Australia/Sydney"

internal fun Analytics.trackJourneyCardExpandEvent(hasStarted: Boolean) {
    track(AnalyticsEvent.JourneyCardExpandEvent(hasStarted = hasStarted))
}

internal fun Analytics.trackJourneyCardCollapseEvent(hasStarted: Boolean) {
    track(AnalyticsEvent.JourneyCardCollapseEvent(hasStarted = hasStarted))
}

/**
 * Tracks the share journey button tap with type-safe inputs.
 *
 * Accepts strongly-typed [Instant] and [Duration] so the formatted strings are guaranteed
 * to be in the correct format — no raw UI display strings are trusted as input.
 *
 * @param transportModeLines The full list of [TransportModeLine] from [JourneyCardInfo].
 *                           Mode names and line IDs are derived here, not passed as strings.
 * @param legCount           Total legs from [JourneyCardInfo.legs].size.
 * @param originInstant      Parsed from [JourneyCardInfo.originUtcDateTime] — typed UTC [Instant].
 *                           Formatted to `"8:25 AM"` (AEST, 12-hour) inside this function.
 * @param travelDuration     Computed as `destinationInstant - originInstant` — typed [Duration].
 *                           Formatted to `"30 mins"` or `"1h 5m"` inside this function.
 * @param isPastDeparture    [JourneyCardInfo.hasJourneyStarted] at share time.
 */
@OptIn(ExperimentalTime::class)
internal fun Analytics.trackShareJourneyClickEvent(
    transportModeLines: ImmutableList<TransportModeLine>,
    legCount: Int,
    originInstant: Instant,
    travelDuration: Duration,
    isPastDeparture: Boolean,
) {
    track(
        AnalyticsEvent.ShareJourneyClickEvent(
            transportModes = transportModeLines.joinToString(",") { it.transportMode.name },
            lines = transportModeLines.joinToString(",") { it.lineName },
            legCount = legCount,
            // Instant → AEST LocalDateTime → "8:25 AM" — format is guaranteed by toHHMM()
            originTime = originInstant
                .toLocalDateTime(TimeZone.of(AEST_TIMEZONE))
                .toHHMM(),
            // Duration → "30 mins" / "1h 5m" — format guaranteed by toFormattedDurationTimeString()
            totalTravelTime = travelDuration.toFormattedDurationTimeString(),
            isPastDeparture = isPastDeparture,
        ),
    )
}
