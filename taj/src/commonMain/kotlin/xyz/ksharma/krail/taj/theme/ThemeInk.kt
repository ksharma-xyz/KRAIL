package xyz.ksharma.krail.taj.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Derives the colour the rider's theme wears when it is used as **ink** — text, an icon, a
 * stroke — drawn straight onto one of the app's own grounds.
 *
 * ## Why the theme colour cannot be used raw
 *
 * A theme colour is chosen to look like the product, not to be legible on a near-black surface.
 * Purple Drip measures 2.95:1 on the dark surface and 2.49:1 on the bottom sheet, against a 4.5
 * requirement for text. It is the only shipped theme that fails, but "only one fails today" is
 * not a property anyone can rely on for the next theme.
 *
 * ## Why one ink per scheme, rather than one per ground
 *
 * The obvious thing is to adapt against whatever ground the ink happens to land on. That produces
 * a different purple on the surface, on a sheet and on a card, and a sheet opened over a screen
 * shows two of them at once. So the ink is derived **once per theme and scheme**, against the
 * hardest ground in that scheme, and used everywhere. Harder ground means more contrast
 * everywhere else, so one value clears them all.
 *
 * [themeInkColorOn] exists for the genuine exception: ink drawn on a ground that is not one of
 * these at all, such as the time picker's hand on its own theme-tinted dial.
 *
 * ## Why the hardest ground is computed and not named
 *
 * Naming it means a new ground token silently invalidates every ink. Picking it by luminance from
 * [inkGrounds] means adding a token to that list is the only step, and `ThemeInkContrastTest`
 * fails until a new [KrailColors] ground is either listed or explicitly excused.
 */
fun Color.asThemeInk(
    darkMode: Boolean,
    minContrast: Float = DEFAULT_TEXT_SIZE_CONTRAST_AA,
): Color = ensureContrastWith(
    background = hardestInkGround(darkMode),
    toward = if (darkMode) md_theme_dark_onSurface else md_theme_light_onSurface,
    minContrast = minContrast,
)

/**
 * The ground in [inkGrounds] that a scheme-appropriate ink has the least contrast against.
 *
 * Dark inks are light, so the *lightest* ground is hardest; light inks are dark, so the *darkest*
 * ground is hardest. Getting this backwards is easy and silent, which is why it is arithmetic
 * rather than a constant.
 */
internal fun hardestInkGround(darkMode: Boolean): Color {
    val grounds = inkGrounds(darkMode)
    return if (darkMode) {
        grounds.maxBy { it.luminance() }
    } else {
        grounds.minBy { it.luminance() }
    }
}

/**
 * Every opaque ground the rider's theme colour can be drawn onto as ink.
 *
 * This is the list `ThemeInkContrastTest` measures against, and the list it holds against
 * [KrailColors] so a newly added surface token cannot quietly skip the check.
 */
internal fun inkGrounds(darkMode: Boolean): List<Color> = if (darkMode) {
    listOf(
        md_theme_dark_surface,
        md_theme_dark_modal_sheet_background,
        md_theme_dark_discover_chip_background,
        md_theme_dark_discover_card_background,
        md_theme_dark_theme_selection_background,
        md_theme_dark_past_departure_row_surface,
    )
} else {
    listOf(
        md_theme_light_surface,
        md_theme_light_modal_sheet_background,
        md_theme_light_discover_chip_background,
        md_theme_light_discover_card_background,
        md_theme_light_theme_selection_background,
        md_theme_light_past_departure_row_surface,
    )
}
