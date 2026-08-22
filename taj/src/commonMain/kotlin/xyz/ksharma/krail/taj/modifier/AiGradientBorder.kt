package xyz.ksharma.krail.taj.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.animations.AiSpinDefaults
import xyz.ksharma.krail.taj.animations.rememberAiSpinAngle
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiGradientTokens
import xyz.ksharma.krail.taj.tokens.StrokeTokens
import kotlin.math.hypot

/**
 * The card-level counterpart to [xyz.ksharma.krail.taj.components.AiWheelMark] — a hairline
 * [AiGradientTokens] border that rotates like a sweep gradient, sharing the exact same
 * spin/settle timing via [rememberAiSpinAngle] so a wheel and its card animate in the same
 * rhythm when driven by the same `spinning` flag.
 *
 * The border is always the same gradient; only its rotation speed changes (constant while
 * [spinning], decelerating to a stop when it flips to `false`) — never swapped for a
 * different gradient at rest, which reads as the color abruptly disappearing.
 *
 * The ring's outline never rotates — only the paint inside it does. An earlier version
 * rotated the `drawRoundRect` geometry itself, which is only safe for a circle; on a wide
 * rectangular card its corners swing far outside the card at most angles (a rotated
 * rectangle's diagonal exceeds its own bounds). The fix: draw the static stroke ring into
 * an offscreen layer as an alpha mask, then composite a rotating gradient-filled square
 * into it with [BlendMode.SrcIn] — every Compose Multiplatform target (Android and iOS
 * both render through Skia) supports this without a platform-specific shader matrix.
 *
 * @param colors Gradient stops for the sweep. Defaults to [AiGradientTokens]; pass
 * [xyz.ksharma.krail.taj.tokens.AiCoolGradientTokens] for AI-input surfaces instead of
 * AI-summary surfaces — see [xyz.ksharma.krail.taj.components.AiWheelMark]'s `colors` param
 * for the same distinction on the mark itself.
 */
@Composable
fun Modifier.aiGradientBorder(
    spinning: Boolean,
    cornerRadius: Dp,
    strokeWidth: Dp = StrokeTokens.Thick,
    colors: List<Color> = AiGradientTokens.stops,
    // Lets a caller fade the whole ring in and out. At zero the ring is not drawn at all,
    // which is what a surface that only wears this while it works needs: without it the
    // border is simply always there, and "working" reads as a ring that stops turning.
    alpha: Float = 1f,
    // One full turn of the paint. Default is the shared rhythm every AI mark uses; a surface
    // where the spin is the whole show may run faster (see rememberAiSpinAngle).
    spinDurationMillis: Int = AiSpinDefaults.SPIN_DURATION_MILLIS,
): Modifier {
    val angle = rememberAiSpinAngle(spinning, spinDurationMillis)
    if (alpha <= 0f) return this
    val brush = remember(colors) {
        Brush.sweepGradient(colors + colors.first())
    }

    return this.drawWithContent {
        drawContent()
        val strokePx = strokeWidth.toPx()
        val half = strokePx / 2f
        val ringTopLeft = Offset(half, half)
        val ringSize = Size(size.width - strokePx, size.height - strokePx)

        // The centreline's radius, not the shape's.
        //
        // [cornerRadius] is the radius of the surface this ring sits on, which is the radius
        // its OUTER edge has to follow. The stroke is centred on a rect inset by half its
        // width, and a stroke's outer edge curves at the centreline radius plus that same
        // half. Passing the shape's radius straight through therefore drew the outer edge at
        // `cornerRadius + half`: a tighter-cornered ring than the card it was tracing, so at
        // every corner the ring pulled inward and the card's own background showed as a pale
        // rim outside it. At the dialog's working stroke that rim was 3dp of white.
        //
        // Floored at zero, so a stroke thicker than the radius degrades to a square-cornered
        // ring rather than inverting into a negative radius.
        val ringCornerRadius = (cornerRadius.toPx() - half).coerceAtLeast(0f)

        // The gradient-filled square must fully cover the ring's bounding box at every
        // rotation angle, so its side is the card's own diagonal, centered on the card.
        val coverSide = hypot(size.width, size.height)
        val coverTopLeft = Offset(
            (size.width - coverSide) / 2f,
            (size.height - coverSide) / 2f,
        )

        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), Paint())

            // 1. Static mask: the stroke ring, unrotated. This is the only thing that
            // defines where paint can land — its geometry never moves.
            drawRoundRect(
                // The mask's own alpha scales the gradient composited into it by SrcIn, so
                // fading the mask fades the ring.
                color = Color.Black.copy(alpha = alpha),
                style = Stroke(width = strokePx),
                cornerRadius = CornerRadius(ringCornerRadius),
                topLeft = ringTopLeft,
                size = ringSize,
            )

            // 2. Rotating paint: composited only where the mask has alpha, so this is
            // the only thing that visibly moves.
            rotate(degrees = angle) {
                drawRect(
                    brush = brush,
                    topLeft = coverTopLeft,
                    size = Size(coverSide, coverSide),
                    blendMode = BlendMode.SrcIn,
                )
            }

            canvas.restore()
        }
    }
}

// region Previews

/**
 * The exact stack the Ask KRAIL dialog wears: a clipped, filled, rounded surface with this ring
 * on top of it, over a dark backdrop that makes any gap between the two visible.
 *
 * It exists because a gap was there and nobody could see it in a preview. The ring kept the
 * shape's corner radius for its own centreline, so its outer edge curved at a tighter radius
 * than the card, and the card's white background showed through at every corner. On a white
 * card against a grey scrim that reads as a pale rim, which is easy to mistake for a shadow.
 *
 * The backdrop is deliberately dark and the surface deliberately light: the failure is invisible
 * when both are the same colour, which is exactly how it survived.
 */
@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAiGradientBorderOnFilledSurface() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        Box(
            modifier = Modifier
                .background(Color(0xFF404040))
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .aiGradientBorder(
                        spinning = false,
                        cornerRadius = 28.dp,
                        strokeWidth = 6.dp,
                    ),
            )
        }
    }
}

// endregion
