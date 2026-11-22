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
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeChoice

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
    // The same write and the same event as the Settings theme screen, so the two can never differ.
    private val theme = ThemeChoice(sandook, analytics)

    /** Stores the theme the rider has picked, so the app starts in it. One small local write. */
    suspend fun saveTheme(themeId: Int) = withContext(ioDispatcher) { theme.save(themeId) }

    /** Reports the rider's choice of [themeId], with no parameter beyond the id the Settings screen sends. */
    fun trackTheme(themeId: Int) = theme.track(themeId)

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

/**
 * The funnel reads from two screen names, and a new name needs a ledger row, so the rebuilt Intro
 * keeps them: the theme and Park & Ride screens are the Intro, and the screens that ask for a trip
 * are the first trip. [shouldTrackScreenView] keeps the first two from sending the Intro name twice.
 */
internal fun IntroStep.toAnalyticsScreen(): AnalyticsScreen = when (this) {
    IntroStep.THEME, IntroStep.PARK_RIDE -> AnalyticsScreen.Intro
    IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.CONFIRM -> AnalyticsScreen.IntroFirstTrip
}

/**
 * Whether showing [next] is a screen view to report. The first screen the rider sees is, and after that a
 * screen is only when its analytics name differs from the one before: the theme and Park & Ride screens share
 * the Intro name, so going from one to the other is the same screen and sends nothing, and entering the
 * Intro sends the name once. [previous] is null before any screen has been shown, such as after a killed
 * process, which is a new run.
 */
internal fun shouldTrackScreenView(previous: IntroStep?, next: IntroStep): Boolean =
    previous == null || previous.toAnalyticsScreen() != next.toAnalyticsScreen()
