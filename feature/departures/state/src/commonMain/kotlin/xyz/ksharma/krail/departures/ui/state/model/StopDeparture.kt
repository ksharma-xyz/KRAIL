package xyz.ksharma.krail.departures.ui.state.model

import androidx.compose.runtime.Stable

/**
 * A single upcoming departure from a stop, ready for display in the UI.
 *
 * This is a lean, UI-focused model derived from [DepartureMonitorResponse.StopEvent].
 * Only the fields required to render a departure row (similar to a leg view) are kept here.
 *
 * Display guideline:
 *  - Show [lineNumber] in a coloured badge using [lineColorCode].
 *  - Show [destinationName] as the main label (e.g. "towards Liverpool").
 *  - Show [departureTimeText] as the secondary label (e.g. "11:30 pm").
 *  - Show [platformText] when non-null (e.g. "Platform 1", "Stand A").
 *  - Use [isRealTime] to optionally show a real-time indicator.
 *
 * Time handling:
 *  - [departureUtcDateTime] is an ISO-8601 string used for relative time calculations
 *    (e.g. "in 5 mins"). Prefer [departureTimeEstimatedUtc] when non-null.
 *  - [departureTimeText] is a pre-formatted local time string for direct display.
 */
@Stable
data class StopDeparture(

    /**
     * Short line identifier shown inside the coloured badge.
     * Examples: "T1", "333", "F1", "L2", "M".
     */
    val lineNumber: String,

    /**
     * Hex colour code for the line badge background.
     * Derived from the NSW Transport line colour palette.
     * Example: "#F99D1C" for T1 North Shore / Western line.
     */
    val lineColorCode: String,

    /**
     * Human-readable transport mode name.
     * Examples: "Train", "Bus", "Ferry", "Metro", "Light Rail", "Coach".
     */
    val transportModeName: String,

    /**
     * The final destination / terminus of the service.
     * Shown as the primary departure label, e.g. "Liverpool" or "Central".
     */
    val destinationName: String,

    /**
     * Pre-formatted local departure time string for display, e.g. "11:30 pm".
     * Derived from [departureUtcDateTime] using the device locale / timezone.
     */
    val departureTimeText: String,

    /**
     * ISO-8601 UTC departure time used for relative-time calculations ("in X mins").
     * This is the estimated time when real-time data is available, otherwise planned.
     * Kept so the ViewModel can periodically recompute [relativeTimeText].
     */
    val departureUtcDateTime: String,

    /**
     * Pre-computed relative time string for display, e.g. "in 5 mins".
     * Computed by the ViewModel and refreshed periodically — the composable must
     * use this directly and must NOT perform any time calculations itself.
     */
    val relativeTimeText: String = "",

    /**
     * Platform or stand label when available, e.g. "Platform 1", "Stand A".
     * Null when the stop does not have platform-level information.
     */
    val platformText: String? = null,

    /**
     * True when [departureUtcDateTime] is sourced from a real-time estimate rather
     * than the scheduled time. Use to show a real-time indicator in the UI.
     */
    val isRealTime: Boolean = false,

    /**
     * The original scheduled (planned) departure time as a formatted local string,
     * e.g. "11:28 am". Non-null only when [isRealTime] is true and the estimated
     * time differs from the planned time. Used to show a strikethrough original time
     * alongside a "Delayed X min" or "Early X min" label.
     */
    val scheduledTimeText: String? = null,

    /**
     * Delay in whole minutes relative to the planned departure.
     * Positive = delayed, negative = early, 0 = on time.
     * Only meaningful when [isRealTime] is true.
     */
    val delayMinutes: Int = 0,

    /**
     * Pre-computed date section label for grouping departures by day.
     * Values: "Today", "Tomorrow", or a short date like "Wed 9 Apr".
     * Computed by the mapper so the composable never calls date APIs directly.
     */
    val dateLabel: String = "",

    /**
     * Stable unique key for this departure, built from [transportation.id] (route+direction+period)
     * combined with [departureTimePlanned]. The API's [transportation.id] alone is shared by all
     * services on the same route variant; appending the planned time makes it unique per run.
     * Null when either field is missing from the API response.
     */
    val tripId: String? = null,
)
