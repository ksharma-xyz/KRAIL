package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

// A letter starts this much smaller and grows to its size as it rises, the same small growth the headlines have.
private const val LETTER_START_SCALE = 0.85f

/**
 * `#LET'S KRAIL`, on two lines, huge, in the app's boldest display style and italic, with the brand gradient
 * across it and a glow round the letters. It is taj [Text] and nothing else: no new font and no size written here.
 * The size is the tagline token times [WORDMARK_SCALE], then scaled down, never squeezed, so the widest line fits
 * the width it is given, whatever the screen and the text size.
 *
 * Every letter is its own text, a unit that rises and fades in through `graphicsLayer`, read inside the layer
 * lambda from [frame], so a frame redraws and recomposes and lays out nothing. The gradient is drawn once over the
 * whole mark by blending it into the letters (`BlendMode.SrcIn` in an offscreen layer that holds only the mark, so
 * it never touches what is behind). The glow is the text style's own coloured [Shadow], whose blur and opacity
 * come in [GLOW_STEPS] steps, so the style changes at most that many times and not on every frame. There is no
 * `Modifier.blur`.
 *
 * The mark leaves room round the letters for the glow, so the glow is never cut by the layer.
 */
@Composable
internal fun FinaleWordmark(
    frame: State<FinaleFrame>,
    themeColour: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val layout = rememberWordmarkLayout(availableWidthPx = constraints.maxWidth.toFloat())
        val glow by remember(frame) { derivedStateOf { glowStep(frame.value.glow) } }
        val letterStyle = rememberGlowStyle(layout.style, glow, themeColour)
        val pad = with(LocalDensity.current) { layout.padPx.toDp() }
        Column(
            modifier = Modifier
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    alpha = frame.value.wordmarkAlpha
                }
                .drawWithContent {
                    drawContent()
                    drawGradient(frame.value, themeColour, layout.padPx)
                }
                .padding(pad),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WORDMARK_LINES.forEachIndexed { line, text ->
                WordmarkLine(text, firstLetterOfLine(line), letterStyle, frame)
            }
        }
    }
}

@Composable
private fun WordmarkLine(text: String, firstLetter: Int, style: TextStyle, frame: State<FinaleFrame>) {
    Row {
        text.forEachIndexed { offset, letter ->
            Text(
                text = letter.toString(),
                style = style,
                color = KrailTheme.colors.onSurface,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    val progress = frame.value.letterProgress(firstLetter + offset)
                    alpha = progress.coerceIn(0f, 1f)
                    translationY = (1f - progress) * WORDMARK_RISE_FRACTION * size.height
                    val grown = LETTER_START_SCALE + (1f - LETTER_START_SCALE) * progress
                    scaleX = grown
                    scaleY = grown
                },
            )
        }
    }
}

// The gradient over the letters already drawn: only where they are, as their own opacity, because SrcIn keeps the
// source only where the layer already has something.
private fun DrawScope.drawGradient(
    frame: FinaleFrame,
    themeColour: Color,
    padPx: Float,
) {
    val (start, end) = gradientRangePx(size.width - 2f * padPx, gradientTravelAt(frame.wordmarkMs))
    drawRect(
        brush = Brush.horizontalGradient(
            colors = finaleGradientColors(frame.colourBlend, themeColour),
            startX = padPx + start,
            endX = padPx + end,
        ),
        blendMode = BlendMode.SrcIn,
    )
}

// The text style with the glow at [step]: a coloured shadow straight under the letter, so it is a halo, with its
// blur and opacity from the step. At step 0 there is none.
@Composable
private fun rememberGlowStyle(base: TextStyle, step: Int, colour: Color): TextStyle {
    val sizePx = with(LocalDensity.current) { base.fontSize.toPx() }
    return remember(base, step, colour, sizePx) {
        if (step == 0) {
            base
        } else {
            base.copy(
                shadow = Shadow(
                    color = colour.copy(alpha = glowAlpha(step)),
                    offset = Offset.Zero,
                    blurRadius = glowBlurPx(step, sizePx),
                ),
            )
        }
    }
}

/** The text style every letter wears, fitted to the width, and the room round the mark that the glow needs. */
private class WordmarkLayout(val style: TextStyle, val padPx: Float)

@Composable
private fun rememberWordmarkLayout(availableWidthPx: Float): WordmarkLayout {
    val token = KrailTheme.typography.introTagline
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(token, measurer, density, availableWidthPx) {
        fitWordmark(token, measurer, density, availableWidthPx)
    }
}

// The wordmark at its full size, measured line by line, and then scaled down by the one scale the widest line
// needs once the room for the glow on both sides is taken out of the width.
private fun fitWordmark(
    token: TextStyle,
    measurer: TextMeasurer,
    density: Density,
    availableWidthPx: Float,
): WordmarkLayout {
    val full = token.copy(
        fontSize = token.fontSize * WORDMARK_SCALE,
        lineHeight = token.lineHeight * WORDMARK_SCALE,
        fontStyle = FontStyle.Italic,
    )
    val fullPx = with(density) { full.fontSize.toPx() }
    val room = availableWidthPx - 2f * glowBlurPx(GLOW_STEPS, fullPx)
    val widths = WORDMARK_LINES.map { line ->
        line.sumOf { letter -> measurer.measure(letter.toString(), full, softWrap = false, maxLines = 1).size.width }
    }.map { it.toFloat() }
    val scale = wordmarkFitScale(widths, room)
    return WordmarkLayout(
        style = full.copy(fontSize = full.fontSize * scale, lineHeight = full.lineHeight * scale),
        padPx = glowBlurPx(GLOW_STEPS, fullPx * scale),
    )
}
