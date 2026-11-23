package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

// HSL and back, for the accent ink ladder of the Intro: pure functions of a colour, so each is pinned with no composition.

/** The colour in HSL, with the hue in degrees and the saturation and lightness from 0 to 1. */
internal fun Color.toHsl(): Triple<Float, Float, Float> {
    val maxChannel = max(red, max(green, blue))
    val minChannel = min(red, min(green, blue))
    val delta = maxChannel - minChannel
    val lightness = (maxChannel + minChannel) / 2f
    if (delta == 0f) return Triple(0f, 0f, lightness)
    val saturation = delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    val sector = when (maxChannel) {
        red -> ((green - blue) / delta).mod(HUE_SECTORS)
        green -> (blue - red) / delta + 2f
        else -> (red - green) / delta + BLUE_SECTOR_OFFSET
    }
    return Triple(sector * DEGREES_PER_SECTOR, saturation, lightness)
}

/** The opaque colour with [hue] in degrees and [saturation] and [lightness] from 0 to 1. */
internal fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val chroma = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val sector = (hue / DEGREES_PER_SECTOR).mod(HUE_SECTORS)
    val second = chroma * (1f - kotlin.math.abs(sector.mod(2f) - 1f))
    val (r, g, b) = when (sector.toInt()) {
        0 -> Triple(chroma, second, 0f)
        1 -> Triple(second, chroma, 0f)
        2 -> Triple(0f, chroma, second)
        SECTOR_CYAN -> Triple(0f, second, chroma)
        SECTOR_BLUE -> Triple(second, 0f, chroma)
        else -> Triple(chroma, 0f, second)
    }
    val match = lightness - chroma / 2f
    return Color(red = r + match, green = g + match, blue = b + match)
}

private const val HUE_SECTORS = 6f
private const val DEGREES_PER_SECTOR = 60f
private const val BLUE_SECTOR_OFFSET = 4f
private const val SECTOR_CYAN = 3
private const val SECTOR_BLUE = 4
