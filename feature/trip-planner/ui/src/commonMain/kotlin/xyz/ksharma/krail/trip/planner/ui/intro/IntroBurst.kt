package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** How long the burst takes from the first frame to nothing left, in milliseconds. */
internal const val BURST_DURATION_MS = 800

/** How long the plain fade that replaces the whole celebration takes when motion is reduced. */
internal const val CELEBRATION_FADE_MS = 200

/** How many shapes the burst throws. Even, so they spread round the star in pairs. */
internal const val BURST_COUNT = 12

// How far the furthest shape travels, as a multiple of the mark's own half size.
private const val BURST_REACH = 1.9f

// The nearest a shape starts to the centre and the share of its reach it travels, so none sits on the star.
private const val BURST_START = 0.35f

/** The kinds of shape in the burst. Plain shapes only: no emoji and no text. */
internal enum class BurstShape { Circle, Square }

/**
 * One shape of the burst, fixed by its place in the ring so the burst looks the same every time it plays and a
 * test can name every shape. [angle] is where it flies, in degrees from the right going clockwise. [reach] is
 * the share of the full distance it flies. [size] is its width as a share of the mark's half size. [spin] is
 * how far it turns on the way out, in degrees.
 */
internal data class BurstParticle(
    val angle: Float,
    val reach: Float,
    val size: Float,
    val shape: BurstShape,
    val spin: Float,
)

private const val FULL_TURN = 360f
private const val REACH_STEPS = 3
private const val REACH_FROM = 0.7f
private const val REACH_SPAN = 0.3f
private const val SIZE_FROM = 0.16f
private const val SIZE_SPAN = 0.1f
private const val SPIN = 120f

/**
 * The shapes of the burst: [count] of them round a full turn, alternating circle and square, with three reaches
 * and two sizes cycling so the ring is not a perfect circle. It is a pure function of [count].
 */
internal fun burstParticles(count: Int = BURST_COUNT): List<BurstParticle> = List(count) { index ->
    BurstParticle(
        angle = index * FULL_TURN / count,
        reach = REACH_FROM + REACH_SPAN * (index % REACH_STEPS) / (REACH_STEPS - 1),
        size = SIZE_FROM + SIZE_SPAN * (index % 2),
        shape = if (index % 2 == 0) BurstShape.Circle else BurstShape.Square,
        spin = if (index % 2 == 0) 0f else SPIN,
    )
}

/**
 * What one shape looks like at [progress] of the burst, 0 to 1: [distance] is the share of the way out it has
 * flown, fast at first and slowing, [alpha] is how opaque it is, full at the start and nothing at the end, and
 * [scale] is its size as a share of its full size, shrinking as it goes. Outside 0 to 1 it is clamped.
 */
internal data class BurstFrame(val distance: Float, val alpha: Float, val scale: Float)

private const val SHRINK_TO = 0.4f

internal fun burstFrame(progress: Float): BurstFrame {
    val p = progress.coerceIn(0f, 1f)
    val easedOut = 1f - (1f - p) * (1f - p)
    return BurstFrame(distance = easedOut, alpha = 1f - p * p, scale = 1f - (1f - SHRINK_TO) * p)
}

/** What the star's celebration does, for the way the screen is shown. */
internal enum class CelebrationMode {
    /** The star pops, wobbles and throws the burst. */
    Burst,

    /** Motion is reduced, so the star is at rest and fades in, with no burst. */
    Fade,

    /** The star is simply there: a preview, a screenshot, or a celebration that already played. */
    Settled,
}

/**
 * Which celebration to play. A headline with no celebration, a preview or screenshot, and one that already played,
 * such as after a turn of the screen, are settled. Otherwise reduced motion fades the star in and everything else
 * throws the burst.
 */
internal fun celebrationMode(
    celebrate: Boolean,
    reduceMotion: Boolean,
    inspection: Boolean,
    played: Boolean,
): CelebrationMode = when {
    !celebrate || inspection || played -> CelebrationMode.Settled
    reduceMotion -> CelebrationMode.Fade
    else -> CelebrationMode.Burst
}

/**
 * Draws the burst behind the node at [progress], read when drawing so a frame costs no recomposition. It draws outside
 * the node's own bounds, and whatever clips the screen clips it.
 */
internal fun Modifier.introBurst(progress: () -> Float, color: Color): Modifier = drawBehind {
    drawBurst(progress(), color)
}

/**
 * The burst at [progress], 0 to 1, round the centre of this scope: [BURST_COUNT] circles and squares in [color] flying
 * out and fading. At 0 and at 1 it draws nothing, so a burst that is over leaves no trace. Plain shapes only, no emoji.
 */
internal fun DrawScope.drawBurst(progress: Float, color: Color) {
    if (progress <= 0f || progress >= 1f) return
    val frame = burstFrame(progress)
    val half = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    burstParticles().forEach { particle ->
        val radians = particle.angle * PI.toFloat() / HALF_TURN
        val away = half * (BURST_START + (BURST_REACH - BURST_START) * particle.reach * frame.distance)
        val at = Offset(center.x + cos(radians) * away, center.y + sin(radians) * away)
        val edge = half * particle.size * frame.scale * 2f
        val shapeColor = color.copy(alpha = color.alpha * frame.alpha)
        when (particle.shape) {
            BurstShape.Circle -> drawCircle(color = shapeColor, radius = edge / 2f, center = at)
            BurstShape.Square -> rotate(degrees = particle.spin * progress, pivot = at) {
                drawRect(
                    color = shapeColor,
                    topLeft = Offset(at.x - edge / 2f, at.y - edge / 2f),
                    size = Size(edge, edge),
                )
            }
        }
    }
}

private const val HALF_TURN = 180f
