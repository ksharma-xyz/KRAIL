package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import xyz.ksharma.krail.taj.theme.KrailTheme

/** The text style every letter wears, fitted to the width, and the room round the mark that the glow or the shadow needs. */
internal class WordmarkLayout(val style: TextStyle, val padPx: Float)

@Composable
internal fun rememberWordmarkLayout(availableWidthPx: Float, look: FinaleWordmarkLook): WordmarkLayout {
    val token = KrailTheme.typography.introTagline
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(token, measurer, density, availableWidthPx, look) {
        fitWordmark(token, measurer, density, availableWidthPx, look)
    }
}

// The room round the mark at a letter size of [letterSizePx]: the glow's full blur, or the shadow's blur and drop.
private fun roomPx(look: FinaleWordmarkLook, letterSizePx: Float): Float =
    look.staticShadow?.scaledTo(letterSizePx)?.roomPx() ?: glowBlurPx(GLOW_STEPS, letterSizePx)

// The wordmark at its full size, measured line by line, and then scaled down by the one scale the widest line
// needs once the room for the glow or the shadow on both sides is taken out of the width.
private fun fitWordmark(
    token: TextStyle,
    measurer: TextMeasurer,
    density: Density,
    availableWidthPx: Float,
    look: FinaleWordmarkLook,
): WordmarkLayout {
    val full = token.copy(
        fontSize = token.fontSize * WORDMARK_SCALE,
        lineHeight = token.lineHeight * WORDMARK_SCALE,
        fontStyle = FontStyle.Italic,
    )
    val fullPx = with(density) { full.fontSize.toPx() }
    val room = availableWidthPx - 2f * roomPx(look, fullPx)
    val widths = WORDMARK_LINES.map { line ->
        line.sumOf { letter -> measurer.measure(letter.toString(), full, softWrap = false, maxLines = 1).size.width }
    }.map { it.toFloat() }
    val scale = wordmarkFitScale(widths, room)
    return WordmarkLayout(
        style = full.copy(fontSize = full.fontSize * scale, lineHeight = full.lineHeight * scale),
        padPx = roomPx(look, fullPx * scale),
    )
}
