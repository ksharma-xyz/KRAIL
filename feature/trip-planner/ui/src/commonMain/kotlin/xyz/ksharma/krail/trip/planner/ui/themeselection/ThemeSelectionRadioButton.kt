package xyz.ksharma.krail.trip.planner.ui.themeselection

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.scalingKlickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.components.themeBackgroundColor

@Composable
fun ThemeSelectionRadioButton(
    themeStyle: KrailThemeStyle,
    onClick: (KrailThemeStyle) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val highlightColor = themeBackgroundColor(themeStyle)
    val density = LocalDensity.current

    // Calculate total path length (sum of all line widths)
    val lineWidths = remember(textLayoutResult) {
        textLayoutResult?.let { layout ->
            List(layout.lineCount) { line ->
                layout.getLineRight(line) - layout.getLineLeft(line)
            }
        } ?: emptyList()
    }
    val totalPathLength = lineWidths.sum()

    // Animate currentPathLength at a constant speed
    var currentPathLength by remember { mutableStateOf(0f) }
    LaunchedEffect(selected, textLayoutResult) {
        if (selected && textLayoutResult != null && totalPathLength > 0f) {
            val speedPxPerMs = 1.2f
            val duration = (totalPathLength / speedPxPerMs).toInt()
            currentPathLength = 0f
            animate(
                initialValue = 0f,
                targetValue = totalPathLength,
                animationSpec = tween(durationMillis = duration)
            ) { value, _ ->
                currentPathLength = value
            }
        } else {
            currentPathLength = 0f
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .scalingKlickable { onClick(themeStyle) }
            .padding(vertical = 24.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color = themeStyle.hexColorCode.hexToComposeColor()),
        )

        Box(
            modifier = Modifier
                .padding(start = 16.dp)
        ) {
            if (selected && textLayoutResult != null) {
                val textWidth = with(density) { textLayoutResult!!.size.width.toDp() }
                val textHeight = with(density) { textLayoutResult!!.size.height.toDp() }
                Canvas(
                    modifier = Modifier
                        .width(textWidth)
                        .padding(horizontal = 12.dp)
                    // Height is not fixed, so it matches text height
                ) {
                    val layout = textLayoutResult!!
                    val lineCount = layout.lineCount
                    val strokeWidth = 40.dp.toPx()
                    val amplitude = 14.dp.toPx()
                    val waveLength = 36.dp.toPx()
                    var remaining = currentPathLength

                    for (line in 0 until lineCount) {
                        val left = layout.getLineLeft(line)
                        val right = layout.getLineRight(line)
                        val lineWidth = right - left
                        if (remaining <= 0f) break
                        val highlightEnd = left + remaining.coerceAtMost(lineWidth)
                        val top = layout.getLineTop(line)
                        val bottom = layout.getLineBottom(line)
                        val centerY = (top + bottom) / 2f

                        val path = Path()
                        var x = left
                        var up = true
                        path.moveTo(x, centerY)
                        while (x < highlightEnd) {
                            val nextX = (x + waveLength).coerceAtMost(highlightEnd)
                            val controlX1 = x + waveLength / 3
                            val controlX2 = x + 2 * waveLength / 3
                            val endY = if (up) centerY - amplitude else centerY + amplitude
                            path.cubicTo(
                                controlX1, centerY,
                                controlX2, endY,
                                nextX, endY
                            )
                            x = nextX
                            up = !up
                        }
                        drawPath(
                            path = path,
                            color = highlightColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            )
                        )
                        remaining -= lineWidth
                    }
                }
            }

            Text(
                text = themeStyle.tagLine,
                style = KrailTheme.typography.title.copy(fontWeight = FontWeight.Normal),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 2.dp),
                onTextLayout = { textLayoutResult = it },
                color = KrailTheme.colors.onSurface,
                textAlign = TextAlign.Start,
            )
        }
    }
}
