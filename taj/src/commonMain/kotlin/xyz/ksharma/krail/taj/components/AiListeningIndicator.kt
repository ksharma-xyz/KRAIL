package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiCoolGradientTokens

private const val RING_COUNT = 3
private const val RING_CYCLE_MS = 2600
private const val RING_START_SCALE = 0.5f
private const val RING_END_SCALE = 1.5f
private const val RING_START_ALPHA = 0.55f
private const val RING_FADE_POINT = 0.7f
private const val RING_FADE_ALPHA = 0.10f

private const val TURN_CYCLE_MS = 2600
private const val TURN_OVERSHOOT_DEGREES = 352f
private const val TURN_SETTLE_BACK_DEGREES = 338f
private const val TURN_FULL_DEGREES = 360f
private const val TURN_OVERSHOOT_AT_MS = 1196 // 46% of the cycle
private const val TURN_SETTLE_AT_MS = 1560 // 60%
private const val TURN_REST_AT_MS = 2028 // 78%, then still until the loop restarts

private val IndicatorSize = 64.dp
private val WheelSize = 32.dp
private val RingStroke = 1.5.dp

/**
 * What the AI surface shows while it is busy, in the two ways it can be busy.
 *
 * The two modes are deliberately different motions rather than the same spinner with different
 * words next to it:
 *
 * - [AiActivity.Listening] does not rotate at all. The rings carry it. A wheel spinning inside
 *   rings that are already radiating is two things shouting the same thing, and the rider is
 *   being asked to speak, not to watch.
 * - [AiActivity.Working] turns once, overshoots slightly, settles back, then holds still for a
 *   beat before going again. Rotation therefore means thinking, and only thinking, which is
 *   what lets it mean anything at all.
 *
 * Replaces a waveform. A waveform implies it is drawing what it hears, and it was not: the bars
 * were a sine wave on a timer, identical whether the rider spoke or sat in silence.
 */
@Composable
fun AiListeningIndicator(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    activity: AiActivity = AiActivity.Listening,
    colors: List<Color> = AiCoolGradientTokens.stops,
) {
    Box(
        modifier = modifier.size(IndicatorSize),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            RadiatingRings(colors = colors)
        }
        // Listening leaves the wheel still and lets the rings speak; working turns it. Rotated
        // here rather than through AiWheelMark's own `spinning`, which is a continuous spin at
        // a fixed rate: this needs a turn with an overshoot, a settle and a pause in it, and
        // widening that component's API for one caller would push the shape of this animation
        // into every other place the mark is drawn.
        val turn = if (active && activity == AiActivity.Working) rememberWorkingTurn() else 0f
        AiWheelMark(
            spinning = false,
            colors = colors,
            markSize = WheelSize,
            modifier = Modifier.graphicsLayer { rotationZ = turn },
        )
    }
}

/** Which kind of busy, which decides whether the wheel turns. */
enum class AiActivity { Listening, Working }

/**
 * Three rings on a staggered loop, each growing out of the wheel and fading.
 *
 * Drawn in one Canvas off a single animation value rather than as three animated composables:
 * the stagger is a phase offset on the same clock, so there is one animation running instead of
 * three, and they can never drift apart.
 */
@Composable
private fun RadiatingRings(colors: List<Color>, modifier: Modifier = Modifier) {
    // Animatable started by LaunchedEffect rather than rememberInfiniteTransition, which starts
    // its clock whether or not the value is read and hangs Robolectric's screenshot capture
    // waiting for that clock to go idle. Same reason rememberAiSpinAngle does it this way.
    val phase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        phase.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = RING_CYCLE_MS, easing = LinearEasing),
            ),
        )
    }
    val ringColor = colors.firstOrNull() ?: Color.Unspecified

    Canvas(modifier = modifier.size(IndicatorSize)) {
        val strokePx = RingStroke.toPx()
        val baseRadius = size.minDimension / 2f - strokePx
        repeat(RING_COUNT) { index ->
            val ringPhase = (phase.value + index.toFloat() / RING_COUNT) % 1f
            val scale = RING_START_SCALE + (RING_END_SCALE - RING_START_SCALE) * ringPhase
            drawCircle(
                color = ringColor,
                radius = baseRadius * scale,
                alpha = ringAlpha(ringPhase),
                style = Stroke(width = strokePx),
            )
        }
    }
}

/**
 * Full strength as a ring leaves the wheel, most of the fade done by [RING_FADE_POINT], gone at
 * the edge. Fading late rather than linearly keeps the ring readable while it is still near the
 * wheel, which is where it reads as coming *from* something.
 */
private fun ringAlpha(ringPhase: Float): Float = when {
    ringPhase <= RING_FADE_POINT ->
        RING_START_ALPHA - (RING_START_ALPHA - RING_FADE_ALPHA) * (ringPhase / RING_FADE_POINT)
    else -> {
        val tail = (ringPhase - RING_FADE_POINT) / (1f - RING_FADE_POINT)
        RING_FADE_ALPHA * (1f - tail)
    }
}

/**
 * One turn, a small overshoot past the top, a settle back to it, then stillness until the loop
 * comes round. The pause is what stops it reading as a spinner that could run forever.
 */
@Composable
private fun rememberWorkingTurn(): Float {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        angle.animateTo(
            targetValue = TURN_FULL_DEGREES,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = TURN_CYCLE_MS
                    0f at 0 using FastOutSlowInEasing
                    TURN_OVERSHOOT_DEGREES at TURN_OVERSHOOT_AT_MS using FastOutSlowInEasing
                    TURN_SETTLE_BACK_DEGREES at TURN_SETTLE_AT_MS using FastOutSlowInEasing
                    TURN_FULL_DEGREES at TURN_REST_AT_MS
                    TURN_FULL_DEGREES at TURN_CYCLE_MS
                },
                repeatMode = RepeatMode.Restart,
            ),
        )
    }
    return angle.value
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAiListeningIndicatorFrozen() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiListeningIndicator(active = false)
    }
}

@PreviewComponent
@Composable
private fun PreviewAiListeningIndicatorListening() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiListeningIndicator(active = true, activity = AiActivity.Listening)
    }
}

@PreviewComponent
@Composable
private fun PreviewAiListeningIndicatorWorking() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiListeningIndicator(active = true, activity = AiActivity.Working)
    }
}

// endregion
