package xyz.ksharma.krail.taj.theme

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.toHex

val DEFAULT_THEME_STYLE = KrailThemeStyle.Train

/**
 * @param aiGradientPartner The second colour the AI surface's gradient travels to. It has to be a
 *   decision rather than something derived, so it lives here beside the colour it partners: a new
 *   theme cannot compile without choosing one. It used to sit in a map inside
 *   `AiThemeGradientTokens` with a default, which meant a seventh theme silently wore Train's
 *   gradient instead of failing. `ThemeGradientPairTest` holds the hue distance that makes a pair
 *   work.
 */
enum class KrailThemeStyle(
    val hexColorCode: String,
    val id: Int,
    val tagLine: String,
    val aiGradientPartner: Color,
) {
    Train(
        hexColorCode = train_theme.toHex(),
        id = 1,
        tagLine = "On the track, no lookin' back!",
        aiGradientPartner = purple_drip_theme,
    ),

    // Teal into yellow, not into purple. Purple Drip is a magenta, so the teal theme's gradient
    // read as teal and pink, and the two ends fought rather than travelled. Yellow is the same
    // cool-to-warm crossing that works for Bus.
    Metro(
        hexColorCode = metro_theme.toHex(),
        id = 2,
        tagLine = "Surf the sub, no cap!",
        aiGradientPartner = magic_yellow,
    ),

    // Yellow, not pink. Blue into pink travels through purple and reads as somebody else's AI
    // product; blue into yellow is the one pair here that crosses from cool to warm, so the two
    // ends stay told apart wherever the gradient is in its drift.
    Bus(
        hexColorCode = bus_theme.toHex(),
        id = 5,
        tagLine = "Hoppin' the concrete jungle!",
        aiGradientPartner = magic_yellow,
    ),
    PurpleDrip(
        hexColorCode = purple_drip_theme.toHex(),
        id = 7,
        tagLine = "Purple drip, endless trip!",
        aiGradientPartner = bus_theme,
    ),
    Ferry(
        hexColorCode = ferry_theme.toHex(),
        id = 9,
        tagLine = "Smooth sail, no fail!",
        aiGradientPartner = barbie_pink_theme,
    ),
    BarbiePink(
        hexColorCode = barbie_pink_theme.toHex(),
        id = 100,
        tagLine = "Dressed in pink, fastest link!",
        aiGradientPartner = bus_theme,
    ),
}
