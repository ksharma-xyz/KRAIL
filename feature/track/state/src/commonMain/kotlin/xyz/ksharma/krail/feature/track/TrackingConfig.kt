package xyz.ksharma.krail.feature.track

object TrackingConfig {
    const val POLL_INTERVAL_MS = 60_000L
    const val GTFS_RT_POLL_INTERVAL_MS = 30_000L
    const val MAX_TRACKED_TRIPS = 1

    /**
     * Minutes after the scheduled arrival before the trip is considered definitively finished.
     * Once this window elapses the app transitions to [TrackTripState.ArrivedAndFinished],
     * stops all polling, cleans up the tracking card, and navigates the user back.
     */
    const val ARRIVAL_FINISHED_MINUTES = 15L

    /**
     * Hours after the scheduled departure beyond which we never attempt an API call.
     * A trip this old can't be matched in the live feed; attempting wastes the user's time.
     */
    const val DEPARTURE_EXPIRED_HOURS = 2L

    /**
     * Enable per-digit flip animation on the countdown card. Set false for plain InfoTile text.
     * Remove flag and InfoTile fallback once flip animation is validated in production.
     */
    const val USE_FLIP_COUNTDOWN = true
}
