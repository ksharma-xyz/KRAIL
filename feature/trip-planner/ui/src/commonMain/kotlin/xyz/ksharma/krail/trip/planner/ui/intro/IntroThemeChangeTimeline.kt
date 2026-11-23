package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.exp

// The sequence that plays when a badge on the colour screen is tapped, as pure functions of one clock, in milliseconds
// since the tap, so a table can hold every number and no test needs a real clock. One clock runs the whole thing: the
// badge's press, the wash of the new ground, the colours (text, ink and button from the one blend), the old headline
// leaving, the emoji's pop, the new tagline coming in line by line and the button last. Every window is a name.

/** How long the whole sequence takes, and the shorter plain cross-fade that reduced motion has instead. */
internal const val THEME_CHANGE_MS = 700f
internal const val THEME_CHANGE_REDUCED_MS = 200f

// The badge presses softly: it dips over [THEME_PRESS_DIP_MS] and comes back with no overshoot, a critically damped return.
internal const val THEME_PRESS_DIP_MS = 90f
internal const val THEME_PRESS_DEPTH = 0.08f
private const val PRESS_RATE = 16f
private const val PRESS_RETURN_SECONDS = 0.45f

/** The wash of the new ground grows from the tapped badge to cover the window over this long. */
internal const val THEME_WASH_MS = 520f

/** The one colour blend, which the text, the ink, the button and the ground's tint all read, runs over this long. */
internal const val THEME_COLOUR_MS = 600f

/** The button, which fades in last, over the 250 ms it always takes, on the first pick. */
internal const val THEME_BUTTON_AT_MS = 450f

// With reduced motion the headline, the emoji and every colour change over this one window, and nothing else moves.
internal const val THEME_MILLIS_PER_SECOND = 1000f

/** How long the sequence takes for the rider's motion setting. */
internal fun themeChangeDurationMs(reduceMotion: Boolean): Float =
    if (reduceMotion) THEME_CHANGE_REDUCED_MS else THEME_CHANGE_MS

internal fun themeShare(timeMs: Float, fromMs: Float, toMs: Float): Float =
    if (toMs <= fromMs) 1f else ((timeMs - fromMs) / (toMs - fromMs)).coerceIn(0f, 1f)

// Fast at the start and slow at the end, so a line arrives and settles.
internal fun easeOutCubic(x: Float): Float {
    val rest = 1f - x.coerceIn(0f, 1f)
    return 1f - rest * rest * rest
}

/**
 * The tapped badge's scale at [timeMs]: 1 before the tap, down to `1 - THEME_PRESS_DEPTH` at [THEME_PRESS_DIP_MS], and back to 1 with
 * no overshoot, and exactly 1 once it is back. The dance the badge already has is on top of it.
 */
internal fun themePress(timeMs: Float): Float {
    val time = timeMs.coerceAtLeast(0f)
    val depth = if (time < THEME_PRESS_DIP_MS) {
        easeOutCubic(time / THEME_PRESS_DIP_MS)
    } else {
        val seconds = (time - THEME_PRESS_DIP_MS) / THEME_MILLIS_PER_SECOND
        if (seconds >= PRESS_RETURN_SECONDS) 0f else (1f + PRESS_RATE * seconds) * exp(-PRESS_RATE * seconds)
    }
    return 1f - THEME_PRESS_DEPTH * depth
}

/** How far the wash has grown, 0 to 1 of the way to covering the window, from [THEME_WASH_MS]. It starts and ends slowly. */
internal fun themeWash(timeMs: Float): Float = easeInOut(themeShare(timeMs, 0f, THEME_WASH_MS))

/**
 * The one colour blend, 0 for the old theme and 1 for the new, which the text, the ink, the button and the ground all read, so
 * nothing is out of step. [THEME_COLOUR_MS] with motion, and a plain straight line over the whole short window with reduced motion.
 */
internal fun themeColour(timeMs: Float, reduceMotion: Boolean): Float =
    if (reduceMotion) {
        themeShare(
            timeMs,
            0f,
            THEME_CHANGE_REDUCED_MS,
        )
    } else {
        easeInOut(themeShare(timeMs, 0f, THEME_COLOUR_MS))
    }

/** Whether the button is there at [timeMs] when it was not there before the tap: from [buttonAtMs], and at once with reduced motion. */
internal fun themeButtonShown(timeMs: Float, buttonAtMs: Float): Boolean = timeMs >= buttonAtMs

/** The first moment the button shows on a tap: [THEME_BUTTON_AT_MS], and at once with reduced motion. */
internal fun themeButtonAtMs(reduceMotion: Boolean): Float = if (reduceMotion) 0f else THEME_BUTTON_AT_MS

/**
 * Every element of the sequence at one time, as plain numbers, so a table can hold it. [press] is the tapped badge's scale,
 * [wash] how far the new ground has grown, [colour] the one colour blend, [outgoing] how far the old headline has gone,
 * [emojiScale] the emoji's scale and [emojiIsNew] whether it shows the new emoji. The lines of the new headline are
 * [incomingLine], and [done] is whether the sequence is over and everything is settled.
 */
internal data class ThemeChangeFrame(
    val timeMs: Float,
    val reduceMotion: Boolean,
    val press: Float,
    val wash: Float,
    val colour: Float,
    val outgoing: Float,
    val emojiScale: Float,
    val emojiIsNew: Boolean,
    val done: Boolean,
) {
    /** How far line [index] of the new headline has come in. */
    fun incomingLine(index: Int): Float = themeIncomingLine(timeMs, index, reduceMotion)
}

/**
 * The frame [timeMs] after the tap, as a pure function of the time and nothing else. A time before the tap is the tap itself,
 * and one after the end is the settled state. With reduced motion there is no press, no wash and no pop: the colours and the
 * headline cross-fade over [THEME_CHANGE_REDUCED_MS], so [wash] is 1 from the start, which is to say there is no growing circle.
 * [emojiScaleAtStart] is 1 unless the sequence follows one that was cut short.
 */
internal fun themeChangeFrameAt(
    timeMs: Float,
    reduceMotion: Boolean,
    emojiScaleAtStart: Float = 1f,
): ThemeChangeFrame {
    val time = timeMs.coerceAtLeast(0f)
    return ThemeChangeFrame(
        timeMs = time,
        reduceMotion = reduceMotion,
        press = if (reduceMotion) 1f else themePress(time),
        wash = if (reduceMotion) 1f else themeWash(time),
        colour = themeColour(time, reduceMotion),
        outgoing = themeOutgoing(time, reduceMotion),
        emojiScale = themeEmojiScale(time, emojiScaleAtStart, reduceMotion),
        emojiIsNew = themeEmojiIsNew(time, reduceMotion),
        done = time >= themeChangeDurationMs(reduceMotion),
    )
}
