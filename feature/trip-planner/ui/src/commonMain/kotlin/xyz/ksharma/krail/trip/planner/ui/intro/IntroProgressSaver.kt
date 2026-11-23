package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState

/**
 * Writes the Intro's [IntroProgress] to [store] as the state changes, from one collector in [scope], so a save, a later
 * save and the clear at the end never pass each other. A save that is still waiting when a newer one arrives is dropped,
 * because only the latest matters. A state whose progress is the one last handed over, as when the rider types or
 * selects a row, writes nothing.
 */
internal class IntroProgressSaver(
    scope: CoroutineScope,
    store: IntroProgressStore,
    initial: IntroState,
) {
    private var last: String = initial.toProgress().toText()

    // A text is a save and null is the clear.
    private val writes = Channel<String?>(Channel.CONFLATED)

    init {
        scope.launch {
            for (text in writes) if (text == null) store.clear() else store.write(text)
        }
    }

    /** One small write when the progress of [state] differs from the last one handed over. */
    fun save(state: IntroState) {
        val text = state.toProgress().toText()
        if (text == last) return
        last = text
        writes.trySend(text)
    }

    /** Takes the saved progress away. */
    fun clear() {
        writes.trySend(null)
    }
}

/**
 * The state the Intro opens with: what the system saved with the screen when it did, or on a first run the progress kept
 * on the device when there is a valid one, or the first screen. A [replay] never reads the progress.
 */
internal fun SavedStateHandle.openingIntroState(replay: Boolean, store: IntroProgressStore): IntroState = when {
    contains(INTRO_SAVED_STATE_KEY) -> restoreIntroState()
    replay -> IntroState()
    else -> store.read()?.toState() ?: IntroState()
}
