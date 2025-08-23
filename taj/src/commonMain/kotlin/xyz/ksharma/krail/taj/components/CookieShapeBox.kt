package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.shapes.CookieShape
import xyz.ksharma.krail.taj.shapes.buildCookiePath
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor

@Composable
fun CookieShapeBox(
    modifier: Modifier = Modifier,
    fillColor: Color = themeBackgroundColor(),
    // Fallback single color outline (used if outlineBrush == null)
    outlineColor: Color = themeColor(),
    // Provide a Brush for multi-color outline
    outlineBrush: Brush? = null,
    cookieShadow: Shadow? = null,
) {
    val shape = CookieShape()
    Box(
        modifier = modifier
            .then(
                if (cookieShadow != null) {
                    Modifier.dropShadow(
                        shape = shape,
                        shadow = cookieShadow
                    )
                } else Modifier
            )
            .size(180.dp)
            .background(color = fillColor, shape = CookieShape())
    ) {
        Canvas(Modifier.matchParentSize()) {
            val path = buildCookiePath(size, bumps = 9, depthFraction = 0.12f, smooth = true)

            // Fill already applied by background, skip fill here
            val strokeWidth = size.minDimension * 0.018f

            // Primary multi-color (or single color) outline
            when {
                outlineBrush != null ->
                    drawPath(path, outlineBrush, style = Stroke(width = strokeWidth))

                else ->
                    drawPath(path, outlineColor, style = Stroke(width = strokeWidth))
            }
        }
    }
}

// Convenience gradient variants

@Composable
fun CookieShapeBoxSweepGradient() {
    CookieShapeBox(
        outlineBrush = Brush.sweepGradient(
            listOf(
                Color(0xFFFF5722),
                Color(0xFFFFC107),
                Color(0xFF4CAF50),
                Color(0xFF03A9F4),
                Color(0xFF9C27B0),
                Color(0xFFFF5722) // close loop
            )
        )
    )
}

@Composable
fun CookieShapeBoxLinearGradient() {
    CookieShapeBox(
        outlineBrush = Brush.linearGradient(
            colors = listOf(Color.Magenta, Color.Cyan, Color.Yellow),
            start = Offset.Zero,
            end = Offset.Infinite // Compose will normalize to size
        )
    )
}

@Composable
fun CookieShapeBoxRadialGradient() {
    CookieShapeBox(
        outlineBrush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFEE58), Color(0xFFF57F17)),
            center = Offset.Unspecified
        )
    )
}

@Composable
fun CookieShapeCanvas(
    modifier: Modifier = Modifier,
    fill: Color = themeBackgroundColor(),
    stroke: Color = themeColor()
) {
    Canvas(modifier.size(180.dp)) {
        val path = buildCookiePath(size, bumps = 9, depthFraction = 0.13f, smooth = true)
        drawPath(path, fill)
        drawPath(path, stroke, style = Stroke(width = size.minDimension * 0.018f))
    }
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
