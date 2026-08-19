package xyz.ksharma.krail.feature.track.ui

import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.feature.track.TrackTripState
import xyz.ksharma.krail.feature.track.TrackedJourneyDisplay
import xyz.ksharma.krail.feature.track.TrackedLeg
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The pure half of [TripPoller]: reading instants off a journey, turning a duration into the
 * countdown card's (label, value) pair, and naming the states the poll loop stops on.
 *
 * These live outside [TripPoller] because they are decisions about values, not about the
 * poller's state, and because keeping them here is what keeps `computeCountdown` and
 * `startPolling` under detekt's cyclomatic threshold — a local function would not, since
 * detekt counts branches inside one toward its enclosing function.
 */

/** Empty label and value: the countdown card renders nothing. */
internal val EMPTY_COUNTDOWN = "" to ""

@OptIn(ExperimentalTime::class)
internal fun TrackedJourneyDisplay.originInstantOrNull(): Instant? =
    runCatching { Instant.parse(originUtcDateTime) }.getOrNull()

/**
 * Arrival instant, preferring the last transport stop's own time.
 *
 * That is the identical source `TrackedLegView` renders from, so the countdown card and the
 * timeline can never disagree. Falls back to the journey's destination time for a cached or
 * restored journey whose legs pre-date the per-stop times.
 */
@OptIn(ExperimentalTime::class)
internal fun TrackedJourneyDisplay.destinationInstantOrNull(): Instant? {
    val lastTransportStopUtc = (legs.lastOrNull { it is TrackedLeg.Transport } as? TrackedLeg.Transport)
        ?.stops
        ?.lastOrNull()
        ?.utcTime
    return lastTransportStopUtc?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?: runCatching { Instant.parse(destinationUtcDateTime) }.getOrNull()
}

/**
 * A non-negative [duration] as the countdown card shows it: `45s`, `3m 20s`, `12m`, then the
 * app's generic relative string once an hour is in play.
 */
internal fun shortCountdownValue(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    return when {
        totalSeconds < SECONDS_PER_MINUTE -> "${totalSeconds}s"
        totalSeconds < SECONDS_PER_HOUR -> {
            val minutes = totalSeconds / SECONDS_PER_MINUTE
            val seconds = totalSeconds % SECONDS_PER_MINUTE
            if (seconds > 0L) "${minutes}m ${seconds}s" else "${minutes}m"
        }

        else -> duration.toGenericFormattedTimeString()
    }
}

/**
 * Label and value once the trip has departed, where [duration] is the time left until arrival
 * (negative once it has passed).
 *
 * [hasPassedArrivalMoment] is [TripPoller]'s one-way latch: once the journey has been seen to
 * arrive, a later estimate pushing arrival back into the future must not reopen the countdown.
 */
internal fun arrivedOrArrivingLabel(
    duration: Duration,
    hasPassedArrivalMoment: Boolean,
): Pair<String, String> {
    val totalSeconds = duration.inWholeSeconds
    return when {
        hasPassedArrivalMoment && totalSeconds > 0L -> LABEL_ARRIVED to LABEL_JUST_NOW
        totalSeconds <= -SECONDS_PER_MINUTE -> LABEL_ARRIVED to duration.toGenericFormattedTimeString()
        totalSeconds < 0L -> LABEL_ARRIVED to LABEL_JUST_NOW
        else -> LABEL_ARRIVING_IN to shortCountdownValue(duration)
    }
}

/**
 * Why the trip poll loop should stop, or `null` to keep polling.
 *
 * Anything that is not [TrackTripState.Tracking] is terminal: the journey has arrived, has
 * finished, or was never found, and none of those get better by polling again.
 */
internal fun pollLoopExitReason(state: TrackTripState): String? = when (state) {
    is TrackTripState.Tracking -> null
    is TrackTripState.Arrived -> "Arrived"
    TrackTripState.ArrivedAndFinished -> "ArrivedAndFinished"
    else -> state::class.simpleName ?: "unknown state"
}

internal const val SECONDS_PER_MINUTE = 60L
internal const val SECONDS_PER_HOUR = 3600L
internal const val LABEL_ARRIVED = "Arrived"
internal const val LABEL_JUST_NOW = "just now"
internal const val LABEL_ARRIVING_IN = "Arriving in"
internal const val LABEL_DEPARTING_IN = "Departing in"
