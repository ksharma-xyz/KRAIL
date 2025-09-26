package xyz.ksharma.krail.trip.planner.ui.components.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.hexToComposeColor
import kotlin.math.min

fun Modifier.gradientBorder(
    pageOffset: Float,
    colorsList: List<String>,
    borderThickness: Dp = 8.dp,
    cornerRadius: Dp = 24.dp,
): Modifier = this.drawWithContent {
    val fraction = min(1f, pageOffset)
    val grey = Color(0xFF888888)
    val gradientColors = colorsList
        .map { it.hexToComposeColor() }
        .map { originalColor -> androidx.compose.ui.graphics.lerp(originalColor, grey, fraction) }
    val gradientBrush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height),
    )
    drawContent()
    drawRoundRect(
        brush = gradientBrush,
        size = size,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = Stroke(width = borderThickness.toPx()),
    )
}

internal fun lerp(start: Dp, end: Dp, fraction: Float): Dp = start + (end - start) * fraction

internal fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction
