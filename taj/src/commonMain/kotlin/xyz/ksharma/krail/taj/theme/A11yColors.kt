package xyz.ksharma.krail.taj.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// https://www.w3.org/TR/WCAG21/#contrast-minimum
private const val DEFAULT_TEXT_SIZE_CONTRAST_AA = 4.5f

// when font scale greater than 1.2f. Text size is 18dp default and 14dp bold.
private const val LARGE_TEXT_SIZE_CONTRAST_AA = 3.0f

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
