package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme

// A letter starts this much smaller and grows to its size as it rises, the same small growth the headlines have. Only the
// looks with an opacity on each letter use it.
private const val LETTER_START_SCALE = 0.85f

/**
 * `#LET'S KRAIL`, on two lines, huge, in the app's boldest display style and italic, filled as [FINALE_WORDMARK_FILL] says.
 * It is taj [Text] and nothing else: no new font and no size written here. The size is the tagline token times
 * [WORDMARK_SCALE], then scaled down, never squeezed, so the widest line fits the width it is given, whatever the screen and
 * the text size.
 *
 * With the website's fill ([FinaleWordmarkFill.WebsiteModes], the default, see [finaleWordmarkLook]) the whole word is one
 * layer: it fades in as one opaque picture, with the opacity on that layer alone, and no letter has an opacity of its own, so
 * no two glyphs are ever translucent layers over each other. The letters still rise one after another, by translation only. The
 * fill is a solid gradient of the four mode colours across the whole word, drawn once over the letters by blending it into them
 * (`BlendMode.SrcIn` in an offscreen layer that holds only the letters, so it never touches what is behind). Under them is one
 * soft drop shadow, black at a low opacity, set once on the text style and never changed. There is no glow and no
 * `Modifier.blur`.
 *
 * With the other two fills it is as it was: every letter is its own text with an opacity, and the glow is the text style's
 * own coloured [Shadow], whose blur and opacity come in [GLOW_STEPS] steps, so the style changes at most that many times and
 * not on every frame. Everything is read inside the layer lambdas from [frame], so a frame redraws and recomposes and lays out
 * nothing.
 *
 * The mark leaves room round the letters for the glow or the shadow, so neither is cut by the layer.
 */
@Composable
internal fun FinaleWordmark(
    frame: State<FinaleFrame>,
    themeColour: Color,
    modifier: Modifier = Modifier,
    look: FinaleWordmarkLook = finaleWordmarkLook(),
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val layout = rememberWordmarkLayout(availableWidthPx = constraints.maxWidth.toFloat(), look = look)
        val brand = remember(themeColour) { finaleBrandStops(themeColour) }
        val pad = with(LocalDensity.current) { layout.padPx.toDp() }
        if (look.glowAnimated) {
            GlowWordmark(frame, themeColour, brand, layout, pad, look)
        } else {
            StaticWordmark(frame, themeColour, brand, layout, pad, look)
        }
    }
}

// The wordmark as it was: the glow round the letters, each letter with an opacity, the gradient sliding across.
@Composable
private fun GlowWordmark(
    frame: State<FinaleFrame>,
    themeColour: Color,
    brand: List<Color>,
    layout: WordmarkLayout,
    pad: Dp,
    look: FinaleWordmarkLook,
) {
    val glow by remember(frame) { derivedStateOf { glowStep(frame.value.glow) } }
    val letterStyle = rememberGlowStyle(layout.style, glow, themeColour)
    Column(
        modifier = Modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = frame.value.wordmarkAlpha
            }
            .drawWithContent {
                drawContent()
                drawGradient(frame.value, themeColour, brand, layout.padPx, look)
            }
            .padding(pad),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WordmarkLetters(letterStyle, frame, letterAlpha = true, color = KrailTheme.colors.onSurface)
    }
}

// The website's wordmark: one layer for the whole word's opacity, the shadow under the letters and the solid sweep over them.
@Composable
private fun StaticWordmark(
    frame: State<FinaleFrame>,
    themeColour: Color,
    brand: List<Color>,
    layout: WordmarkLayout,
    pad: Dp,
    look: FinaleWordmarkLook,
) {
    val shadowAlpha = look.staticShadow?.alpha ?: 0f
    val shadowStyle = rememberShadowStyle(layout.style, look.staticShadow)
    Box(
        modifier = Modifier.graphicsLayer {
            // The one layer that carries the word's opacity. Everything in it is opaque, so it fades as a single picture.
            compositingStrategy = CompositingStrategy.Offscreen
            alpha = frame.value.wordmarkAlpha * wordmarkFadeInAt(frame.value.wordmarkMs)
        },
    ) {
        // The shadow, under the letters: the same letters in the shadow's own black, which the fill covers exactly. It is
        // for the eye only, so a screen reader and a test do not find it.
        Column(
            modifier = Modifier.padding(pad).clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WordmarkLetters(shadowStyle, frame, letterAlpha = false, color = Color.Black.copy(alpha = shadowAlpha))
        }
        // The fill: the letters, with the sweep blended into them, in a layer that holds only them.
        Column(
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawGradient(frame.value, themeColour, brand, layout.padPx, look)
                }
                .padding(pad),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WordmarkLetters(layout.style, frame, letterAlpha = false, color = KrailTheme.colors.onSurface)
        }
    }
}

@Composable
private fun WordmarkLetters(style: TextStyle, frame: State<FinaleFrame>, letterAlpha: Boolean, color: Color) {
    WORDMARK_LINES.forEachIndexed { line, text ->
        WordmarkLine(text, firstLetterOfLine(line), style, frame, letterAlpha, color)
    }
}

@Composable
private fun WordmarkLine(
    text: String,
    firstLetter: Int,
    style: TextStyle,
    frame: State<FinaleFrame>,
    letterAlpha: Boolean,
    color: Color,
) {
    Row {
        text.forEachIndexed { offset, letter ->
            Text(
                text = letter.toString(),
                style = style,
                color = color,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    val progress = frame.value.letterProgress(firstLetter + offset)
                    translationY = (1f - progress) * WORDMARK_RISE_FRACTION * size.height
                    if (letterAlpha) {
                        alpha = progress.coerceIn(0f, 1f)
                        val grown = LETTER_START_SCALE + (1f - LETTER_START_SCALE) * progress
                        scaleX = grown
                        scaleY = grown
                    }
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
    brand: List<Color>,
    padPx: Float,
    look: FinaleWordmarkLook,
) {
    val (start, end) = wordmarkGradientRangePx(look, size.width - 2f * padPx, gradientTravelAt(frame.wordmarkMs))
    drawRect(
        brush = Brush.horizontalGradient(
            colors = finaleGradientColors(frame.colourBlend, themeColour, brand),
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

// The text style with the one static drop shadow, scaled to the size of the letter. It is built once for a size and never
// again, so the style does not change while the finale runs.
@Composable
private fun rememberShadowStyle(base: TextStyle, shadow: WordmarkShadow?): TextStyle {
    val sizePx = with(LocalDensity.current) { base.fontSize.toPx() }
    return remember(base, shadow, sizePx) {
        if (shadow == null) {
            base
        } else {
            val scaled = shadow.scaledTo(sizePx)
            base.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = scaled.alpha),
                    offset = Offset(0f, scaled.offsetYPx),
                    blurRadius = scaled.blurPx,
                ),
            )
        }
    }
}

/** The text style every letter wears, fitted to the width, and the room round the mark that the glow or the shadow needs. */
private class WordmarkLayout(val style: TextStyle, val padPx: Float)

@Composable
private fun rememberWordmarkLayout(availableWidthPx: Float, look: FinaleWordmarkLook): WordmarkLayout {
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
