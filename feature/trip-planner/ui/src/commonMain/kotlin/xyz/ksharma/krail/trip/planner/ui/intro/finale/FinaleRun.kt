package xyz.ksharma.krail.trip.planner.ui.intro.finale

// The fade out starts once the destination has had at least this many drawn frames since the navigation was
// carried out, and at least this long, so the app is on screen and still before the colour lifts. A tap drops
// the minimum hold. Whatever happens, the fade starts at most [SETTLE_MAX_MS] after the app was asked to
// navigate, so the colour can never hang over the app.
internal const val SETTLE_MIN_FRAMES = 2
internal const val SETTLE_MIN_HOLD_MS = 300L
internal const val SETTLE_MAX_MS = 1500L

/** What the run remembers between frames. Everything in it is a time in milliseconds since the first frame. */
internal data class FinaleRunState(
    val skipped: Boolean = false,
    val navigateIssuedAtMs: Long? = null,
    val navigatedAtMs: Long? = null,
    val framesSinceNavigated: Int = 0,
    val fadeStartedAtMs: Long? = null,
    val fadeMs: Long = FINALE_FADE_MS,
)

/**
 * What a frame is told: how long since the first frame, whether the navigation has been carried out (the Intro says
 * so after it navigates), and whether the rider tapped since the last frame.
 */
internal data class FinaleTick(
    val elapsedMs: Long,
    val navigated: Boolean = false,
    val tapped: Boolean = false,
)

/** The result of a frame: what to remember, what to draw, and whether to ask the app to navigate now. */
internal data class FinaleStep(
    val state: FinaleRunState,
    val frame: FinaleFrame,
    val navigateNow: Boolean,
) {
    /** Nothing is left to draw, so the run is over. */
    val finished: Boolean get() = frame.phase == FinalePhase.DONE
}

/**
 * One frame of the run, as a pure function, so every rule below is a table and no clock is needed.
 *
 * The clock the frame is drawn at is the real one until the colour covers the screen, and then it waits there,
 * at [FINALE_COVER_MS], while the app is asked to navigate (once) and draws. It starts again, from the covered
 * moment, when the destination has settled, see [isSettled]. A tap moves the clock to the covered moment at once
 * and shortens the fade, which is the only gesture and is never a button.
 *
 * Reduced motion covers at 0, so the app is asked to navigate on the first frame and the colour cross-fades away.
 */
internal fun finaleStep(state: FinaleRunState, tick: FinaleTick, reduceMotion: Boolean): FinaleStep {
    val skipped = state.skipped || tick.tapped
    val fadeMs = if (state.fadeStartedAtMs == null) finaleFadeMs(skipped, reduceMotion) else state.fadeMs
    val frame = finaleFrameAt(
        elapsedMs = clockMs(state, tick.elapsedMs, skipped, finaleCoverMs(reduceMotion)),
        reduceMotion = reduceMotion,
        fadeMs = fadeMs,
    )
    val navigateNow = frame.phase >= FinalePhase.NAVIGATE && state.navigateIssuedAtMs == null
    val issuedAt = if (navigateNow) tick.elapsedMs else state.navigateIssuedAtMs
    var next = observed(state.copy(skipped = skipped, navigateIssuedAtMs = issuedAt), tick)
    if (state.fadeStartedAtMs == null && isSettled(next, tick.elapsedMs)) {
        next = next.copy(fadeStartedAtMs = tick.elapsedMs, fadeMs = fadeMs)
    }
    return FinaleStep(state = next, frame = frame, navigateNow = navigateNow)
}

/**
 * The time the frame is drawn at. Before the fade it is the real time held at [coverMs], or [coverMs] straight
 * away after a tap. After the fade started it is [coverMs] plus the real time since then.
 */
internal fun clockMs(state: FinaleRunState, elapsedMs: Long, skipped: Boolean, coverMs: Long): Long {
    val fadeStartedAt = state.fadeStartedAtMs
    return when {
        fadeStartedAt != null -> coverMs + (elapsedMs - fadeStartedAt).coerceAtLeast(0L)
        skipped -> coverMs
        else -> minOf(elapsedMs, coverMs)
    }
}

// Counts the frames drawn since the Intro said it had navigated. The frame it says so on is the first, and
// is not counted, because the destination's first composition happens in the frame after the call.
private fun observed(state: FinaleRunState, tick: FinaleTick): FinaleRunState = when {
    state.navigateIssuedAtMs == null || !tick.navigated -> state
    state.navigatedAtMs == null -> state.copy(navigatedAtMs = tick.elapsedMs, framesSinceNavigated = 0)
    else -> state.copy(framesSinceNavigated = state.framesSinceNavigated + 1)
}

/**
 * Whether the fade out may start at [nowMs]: the destination has been drawn for [SETTLE_MIN_FRAMES] frames and for
 * [SETTLE_MIN_HOLD_MS] (no hold after a tap), or [SETTLE_MAX_MS] has passed since the app was asked to navigate.
 * Before the app was asked, it never may.
 */
internal fun isSettled(state: FinaleRunState, nowMs: Long): Boolean {
    val issuedAt = state.navigateIssuedAtMs
    val navigatedAt = state.navigatedAtMs
    val hold = if (state.skipped) 0L else SETTLE_MIN_HOLD_MS
    return when {
        issuedAt == null -> false
        nowMs - issuedAt >= SETTLE_MAX_MS -> true
        navigatedAt == null -> false
        else -> state.framesSinceNavigated >= SETTLE_MIN_FRAMES && nowMs - navigatedAt >= hold
    }
}
