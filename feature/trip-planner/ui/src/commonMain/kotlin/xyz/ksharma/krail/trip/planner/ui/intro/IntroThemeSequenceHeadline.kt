package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.themeInkColor
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * The colour screen's headline while it is driven by the theme change sequence ([IntroThemeChange]), in the same layout as the
 * headline the other screens have: the emoji, then the lines, all the same boldest display style, with room kept for [minLines].
 * What changes with a tap is played on the one clock:
 *
 * - the old lines ease up and fade out together, then the new ones come in one after another, each rising into its place, so the two
 *   are never on screen as translucent layers over each other, and each line is one opaque text with its own opacity;
 * - the emoji, one text, shrinks, becomes the new one and springs back;
 * - the text colour is the sequence's, on the same blend as the button and the ground, and once it is over it is the live ink of the
 *   theme (or the surface text before a pick), so a change of light and dark still reaches it.
 *
 * Everything that moves is read inside a `graphicsLayer` lambda from the clock, so a frame draws and does not lay out. For a screen
 * reader the whole headline is one heading of the new phrase, and the emoji is hidden from it.
 */
@Composable
internal fun IntroThemeSequenceHeadline(change: IntroThemeChange, minLines: Int, modifier: Modifier = Modifier) {
    val headline = change.spec.headline
    val phrase = headline.incoming.spoken
    val lineGap = KrailTheme.dimensions.spacingS
    val emojiGap = introEmojiTitleGap(KrailTheme.dimensions.spacingXXL, LocalDensity.current.fontScale)
    val chosen = change.spec.target.themeId != null
    val settledInk = if (chosen) themeInkColor() else KrailTheme.colors.onSurface
    val ink = if (change.isRunning) sequenceInk(change) else settledInk
    val emoji = if (change.emojiShowsNew) headline.incoming.emoji else headline.emojiBefore
    // Each headline is drawn at its own size, the question fitted to its room and a tagline at the ordinary one, and the room kept
    // is read in the screen's density, so it is the same whichever shows.
    val incomingDensity = fittedHeadlineDensity(headline.incoming)
    val outgoingDensity = fittedHeadlineDensity(headline.outgoing)
    val reservedHeight = reservedHeadlineHeight(minLines, hasEmoji = true, emojiGap = emojiGap)
    CompositionLocalProvider(LocalDensity provides headlineDensity()) {
        Column(
            modifier = modifier
                .heightIn(min = reservedHeight)
                .clearAndSetSemantics {
                    heading()
                    text = AnnotatedString(phrase)
                },
            verticalArrangement = Arrangement.spacedBy(lineGap),
            horizontalAlignment = Alignment.Start,
        ) {
            if (emoji.isNotEmpty()) {
                Text(
                    text = emoji,
                    style = KrailTheme.typography.emoji,
                    modifier = Modifier.padding(bottom = emojiGap - lineGap).graphicsLayer {
                        val scale = change.emojiScaleNow()
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
            Box {
                headline.outgoing?.let { leaving ->
                    SequenceLines(
                        lines = leaving.lines,
                        colour = ink,
                        density = outgoingDensity,
                        alphaOf = change::outgoingAlphaNow,
                        riseDpOf = change::outgoingRiseNowDp,
                    )
                }
                SequenceLines(
                    lines = headline.incoming.lines,
                    colour = ink,
                    density = incomingDensity,
                    alphaOf = change::incomingAlphaNow,
                    riseDpOf = change::incomingRiseNowDp,
                )
            }
        }
    }
}

// The text colour on the sequence's blend. It changes with the clock, so it is read through a derived state, and only while it runs.
@Composable
private fun sequenceInk(change: IntroThemeChange): Color {
    val ink by remember(change) { derivedStateOf { change.textNow() } }
    return ink
}

@Composable
private fun SequenceLines(
    lines: List<String>,
    colour: Color,
    density: Density,
    alphaOf: (Int) -> Float,
    riseDpOf: (Int) -> Float,
) {
    val display = KrailTheme.typography.introTagline.asHeadlineStyle()
    CompositionLocalProvider(LocalDensity provides density) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingS),
            horizontalAlignment = Alignment.Start,
        ) {
            lines.forEachIndexed { index, line ->
                Text(
                    text = introHeadlineDisplay(line),
                    style = display,
                    color = colour,
                    modifier = Modifier.graphicsLayer {
                        alpha = alphaOf(index)
                        translationY = riseDpOf(index).dp.toPx()
                    },
                )
            }
        }
    }
}
