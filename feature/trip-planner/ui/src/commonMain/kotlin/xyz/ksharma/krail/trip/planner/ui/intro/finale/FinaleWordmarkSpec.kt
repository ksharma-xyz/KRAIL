package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The wordmark's lines. On a phone the two words are two lines, each its own row of letters, so a word is
 * never split and never sits beside a sibling. The hash and the apostrophe are part of the mark.
 */
internal val WORDMARK_LINES: List<String> = listOf("#LET'S", "KRAIL")

/** What a screen reader says for the whole overlay, once. The letters are not read one by one. */
internal const val FINALE_ACCESSIBILITY_LABEL = "Let's KRAIL"

/** How many letters the wordmark has. Each is its own animated unit. */
internal val WORDMARK_LETTER_COUNT: Int = WORDMARK_LINES.sumOf { it.length }

/** The index of the first letter of line [line], counting every letter of the lines before it. */
internal fun firstLetterOfLine(line: Int): Int = WORDMARK_LINES.take(line).sumOf { it.length }

/**
 * The wordmark is the app's boldest display style, the tagline's, at this multiple of its size. It is a multiple
 * of the token, so the size follows the type scale and no size is written here.
 */
internal const val WORDMARK_SCALE = 3f

/** A letter starts this share of its own height below its place and rises into it. */
internal const val WORDMARK_RISE_FRACTION = 0.6f

// The glow is quantised to this many steps, so the text style changes at most this many times during the whole
// finale and the letters recompose only when a step changes, not on every frame.
internal const val GLOW_STEPS = 12

// The glow's blur is this share of the letter's size when the glow is full, and its opacity at full.
private const val GLOW_BLUR_RATIO = 0.45f
private const val GLOW_MAX_ALPHA = 0.9f

// The least a wordmark scales down to. Below this the screen is not wide enough for a word at any size
// worth reading, and the largest text sizes are not allowed to push the mark off the screen.
private const val MIN_FIT_SCALE = 0.1f

/**
 * The scale that makes a line [measuredWidthPx] wide fit in [availableWidthPx]: 1 when it already fits, and the share
 * that does when it does not. The size goes down and the letters never squeeze or overlap, and a line that is
 * not measured yet is left at 1.
 */
internal fun fitScale(measuredWidthPx: Float, availableWidthPx: Float): Float = when {
    measuredWidthPx <= 0f -> 1f
    availableWidthPx <= 0f -> MIN_FIT_SCALE
    measuredWidthPx <= availableWidthPx -> 1f
    else -> max(MIN_FIT_SCALE, availableWidthPx / measuredWidthPx)
}

/**
 * The one scale for the whole wordmark: the smallest any line needs, so the lines are always the same size and
 * the widest word is the one that fits. Large text and a narrow screen make the whole mark smaller together.
 */
internal fun wordmarkFitScale(lineWidthsPx: List<Float>, availableWidthPx: Float): Float =
    lineWidthsPx.fold(1f) { scale, width -> min(scale, fitScale(width, availableWidthPx)) }

/** The glow, 0 to 1, as a whole step from 0 to [GLOW_STEPS]. */
internal fun glowStep(glow: Float): Int = (glow.coerceIn(0f, 1f) * GLOW_STEPS).roundToInt()

/** The glow's blur radius in pixels at [step], for a letter [letterSizePx] tall. 0 at step 0. */
internal fun glowBlurPx(step: Int, letterSizePx: Float): Float =
    step.coerceIn(0, GLOW_STEPS).toFloat() / GLOW_STEPS * GLOW_BLUR_RATIO * letterSizePx

/** The glow's opacity at [step]: none at step 0 and [GLOW_MAX_ALPHA] at the last. */
internal fun glowAlpha(step: Int): Float = step.coerceIn(0, GLOW_STEPS).toFloat() / GLOW_STEPS * GLOW_MAX_ALPHA

/**
 * How far the circle that floods the screen is, in pixels, when it is [share] of the way grown from a centre at
 * ([centerX], [centerY]) on a screen [width] by [height]. At 1 it reaches the farthest corner, which is the
 * whole screen from any centre.
 */
internal fun floodRadiusPx(share: Float, width: Float, height: Float, centerX: Float, centerY: Float): Float {
    val farX = max(centerX, width - centerX)
    val farY = max(centerY, height - centerY)
    return share.coerceIn(0f, 1f) * sqrt(farX * farX + farY * farY)
}
