package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.CookieShapeBoxDefaults.BUMPS
import xyz.ksharma.krail.taj.components.CookieShapeBoxDefaults.SIZE
import xyz.ksharma.krail.taj.magicBorderColors
import xyz.ksharma.krail.taj.shapes.CookieShape
import xyz.ksharma.krail.taj.shapes.buildCookiePath
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * A box with a cookie-shaped background and optional multi-color stroke.
 * This is a reusable component that can be used to create cookie-like backgrounds or containers.
 *
 * @param modifier Modifier to be applied to the box.
 * @param backgroundColor Background color of the box, defaults to theme background color.
 * @param strokeColor Fallback single color stroke, used if [outlineBrush] is null
 * @param outlineBrush Optional Brush for multi-color stroke. If null, [strokeColor] is used.
 * @param cookieShadow Optional shadow to apply to the cookie shape.
 *                    If provided, it will be applied as a drop shadow to the box.
 *                    Defaults to null.
 * @see CookieShape
 */
@Composable
fun CookieShapeBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = KrailTheme.colors.surface,
    strokeColor: Color = Color.Transparent,
    outlineBrush: Brush? = null,
    cookieShadow: Shadow? = null,
    content: @Composable () -> Unit = {},
) {
    val shape = CookieShape()
    Box(
        modifier = modifier
            .then(
                if (cookieShadow != null) {
                    Modifier.dropShadow(
                        shape = shape,
                        shadow = cookieShadow,
                    )
                } else {
                    Modifier
                },
            )
            .size(SIZE)
            .background(color = backgroundColor, shape = CookieShape()),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val path = buildCookiePath(size, bumps = BUMPS, depthFraction = 0.12f, smooth = true)

            // Fill already applied by background, skip fill here
            val strokeWidth = size.minDimension * 0.018f

            // Primary multi-color (or single color) outline
            when {
                outlineBrush != null ->
                    drawPath(path, outlineBrush, style = Stroke(width = strokeWidth))

                else ->
                    drawPath(path, strokeColor, style = Stroke(width = strokeWidth))
            }
        }

        content()
    }
}

@Composable
fun CookieShapeCanvas(
    modifier: Modifier = Modifier,
    backgroundColor: Color = KrailTheme.colors.surface,
    stroke: Color = Color.Transparent,
) {
    Canvas(modifier.size(SIZE)) {
        val path = buildCookiePath(size, bumps = BUMPS, depthFraction = 0.13f, smooth = true)
        drawPath(path, backgroundColor)
        drawPath(path, stroke, style = Stroke(width = size.minDimension * 0.018f))
    }
}

object CookieShapeBoxDefaults {
    val SIZE = 180.dp
    val SHADOW_RADIUS = 12.dp
    val SHADOW_SPREAD = 4.dp

    // closer to actual size; avoids over-stretch washout
    val SHADOW_GRADIENT_END = Offset(400f, 400f)
    const val SHADOW_ALPHA = 0.95f // slightly under 1 to keep saturation

    const val BUMPS = 9

    @Composable
    fun cookieShapeShadow(): Shadow = Shadow(
        radius = SHADOW_RADIUS, // keep value low, lower blur -> less muddy
        spread = SHADOW_SPREAD, // extend outward so it is visible
        brush = Brush.linearGradient(
            colors = magicBorderColors(),
            start = Offset.Zero,
            end = SHADOW_GRADIENT_END,
        ),
        alpha = SHADOW_ALPHA,
    )
}

@Preview
@Composable
private fun CookiePreviewBox() {
    KrailTheme {
        CookieShapeBox()
    }
}

@Preview
@Composable
private fun CookiePreviewCanvas() {
    KrailTheme {
        CookieShapeCanvas()
    }
}

@Preview
@Composable
private fun CookieShapeBoxSweepGradient() {
    CookieShapeBox(
        outlineBrush = Brush.sweepGradient(
            listOf(
                Color(0xFFFF5722),
                Color(0xFFFFC107),
                Color(0xFF4CAF50),
                Color(0xFF03A9F4),
            ),
        ),
    )
}

@Preview
@Composable
private fun CookieShapeBoxRadialGradient() {
    KrailTheme {
        CookieShapeBox(
            outlineBrush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFEE58), Color(0xFFF57F17)),
                center = Offset.Unspecified,
            ),
        )
    }
}
