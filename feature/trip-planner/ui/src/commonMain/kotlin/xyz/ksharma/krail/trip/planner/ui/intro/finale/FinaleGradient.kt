package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.tokens.AiGradientTokens
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens

/**
 * The one switch for the wordmark's colours. True builds the gradient from the colour the rider chose and that theme's
 * own partner colour ([themeGradientStops]), with no stop darker than the chosen colour. False is the gradient the
 * wordmark had before, the app's four AI gradient stops, which stay available so the two can be compared on a device.
 * Nothing else changes between them: the four stops, the slide, the blend to the theme colour, the glow and the timing.
 */
internal const val FINALE_GRADIENT_FROM_THEME = true

// The weights of red, green and blue in the brightness of a colour (Rec. 709), applied to the colour's own channels.
private const val LUMA_RED = 0.2126f
private const val LUMA_GREEN = 0.7152f
private const val LUMA_BLUE = 0.0722f

/** The brightness of [colour], 0 for black and 1 for white. A mix with white raises it in a straight line. */
internal fun lumaOf(colour: Color): Float =
    LUMA_RED * colour.red + LUMA_GREEN * colour.green + LUMA_BLUE * colour.blue

/**
 * The colours the wordmark's gradient starts from: the ones built from the chosen theme when [fromTheme], and the app's
 * four AI gradient stops when not. They are the four colours the blend then moves to the theme colour ([finaleGradientColors]).
 */
internal fun finaleBrandStops(
    themeColour: Color,
    fromTheme: Boolean = FINALE_GRADIENT_FROM_THEME,
): List<Color> = if (fromTheme) themeGradientStops(themeColour) else AiGradientTokens.stops

/**
 * Four stops for the theme whose colour is [themeColour]: the colour itself, the point halfway to its partner round the
 * colour wheel (the app's own AI gradient for that theme), the partner, and the halfway point again, so the gradient
 * travels out to the partner and back. Bright colours only: a stop that is darker than [themeColour] is lightened
 * towards white until it is as light as [themeColour], so none is darker than the colour the rider chose. A colour
 * that is no theme's takes the default theme's pair.
 */
internal fun themeGradientStops(themeColour: Color): List<Color> {
    val style = KrailThemeStyle.entries.firstOrNull { it.hexColorCode.hexToComposeColor() == themeColour }
        ?: DEFAULT_THEME_STYLE
    val (theme, halfway, partner) = AiThemeGradientTokens.stopsFor(style)
    val floor = lumaOf(theme)
    return listOf(theme, halfway, partner, halfway).map { liftToLuma(it, floor) }
}

// A colour keeps each channel in half precision, so a lift that lands exactly on the floor can read a hair under it. The
// share goes up by this much until it does not.
private const val LIFT_NUDGE = 0.001f

/**
 * [colour], or when it is darker than [floor], the least mix of it with white that is as light as [floor], as the
 * colour holds it. Its hue is the nearest it can be to what it was, and it is never darker than it was.
 */
internal fun liftToLuma(colour: Color, floor: Float): Color {
    val luma = lumaOf(colour)
    if (luma >= floor || luma >= 1f) return colour
    var share = ((floor - luma) / (1f - luma)).coerceIn(0f, 1f)
    var lifted = towardsWhite(colour, share)
    while (lumaOf(lifted) < floor && share < 1f) {
        share = (share + LIFT_NUDGE).coerceAtMost(1f)
        lifted = towardsWhite(colour, share)
    }
    return lifted
}

private fun towardsWhite(colour: Color, share: Float) = Color(
    red = colour.red + (1f - colour.red) * share,
    green = colour.green + (1f - colour.green) * share,
    blue = colour.blue + (1f - colour.blue) * share,
    alpha = colour.alpha,
)
