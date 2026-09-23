package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A cloud of the AI colours, for a surface that is listening. The cloud IS the surface: there
 * is no card or core under the content, only soft blobs of [colors] spread under all of it,
 * swelling with [voiceLevel] while the rider speaks, orbiting together while [orbiting], and
 * dimming while [quiet].
 *
 * It had an opaque squircle core once, with the blobs ringing its edge. On a phone the core
 * read as a dark slab and hid most of the colour, which was the whole point of the surface.
 * Every blob has no edge of its own, so the cloud has no outline that could move under text.
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
        content()
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
    // Each colour twice, on opposite sides, so a wide surface is covered end to end rather
    // than lit in four patches.
    val blobs = colors + colors
    blobs.forEachIndexed { index, color ->
        val seed = index.toFloat()
        val home = seed / blobs.size * TWO_PI
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

// Room around the content for the cloud's soft edge, so the colour fades out past the words
// rather than stopping at them.
private val RingSpace = 28.dp

private const val TWO_PI = (2 * PI).toFloat()
private const val MILLIS_PER_SECOND = 1_000f
private const val MAX_FRAME_SECONDS = 0.1f

private const val LIGHT_PEAK_ALPHA = 0.55f
private const val DARK_PEAK_ALPHA = 0.5f
private const val MID_ALPHA_RATIO = 0.45f
private const val BLOB_MID_STOP = 0.45f
private const val QUIET_STRENGTH = 0.35f
private const val ORBIT_STRENGTH = 1.15f
private const val STRENGTH_MILLIS = 500

private const val BLOB_RADIUS_FRAC = 0.5f
private const val BLOB_ENERGY_GAIN = 0.18f
private const val RING_REACH = 0.42f
private const val RING_REACH_WOBBLE = 0.06f
private const val DRIFT_RADIANS = 0.35f
private const val BASE_PERIOD_SECONDS = 9f
private const val PERIOD_STEP_SECONDS = 2f
private const val BREATHE_SECONDS = 7f

private const val ORBIT_RADIANS_PER_SECOND = TWO_PI / 1.6f
private const val ORBIT_EASE_PER_SECOND = 3f
private const val LEVEL_RISE_PER_SECOND = 14f
private const val LEVEL_FALL_PER_SECOND = 3f
