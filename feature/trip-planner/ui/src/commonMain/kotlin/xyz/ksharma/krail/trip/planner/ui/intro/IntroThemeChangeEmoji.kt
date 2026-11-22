package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// The emoji of the theme change sequence: it shrinks, becomes the new one, and springs back.

// The emoji shrinks to [THEME_EMOJI_LOW] over [THEME_EMOJI_SHRINK_MS], is the new one from there, and springs back to 1 with a
// small overshoot, a short underdamped spring.
internal const val THEME_EMOJI_SHRINK_MS = 120f
internal const val THEME_EMOJI_LOW = 0.6f
private const val EMOJI_OMEGA = 24f
private const val EMOJI_ZETA = 0.55f
private const val EMOJI_SPRING_SECONDS = 0.5f

/** Whether the emoji is the new one yet. It changes while it is at its smallest, and half way through with reduced motion. */
internal fun themeEmojiIsNew(timeMs: Float, reduceMotion: Boolean): Boolean =
    timeMs >= if (reduceMotion) THEME_CHANGE_REDUCED_MS / 2f else THEME_EMOJI_SHRINK_MS

/**
 * The emoji's scale at [timeMs], from [scaleAtStart], which is 1 unless a sequence was cut short while it was moving: it shrinks to
 * [THEME_EMOJI_LOW] over [THEME_EMOJI_SHRINK_MS], then springs to 1 and overshoots a little, and is exactly 1 after
 * [EMOJI_SPRING_SECONDS]. With reduced motion it does not pop, and only goes to 1 if it was cut short.
 */
internal fun themeEmojiScale(timeMs: Float, scaleAtStart: Float, reduceMotion: Boolean): Float {
    if (reduceMotion) return scaleAtStart + (1f - scaleAtStart) * themeShare(timeMs, 0f, THEME_CHANGE_REDUCED_MS)
    val low = min(scaleAtStart, THEME_EMOJI_LOW)
    if (timeMs < THEME_EMOJI_SHRINK_MS) {
        return scaleAtStart + (low - scaleAtStart) * easeInOut(themeShare(timeMs, 0f, THEME_EMOJI_SHRINK_MS))
    }
    val seconds = (timeMs - THEME_EMOJI_SHRINK_MS) / THEME_MILLIS_PER_SECOND
    if (seconds >= EMOJI_SPRING_SECONDS) return 1f
    val damped = EMOJI_OMEGA * sqrt(1f - EMOJI_ZETA * EMOJI_ZETA)
    val decay = exp(-EMOJI_ZETA * EMOJI_OMEGA * seconds)
    val wave = cos(damped * seconds) + EMOJI_ZETA * EMOJI_OMEGA / damped * sin(damped * seconds)
    return 1f - (1f - low) * decay * wave
}
