package xyz.ksharma.krail.taj

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import kotlin.math.absoluteValue

fun String.hexToComposeColor(): Color {
    require(isValidHexColorCode()) {
        "Invalid hex color code: $this. Hex color codes must be in the format #RRGGBB or #AARRGGBB."
    }

    // Remove the leading '#' if present
    val hex = removePrefix("#")

    // Parse the hex value
    return when (hex.length) {
        6 -> {
            // If the string is in the format RRGGBB, add full opacity (FF) at the start
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            Color(red = r / 255f, green = g / 255f, blue = b / 255f)
        }

        8 -> {
            // If the string is in the format AARRGGBB
            val a = hex.substring(0, 2).toInt(16)
            val r = hex.substring(2, 4).toInt(16)
            val g = hex.substring(4, 6).toInt(16)
            val b = hex.substring(6, 8).toInt(16)
            Color(alpha = a / 255f, red = r / 255f, green = g / 255f, blue = b / 255f)
        }

        else -> throw IllegalArgumentException("Invalid hex color format. Use #RRGGBB or #AARRGGBB.")
    }
}

/**
 * Checks if a string is a valid hexadecimal color code.
 *
 * This function uses a regular expression to validate the format of the
 * provided string. A valid hex color code must start with "#" followed by
 * either 6 or 8 hexadecimal digits (0-9, A-F, a-f).
 *
 * @return true if the string is a valid hex color code, false otherwise.
 */
private fun String.isValidHexColorCode(): Boolean {
    // Regular expression to match #RRGGBB or #RRGGBBAA hex color codes
    val hexColorRegex = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})\$")
    return hexColorRegex.matches(this)
}

@Composable
fun themeColor(): Color {
    val themeColor by LocalThemeColor.current
    return themeColor.hexToComposeColor()
}

@Composable
fun themeBackgroundColor(): Color {
    val themeColor by LocalThemeColor.current
    return if (isSystemInDarkTheme()) {
        themeColor.hexToComposeColor().copy(alpha = 0.45f)
    } else {
        themeColor.hexToComposeColor().copy(alpha = 0.15f)
    }
}

/**
 * Extension function to brighten a Compose Color.
 *
 * This function increases the lightness of the given Compose Color by the specified factor.
 * It converts the color to ARGB format, brightens it, and then converts it back to a Compose Color.
 *
 * @param factor The factor by which to brighten the color (default is 20%).
 *
 * @return The brightened Compose [Color].
 */
fun Color.brighten(factor: Float = 0.2f): Color {
    val argb = this.toArgb()
    val brightenedArgb = brightenColor(argb, factor)
    return Color(brightenedArgb)
}

/**
 * Update the theme color to make text more readable on top of it.
 *
 * Steps
 * 1. Convert to HSL, so we can modify the lightness without changing the core hue and saturation.
 * For 0xFFEE343F (R: 238, G: 52, B: 63):  Hue ≈ 355°, Saturation ≈ 85%, Lightness ≈ 57%
 * 2. Increase lightness by 20%
 * 3. Convert back to RGB
 *
 * @param color The color to brighten, in ARGB format.
 * @param factor The factor by which to brighten the color (default is 20%)
 *
 * @return The brightened color in ARGB format
 */
fun brightenColor(color: Int, factor: Float = 0.2f): Int {
    // Extract RGB components from the color
    val red = (color shr 16 and 0xFF) / 255f
    val green = (color shr 8 and 0xFF) / 255f
    val blue = (color and 0xFF) / 255f

    // Convert RGB to HSV
    val hsv = rgbToHsv(red, green, blue)

    // Adjust brightness (value) within bounds
    hsv[2] = (hsv[2] + factor).coerceIn(0f, 1f)

    // Convert back to RGB and return the color as an Int
    return hsvToColor(hsv)
}

private fun rgbToHsv(r: Float, g: Float, b: Float): FloatArray {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val h: Float = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta + (if (g < b) 6 else 0)) % 6
        max == g -> (b - r) / delta + 2
        else -> (r - g) / delta + 4
    } * 60

    val s: Float = if (max == 0f) 0f else delta / max
    val v: Float = max

    return floatArrayOf(h, s, v)
}

