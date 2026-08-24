package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.animations.rememberAiSpinAngle
import xyz.ksharma.krail.taj.modifier.aiGradientBorder
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.tokens.AiCoolGradientTokens
import xyz.ksharma.krail.taj.tokens.AiGradientTokens
import xyz.ksharma.krail.taj.tokens.ButtonTokens
import xyz.ksharma.krail.taj.tokens.StrokeTokens

/**
 * The way in to speaking: an ordinary mic on an ordinary button, wearing the AI gradient as a
 * ring around its edge.
 *
 * The colour is deliberately all in the ring. The glyph and the fill stay the theme's own
 * content and surface colours, so the button reads as a mic first and as an AI surface second,
 * which is the right order for a control whose job is to open a microphone. A gradient-filled
 * glyph would have said "AI" before it said "speak".
 *
 * It replaced [AiWheelMark] in this position. The wheel is KRAIL's mark for *on-device AI did
 * this*, which is what the alert-summary card needs, but on the way in it named the technology
 * rather than the action: a rider looking for a way to say where they are going has no reason
 * to recognise a wheel. The mark stays where it belongs and the door says what it opens.
 *
 * @param spinning While `true`, the ring turns, then decelerates to a stop when it flips back
 * (see [rememberAiSpinAngle]). The ring is drawn either way: at rest it is this surface's
 * identity, and turning is reserved for the beat when something is actually happening.
 * @param colors Gradient stops for the ring. Defaults to [AiGradientTokens]; pass
 * [AiCoolGradientTokens] or a theme-derived set to match a surrounding surface.
 */
@Composable
fun AiMicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    spinning: Boolean = false,
    colors: List<Color> = AiGradientTokens.stops,
    contentDescription: String? = null,
    onClickLabel: String? = null,
) {
    RoundIconButton(
        onClick = onClick,
        onClickLabel = onClickLabel,
        // On the button's own edge, so the ring traces the control rather than floating inside
        // it. Half the button's size is the radius that makes this rounded rect a circle, and
        // it is read from the same token the button sizes itself with so the two cannot drift.
        modifier = modifier.aiGradientBorder(
            spinning = spinning,
            cornerRadius = ButtonTokens.RoundButtonSize / 2,
            // Thinner than the modifier's default, which is tuned for a card's edge. At 4dp on
            // a 48dp circle the ring stops being a border and becomes the button.
            strokeWidth = StrokeTokens.Regular,
            colors = colors,
        ),
        content = {
            Image(
                imageVector = MicIcon,
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(LocalContentColor.current),
                modifier = Modifier.size(KrailTheme.dimensions.iconDefault),
            )
        },
    )
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAiMicButtonAtRest() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        AiMicButton(onClick = {}, contentDescription = "Ask KRAIL")
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAiMicButtonCool() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        AiMicButton(
            onClick = {},
            colors = AiCoolGradientTokens.stops,
            contentDescription = "Ask KRAIL",
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewAiMicButtonSpinning() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        AiMicButton(onClick = {}, spinning = true, contentDescription = "Ask KRAIL")
    }
}

// endregion
