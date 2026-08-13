package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiCoolGradientTokens

private const val RING_COUNT = 3
private const val RIPPLE_CYCLE_MS = 2400
private const val RIPPLE_MIN_SCALE = 0.45f
private const val RIPPLE_MAX_SCALE = 1.05f
private const val RIPPLE_MAX_ALPHA = 0.55f
private val IndicatorSize = 96.dp
private val RingStrokeWidth = 1.5.dp

/**
 * Thinking/processing state: the same [AiWheelMark] as [AiListeningIndicator], just
 * spinning faster, with three staggered rings expanding outward from its rim — the real
 * counterpart to `ai_search_input_mockup.html` stage 04. One shared "phase" driver offsets
 * the 3 rings by a third of the cycle each rather than 3 independent `Animatable`s, mirroring
 * the mockup's staggered `animation-delay` approach.
 */
@Composable
fun AiThinkingIndicator(modifier: Modifier = Modifier, active: Boolean = true) {
    Box(modifier = modifier.size(IndicatorSize), contentAlignment = Alignment.Center) {
        RippleRings(active = active)
        AiWheelMark(spinning = active, colors = AiCoolGradientTokens.stops)
    }
}

@Composable
private fun RippleRings(active: Boolean, modifier: Modifier = Modifier) {
    // Animatable + LaunchedEffect(active), not rememberInfiniteTransition unconditionally —
    // see the identical note in AiScanSweep.kt / AiListeningIndicator.kt's AiWaveform.
    val phaseState = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            phaseState.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = RIPPLE_CYCLE_MS, easing = LinearEasing),
                ),
            )
        } else {
            phaseState.snapTo(0f)
        }
    }
    val phase = phaseState.value
    val color = AiCoolGradientTokens.Violet

    Canvas(modifier = modifier.size(IndicatorSize)) {
        val strokePx = RingStrokeWidth.toPx()
        val maxRadius = size.minDimension / 2f
        for (ring in 0 until RING_COUNT) {
            val ringPhase = (phase + ring.toFloat() / RING_COUNT) % 1f
            val scale = RIPPLE_MIN_SCALE + (RIPPLE_MAX_SCALE - RIPPLE_MIN_SCALE) * ringPhase
            val alpha = if (active) RIPPLE_MAX_ALPHA * (1f - ringPhase) else 0f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = maxRadius * scale,
                style = Stroke(width = strokePx),
            )
        }
    }
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAiThinkingIndicatorFrozen() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiThinkingIndicator(active = false)
    }
}

@PreviewComponent
@Composable
private fun PreviewAiThinkingIndicatorActive() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiThinkingIndicator(active = true)
    }
}

// endregion
