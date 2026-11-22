package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlin.math.PI
import kotlin.math.sin

// The finale's timeline, in milliseconds from its first frame. The wordmark enters, holds while the glow
// breathes once, blends into the rider's theme colour, and that colour floods the screen. At full cover the app
// navigates underneath, and the colour then fades out. These are starting points to be tuned on a phone, and
// each is a name so a table can hold it.
internal const val FINALE_ENTER_END_MS = 600L
internal const val FINALE_HOLD_END_MS = 1500L
internal const val FINALE_BLEND_END_MS = 2000L

/** The moment the colour covers the whole screen, and the moment the app is asked to navigate. */
internal const val FINALE_COVER_MS = 2500L

/** How long the colour takes to fade out when nobody taps, and the shorter fade after a tap. */
internal const val FINALE_FADE_MS = 700L
internal const val FINALE_SKIP_FADE_MS = 300L

/** With reduced motion there is no wordmark and no flood: the colour is there at once and fades in this long. */
internal const val FINALE_REDUCED_FADE_MS = 200L

// The ground behind the wordmark fades in over this long, so the wordmark never sits on a screen that is still there.
private const val GROUND_FADE_IN_MS = 250L

// The glow rises to its base while the wordmark enters, swells to its peak and back once during the hold,
// stays at the base while the colour blends, and goes out as the flood takes over.
internal const val GLOW_BASE = 0.6f
internal const val GLOW_PEAK = 1f

/** The steps of the finale, in the order they come. */
internal enum class FinalePhase {
    /** The letters spring up one after another. */
    ENTER,

    /** The wordmark holds and the glow breathes once. */
    HOLD,

    /** The gradient and the glow become the theme colour. */
    BLEND,

    /** The theme colour grows out from the wordmark. */
    FLOOD,

    /** The colour covers the screen. The app is asked to navigate, once, while the clock is here. */
    NAVIGATE,

    /** The colour fades out over the app. */
    FADE,

    /** Nothing is left to draw. */
    DONE,
}

/**
 * Everything one frame of the finale needs, as plain numbers, so a table can hold it and the drawing only reads it.
 * [wordmarkMs] is how long the wordmark has been entering, which fixes every letter, see [letterProgress].
 * [wordmarkAlpha] and [groundAlpha] are the wordmark's and the backdrop's opacity. [glow] is 0 to 1.
 * [colourBlend] is 0 for the brand gradient and 1 for the theme colour. [floodRadius] is the share of the way
 * the circle has grown to the farthest corner of the screen, 1 being everything covered. [overlayAlpha] is
 * the opacity of the whole thing, which is 1 until the fade and 0 at the end.
 */
internal data class FinaleFrame(
    val phase: FinalePhase,
    val wordmarkMs: Long,
    val wordmarkAlpha: Float,
    val glow: Float,
    val colourBlend: Float,
    val floodRadius: Float,
    val groundAlpha: Float,
    val overlayAlpha: Float,
) {
    /** Letter [index] of the wordmark, 0 not started and 1 in place, a little over 1 while it overshoots. */
    fun letterProgress(index: Int): Float = letterProgressAt(index, wordmarkMs)
}

/** When the colour covers the screen: [FINALE_COVER_MS], and at once when motion is reduced. */
internal fun finaleCoverMs(reduceMotion: Boolean): Long = if (reduceMotion) 0L else FINALE_COVER_MS

/** How long the colour takes to fade out. A tap and reduced motion both make it short. */
internal fun finaleFadeMs(skipped: Boolean, reduceMotion: Boolean): Long = when {
    reduceMotion -> FINALE_REDUCED_FADE_MS
    skipped -> FINALE_SKIP_FADE_MS
    else -> FINALE_FADE_MS
}

