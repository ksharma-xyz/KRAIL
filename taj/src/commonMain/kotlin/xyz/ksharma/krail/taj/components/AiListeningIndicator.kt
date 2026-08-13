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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiCoolGradientTokens
import kotlin.math.abs
import kotlin.math.sin

private const val BAR_COUNT = 14
private const val WAVE_CYCLE_MS = 5200
private const val PHASE_STEP = 0.5f
private const val MIN_BAR_HEIGHT_FRACTION = 0.14f
private const val MAX_BAR_HEIGHT_FRACTION = 0.9f
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
fun AiListeningIndicator(modifier: Modifier = Modifier, active: Boolean = true) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        AiWheelMark(spinning = active, colors = AiCoolGradientTokens.stops)
        AiWaveform(active = active)
    }
}

@Composable
private fun AiWaveform(active: Boolean, modifier: Modifier = Modifier) {
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
    val brush = remember { Brush.verticalGradient(AiCoolGradientTokens.stops) }

    Canvas(modifier = modifier.width(WaveformWidth).height(WaveformHeight)) {
        val barGap = size.width / BAR_COUNT
        val barStrokePx = BarWidth.toPx()
        for (i in 0 until BAR_COUNT) {
            val phase = i * PHASE_STEP
            val amplitude = if (active) {
                abs(sin(time * 2 * kotlin.math.PI * 2 + phase))
            } else {
                MIN_BAR_HEIGHT_FRACTION.toDouble()
            }
            val barHeight = size.height * (
                MIN_BAR_HEIGHT_FRACTION +
                    (MAX_BAR_HEIGHT_FRACTION - MIN_BAR_HEIGHT_FRACTION) * amplitude.toFloat()
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
