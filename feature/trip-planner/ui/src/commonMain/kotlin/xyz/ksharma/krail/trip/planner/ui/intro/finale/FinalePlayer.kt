package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal const val NANOS_PER_MS = 1_000_000L

// A host that finds a run it did not start waits this long for the Intro to say it navigated, and then ends the run.
internal const val RECOVER_WAIT_MS = 1000L

private const val NOT_STARTED = -1L

/**
 * Plays the run to its end with the frame clock, one pure step for each drawn frame ([finaleStep]). It is finite, so
 * it ends by itself, and it is cancelled with the coroutine that runs it, which is when the host leaves the
 * composition or the app is stopped. The only state it writes is [frame], read when drawing, and it asks the Intro
 * to navigate through [signal] on the frame the step says to.
 */
internal suspend fun playFinale(
    signal: IntroFinaleSignal,
    frame: MutableState<FinaleFrame>,
    tapped: MutableState<Boolean>,
    reduceMotion: Boolean,
) {
    var state = FinaleRunState()
    var startNanos = NOT_STARTED
    var finished = false
    while (!finished) {
        withFrameNanos { nanos ->
            if (startNanos == NOT_STARTED) startNanos = nanos
            val tick = FinaleTick(
                elapsedMs = (nanos - startNanos) / NANOS_PER_MS,
                navigated = signal.navigated.value,
                tapped = tapped.value,
            )
            tapped.value = false
            val step = finaleStep(state, tick, reduceMotion)
            state = step.state
            frame.value = step.frame
            if (step.navigateNow) signal.requestNavigate()
            finished = step.finished
        }
    }
}

/**
 * A run this host did not start, which is what a turn of the screen or a stopped app leaves behind. It is not played
 * again. If the Intro has not navigated, it is asked to, and waited for for at most [RECOVER_WAIT_MS], and then the
 * caller ends the run, so the finale jumps to its end and the layer is gone.
 */
internal suspend fun recoverFinale(signal: IntroFinaleSignal) {
    if (!signal.navigated.value) {
        signal.requestNavigate()
        withTimeoutOrNull(RECOVER_WAIT_MS) { signal.navigated.first { it } }
    }
}
