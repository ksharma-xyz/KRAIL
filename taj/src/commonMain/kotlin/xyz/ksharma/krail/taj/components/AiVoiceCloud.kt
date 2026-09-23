package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * A cloud of the AI colours around a steady core, for a surface that is listening.
 *
 * The rings are the part that moves: soft blobs of [colors] drifting round the core's edge,
 * swelling with [voiceLevel] while the rider speaks, orbiting together while [orbiting], and
 * dimming to a whisper while [quiet]. The core is the part that does not. It is opaque in both
 * themes and its outline only breathes by a few percent, because the rider's words and the
 * surface's controls sit on it, and text on an edge that keeps changing shape is text that
 * cannot be read.
 *
 * Drawn the way [CloudGradientBackground] is, for the same reasons: three-stop radial blobs
 * with no `Modifier.blur`, and one draw lambda re-run per frame.
 *
 * [voiceLevel] is read at draw time, never in composition. It changes many times a second while
 * someone speaks, and reading it in composition would recompose the whole surface to move a
 * gradient. It is smoothed here as well, so a jumpy level still moves the cloud like breath
 * rather than like a meter.
 *
 * The clock is a frame loop on [withInfiniteAnimationFrameMillis], started by [LaunchedEffect].
 * The infinite variant is what lets a UI test's infinite-animation policy pause it; a plain
 * withFrameMillis loop keeps asking for frames and Compose never goes idle, which hung
 * `AskKrailHandoffFlowTest` for its whole sixty seconds.
 */
@Composable
fun AiVoiceCloud(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    voiceLevel: () -> Float = { 0f },
    orbiting: Boolean = false,
    quiet: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val coreColor = KrailTheme.colors.surface
    val peakAlpha = if (isAppInDarkMode()) DARK_PEAK_ALPHA else LIGHT_PEAK_ALPHA
    val strength by animateFloatAsState(
        targetValue = when {
            quiet -> QUIET_STRENGTH
            orbiting -> ORBIT_STRENGTH
            else -> 1f
        },
        animationSpec = tween(durationMillis = STRENGTH_MILLIS),
        label = "aiVoiceCloudStrength",
    )
    val orbitTarget by rememberUpdatedState(if (orbiting && !reduceMotion) 1f else 0f)
    val level by rememberUpdatedState(voiceLevel)

    val clock = remember { CloudClock() }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        var last = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                clock.advance(
                    dtSeconds = (now - last) / MILLIS_PER_SECOND,
                    orbitTarget = orbitTarget,
                    levelTarget = level().coerceIn(0f, 1f),
                )
                last = now
            }
        }
    }

    Box(
        // No offscreen layer, unlike the background field. A layer clips to its own bounds, and
        // the blobs deliberately reach past this box's edge: clipped, their soft falloff ended
        // in a hard line. Nothing here blends, so drawing straight onto the parent costs nothing.
        modifier = modifier
            .drawBehind {
                drawRings(
                    colors = colors,
                    peakAlpha = peakAlpha * strength,
                    time = clock.time.floatValue,
                    orbitAngle = clock.orbitAngle.floatValue,
                    energy = clock.energy.floatValue,
                )
            }
            .padding(RingSpace),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    shape = CloudCoreShape(phase = clock.time.floatValue)
                    clip = true
                }
                .background(coreColor),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** Frame-driven state, read only in draw and layer lambdas so a frame never recomposes. */
private class CloudClock {
    val time = mutableFloatStateOf(0f)
    val orbitAngle = mutableFloatStateOf(0f)
    val energy = mutableFloatStateOf(0f)
    private var orbitSpeed = 0f

    fun advance(dtSeconds: Float, orbitTarget: Float, levelTarget: Float) {
        // A dropped frame after a pause would otherwise jump the whole cloud at once.
        val dt = dtSeconds.coerceIn(0f, MAX_FRAME_SECONDS)
        time.floatValue += dt
        orbitSpeed += (orbitTarget - orbitSpeed) * (dt * ORBIT_EASE_PER_SECOND).coerceAtMost(1f)
        orbitAngle.floatValue = (orbitAngle.floatValue + orbitSpeed * ORBIT_RADIANS_PER_SECOND * dt) % TWO_PI
        // Quick to rise and slow to fall, which is how a voice looks: syllables arrive as
        // bursts, and the cloud should hang on to each one for a moment rather than blink.
        val rate = if (levelTarget > energy.floatValue) LEVEL_RISE_PER_SECOND else LEVEL_FALL_PER_SECOND
        val current = energy.floatValue
        energy.floatValue = current + (levelTarget - current) * (dt * rate).coerceAtMost(1f)
    }
}

