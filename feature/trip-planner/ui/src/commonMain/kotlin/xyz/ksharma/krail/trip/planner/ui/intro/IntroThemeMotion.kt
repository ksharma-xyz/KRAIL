package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

// The maths behind the theme badges' two continuous animations, as pure functions of time so a table can
// hold every number and no test needs a clock. The screen reads one clock, in seconds, and these turn it into
// where a badge is. Distances are in dp and angles in degrees, and nothing here knows about pixels.

/**
 * How far a badge floats from its place at its widest, up and down and side to side, in a room with space to spare. Where the room is
 * smaller, [floatAmplitudeDp] is less, so two badges never touch.
 */
internal const val FLOAT_MAX_DP = 8f

/** The root of two: a badge that floats [FLOAT_MAX_DP] each way is, at the corner of that box, this many times as far from its place. */
internal const val SQRT_TWO = 1.4142135f

// The 1 dp of the float that is always kept back, so two badges at their widest still leave a little space between them.
private const val FLOAT_MARGIN_DP = 1f

/**
 * How far the badge that is chosen reaches toward a neighbour with its dance, in dp, at the most: the rise of the dance and the growth of
 * its circle, in any direction. A dancing badge is never moved sideways, so this is more than it can reach on most neighbours.
 */
internal fun danceReachDp(badgeDp: Float): Float = DANCE_RISE_TO_DP + badgeDp / 2f * (DANCE_SCALE_TO - 1f)

// The margin of a dp of float on each of two badges, which is that many times its root two of space between them.
private const val FLOAT_GAP_MARGIN_DP = 2f * SQRT_TWO * FLOAT_MARGIN_DP

/**
 * The least space two badges of [badgeDp] must have between them at rest, edge to edge, for a dance beside a neighbour that does not
 * move at all to leave the margin: the reach of the dance and the margin. A layout with less than this between two badges is not used,
 * because the chosen one could touch the next with no float to take back.
 */
internal fun minClearGapDp(badgeDp: Float): Float = danceReachDp(badgeDp) + FLOAT_GAP_MARGIN_DP

/**
 * How far a badge of [badgeDp] may float from its place, in dp each way, when the two nearest badges are [closestGapDp] apart at rest,
 * edge to edge. It is [FLOAT_MAX_DP] where the room is wide enough and less where it is not, so two badges never touch:
 *
 * - two that float each reach [SQRT_TWO] times the float toward the other, so the float is at most the gap over twice that, less the
 *   [FLOAT_MARGIN_DP] kept back, and
 * - the badge that is chosen dances, and reaches [danceReachDp] toward a neighbour that floats, so the gap less that reach and the
 *   margin has to hold the neighbour's float.
 *
 * It is never negative: with no room to spare the badges stay at their places.
 */
internal fun floatAmplitudeDp(closestGapDp: Float, badgeDp: Float): Float {
    if (closestGapDp.isInfinite()) return FLOAT_MAX_DP
    val floatingTogether = (closestGapDp - FLOAT_GAP_MARGIN_DP) / (2f * SQRT_TWO)
    val danceAndFloat = (closestGapDp - minClearGapDp(badgeDp)) / SQRT_TWO
    return min(FLOAT_MAX_DP, min(floatingTogether, danceAndFloat)).coerceAtLeast(0f)
}

/**
 * One badge's float. [periodSeconds] is how long one loop takes, [phase] is how far round the loop it starts as a share of it, and
 * the two shares are how much of the float's amplitude it uses up and down and side to side, so it goes round a slightly different
 * ellipse from the next. Every badge has its own period and phase, so they never move together, and no share is over 1, so a badge
 * never goes further from its place than [SQRT_TWO] times the float.
 */
internal data class FloatSpec(
    val periodSeconds: Float,
    val phase: Float,
    val verticalShare: Float,
    val horizontalShare: Float,
)

// Nine to fifteen seconds a loop, each different, with phases spread round the loop, so no two badges are ever at the same point of it
// together and the float reads as slow and unhurried, like something in space. The shares are different ellipses.
internal val FloatSpecs: List<FloatSpec> = listOf(
    FloatSpec(periodSeconds = 9f, phase = 0f, verticalShare = 1f, horizontalShare = 0.8f),
    FloatSpec(periodSeconds = 11f, phase = 0.35f, verticalShare = 0.8f, horizontalShare = 1f),
    FloatSpec(periodSeconds = 13f, phase = 0.7f, verticalShare = 1f, horizontalShare = 0.9f),
    FloatSpec(periodSeconds = 15f, phase = 0.15f, verticalShare = 0.9f, horizontalShare = 1f),
)

private const val QUARTER_TURN = 0.25f
private const val TWO_PI = (2.0 * PI).toFloat()

/** Where a badge is relative to its place: right and down are positive, as on screen. */
internal data class FloatOffset(val xDp: Float, val yDp: Float)

/**
 * Where the badge at [index] has floated to, [seconds] after the clock started, with a float of [amplitudeDp]. It goes round an ellipse
 * once every period: up and down by the amplitude and its vertical share, with the sideways movement a quarter turn behind so it
 * is smooth rather than a line. The badges after the last spec wrap round to the first.
 */
internal fun floatOffsetAt(index: Int, seconds: Float, amplitudeDp: Float): FloatOffset {
    val spec = FloatSpecs[index.mod(FloatSpecs.size)]
    val turns = seconds / spec.periodSeconds + spec.phase
    return FloatOffset(
        xDp = amplitudeDp * spec.horizontalShare * sin(TWO_PI * (turns + QUARTER_TURN)),
        yDp = amplitudeDp * spec.verticalShare * sin(TWO_PI * turns),
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
 * The pose of the badge at [index] at [seconds], with a float of [amplitudeDp]. [danceWeight] is how much of it is the dance: 0 is the
 * float alone, 1 is the dance alone, and the shares between are a blend, so a badge that has just been chosen eases
 * from floating into dancing and back, and never jumps.
 */
internal fun badgeFrame(index: Int, seconds: Float, danceWeight: Float, amplitudeDp: Float): BadgeFrame {
    val weight = danceWeight.coerceIn(0f, 1f)
    val drift = floatOffsetAt(index, seconds, amplitudeDp)
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
