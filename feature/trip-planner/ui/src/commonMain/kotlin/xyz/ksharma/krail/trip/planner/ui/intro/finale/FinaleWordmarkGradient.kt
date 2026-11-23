package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.tokens.AiGradientTokens

// The brand gradient is wider than the wordmark and slides across it, so the whole range is seen by the end of
// the hold: it starts at the left edge and ends this share of a width to the left of it.
internal const val GRADIENT_SPAN = 1.6f
internal const val GRADIENT_SLIDE = 0.6f

/**
 * The gradient with every stop moved [blend] of the way to [themeColour], 0 being the [brand] colours as they
 * are and 1 being the theme colour at every stop, which is a solid colour. The wordmark passes the stops
 * [finaleBrandStops] gives it, and with none given these are the app's own AI gradient stops.
 */
internal fun finaleGradientColors(
    blend: Float,
    themeColour: Color,
    brand: List<Color> = AiGradientTokens.stops,
): List<Color> = brand.map { mix(it, themeColour, blend.coerceIn(0f, 1f)) }

// A straight blend of the two colours' channels, so 0 and 1 give back exactly the colour at that end.
private fun mix(from: Color, to: Color, share: Float): Color = Color(
    red = from.red + (to.red - from.red) * share,
    green = from.green + (to.green - from.green) * share,
    blue = from.blue + (to.blue - from.blue) * share,
    alpha = from.alpha + (to.alpha - from.alpha) * share,
)

/**
 * Where the gradient starts and ends across a wordmark [widthPx] wide, [travel] of the way through its slide:
 * from the left edge to [GRADIENT_SPAN] widths along at 0, and [GRADIENT_SLIDE] of a width to the left of that
 * at 1. Returned as the start and the end.
 */
internal fun gradientRangePx(widthPx: Float, travel: Float): Pair<Float, Float> {
    val start = -widthPx * GRADIENT_SLIDE * travel.coerceIn(0f, 1f)
    return start to start + widthPx * GRADIENT_SPAN
}

/**
 * Where the gradient sits across a word [widthPx] wide for [look]: sliding with [travel] when the look slides it, and, when it does
 * not, still, from the left edge to the right edge of the word, whatever [travel] is. The website's sweep is the second.
 */
internal fun wordmarkGradientRangePx(look: FinaleWordmarkLook, widthPx: Float, travel: Float): Pair<Float, Float> =
    if (look.gradientSlides) gradientRangePx(widthPx, travel) else 0f to widthPx