private fun hsvToColor(hsv: FloatArray): Int {
    val h = hsv[0]
    val s = hsv[1]
    val v = hsv[2]

    val c = v * s
    val x = c * (1 - ((h / 60) % 2 - 1).absoluteValue)
    val m = v - c

    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val red = ((r + m) * 255).toInt()
    val green = ((g + m) * 255).toInt()
    val blue = ((b + m) * 255).toInt()

    return (255 shl 24) or (red shl 16) or (green shl 8) or blue
}

@Composable
fun themeContentColor(): Color {
    val themeContentColor by LocalThemeContentColor.current
    return themeContentColor.hexToComposeColor()
}

/**
 * Converts a Compose Color object to a hexadecimal color string.
 *
 * @return A string representing the hexadecimal color code (e.g., "#FF0000" for red).
 */
fun Color.toHex(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    val alpha = (this.alpha * 255).toInt()
    return "#${alpha.toHex()}${red.toHex()}${green.toHex()}${blue.toHex()}"
}

private fun Int.toHex(): String {
    return this.toString(16).padStart(2, '0').uppercase()
}

fun Color.darken(factor: Float = 0.2f): Color {
    val argb = this.toArgb()
    val darkArgb = darkenColor(argb, factor)
    return Color(darkArgb)
}

@Composable
fun backgroundColorOf(color: Color): Color {
    return if (isSystemInDarkTheme()) {
        color.darken()
    } else {
        color.brighten()
    }
}

fun darkenColor(color: Int, factor: Float = 0.2f): Int {
    // Extract RGB components from the color
    val red = (color shr 16 and 0xFF) / 255f
    val green = (color shr 8 and 0xFF) / 255f
    val blue = (color and 0xFF) / 255f

    // Convert RGB to HSV
    val hsv = rgbToHsv(red, green, blue)

    // Adjust brightness (value) within bounds
    hsv[2] = (hsv[2] - factor).coerceIn(0f, 1f)

    // Convert back to RGB and return the color as an Int
    return hsvToColor(hsv)
}

@Composable
fun themeSolidBackgroundColor(): Color {
    val themeColor by LocalThemeColor.current
    return if (isSystemInDarkTheme()) {
        // Blend the theme color with dark surface for solid color
        blendColors(
            foreground = themeColor.hexToComposeColor().copy(alpha = 0.45f),
            background = KrailTheme.colors.surface
        )
    } else {
        // Blend the theme color with light surface for solid color
        blendColors(
            foreground = themeColor.hexToComposeColor().copy(alpha = 0.15f),
            background = KrailTheme.colors.surface
        )
    }
}

/**
 * Blends two colors based on the alpha of the foreground color.
 *
 * This function takes a foreground color and a background color, and blends them
 * according to the alpha value of the foreground color. The resulting color is
 * a mix of both colors, with the foreground color's alpha determining how much
 * of each color is present in the final result.
 *
 * @param foreground The foreground color to blend.
 * @param background The background color to blend with.
 * @return A new Color that is a blend of the foreground and background colors.
 */
private fun blendColors(foreground: Color, background: Color): Color {
    val alpha = foreground.alpha
    return Color(
        red = foreground.red * alpha + background.red * (1 - alpha),
        green = foreground.green * alpha + background.green * (1 - alpha),
        blue = foreground.blue * alpha + background.blue * (1 - alpha),
        alpha = 1f
    )
}

/**
 * Returns a list of colors used for the magic border effect.
 *
 * This function provides a predefined list of colors that can be used to create
 * a visually appealing border effect, to be consistent with the Krail theme.
 *
 * @return A list of [Color] objects representing the magic border colors.
 */
@Composable
fun magicBorderColors(): List<Color> = listOf(
    KrailThemeStyle.PurpleDrip.hexColorCode.hexToComposeColor(),
    KrailTheme.colors.magicYellow,
    KrailThemeStyle.BarbiePink.hexColorCode.hexToComposeColor(),
)
