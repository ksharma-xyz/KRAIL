package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

/** One run of the finale. [id] is its number, so a run is played once, and [themeHexColorCode] is the rider's theme. */
internal class FinaleRequest(val id: Long, val themeHexColorCode: String)

/**
 * The one-shot signal between the Intro and the finale's host at the app root. It is an app level singleton, in memory
 * only, and it holds nothing that must survive the process: the Intro is already marked seen before it is used, so a
 * killed process starts the next launch on Saved Trips with no finale.
 *
 * - The Intro [start]s a run the moment the rider presses the finish button, before it writes anything or waits for the
 *   stops, so the rider sees the finale at once. The run is only started when a host is attached, so a screen with no host
 *   (a test, a preview) goes straight on to Saved Trips and nothing waits for a finale that is not there.
 * - The Intro [release]s the run when everything it writes is written and its bounded wait for the stops is over. Until then
 *   the host holds the colour at full cover and does not ask to navigate, so the app is never shown before then, and the
 *   Intro is never seen again under a colour that has faded. The hold is bounded by the Intro's own wait, and by a ceiling in
 *   the player as a last resort.
 * - The host plays the run and, at full cover and once released, asks the Intro to navigate ([requestNavigate]). The Intro
 *   navigates with the callback it has now, and says so ([onNavigated]). Going through the Intro and not holding its
 *   callback means a turn of the screen never leaves a callback for a screen that is gone.
 * - A run is [claim]ed by the host that plays it. A host that finds a run it did not start, after a turn of the
 *   screen or after the app was stopped, does not play it again: it asks for the navigation if it has not happened
 *   and ends the run, so the finale jumps to its end and can never leave a layer over the app.
 *
 * Everything is internal except the class, so the app root can inject it and nothing else can reach it.
 */
class IntroFinaleSignal {
    private val requestState = MutableStateFlow<FinaleRequest?>(null)
    private val navigatedState = MutableStateFlow(false)
    private val navigateChannel = Channel<Unit>(Channel.CONFLATED)
    private val hosts = MutableStateFlow(0)
    private val lastId = MutableStateFlow(0L)
    private val claimedId = MutableStateFlow(NO_RUN)
    private val releasedState = MutableStateFlow(false)

    /** The run being played, or null when there is none, which is nearly always. */
    internal val request: StateFlow<FinaleRequest?> = requestState.asStateFlow()

    /** Whether the Intro has navigated for the run being played. */
    internal val navigated: StateFlow<Boolean> = navigatedState.asStateFlow()

    /** Whether the Intro has finished writing and waiting for the run being played, so the app may be shown. */
    internal val navigationReleased: StateFlow<Boolean> = releasedState.asStateFlow()

    /** The asks to navigate, once each. One nobody has taken yet waits, and a second before it is taken is the same. */
    internal val navigateRequests: Flow<Unit> = navigateChannel.receiveAsFlow()

    /**
     * Starts a run in the theme [themeHexColorCode], held at full cover until [release], and returns true. It returns false, and starts nothing, when no
     * host is attached or a run is already being played, which tells the caller to navigate now.
     */
    internal fun start(themeHexColorCode: String): Boolean {
        val free = hosts.value > 0 && requestState.value == null
        if (free) {
            navigateChannel.tryReceive()
            navigatedState.value = false
            releasedState.value = false
            requestState.value = FinaleRequest(lastId.updateAndGet { it + 1 }, themeHexColorCode)
        }
        return free
    }

    /**
     * The Intro has written everything and its wait is over: the host may now ask it to navigate. Returns true when a run
     * is being played and holds for this, and false when there is none, which tells the caller to navigate now.
     */
    internal fun release(): Boolean {
        val playing = requestState.value != null
        if (playing) releasedState.value = true
        return playing
    }

    /** The host asks the Intro to navigate. */
    internal fun requestNavigate() {
        navigateChannel.trySend(Unit)
    }

    /** The Intro says it has navigated. */
    internal fun onNavigated() {
        navigatedState.value = true
    }

    /** True the first time the run [id] is claimed, and false every time after, so only one host plays it. */
    internal fun claim(id: Long): Boolean = claimedId.getAndUpdate { id } != id

    /** Ends the run [id]. A run that is not the one being played is left alone. */
    internal fun end(id: Long) {
        if (requestState.value?.id == id) {
            requestState.value = null
            navigatedState.value = false
            releasedState.value = false
            navigateChannel.tryReceive()
        }
    }

    internal fun attachHost() {
        hosts.update { it + 1 }
    }

    internal fun detachHost() {
        hosts.update { (it - 1).coerceAtLeast(0) }
    }

    private companion object {
        const val NO_RUN = -1L
    }
}
