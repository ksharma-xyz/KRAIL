package xyz.ksharma.krail.trip.planner.ui.themeselection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    val animationProgress = remember { Animatable(0f) }

    // Animate the highlight when selected
    LaunchedEffect(selected) {
        if (selected) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, animationSpec = tween(600))
        } else {
            animationProgress.snapTo(0f)
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
                .height(32.dp)
        ) {
            // Draw the animated sketch highlight behind the text
            if (selected && textLayoutResult != null) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    val width = textLayoutResult!!.size.width.toFloat()
                    val height = size.height
                    val strokeWidth = 20.dp.toPx()
                    val zigZagAmplitude = 8.dp.toPx()
                    val zigZagStep = 16.dp.toPx()
                    val path = Path()
                    var x = 0f
                    var up = true
                    path.moveTo(x, height / 2)
                    while (x < width * animationProgress.value) {
                        val y =
                            if (up) (height / 2 - zigZagAmplitude) else (height / 2 + zigZagAmplitude)
                        path.lineTo(x, y)
                        x += zigZagStep
                        up = !up
                    }
                    // Ensure the path ends at the current progress
                    path.lineTo(width * animationProgress.value, height / 2)
                    drawPath(
                        path = path,
                        color = highlightColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        )
                    )
                }
            }
            // The text itself
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