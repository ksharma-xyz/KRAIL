package xyz.ksharma.krail.taj.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import xyz.ksharma.krail.taj.brighten
import xyz.ksharma.krail.taj.darken

// https://www.w3.org/TR/WCAG21/#contrast-minimum
internal const val DEFAULT_TEXT_SIZE_CONTRAST_AA = 4.5f

private const val MAX_CONTRAST_ITERATIONS = 20
private const val CONTRAST_STEP = 0.05f
private const val BACKGROUND_LUMINANCE_MIDPOINT = 0.5f

/**
 * Calculates the contrast ratio between two colors
 */
fun Color.contrastRatio(other: Color): Float {
    val luminance1 = this.luminance() + 0.05f
    val luminance2 = other.luminance() + 0.05f
    return if (luminance1 > luminance2) luminance1 / luminance2 else luminance2 / luminance1
}

/**
 * Returns a foreground color that has a contrast ratio of at least 4.0 with the provided background
 * color.
 */
/**
 * Determines the appropriate foreground color based on the provided background color.
 * If a foreground color is provided, it checks the contrast ratio between the foreground
 * and background colors. If the contrast ratio is sufficient (>= 4.0), it returns the
 * provided foreground color. Otherwise, it defaults to checking predefined light and dark
 * theme colors and returns the one with a sufficient contrast ratio.
 *
 * @param backgroundColor The background color to compare against.
 * @param foregroundColor An optional foreground color to check for contrast ratio.
 * @return The appropriate foreground color with sufficient contrast ratio.
 */
fun getForegroundColor(
    backgroundColor: Color,
    foregroundColor: Color? = null,
): Color {
    // If a foreground color is provided, check its contrast ratio
    foregroundColor?.let { color ->
        if (color.contrastRatio(backgroundColor) >= DEFAULT_TEXT_SIZE_CONTRAST_AA) return color
    }

    // Default to predefined light and dark theme colors
    val lightForegroundColor = md_theme_dark_onSurface
    val darkForegroundColor = md_theme_light_onSurface
    val highContrastLightForegroundColor = Color(0xFF000000) // High contrast light color
    val highContrastDarkForegroundColor = Color(0xFFFFFFFF) // High contrast dark color

    // Return the color with a sufficient contrast ratio
    return if (lightForegroundColor.contrastRatio(backgroundColor) >= DEFAULT_TEXT_SIZE_CONTRAST_AA) {
        lightForegroundColor
    } else if (darkForegroundColor.contrastRatio(backgroundColor) >= DEFAULT_TEXT_SIZE_CONTRAST_AA) {
        darkForegroundColor
    } else if (highContrastLightForegroundColor.contrastRatio(backgroundColor) >= DEFAULT_TEXT_SIZE_CONTRAST_AA) {
        highContrastLightForegroundColor
    } else {
        highContrastDarkForegroundColor
    }
}

fun shouldUseDarkIcons(backgroundColor: Color): Boolean {
    return getForegroundColor(backgroundColor = backgroundColor) == md_theme_dark_onSurface
}

/**
 * Adjusts this color to meet WCAG AA contrast ratio against [background].
 *
 * If the current contrast is already sufficient, the original color is returned unchanged.
 * Otherwise the color is iteratively brightened (on dark backgrounds) or darkened (on light
 * backgrounds) in small HSV-Value steps until the target ratio is reached or the step limit
 * is exhausted — whichever comes first.
 *
 * This preserves the hue and saturation of the original line color so transport brand colors
 * remain recognisable while remaining legible.
 *
 * @param background The surface color the text will be drawn on.
 * @param minContrast Minimum contrast ratio required (default: WCAG AA 4.5:1 for normal text).
 */
