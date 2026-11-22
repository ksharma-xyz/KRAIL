package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlin.math.cos
import kotlin.math.exp

// One letter enters this long after the one before it, and takes this long to settle, so the last of the
// eleven starts at 350 ms and settles at the end of the entrance.
internal const val WORDMARK_LETTER_STAGGER_MS = 35L
internal const val WORDMARK_LETTER_MS = 250L

// The letter's spring: e to the minus decay times a cosine, which starts at 0, overshoots by about a tenth and
// settles. The numbers are a decay of 4.5 and 0.85 of a turn over the letter's time.
private const val SPRING_DECAY = 4.5f
private const val SPRING_FREQUENCY = 5.34f

/**
 * Letter [index] of the wordmark [wordmarkMs] after it started entering. Each letter starts
 * [WORDMARK_LETTER_STAGGER_MS] after the one before and springs for [WORDMARK_LETTER_MS]: 0 before it starts,
 * a little over 1 as it overshoots, and exactly 1 once it has settled and for ever after.
 */
internal fun letterProgressAt(index: Int, wordmarkMs: Long): Float {
    val position = (wordmarkMs - index * WORDMARK_LETTER_STAGGER_MS).toFloat() / WORDMARK_LETTER_MS
    return when {
        position <= 0f -> 0f
        position >= 1f -> 1f
        else -> 1f - exp(-SPRING_DECAY * position) * cos(SPRING_FREQUENCY * position)
    }
}

/** How far the brand gradient has travelled across the wordmark, 0 to 1, over the entrance and the hold. */
internal fun gradientTravelAt(wordmarkMs: Long): Float = smoothstep(shareOf(wordmarkMs, 0L, FINALE_HOLD_END_MS))

// The whole word fades in as one layer over this long, while its letters rise one after another.
internal const val WORDMARK_FADE_IN_MS = 250L

/** The opacity of the whole word [wordmarkMs] after it started entering: 0 at the start and 1 from [WORDMARK_FADE_IN_MS] on. */
internal fun wordmarkFadeInAt(wordmarkMs: Long): Float = smoothstep(shareOf(wordmarkMs, 0L, WORDMARK_FADE_IN_MS))
