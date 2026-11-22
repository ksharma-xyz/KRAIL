package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SaveTripClickEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * Everything the Intro writes or reports once it finishes. Kept apart from the view model so the
 * view model decides the order and this class does the work, and so each piece is one call.
 */
internal class IntroCompletion(
    private val sandook: Sandook,
    private val preferences: SandookPreferences,
    private val analytics: Analytics,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /** Saved with the same id as every other path, [Trip.tripId]. */
    suspend fun saveTrip(trip: Trip) = withContext(ioDispatcher) {
        sandook.insertOrReplaceTrip(
            tripId = trip.tripId,
            fromStopId = trip.fromStopId,
            fromStopName = trip.fromStopName,
            toStopId = trip.toStopId,
            toStopName = trip.toStopName,
        )
    }

    suspend fun writeSuggestionShown() = withContext(ioDispatcher) {
        preferences.setBoolean(SandookPreferences.KEY_PARK_RIDE_SUGGESTION_SHOWN, true)
    }

    suspend fun writeIntroSeen() = withContext(ioDispatcher) {
        preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO, true)
    }

    /** The `view_screen` event for the step the rider is looking at. */
    fun trackStepView(step: IntroStep) {
        analytics.trackScreenViewEvent(step.toAnalyticsScreen())
    }

    /**
     * Stop ids only, never a name or a coordinate. [savedTrip] is the trip this finish wrote, or
     * null when it wrote none, and only a trip written now counts as a save from the Intro.
     */
    fun track(outcome: IntroOutcome, savedTrip: Trip?) {
        savedTrip?.let {
            analytics.track(
                SaveTripClickEvent(
                    fromStopId = it.fromStopId,
                    toStopId = it.toStopId,
                    source = SaveTripClickEvent.SOURCE_INTRO,
                ),
            )
        }
        analytics.track(outcome.toEvent())
    }
}

/** Step 1 is the Intro screen and step 2 is its own, so the funnel reads from the two. */
internal fun IntroStep.toAnalyticsScreen(): AnalyticsScreen = when (this) {
    IntroStep.PARK_RIDE -> AnalyticsScreen.Intro
    IntroStep.FIRST_TRIP -> AnalyticsScreen.IntroFirstTrip
}
