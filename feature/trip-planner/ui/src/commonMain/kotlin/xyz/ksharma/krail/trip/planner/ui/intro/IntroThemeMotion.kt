package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// The maths behind the theme badges' two continuous animations, as pure functions of time so a table can
// hold every number and no test needs a clock. The screen reads one clock, in seconds, and these turn it into
// where a badge is. Distances are in dp and angles in degrees, and nothing here knows about pixels.

/** How far a drifting badge may wander from its place, up and down and side to side. */
internal const val FLOAT_MAX_VERTICAL_DP = 6f
internal const val FLOAT_MAX_HORIZONTAL_DP = 4f

/**
 * One badge's drift. [periodSeconds] is how long one loop takes, [phase] is how far round the loop it starts
 * as a share of it, and the two amplitudes are how far it moves. Every badge has its own period and phase, so
 * they never move together.
 */
internal data class FloatSpec(
    val periodSeconds: Float,
    val phase: Float,
    val verticalDp: Float,
    val horizontalDp: Float,
)

// Eight to thirteen seconds a loop, each different, with phases spread round the loop, so no two badges are ever
// at the same point of it together. The amplitudes are under the maximums, so the drift reads as breathing.
internal val FloatSpecs: List<FloatSpec> = listOf(
    FloatSpec(periodSeconds = 8f, phase = 0f, verticalDp = 6f, horizontalDp = 3f),
    FloatSpec(periodSeconds = 10f, phase = 0.35f, verticalDp = 5f, horizontalDp = 4f),
    FloatSpec(periodSeconds = 12f, phase = 0.7f, verticalDp = 6f, horizontalDp = 3.5f),
    FloatSpec(periodSeconds = 13f, phase = 0.15f, verticalDp = 4.5f, horizontalDp = 4f),
)

private const val QUARTER_TURN = 0.25f
private const val TWO_PI = (2.0 * PI).toFloat()

/** Where a badge is relative to its place: right and down are positive, as on screen. */
internal data class FloatOffset(val xDp: Float, val yDp: Float)

/**
 * Where the badge at [index] has drifted to, [seconds] after the clock started. It goes round an ellipse once
 * every period: up and down by the vertical amplitude, with the sideways movement a quarter turn behind so it
 * is smooth rather than a line. The badges after the last spec wrap round to the first.
 */
internal fun floatOffsetAt(index: Int, seconds: Float): FloatOffset {
    val spec = FloatSpecs[index.mod(FloatSpecs.size)]
    val turns = seconds / spec.periodSeconds + spec.phase
    return FloatOffset(
        xDp = spec.horizontalDp * sin(TWO_PI * (turns + QUARTER_TURN)),
        yDp = spec.verticalDp * sin(TWO_PI * turns),
    )
}

// The website's pill-dance: a hop and a wobble, 0.65 s each way, easing in and out, and going back the way it
// came, so a full swing out and home is twice that.
internal const val DANCE_LEG_SECONDS = 0.65f
internal const val DANCE_ROTATION_DEGREES = 10f
internal const val DANCE_RISE_FROM_DP = 2f
internal const val DANCE_RISE_TO_DP = 8f
internal const val DANCE_SCALE_FROM = 1.05f
internal const val DANCE_SCALE_TO = 1.10f

/** A badge's pose: how far it is moved and how it is turned and scaled. Up is negative [yDp], as on screen. */
internal data class BadgeFrame(val xDp: Float, val yDp: Float, val rotationDegrees: Float, val scale: Float)

/** A badge sitting at its place, which is also what a badge with no motion shows. */
internal val RestFrame = BadgeFrame(xDp = 0f, yDp = 0f, rotationDegrees = 0f, scale = 1f)

/**
 * The dance at [seconds]: it turns from -10 to +10 degrees, rises from 2 to 8 dp and grows from 1.05 to 1.10,
 * easing in and out, over [DANCE_LEG_SECONDS], then makes the same trip back, for as long as the clock runs. At
 * time zero it is at the first end.
 */
internal fun danceAt(seconds: Float): BadgeFrame {
    val legs = seconds / DANCE_LEG_SECONDS
    val whole = floor(legs)
    val progress = legs - whole
    val outward = whole.toInt().mod(2) == 0
    val eased = easeInOut(if (outward) progress else 1f - progress)
    return BadgeFrame(
        xDp = 0f,
        yDp = -lerp(DANCE_RISE_FROM_DP, DANCE_RISE_TO_DP, eased),
        rotationDegrees = lerp(-DANCE_ROTATION_DEGREES, DANCE_ROTATION_DEGREES, eased),
        scale = lerp(DANCE_SCALE_FROM, DANCE_SCALE_TO, eased),
    )
}

/** A smooth start and a smooth stop: 0 at 0, 1 at 1, and flat at both ends. */
internal fun easeInOut(progress: Float): Float = (1f - cos(PI.toFloat() * progress.coerceIn(0f, 1f))) / 2f

private fun lerp(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

/**
 * The pose of the badge at [index] at [seconds]. [danceWeight] is how much of it is the dance: 0 is the drift
 * alone, 1 is the dance alone, and the shares between are a blend, so a badge that has just been chosen eases
 * from drifting into dancing and back, and never jumps.
 */
internal fun badgeFrame(index: Int, seconds: Float, danceWeight: Float): BadgeFrame {
    val weight = danceWeight.coerceIn(0f, 1f)
    val drift = floatOffsetAt(index, seconds)
    val dance = danceAt(seconds)
    return BadgeFrame(
        xDp = lerp(drift.xDp, dance.xDp, weight),
        yDp = lerp(drift.yDp, dance.yDp, weight),
        rotationDegrees = lerp(0f, dance.rotationDegrees, weight),
        scale = lerp(1f, dance.scale, weight),
    )
}

/** How far a chosen badge lifts with no motion, in dp. Up is negative [BadgeFrame.yDp]. */
internal const val SETTLED_LIFT_DP = 4f

/**
 * The pose with no motion: reduced motion, a preview and a screenshot. A badge that is not chosen is at its
 * place, and the chosen one simply sits lifted by [SETTLED_LIFT_DP].
 */
internal fun settledFrame(selected: Boolean): BadgeFrame =
    if (selected) RestFrame.copy(yDp = -SETTLED_LIFT_DP) else RestFrame

/**
 * Whether the two continuous animations run: not with reduced motion, and not in a preview or a screenshot,
 * which show the settled frame. Whether the screen is resumed is a second gate, held by the clock.
 */
internal fun themeMotionRuns(reduceMotion: Boolean, inspection: Boolean): Boolean = !reduceMotion && !inspection

// The badge is 64 dp at the default text size, and grows with the text so its letter always fits, up to a
// half as big again. It is never smaller than 64 dp, and a touch target is never smaller than 48 dp.
internal const val BADGE_BASE_SIZE_DP = 64f
internal const val BADGE_MAX_GROWTH = 1.5f

/** The badge's side in dp at [fontScale]: 64 dp, growing with the text to at most 96 dp. */
internal fun badgeSizeDp(fontScale: Float): Float = BADGE_BASE_SIZE_DP * fontScale.coerceIn(1f, BADGE_MAX_GROWTH)

/** The ring of a badge in dp: the regular one, and thicker for the chosen badge when there is no motion to show it. */
internal fun ringWidthDp(selected: Boolean, motionRuns: Boolean, regularDp: Float, thickDp: Float): Float =
    if (selected && !motionRuns) thickDp else regularDp
