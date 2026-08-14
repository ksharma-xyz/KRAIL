package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.animations.rememberAiSpinAngle
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiCoolGradientTokens
import xyz.ksharma.krail.taj.tokens.AiGradientTokens
import xyz.ksharma.krail.taj.tokens.IconSizeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SPOKE_COUNT = 6
private const val DEGREES_PER_SPOKE = 360f / SPOKE_COUNT
private const val RIM_RADIUS_FRACTION = 0.9f
private const val HUB_RADIUS_FRACTION = 0.28f
private const val SPOKE_OUTER_FRACTION = 0.92f
private const val RIM_STROKE_FRACTION = 0.22f
private const val SPOKE_STROKE_FRACTION = 0.16f
private const val DEGREES_TO_RADIANS = PI / 180.0

/**
 * KRAIL's on-device-AI mark: a wheel (rim, spokes, hub), not a sparkle — the 4-point
 * sparkle is Google Gemini's actual mark. The gradient lives in the wheel's own stroke, not
 * a filled badge behind it.
 *
 * @param spinning While `true`, rotates at a slow constant speed (the "generating" state),
 * then decelerates to a full stop when it flips to `false` — see [rememberAiSpinAngle].
 * @param colors Gradient stops for the rim/hub/spokes. Defaults to [AiGradientTokens] (the
 * "this text was generated from these transport modes" signal used on the alert-summary
 * card); pass [xyz.ksharma.krail.taj.tokens.AiCoolGradientTokens] for the "you're talking
 * to the AI right now" signal instead — same mark, same motion, different gradient role.
 * @param markSize Side length of the mark. Defaults to [IconSizeTokens.S] (this mark's usual
 * small-indicator context); pass a caller's own icon-size token to match a specific
 * surrounding button/icon, e.g. sitting inside a [RoundIconButton] alongside other icons.
 */
@Composable
fun AiWheelMark(
    spinning: Boolean,
    modifier: Modifier = Modifier,
    colors: List<Color> = AiGradientTokens.stops,
    markSize: Dp = IconSizeTokens.S,
) {
    val angle = rememberAiSpinAngle(spinning)
    // Horizontal, not the default corner to corner. The mark is a circle inside a square, so a
    // diagonal gradient spends its first and last stretch in the empty corners and the two end
    // colours barely appear on the wheel itself — a theme's own colour was getting cropped out
    // of its own gradient. Left edge to right edge puts both ends on the rim where they show.
    val gradientBrush = remember(colors) { Brush.horizontalGradient(colors) }

    Canvas(
        modifier = modifier
            .size(markSize)
            .graphicsLayer { rotationZ = angle },
    ) {
        val radius = size.minDimension / 2f
        val rimRadius = radius * RIM_RADIUS_FRACTION
        val hubRadius = radius * HUB_RADIUS_FRACTION
        val spokeOuter = rimRadius * SPOKE_OUTER_FRACTION

        drawCircle(
            brush = gradientBrush,
            radius = rimRadius,
            style = Stroke(width = radius * RIM_STROKE_FRACTION),
        )
        drawCircle(brush = gradientBrush, radius = hubRadius)

        for (i in 0 until SPOKE_COUNT) {
            val radians = i * DEGREES_PER_SPOKE * DEGREES_TO_RADIANS
            val dx = cos(radians).toFloat()
            val dy = sin(radians).toFloat()
            drawLine(
                brush = gradientBrush,
                start = center + Offset(dx * hubRadius, dy * hubRadius),
                end = center + Offset(dx * spokeOuter, dy * spokeOuter),
                strokeWidth = radius * SPOKE_STROKE_FRACTION,
                cap = StrokeCap.Round,
            )
        }
    }
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAiWheelMarkAtRest() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiWheelMark(spinning = false, modifier = Modifier)
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAiWheelMarkCool() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiWheelMark(spinning = false, modifier = Modifier, colors = AiCoolGradientTokens.stops)
    }
}

@PreviewComponent
@Composable
private fun PreviewAiWheelMarkSpinning() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiWheelMark(spinning = true, modifier = Modifier)
    }
}

// endregion
