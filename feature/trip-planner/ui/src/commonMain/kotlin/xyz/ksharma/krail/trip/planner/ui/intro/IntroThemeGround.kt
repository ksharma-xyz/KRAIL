package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color

// How strongly the theme colour tints the ground, light and dark: the strengths of taj's own `themeSolidBackgroundColor`, which a
// test holds this to, so the ground the wash grows into is the ground the screen settles on.
private const val GROUND_TINT_LIGHT = 0.15f
private const val GROUND_TINT_DARK = 0.45f

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
