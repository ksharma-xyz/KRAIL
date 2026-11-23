package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.ensureContrastWith
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.themeDecorColor

// How strongly the theme colour tints the ground, light and dark: the strengths of taj's own `themeSolidBackgroundColor`, which a
// test holds this to, so the ground the wash grows into is the ground the screen settles on.
private const val GROUND_TINT_LIGHT = 0.15f
private const val GROUND_TINT_DARK = 0.45f

/** The least contrast the tagline's ink has on the tinted ground in light mode: 7 to 1, with the theme's hue kept. */
internal const val TAGLINE_MIN_CONTRAST_LIGHT = 7.0f

/**
 * The ink of the tagline that replaces the question once a theme is chosen, on [ground], the tinted ground it is actually drawn on
 * ([introGroundColour]).
 *
 * - In dark mode it is [onSurface], the page's near white text ink, as it is. That is 6.6 to 8.2 to 1 on the grounds of the four
 *   themes the Intro offers, where the theme colour pulled towards it only until it reached 4.5 to 1 read as pale. A tint that
 *   reached 6 to 1 would be within a shade of the ink for Train, Bus and Ferry, so the ink is used.
 * - In light mode it is the theme colour [theme] made deeper by taj's blend ladder towards [onSurface] ([ensureContrastWith]) at
 *   [TAGLINE_MIN_CONTRAST_LIGHT], so it keeps the hue and has at least 7 to 1.
 *
 * taj's ink for the theme ([xyz.ksharma.krail.taj.theme.asThemeInk]) is worked out against the grounds of the app's plain surfaces,
 * which in dark mode are darker than this one, so on the tint it measured 2.75 to 2.98 to 1 there, and the ladder always ends at
 * [onSurface], which has the contrast on any ground the tint makes.
 */
internal fun introTaglineInk(theme: Color, ground: Color, onSurface: Color, dark: Boolean): Color =
    if (dark) {
        onSurface
    } else {
        theme.ensureContrastWith(background = ground, toward = onSurface, minContrast = TAGLINE_MIN_CONTRAST_LIGHT)
    }

/**
 * The tagline's ink for the theme the app is in, on the ground the Intro draws for it: the same [introTaglineInk], read from the
 * theme colour, the page and the mode, and remembered, so it is one colour computation for a screen and none per frame.
 */
@Composable
internal fun introTaglineInkColor(): Color {
    val theme = themeDecorColor()
    val surface = KrailTheme.colors.surface
    val onSurface = KrailTheme.colors.onSurface
    val dark = isAppInDarkMode()
    return remember(theme, surface, onSurface, dark) {
        introTaglineInk(theme, introGroundColour(theme, surface, dark), onSurface, dark)
    }
}

/**
 * The colour of the window's ground once a theme is chosen: [theme] at a light tint over [surface], opaque, which is what taj's
 * `themeSolidBackgroundColor` gives for that theme. It is a pure function of colours, so the wash can be made for a theme that
 * is not the one the app is in yet.
 */
internal fun introGroundColour(theme: Color, surface: Color, dark: Boolean): Color {
    val tint = if (dark) GROUND_TINT_DARK else GROUND_TINT_LIGHT
    return Color(
        red = theme.red * tint + surface.red * (1f - tint),
        green = theme.green * tint + surface.green * (1f - tint),
        blue = theme.blue * tint + surface.blue * (1f - tint),
        alpha = 1f,
    )
}
