package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiCoolGradientTokens
import kotlin.math.sin

private const val BAR_COUNT = 14
private const val WAVE_CYCLE_MS = 6000
private const val WAVES_PER_CYCLE = 2f
private const val PHASE_STEP = 0.5f
private const val MIN_BAR_HEIGHT_FRACTION = 0.14f
private const val MAX_BAR_HEIGHT_FRACTION = 0.9f
private const val ENVELOPE_FLOOR = 0.5f
private const val ENVELOPE_RANGE = 0.5f
private const val TAU = 2.0 * kotlin.math.PI
private val WaveformWidth = 120.dp
private val WaveformHeight = 40.dp
private val BarWidth = 4.dp

/**
 * Listening state: [AiWheelMark] spinning with [AiCoolGradientTokens], plus an
 * amplitude-style waveform underneath — the real counterpart to `ai_search_input_mockup.html`
 * stage 03's CSS sine-wave demo, driven by [kotlin.math.sin] over a single looping time
 * value instead of one `Animatable` per bar (14 independent infinite animations would be
 * wasteful for a component that's already spinning the wheel on its own driver).
 */
@Composable
fun AiListeningIndicator(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    colors: List<Color> = AiCoolGradientTokens.stops,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        AiWheelMark(spinning = active, colors = colors)
        AiWaveform(active = active, colors = colors)
    }
}

@Composable
private fun AiWaveform(active: Boolean, colors: List<Color>, modifier: Modifier = Modifier) {
    // Animatable + LaunchedEffect(active), not rememberInfiniteTransition unconditionally —
    // the latter starts its animation clock regardless of whether the value is read, which
    // hangs Robolectric's screenshot capture (it waits for the clock to go idle). Same
    // reasoning as rememberAiSpinAngle only starting infiniteRepeatable when spinning.
    val timeState = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            timeState.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = WAVE_CYCLE_MS, easing = LinearEasing),
                ),
            )
        } else {
            timeState.snapTo(0f)
        }
    }
    val time = timeState.value
    val brush = remember(colors) { Brush.verticalGradient(colors) }

    Canvas(modifier = modifier.width(WaveformWidth).height(WaveformHeight)) {
        val barGap = size.width / BAR_COUNT
        val barStrokePx = BarWidth.toPx()
        for (i in 0 until BAR_COUNT) {
            val amplitude = if (active) barAmplitude(time, i) else 0f
            val barHeight = size.height * (
                MIN_BAR_HEIGHT_FRACTION +
                    (MAX_BAR_HEIGHT_FRACTION - MIN_BAR_HEIGHT_FRACTION) * amplitude
                )
            val x = barGap * i + barGap / 2f
            drawLine(
                brush = brush,
                start = androidx.compose.ui.geometry.Offset(x, size.height / 2f + barHeight / 2f),
                end = androidx.compose.ui.geometry.Offset(x, size.height / 2f - barHeight / 2f),
                strokeWidth = barStrokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Height fraction for bar [index] at loop position [time] (0..1).
 *
 * Two sines multiplied, both on periods that divide the loop so it repeats seamlessly:
 *
 * - the travelling wave runs [WAVES_PER_CYCLE] times per loop, and [PHASE_STEP] spreads a
 *   little over one full wave across the row, so the crest reads as moving left to right
 *   rather than every bar pumping at once;
 * - the envelope runs once per loop and scales the whole row between [ENVELOPE_FLOOR] and
 *   full height, so the waveform swells and eases off the way a voice does.
 *
 * The sines are mapped from -1..1 into 0..1 rather than passed through `abs`. `abs` folds the
 * negative half back up, which both doubles the apparent rate and puts a hard corner at every
 * zero crossing — bars snapping off the floor is exactly what made this read as frantic.
 */
private fun barAmplitude(time: Float, index: Int): Float {
    val wave = sin(TAU * WAVES_PER_CYCLE * time + index * PHASE_STEP).toUnitRange()
    val envelope = ENVELOPE_FLOOR + ENVELOPE_RANGE * sin(TAU * time).toUnitRange()
    return wave * envelope
}

private fun Double.toUnitRange(): Float = ((this + 1.0) / 2.0).toFloat()

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
private fun PreviewAiListeningIndicatorActive() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiListeningIndicator(active = true)
    }
}

// endregion
