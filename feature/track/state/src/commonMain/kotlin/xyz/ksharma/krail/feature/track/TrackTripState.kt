package xyz.ksharma.krail.feature.track

sealed interface TrackTripState {

    /** Initial state: show journey info and ask user to confirm tracking. */
    data class Prompt(val deepLink: TripDeepLink) : TrackTripState

    data object Initial : TrackTripState

    /** First API call in progress after user confirms. Carries route info so UI can show origin/destination header. */
    data class Loading(val deepLink: TripDeepLink? = null, val emoji: String = "\uD83D\uDE86") : TrackTripState

    /**
     * Actively polling. [isRefreshing] is true during the live API call.
     *
     * The [journey] reflects the most recent API response. The number of legs in
     * [TrackedJourneyDisplay.legs] can change between polls if TfNSW issues a real-time
     * service alteration — for example, a train that was originally a direct service may
     * temporarily appear as a multi-leg journey requiring an interchange if the service is
     * curtailed at an intermediate station. This is accurate live data and is displayed as-is.
     */
    data class Tracking(
        val journey: TrackedJourneyDisplay,
        val isRefreshing: Boolean = false,
    ) : TrackTripState

    /** Arrival time has passed. Displays last known data; no more API calls. */
    data class Arrived(val journey: TrackedJourneyDisplay) : TrackTripState

    /**
     * Trip is definitively over — either [TrackingConfig.ARRIVAL_FINISHED_MINUTES] past arrival
     * or [TrackingConfig.DEPARTURE_EXPIRED_HOURS] past departure. No API call is made.
     * The screen auto-navigates back and tracking is cleaned up immediately.
     */
    data object ArrivedAndFinished : TrackTripState

    /**
     * Journey not found in the live API response — service may be cancelled, rescheduled,
     * or severely disrupted to the point where TfNSW drops it from the response entirely.
     * Only shown for trips within the active tracking window.
     *
     * Note: a partial disruption (e.g. train curtailed mid-route, requiring an interchange)
     * does NOT produce this state — the API still returns the journey but restructures it
     * into multiple legs. That case stays in [Tracking] and displays the updated leg count.
     */
    data object NotFound : TrackTripState

    /** Network or parsing error during a live API call within the active tracking window. */
    data object Error : TrackTripState

    /**
     * User opened a new deep link but [TrackingConfig.MAX_TRACKED_TRIPS] is already reached.
     * Show a notice so user can stop the current one before starting another.
     */
    data class AlreadyTracking(
        val currentDeepLink: TripDeepLink,
        val requestedDeepLink: TripDeepLink,
    ) : TrackTripState
}
