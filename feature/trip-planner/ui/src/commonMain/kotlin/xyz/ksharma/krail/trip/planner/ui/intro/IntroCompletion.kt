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
    /** Where the rider's progress through the Intro is kept, so a relaunch after a kill carries on. */
    val progress: IntroProgressStore = IntroProgressStore(preferences, ioDispatcher),
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

    /** Whether the Intro was finished before, so this run is a replay from Settings. One small read. */
    fun hasSeenIntro(): Boolean = preferences.getBoolean(SandookPreferences.KEY_HAS_SEEN_INTRO) == true

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
 * keeps them: the Park & Ride screen, which opens the Intro, and the theme screen, which closes it, are the Intro,
 * and the screens that ask for a trip are the first trip. The order is therefore Intro, first trip, Intro, and a
 * run sends the Intro name twice, once at each end, with the first trip between. [shouldTrackScreenView] sends one
 * view for each run of screens that share a name.
 */
internal fun IntroStep.toAnalyticsScreen(): AnalyticsScreen = when (this) {
    IntroStep.PARK_RIDE, IntroStep.THEME -> AnalyticsScreen.Intro
    IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.CONFIRM -> AnalyticsScreen.IntroFirstTrip
}

/**
 * Whether showing [next] is a screen view to report. The first screen the rider sees is, and after that a
 * screen is only when its analytics name differs from the one before: the destination, origin and confirm screens
 * share the first trip name, so going from one to the next is the same screen and sends nothing, and the theme
 * screen after them is the Intro again, so it sends. [previous] is null before any screen has been shown, such as
 * after a killed process, which is a new run.
 */
internal fun shouldTrackScreenView(previous: IntroStep?, next: IntroStep): Boolean =
    previous == null || previous.toAnalyticsScreen() != next.toAnalyticsScreen()