/**
 * The frame [elapsedMs] after the finale started, as a pure function of the time and nothing else. A negative
 * time is the first frame. [fadeMs] is how long the fade out takes, so a tap can shorten it.
 *
 * Reduced motion has no wordmark, no glow and no flood: the colour is there at 0 and cross-fades to the app over
 * [FINALE_REDUCED_FADE_MS], and the app is asked to navigate at 0.
 *
 * The phase is [FinalePhase.NAVIGATE] at exactly the covered moment and nowhere else, so a clock that waits
 * there for the destination to be drawn reads NAVIGATE for as long as it waits.
 */
internal fun finaleFrameAt(
    elapsedMs: Long,
    reduceMotion: Boolean,
    fadeMs: Long = finaleFadeMs(skipped = false, reduceMotion = reduceMotion),
): FinaleFrame {
    val time = elapsedMs.coerceAtLeast(0L)
    return if (reduceMotion) reducedFrameAt(time, fadeMs) else fullFrameAt(time, fadeMs)
}

private fun fullFrameAt(time: Long, fadeMs: Long): FinaleFrame {
    val flood = smoothstep(shareOf(time, FINALE_BLEND_END_MS, FINALE_COVER_MS))
    return FinaleFrame(
        phase = fullPhaseAt(time, fadeMs),
        wordmarkMs = time,
        wordmarkAlpha = 1f - flood,
        glow = glowAt(time),
        colourBlend = smoothstep(shareOf(time, FINALE_HOLD_END_MS, FINALE_BLEND_END_MS)),
        floodRadius = flood,
        groundAlpha = if (time >= FINALE_COVER_MS) 0f else smoothstep(shareOf(time, 0L, GROUND_FADE_IN_MS)),
        overlayAlpha = fadeAlphaAt(time - FINALE_COVER_MS, fadeMs),
    )
}

private fun reducedFrameAt(time: Long, fadeMs: Long): FinaleFrame = FinaleFrame(
    phase = when {
        time <= 0L -> FinalePhase.NAVIGATE
        time < fadeMs -> FinalePhase.FADE
        else -> FinalePhase.DONE
    },
    wordmarkMs = 0L,
    wordmarkAlpha = 0f,
    glow = 0f,
    colourBlend = 1f,
    floodRadius = 1f,
    groundAlpha = 0f,
    overlayAlpha = 1f - shareOf(time, 0L, fadeMs),
)

private fun fullPhaseAt(time: Long, fadeMs: Long): FinalePhase = when {
    time < FINALE_ENTER_END_MS -> FinalePhase.ENTER
    time < FINALE_HOLD_END_MS -> FinalePhase.HOLD
    time < FINALE_BLEND_END_MS -> FinalePhase.BLEND
    time < FINALE_COVER_MS -> FinalePhase.FLOOD
    time == FINALE_COVER_MS -> FinalePhase.NAVIGATE
    time < FINALE_COVER_MS + fadeMs -> FinalePhase.FADE
    else -> FinalePhase.DONE
}

private fun glowAt(time: Long): Float = when {
    time < FINALE_ENTER_END_MS -> GLOW_BASE * smoothstep(shareOf(time, 0L, FINALE_ENTER_END_MS))
    time < FINALE_HOLD_END_MS -> {
        // One breath: up to the peak at the middle of the hold and back, along half a sine wave.
        val breath = sin(PI.toFloat() * shareOf(time, FINALE_ENTER_END_MS, FINALE_HOLD_END_MS))
        GLOW_BASE + (GLOW_PEAK - GLOW_BASE) * breath
    }
    time < FINALE_BLEND_END_MS -> GLOW_BASE
    time < FINALE_COVER_MS -> GLOW_BASE * (1f - smoothstep(shareOf(time, FINALE_BLEND_END_MS, FINALE_COVER_MS)))
    else -> 0f
}

// 1 until the fade starts, then 1 down to 0 over [fadeMs], eased at both ends so the app arrives gently.
private fun fadeAlphaAt(sinceCoverMs: Long, fadeMs: Long): Float =
    if (sinceCoverMs <= 0L) 1f else 1f - smoothstep(shareOf(sinceCoverMs, 0L, fadeMs))
