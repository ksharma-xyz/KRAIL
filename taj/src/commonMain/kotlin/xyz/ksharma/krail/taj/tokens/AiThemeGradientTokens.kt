package xyz.ksharma.krail.taj.tokens

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.theme.DEFAULT_THEME_STYLE
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.magic_yellow

/**
 * The gradient the AI search surface wears: the rider's own theme colour, plus one other
 * KRAIL colour far enough around the wheel to read as a second colour rather than as the
 * first one with a wobble.
 *
 * [AiCoolGradientTokens] is a fixed blue/violet/pink that ignores the theme. That was the
 * right call for a small mark tucked into a row; on a surface that fills the bottom of the
 * screen it reads as somebody else's product dropped into KRAIL. This one moves with the
 * theme so the surface still belongs to the app the rider chose to look at.
 *
 * The partner is not the nearest colour. Anything under roughly 40 degrees of hue apart
 * blends into one muddy band, so each pair here sits between 97 and 134 degrees apart: far
 * enough that the gradient has somewhere to travel, near enough to stay related.
 *
 * Three stops, not two. A straight RGB blend across that much hue passes near grey in the
 * middle (teal into violet is the worst of them). [midStop] bends the path around that dull
 * zone by taking the midpoint in HSV along the shorter arc, so the middle of the gradient
 * stays as saturated as its ends.
 */
object AiThemeGradientTokens {

    private val Train = Color(0xFFF6891F)
    private val Metro = Color(0xFF009B77)
    private val Bus = Color(0xFF00B5EF)
    private val PurpleDrip = Color(0xFFAC00C9)
    private val Ferry = Color(0xFF5AB031)
    private val BarbiePink = Color(0xFFE0218A)
    private val MagicYellow = magic_yellow

    private val partners: Map<KrailThemeStyle, Pair<Color, Color>> = mapOf(
        KrailThemeStyle.Train to (Train to PurpleDrip),
        // Teal into yellow, not into purple. Purple Drip is a magenta, so the teal theme's
        // gradient read as teal and pink, and the two ends fought rather than travelled. Yellow
        // is the same cool-to-warm crossing that works for Bus, at 119 degrees.
        KrailThemeStyle.Metro to (Metro to MagicYellow),
        // Yellow, not pink. Blue into pink travels through purple and reads as somebody
        // else's AI product; blue into yellow is the one pair here that crosses from cool to
        // warm, so the two ends stay told apart wherever the gradient is in its drift. It is
        // also KRAIL's own yellow rather than a colour invented for this.
        KrailThemeStyle.Bus to (Bus to MagicYellow),
        KrailThemeStyle.PurpleDrip to (PurpleDrip to Bus),
        KrailThemeStyle.Ferry to (Ferry to BarbiePink),
        KrailThemeStyle.BarbiePink to (BarbiePink to Bus),
    )

    /**
     * Theme colour first, so the gradient starts on the colour the rider picked and travels
     * away from it. Bus and Purple Drip share a pair led from opposite ends, which is the
     * point: the theme end is what changes.
     */
    fun stopsFor(style: KrailThemeStyle): List<Color> {
        val (start, end) = partners[style] ?: partners.getValue(DEFAULT_THEME_STYLE)
        return listOf(start, midStop(start, end), end)
    }

    /**
     * Looks the theme up by its `#AARRGGBB` code, since that is what `LocalThemeColor` carries
     * around the app rather than the enum. An unknown code falls back to the default theme's
     * pair instead of drawing nothing.
     */
    fun stopsFor(themeHexColorCode: String): List<Color> =
        stopsFor(
            KrailThemeStyle.entries.firstOrNull {
                it.hexColorCode.equals(themeHexColorCode, ignoreCase = true)
            } ?: DEFAULT_THEME_STYLE,
        )
}

private const val HUE_DEGREES = 360f
private const val HUE_SECTOR_DEGREES = 60f
private const val HALF_TURN_DEGREES = 180f
private const val HUE_SECTORS = 6f
private const val GREEN_SECTOR_OFFSET = 2f
private const val BLUE_SECTOR_OFFSET = 4f
private const val SECTOR_PAIR = 2f
private const val SECTOR_GREEN_TO_CYAN = 2
private const val SECTOR_CYAN_TO_BLUE = 3
private const val SECTOR_BLUE_TO_MAGENTA = 4

/** Midpoint of the shorter hue arc between two colours, at their average saturation and value. */
private fun midStop(start: Color, end: Color): Color {
    val (startHue, startSaturation, startValue) = start.toHsv()
    val (endHue, endSaturation, endValue) = end.toHsv()

    var delta = endHue - startHue
    if (delta > HALF_TURN_DEGREES) delta -= HUE_DEGREES
    if (delta < -HALF_TURN_DEGREES) delta += HUE_DEGREES

    val midHue = (startHue + delta / 2f + HUE_DEGREES) % HUE_DEGREES
    return hsvToColor(
        hue = midHue,
        saturation = (startSaturation + endSaturation) / 2f,
        value = (startValue + endValue) / 2f,
    )
}

private data class Hsv(val hue: Float, val saturation: Float, val value: Float)

private fun Color.toHsv(): Hsv {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val chroma = max - min

    val hue = when {
        chroma == 0f -> 0f
        max == red -> HUE_SECTOR_DEGREES * (((green - blue) / chroma) % HUE_SECTORS)
        max == green -> HUE_SECTOR_DEGREES * (((blue - red) / chroma) + GREEN_SECTOR_OFFSET)
        else -> HUE_SECTOR_DEGREES * (((red - green) / chroma) + BLUE_SECTOR_OFFSET)
    }

    return Hsv(
        hue = (hue + HUE_DEGREES) % HUE_DEGREES,
        saturation = if (max == 0f) 0f else chroma / max,
        value = max,
    )
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val chroma = value * saturation
    val sector = hue / HUE_SECTOR_DEGREES
    val second = chroma * (1f - kotlin.math.abs((sector % SECTOR_PAIR) - 1f))
    val match = value - chroma

    val (red, green, blue) = when (sector.toInt()) {
        0 -> Triple(chroma, second, 0f)
        1 -> Triple(second, chroma, 0f)
        SECTOR_GREEN_TO_CYAN -> Triple(0f, chroma, second)
        SECTOR_CYAN_TO_BLUE -> Triple(0f, second, chroma)
        SECTOR_BLUE_TO_MAGENTA -> Triple(second, 0f, chroma)
        else -> Triple(chroma, 0f, second)
    }

    return Color(red = red + match, green = green + match, blue = blue + match)
}