private fun DrawScope.drawRings(
    colors: List<Color>,
    peakAlpha: Float,
    time: Float,
    orbitAngle: Float,
    energy: Float,
) {
    if (colors.isEmpty()) return
    val centre = Offset(size.width / 2f, size.height / 2f)
    val blobRadius = size.minDimension * (BLOB_RADIUS_FRAC + BLOB_ENERGY_GAIN * energy)
    colors.forEachIndexed { index, color ->
        val seed = index.toFloat()
        val home = seed / colors.size * TWO_PI
        val drift = DRIFT_RADIANS * sin(time * TWO_PI / (BASE_PERIOD_SECONDS + seed * PERIOD_STEP_SECONDS))
        val angle = home + drift + orbitAngle
        val reach = RING_REACH + RING_REACH_WOBBLE * cos(time * TWO_PI / (BREATHE_SECONDS + seed))
        val blobCentre = Offset(
            x = centre.x + size.width / 2f * reach * cos(angle),
            y = centre.y + size.height / 2f * reach * sin(angle),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to color.copy(alpha = peakAlpha),
                    BLOB_MID_STOP to color.copy(alpha = peakAlpha * MID_ALPHA_RATIO),
                    1f to Color.Transparent,
                ),
                center = blobCentre,
                radius = blobRadius,
            ),
            radius = blobRadius,
            center = blobCentre,
        )
    }
}

/**
 * A squircle whose edge breathes by a few percent.
 *
 * A superellipse rather than a rounded rectangle, so the corners read as soft rather than as
 * a card; a wobble that small, so the rider's words never meet the moving edge. The wobble is
 * two low-frequency waves on coprime cycles, so the outline never visibly repeats.
 */
private class CloudCoreShape(private val phase: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val halfWidth = size.width / 2f
        val halfHeight = size.height / 2f
        for (step in 0..OUTLINE_STEPS) {
            val theta = step.toFloat() / OUTLINE_STEPS * TWO_PI
            // Inward only, between 1 - CORE_WOBBLE and 1. The core's fill is its own bounds, so
            // an outward bulge would show nothing and read as a flat spot.
            val waves = (
                sin(theta * WOBBLE_LOBES_A + phase * WOBBLE_SPEED_A) +
                    sin(theta * WOBBLE_LOBES_B - phase * WOBBLE_SPEED_B)
                ) / 2f
            val wobble = 1f - CORE_WOBBLE * (1f + waves) / 2f
            val c = cos(theta)
            val s = sin(theta)
            val x = halfWidth + halfWidth * wobble * sign(c) * abs(c).pow(SUPERELLIPSE_POWER)
            val y = halfHeight + halfHeight * wobble * sign(s) * abs(s).pow(SUPERELLIPSE_POWER)
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

// Room around the core for the rings to show in. The core is laid out inside it, so the rings
// never sit under the rider's words.
private val RingSpace = 28.dp

private const val TWO_PI = (2 * PI).toFloat()
private const val MILLIS_PER_SECOND = 1_000f
private const val MAX_FRAME_SECONDS = 0.1f

private const val LIGHT_PEAK_ALPHA = 0.7f
private const val DARK_PEAK_ALPHA = 0.55f
private const val MID_ALPHA_RATIO = 0.45f
private const val BLOB_MID_STOP = 0.45f
private const val QUIET_STRENGTH = 0.35f
private const val ORBIT_STRENGTH = 1.15f
private const val STRENGTH_MILLIS = 500

private const val BLOB_RADIUS_FRAC = 0.42f
private const val BLOB_ENERGY_GAIN = 0.16f
private const val RING_REACH = 0.62f
private const val RING_REACH_WOBBLE = 0.06f
private const val DRIFT_RADIANS = 0.35f
private const val BASE_PERIOD_SECONDS = 9f
private const val PERIOD_STEP_SECONDS = 2f
private const val BREATHE_SECONDS = 7f

private const val ORBIT_RADIANS_PER_SECOND = TWO_PI / 1.6f
private const val ORBIT_EASE_PER_SECOND = 3f
private const val LEVEL_RISE_PER_SECOND = 14f
private const val LEVEL_FALL_PER_SECOND = 3f

// 2 / n for a superellipse of order n; 0.5 is n = 4, a squircle.
private const val SUPERELLIPSE_POWER = 0.5f
private const val OUTLINE_STEPS = 96
private const val CORE_WOBBLE = 0.025f
private const val WOBBLE_LOBES_A = 3f
private const val WOBBLE_LOBES_B = 2f
private const val WOBBLE_SPEED_A = 0.7f
private const val WOBBLE_SPEED_B = 0.45f
