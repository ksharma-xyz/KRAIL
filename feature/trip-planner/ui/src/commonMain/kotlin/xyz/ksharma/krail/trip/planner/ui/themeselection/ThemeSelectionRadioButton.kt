package xyz.ksharma.krail.trip.planner.ui.themeselection

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
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
                animationSpec = tween(durationMillis = duration),
            ) { value, _ ->
                currentPathLength = value
            }
        } else {
            currentPathLength = 0f
        }
    }

    // Pulse animation for glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .scalingKlickable { onClick(themeStyle) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (selected) {
                        Modifier.drawBehind {
                            val color = themeStyle.hexColorCode.hexToComposeColor()
                            withTransform({
                                scale(pulseScale, pulseScale, pivot = center)
                            }) {
                                drawCircle(
                                    color = color.copy(alpha = pulseAlpha),
                                    radius = size.minDimension / 2,
                                )
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color = themeStyle.hexColorCode.hexToComposeColor()),
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 16.dp),
        ) {
            if (selected && textLayoutResult != null) {
                val textWidth = with(density) { textLayoutResult!!.size.width.toDp() }
                Canvas(
                    modifier = Modifier
                        .width(textWidth)
                        .padding(horizontal = 12.dp),
                ) {
                    val layout = textLayoutResult!!
                    val lineCount = layout.lineCount
                    val strokeWidth = 32.dp.toPx()
                    val amplitude = 14.dp.toPx()
                    val waveLength = 32.dp.toPx()
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
                                controlX1,
                                centerY,
                                controlX2,
                                endY,
                                nextX,
                                endY,
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
                            ),
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

@Preview
@Composable
private fun ThemeSelectionRadioButtonPreview() {
    KrailTheme {
        ThemeSelectionRadioButton(
            themeStyle = KrailThemeStyle.Bus,
            onClick = {},
            selected = true,
        )
    }
}

@Preview
@Composable
private fun ThemeSelectionRadioButtonUnselectedPreview() {
    KrailTheme {
        ThemeSelectionRadioButton(
            themeStyle = KrailThemeStyle.Bus,
            onClick = {},
            selected = false,
        )
    }
}
