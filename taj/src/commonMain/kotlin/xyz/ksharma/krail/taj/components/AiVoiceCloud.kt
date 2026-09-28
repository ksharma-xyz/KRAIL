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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A cloud of the AI colours, for a surface that is listening. The cloud IS the surface: there
 * is no card or core under the content, only soft blobs of [colors] spread under all of it,
 * swelling with [voiceLevel] while the rider speaks, stirring in place with colour flowing through it while [orbiting], and
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
                    clock = clock,
                )
            }
            .padding(RingSpace),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Frame-driven state, read only in draw and layer lambdas so a frame never recomposes.
 *
 * [flow] and [colourPhase] are plain fields rather than state: [time] changes on every frame the
 * loop runs, and reading it in draw is what invalidates the draw. Only the loop writes them.
 */
private class CloudClock {
    val time = mutableFloatStateOf(0f)
    val energy = mutableFloatStateOf(0f)

    /** How far into the working look the cloud is, 0 at rest and 1 fully working. */
    val think = mutableFloatStateOf(0f)

    /**
     * The clock the blobs drift and breathe on. It runs faster while working, so the same
     * motions the cloud makes at rest simply quicken: the cloud stirs in place. It never
     * orbits. Blobs swept around the ring separated from one another as they went, and each
     * one read as a disc of colour rather than as part of a cloud.
     */
    var flow = 0f
        private set
    var colourPhase = 0f
        private set

    fun advance(dtSeconds: Float, orbitTarget: Float, levelTarget: Float) {
        // A dropped frame after a pause would otherwise jump the whole cloud at once.
        val dt = dtSeconds.coerceIn(0f, MAX_FRAME_SECONDS)
        time.floatValue += dt

        // Eases in quickly, so Send is answered at once, and out slowly, so the cloud settles
        // back to listening instead of stopping.
        val current = think.floatValue
        val thinkRate = if (orbitTarget > current) THINK_RISE_PER_SECOND else THINK_FALL_PER_SECOND
        val thinking = current + (orbitTarget - current) * (dt * thinkRate).coerceAtMost(1f)
        think.floatValue = thinking

        flow += dt * (1f + FLOW_SPEEDUP * thinking)
        colourPhase = (colourPhase + thinking * dt / COLOUR_CYCLE_SECONDS) % 1f

        // Quick to rise and slow to fall, which is how a voice looks: syllables arrive as
        // bursts, and the cloud should hang on to each one for a moment rather than blink.
        val rate = if (levelTarget > energy.floatValue) LEVEL_RISE_PER_SECOND else LEVEL_FALL_PER_SECOND
        val level = energy.floatValue
        energy.floatValue = level + (levelTarget - level) * (dt * rate).coerceAtMost(1f)
    }
}

private fun DrawScope.drawRings(colors: List<Color>, peakAlpha: Float, clock: CloudClock) {
    if (colors.isEmpty()) return
    // Read here, inside draw, so each frame re-draws without recomposing. time is read only to
    // subscribe the draw to the frame loop; the motion itself runs on flow.
    clock.time.floatValue
    val think = clock.think.floatValue
    val energy = clock.energy.floatValue
    val flow = clock.flow
    val colourPhase = clock.colourPhase
    val centre = Offset(size.width / 2f, size.height / 2f)
    val blobRadius = size.minDimension * (BLOB_RADIUS_FRAC + BLOB_ENERGY_GAIN * energy)
    val driftRadians = DRIFT_RADIANS + THINK_DRIFT_RADIANS * think
    // Each colour twice, on opposite sides, so a wide surface is covered end to end rather
    // than lit in four patches.
    val blobs = colors + colors
    blobs.forEachIndexed { index, restColour ->
        val seed = index.toFloat()
        val home = seed / blobs.size * TWO_PI
        val drift = driftRadians * sin(flow * TWO_PI / (BASE_PERIOD_SECONDS + seed * PERIOD_STEP_SECONDS))
        val angle = home + drift
        val reach = RING_REACH + RING_REACH_WOBBLE * cos(flow * TWO_PI / (BREATHE_SECONDS + seed))
        val blobCentre = Offset(
            x = centre.x + size.width / 2f * reach * cos(angle),
            y = centre.y + size.height / 2f * reach * sin(angle),
        )
        // While working, each blob travels along the gradient instead of holding its one
        // colour, offset from its neighbours so the colour flows through the cloud while the
        // shapes stay put. Blended by think so the change in and out is gradual.
        val travelling = colors.sampleThereAndBack(seed / blobs.size + colourPhase)
        val colour = lerp(restColour, travelling, think)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = softFalloff(colour, peakAlpha),
                center = blobCentre,
                radius = blobRadius,
            ),
            radius = blobRadius,
            center = blobCentre,
        )
    }
}

