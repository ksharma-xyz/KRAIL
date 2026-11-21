package xyz.ksharma.krail.trip.planner.ui.intro.v2

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * Everything the Intro writes. Kept apart from the view model so the view model decides the order
 * and this class does the work. Only the suggestion flag so far, the finish adds the rest.
 */
internal class IntroCompletion(
    private val preferences: SandookPreferences,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun writeSuggestionShown() = withContext(ioDispatcher) {
        preferences.setBoolean(SandookPreferences.KEY_PARK_RIDE_SUGGESTION_SHOWN, true)
    }
}