fun Color.ensureMinimumContrast(
    background: Color,
    minContrast: Float = DEFAULT_TEXT_SIZE_CONTRAST_AA,
): Color {
    if (contrastRatio(background) >= minContrast) return this
    val shouldLighten = background.luminance() < BACKGROUND_LUMINANCE_MIDPOINT
    var adapted = this
    repeat(MAX_CONTRAST_ITERATIONS) {
        if (adapted.contrastRatio(background) >= minContrast) return adapted
        adapted = if (shouldLighten) adapted.brighten(CONTRAST_STEP) else adapted.darken(CONTRAST_STEP)
    }
    return adapted
}

/**
 * Blends this colour toward [toward] until it clears [minContrast] against [background].
 *
 * ## Why this exists alongside [ensureMinimumContrast]
 *
 * [ensureMinimumContrast] raises HSV *value* and nothing else, so it cannot help a colour that
 * is already at full value — every hue with a low luminance ceiling (violets, blues, deep reds)
 * comes back still failing, and silently, because the loop returns the colour anyway when it
 * runs out of steps. Swept over 1920 hue/saturation/value combinations it returns a
 * still-failing colour for 24% of them against the bottom-sheet ground. [ContrastAdaptationTest]
 * holds that number at zero for this function.
 *
 * ## Why a ladder scan and not a binary search
 *
 * The obvious implementation binary-searches the blend factor, which is only correct if contrast
 * rises monotonically along the blend. It does not: Compose's [lerp] interpolates in Oklab, and
 * 110 of 7680 measured ladders reverse somewhere in the middle. So this walks the ladder from
 * the smallest blend up and takes the first rung that clears — no monotonicity assumed, and the
 * result is the least-adapted colour on the ladder rather than merely *a* passing one.
 *
 * ## The background must be opaque
 *
 * [contrastRatio] reads `luminance()`, which ignores the alpha channel, so a translucent
 * [background] is measured as though it were fully opaque and the result over-adapts. Composite
 * a translucent ground over what is behind it before passing it here.
 *
 * ## Why it is total
 *
 * The last rung is [toward] itself. Callers pass the ground's own `onSurface`, which clears every
 * threshold against it by definition — `KrailColorTokenContrastTest` holds that precondition — so
 * a passing rung always exists. When [toward] does not clear, there is nothing better available
 * and it is returned as the best effort.
 *
 * Quantising to [BLEND_LADDER_STEPS] rungs is also what keeps the output stable: a one-digit
 * edit to a surface token can move a continuous search by 1/255 in every derived colour and
 * churn every screenshot golden, where a ladder holds still until the change is big enough to
 * cross a rung.
 *
 * @param background The ground the colour will be drawn on.
 * @param toward The colour to blend towards — the ground's own content colour.
 * @param minContrast Minimum ratio required, before [CONTRAST_SAFETY_MARGIN] is added.
 */
fun Color.ensureContrastWith(
    background: Color,
    toward: Color,
    minContrast: Float = DEFAULT_TEXT_SIZE_CONTRAST_AA,
): Color {
    val target = minContrast + CONTRAST_SAFETY_MARGIN
    if (contrastRatio(background) >= target) return this

    return (1..BLEND_LADDER_STEPS)
        .asSequence()
        .map { rung -> lerp(this, toward, rung.toFloat() / BLEND_LADDER_STEPS) }
        .firstOrNull { candidate -> candidate.contrastRatio(background) >= target }
        ?: toward
}

/**
 * Never land exactly on the threshold. Ratios are compared as floats, the result is quantised to
 * 8 bits per channel on the way into [Color], and anti-aliasing moves the rendered value again —
 * a colour measured at exactly 4.50 is one rounding away from failing.
 */
internal const val CONTRAST_SAFETY_MARGIN = 0.05f

/**
 * Rungs on the blend ladder [ensureContrastWith] walks.
 *
 * Coarse enough that a one-digit token edit cannot shift every derived colour, fine enough that
 * the first passing rung is not far past the threshold. At 16 the shipped themes landed up to 0.5
 * above their target, which is brand identity given away for nothing; 32 halves that, and 64 buys
 * almost nothing on top.
 */
internal const val BLEND_LADDER_STEPS = 32
