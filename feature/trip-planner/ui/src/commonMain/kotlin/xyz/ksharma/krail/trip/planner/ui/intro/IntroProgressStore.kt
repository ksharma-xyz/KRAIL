package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * Where the Intro's progress is kept: one string under [INTRO_PROGRESS_KEY] in the app's own preference store, the one
 * that holds the other first-run flags. It is read once when the Intro opens, and written off the main thread, one small
 * value, each time the progress changes.
 */
internal class IntroProgressStore(
    private val preferences: SandookPreferences,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /** The saved progress, or null when there is none or it cannot be used. One small read. */
    fun read(): IntroProgress? = introProgressFromText(preferences.getString(INTRO_PROGRESS_KEY))

    suspend fun write(text: String) = withContext(ioDispatcher) {
        preferences.setString(INTRO_PROGRESS_KEY, text)
    }

    suspend fun clear() = withContext(ioDispatcher) {
        preferences.deletePreference(INTRO_PROGRESS_KEY)
    }
}
