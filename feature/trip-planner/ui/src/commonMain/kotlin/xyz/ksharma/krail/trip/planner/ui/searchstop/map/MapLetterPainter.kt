package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.KrailTheme

private val MarkerEdge = 2.dp
private const val CAPITAL_HEIGHT_PER_FONT_SIZE = 0.71f

/**
 * A map marker drawn as one picture: a white edge, a [fill] and one white bold [letter]. It is a circle, or a square with
 * rounded corners when [corner] is given. Being one picture, where two markers overlap the upper one covers the lower one
 * whole. An empty [letter] gives the plain shape.
 *
 * The letter is [letterSize] tall in dp and does not follow the font size setting: the marker keeps its size, and a letter
 * that shrank or grew inside it would only be harder to read.
 */
@Composable
internal fun rememberMapLetterPainter(letter: String, fill: Color, letterSize: Dp, corner: Dp? = null): Painter {
    val density = Density(density = LocalDensity.current.density, fontScale = 1f)
    val measurer = rememberTextMeasurer()
    val style = KrailTheme.typography.displayLarge.copy(
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = with(density) { letterSize.toSp() },
        lineHeight = TextUnit.Unspecified,
        letterSpacing = TextUnit.Unspecified,
    )
    return remember(letter, fill, letterSize, corner, density.density, style) {
        MapLetterPainter(
            fill = fill,
            letter = measurer.measure(text = letter, style = style, density = density),
            letterSize = letterSize,
            corner = corner,
        )
    }
}

private class MapLetterPainter(
    private val fill: Color,
    private val letter: TextLayoutResult,
    private val letterSize: Dp,
    private val corner: Dp?,
) : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val edge = MarkerEdge.toPx()
        if (corner == null) {
            drawCircle(color = Color.White)
            drawCircle(color = fill, radius = size.minDimension / 2 - edge)
        } else {
            val radius = corner.toPx()
            drawRoundRect(color = Color.White, cornerRadius = CornerRadius(radius))
            drawRoundRect(
                color = fill,
                topLeft = Offset(edge, edge),
                size = Size(size.width - edge * 2, size.height - edge * 2),
                cornerRadius = CornerRadius(radius - edge),
            )
        }
        // The letter is centred by the height of the capital itself, not by its line box, which has room for descenders.
        val capitalHeight = letterSize.toPx() * CAPITAL_HEIGHT_PER_FONT_SIZE
        drawText(
            textLayoutResult = letter,
            topLeft = Offset(
                x = (size.width - letter.size.width) / 2f,
                y = (size.height + capitalHeight) / 2f - letter.firstBaseline,
            ),
        )
    }
}
