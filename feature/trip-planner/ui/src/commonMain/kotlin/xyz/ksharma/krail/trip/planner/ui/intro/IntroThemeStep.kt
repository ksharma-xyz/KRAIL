package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep

/**
 * The theme screen of the Intro: picking a theme by tapping its badge, and confirming. It runs in [scope],
 * the view model's scope, and starts no timer.
 *
 * Picking stores the theme at once, one small local write, so the app starts in it whatever happens next, and
 * the screen switches the whole app to the colour from the state. The write is last one wins: a pick that
 * follows another cancels the write still running. Confirming reports the choice once, and moves on.
 */
internal class IntroThemeStep(
    private val scope: CoroutineScope,
    private val completion: IntroCompletion,
    private val read: () -> IntroState,
    private val write: (IntroState.() -> IntroState) -> Unit,
) {
    private var saveJob: Job? = null

    // The theme already reported, so going back and confirming the same one again is not a second choice.
    private var reportedThemeId: Int? = null

    /** A badge was tapped. A theme the Intro does not offer, the one already selected, and any other screen do nothing. */
    fun pick(themeId: Int) {
        val state = read()
        if (state.step != IntroStep.THEME || themeChoiceFor(themeId) == null) return
        if (state.selectedThemeId == themeId) return
        write { copy(selectedThemeId = themeId) }
        saveJob?.cancel()
        saveJob = scope.launch { attempt { completion.saveTheme(themeId) } }
    }

    /** `Yes, this one`: reports the choice once and moves to the next screen. Ignored with no theme selected. */
    fun confirm() {
        val state = read()
        val themeId = state.selectedThemeId
        if (state.step != IntroStep.THEME || themeId == null) return
        if (reportedThemeId != themeId) {
            reportedThemeId = themeId
            completion.trackTheme(themeId)
        }
        write { movedOn() }
    }
}