/**
 * A falloff that eases to nothing rather than ramping to it. A straight ramp from a mid stop to
 * transparent left a faint rim where the slope changed, and a blob moving over the others
 * showed that rim as the outline of a circle.
 */
private fun softFalloff(colour: Color, peakAlpha: Float): Array<Pair<Float, Color>> =
    FALLOFF_STOPS.map { (stop, share) -> stop to colour.copy(alpha = peakAlpha * share) }.toTypedArray()

/**
 * A colour from the gradient at [position], going from the first stop to the last and back
 * again rather than wrapping. Wrapping would blend the last stop straight into the first, the
 * one pair the gradient's middle stop exists to keep apart (see AiThemeGradientTokens).
 */
private fun List<Color>.sampleThereAndBack(position: Float): Color {
    if (size == 1) return first()
    val cycle = ((position % 1f) + 1f) % 1f
    val along = (1f - abs(2f * cycle - 1f)) * (size - 1)
    val index = along.toInt().coerceAtMost(size - 2)
    return lerp(this[index], this[index + 1], along - index)
}

// Room around the content for the cloud's soft edge, so the colour fades out past the words
// rather than stopping at them.
private val RingSpace = 28.dp

private const val TWO_PI = (2 * PI).toFloat()
private const val MILLIS_PER_SECOND = 1_000f
private const val MAX_FRAME_SECONDS = 0.1f

private const val LIGHT_PEAK_ALPHA = 0.55f
private const val DARK_PEAK_ALPHA = 0.5f
private const val QUIET_STRENGTH = 0.35f
private const val ORBIT_STRENGTH = 1.05f
private const val STRENGTH_MILLIS = 500

private const val BLOB_RADIUS_FRAC = 0.5f
private const val BLOB_ENERGY_GAIN = 0.18f
private const val RING_REACH = 0.42f
private const val RING_REACH_WOBBLE = 0.06f
private const val DRIFT_RADIANS = 0.35f
private const val BASE_PERIOD_SECONDS = 9f
private const val PERIOD_STEP_SECONDS = 2f
private const val BREATHE_SECONDS = 7f

private const val THINK_RISE_PER_SECOND = 3f
private const val THINK_FALL_PER_SECOND = 1.6f

// While working the drift clock runs this much faster and each blob wanders a little further
// either side of home. Still well short of a neighbour's home, so no blob ever leaves its place.
private const val FLOW_SPEEDUP = 2.5f
private const val THINK_DRIFT_RADIANS = 0.2f

private const val COLOUR_CYCLE_SECONDS = 3.2f

// Roughly a Gaussian: full at the centre, most of the colour gone by the middle, and a long
// tail to nothing, so the edge never shows.
private val FALLOFF_STOPS = listOf(
    0f to 1f,
    0.25f to 0.8f,
    0.5f to 0.42f,
    0.75f to 0.12f,
    1f to 0f,
)
private const val LEVEL_RISE_PER_SECOND = 14f
private const val LEVEL_FALL_PER_SECOND = 3f
