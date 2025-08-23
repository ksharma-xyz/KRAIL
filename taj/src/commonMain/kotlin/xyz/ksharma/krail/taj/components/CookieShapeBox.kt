package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
    color: Color = themeBackgroundColor(),
    outline: Color = themeColor(),
) {
    Box(
        modifier = modifier
            .size(180.dp)
            .background(color = color, shape = CookieShape())
    ) {
        // Optional outline via Canvas overlay
        Canvas(Modifier.matchParentSize()) {
            val path = buildCookiePath(size, bumps = 9, depthFraction = 0.12f, smooth = true)
            drawPath(path, Color.Transparent) // fill already drawn by background
            drawPath(path, outline, style = Stroke(width = size.minDimension * 0.015f))
        }
    }
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
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CookieShapeBox()
        CookieShapeCanvas()
    }
}

@Preview
@Composable
private fun CookiePreviewCanvas() {
    KrailTheme {
        CookieShapeCanvas()

    }
}