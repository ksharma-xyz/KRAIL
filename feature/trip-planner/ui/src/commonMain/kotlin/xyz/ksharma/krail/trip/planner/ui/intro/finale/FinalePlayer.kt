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
 * composition or the app is stopped. At full cover it waits, with no frame callbacks, until the Intro has released the run,
 * which it does when everything it writes is written and its bounded wait for the stops is over. The only state it writes is [frame], read when drawing, and it asks the Intro
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
    var heldAtCover = false
    while (!finished) {
        withFrameNanos { nanos ->
            if (startNanos == NOT_STARTED) startNanos = nanos
            val elapsedMs = (nanos - startNanos) / NANOS_PER_MS
            val ready = navigationReadyAt(signal.navigationReleased.value, elapsedMs, finaleCoverMs(reduceMotion))
            val tick = FinaleTick(
                elapsedMs = elapsedMs,
                navigated = signal.navigated.value,
                tapped = tapped.value,
                navigationReady = ready,
            )
            tapped.value = false
            val step = finaleStep(state, tick, reduceMotion)
            state = step.state
            frame.value = step.frame
            if (step.navigateNow) signal.requestNavigate()
            finished = step.finished
            heldAtCover = step.heldForRelease(tick)
        }
        // Held at full cover for the Intro: nothing changes, so no frame is asked for until it lets go, or the ceiling.
        if (heldAtCover) withTimeoutOrNull(FINALE_RELEASE_CEILING_MS) { signal.navigationReleased.first { it } }
    }
}

// The colour covers the screen and the app has not been asked to navigate because the Intro has not released the run.
private fun FinaleStep.heldForRelease(tick: FinaleTick): Boolean =
    !tick.navigationReady && state.navigateIssuedAtMs == null && frame.phase == FinalePhase.NAVIGATE

/**
 * A run this host did not start, which is what a turn of the screen or a stopped app leaves behind. It is not played
 * again. If the Intro has not navigated, it waits for the Intro to release the run (the Intro's own wait is bounded, and so
 * is this, by [FINALE_RELEASE_CEILING_MS]), asks it to navigate, and waits for that for at most [RECOVER_WAIT_MS], and then
 * the caller ends the run, so the finale jumps to its end and the layer is gone. It never ends the run before the release,
 * because the Intro would then be on screen again with its finish already taken.
 */
internal suspend fun recoverFinale(signal: IntroFinaleSignal) {
    if (!signal.navigated.value) {
        withTimeoutOrNull(FINALE_RELEASE_CEILING_MS) { signal.navigationReleased.first { it } }
        signal.requestNavigate()
        withTimeoutOrNull(RECOVER_WAIT_MS) { signal.navigated.first { it } }
    }
}
