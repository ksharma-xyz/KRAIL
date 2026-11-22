package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.bus_theme
import xyz.ksharma.krail.taj.theme.ferry_theme
import xyz.ksharma.krail.taj.theme.metro_theme
import xyz.ksharma.krail.taj.theme.train_theme
import xyz.ksharma.krail.taj.tokens.AiGradientTokens
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens

/**
 * What the wordmark is filled with. [Original] is the gradient it had first, the app's four AI gradient stops.
 * [ThemePartner] is built from the colour the rider chose and that theme's own partner colour ([themeGradientStops]), with
 * no stop darker than the chosen colour. [WebsiteModes] is the website's own wordmark: the four transport mode colours, train,
 * metro, bus and ferry and then train again, as one solid sweep across the whole word ([websiteSweepStops]), every stop fully
 * opaque, with a soft static drop shadow and no glow ([finaleWordmarkLook]).
 */
internal enum class FinaleWordmarkFill { Original, ThemePartner, WebsiteModes }

/**
 * The one switch for the wordmark's colours, a build time constant and not a Remote Config flag. The three fills stay
 * available so they can be compared on a device. Whatever it is, the timeline, the settle into the chosen theme colour,
 * the flood, a tap to finish and reduced motion are the same.
 */
internal val FINALE_WORDMARK_FILL: FinaleWordmarkFill = FinaleWordmarkFill.WebsiteModes

// The website's mode colours for its wordmark sweep, which are the app's own theme colours: one value each, here by name.
internal val WEBSITE_TRAIN: Color = train_theme
internal val WEBSITE_METRO: Color = metro_theme
internal val WEBSITE_BUS: Color = bus_theme
internal val WEBSITE_FERRY: Color = ferry_theme

/** The four mode colours of the website's wordmark, in the order it sweeps them: train, metro, bus, ferry. All opaque. */
internal fun websiteModeColours(): List<Color> = listOf(WEBSITE_TRAIN, WEBSITE_METRO, WEBSITE_BUS, WEBSITE_FERRY)

/** The sweep: the four mode colours and then train again, so the end of the word is the colour it started with. */
internal fun websiteSweepStops(): List<Color> = websiteModeColours() + WEBSITE_TRAIN

/**
 * How one fill draws the wordmark, as plain values so a table can hold it. [glowAnimated] is the coloured glow round the
 * letters whose blur and opacity change in steps while the finale runs. [staticShadow] is the one drop shadow set once and never
 * changed, or null. [gradientSlides] is whether the gradient slides across the word while it enters, and [letterAlpha] whether
 * each letter fades in as a layer of its own. [wordAlphaLayers] is how many layers carry the whole word's opacity, which is one,
 * the word's, whatever the look, so glyphs never overlap each other as translucent layers.
 */
internal data class FinaleWordmarkLook(
    val glowAnimated: Boolean,
    val staticShadow: WordmarkShadow?,
    val gradientSlides: Boolean,
    val letterAlpha: Boolean,
    val wordAlphaLayers: Int,
)

/**
 * The drop shadow of the website's wordmark, `drop-shadow(0 4px 14px rgba(28,27,26,0.16))`, in pixels at [designFontPx], the
 * largest size its wordmark is set at. It scales with the letters, so the shadow is as soft on a phone as it is there. The
 * colour is black at [alpha], never a theme colour.
 */
internal data class WordmarkShadow(val alpha: Float, val blurPx: Float, val offsetYPx: Float, val designFontPx: Float)

internal const val WORDMARK_SHADOW_ALPHA = 0.16f
internal const val WORDMARK_SHADOW_BLUR_PX = 14f
internal const val WORDMARK_SHADOW_OFFSET_PX = 4f
internal const val WORDMARK_SHADOW_DESIGN_FONT_PX = 130f

/** The shadow in pixels for a letter [fontPx] tall: the website's numbers, scaled by how much smaller or larger the letter is. */
internal fun WordmarkShadow.scaledTo(fontPx: Float): WordmarkShadow {
    val scale = fontPx / designFontPx
    return copy(blurPx = blurPx * scale, offsetYPx = offsetYPx * scale, designFontPx = fontPx)
}

/** The room round the letters a shadow needs so it is not cut by the layer: its blur and its drop. */
internal fun WordmarkShadow.roomPx(): Float = blurPx + offsetYPx

private val WebsiteShadow = WordmarkShadow(
    alpha = WORDMARK_SHADOW_ALPHA,
    blurPx = WORDMARK_SHADOW_BLUR_PX,
    offsetYPx = WORDMARK_SHADOW_OFFSET_PX,
    designFontPx = WORDMARK_SHADOW_DESIGN_FONT_PX,
)

/**
 * The look of [fill]. The website's fill has no glow, one static shadow, no slide, no opacity on a letter and one layer for
 * the word's opacity. The other two are the wordmark as it was: the animated glow, the slide and an opacity for each letter.
 */
internal fun finaleWordmarkLook(fill: FinaleWordmarkFill = FINALE_WORDMARK_FILL): FinaleWordmarkLook = when (fill) {
    FinaleWordmarkFill.WebsiteModes -> FinaleWordmarkLook(
        glowAnimated = false,
        staticShadow = WebsiteShadow,
        gradientSlides = false,
        letterAlpha = false,
        wordAlphaLayers = 1,
    )
    FinaleWordmarkFill.Original, FinaleWordmarkFill.ThemePartner -> FinaleWordmarkLook(
        glowAnimated = true,
        staticShadow = null,
        gradientSlides = true,
        letterAlpha = true,
        wordAlphaLayers = 1,
    )
}

// The weights of red, green and blue in the brightness of a colour (Rec. 709), applied to the colour's own channels.
private const val LUMA_RED = 0.2126f
private const val LUMA_GREEN = 0.7152f
private const val LUMA_BLUE = 0.0722f

/** The brightness of [colour], 0 for black and 1 for white. A mix with white raises it in a straight line. */
internal fun lumaOf(colour: Color): Float =
    LUMA_RED * colour.red + LUMA_GREEN * colour.green + LUMA_BLUE * colour.blue

/**
 * The colours the wordmark's gradient starts from for [fill]: the website's mode sweep ([websiteSweepStops]), the stops built from
 * the chosen theme ([themeGradientStops]) or the app's four AI gradient stops. They are the colours the blend then moves to the
 * theme colour ([finaleGradientColors]).
 */
internal fun finaleBrandStops(
    themeColour: Color,
    fill: FinaleWordmarkFill = FINALE_WORDMARK_FILL,
): List<Color> = when (fill) {
    FinaleWordmarkFill.WebsiteModes -> websiteSweepStops()
    FinaleWordmarkFill.ThemePartner -> themeGradientStops(themeColour)
    FinaleWordmarkFill.Original -> AiGradientTokens.stops
}

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
