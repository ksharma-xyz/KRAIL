package xyz.ksharma.krail.info.tiles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.SHADOW_ALPHA
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.borderWidth
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shadowRadius
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shadowSpread
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shape
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens

/** How an [InfoTile] is painted. Only the surface changes; layout and behaviour are shared. */
enum class InfoTileStyle {
    /** The surface tile with a soft border and glow in the rider's theme colour. */
    Standard,

    /**
     * Filled with the rider's AI gradient, toned down behind the text, with a turning wheel mark
     * before the title. For tips the app works out from live data.
     */
    AiFill,
}

private const val AI_FILL_STOP_SHARE_DARK = 0.4f
private const val AI_FILL_STOP_SHARE_LIGHT = 0.2f

/**
 * The info tile's glow: a soft shadow in the rider's theme colour around [shape] and a thin border of the same
 * colour on its edge. It is the one place those numbers (from [InfoTileDefaults]) are applied, so every surface
 * that wants the tile's look, the tile itself and the Search Stop box, wears the same glow.
 *
 * Apply it before the surface: the shadow must come before any clip, so it can spread outside the shape, and the
 * surface's own fill and clip come after it with the same [shape]. The border is drawn over everything the surface
 * holds, on the edge of the shape.
 */
@Composable
fun Modifier.infoTileGlow(shape: Shape = InfoTileDefaults.shape): Modifier {
    val glow = themeBackgroundColor()
    return this
        .dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = shadowRadius,
                color = glow,
                spread = shadowSpread,
                alpha = SHADOW_ALPHA,
            ),
        )
        .border(width = borderWidth, color = glow, shape = shape)
}

/** The glow and border, applied before the click target so the shadow sits outside it. */
@Composable
internal fun Modifier.infoTileOuter(style: InfoTileStyle): Modifier = when (style) {
    InfoTileStyle.Standard -> infoTileGlow(shape)

    InfoTileStyle.AiFill -> this
}

/** The surface, applied after the click target and clip. */
@Composable
internal fun Modifier.infoTileSurface(style: InfoTileStyle): Modifier = when (style) {
    InfoTileStyle.Standard -> standardSurface()

    InfoTileStyle.AiFill -> background(brush = aiFillBrush(), shape = shape)
}

// The fill sits over the glow's shadow, so the shadow colour does not show through. The border is part of the glow.
@Composable
private fun Modifier.standardSurface(): Modifier = background(color = KrailTheme.colors.surface, shape = shape)

@Composable
private fun aiFillBrush(): Brush {
    val themeColorHex by LocalThemeColor.current
    val stops = remember(themeColorHex) { AiThemeGradientTokens.stopsFor(themeColorHex) }
    val surface = KrailTheme.colors.surface
    // How much of each stop shows through. The text on top stays the surface's own ink, so the
    // fill stays light enough for it to pass 4.5 to 1 on every theme, Magic Yellow included.
    val fraction = if (isAppInDarkMode()) AI_FILL_STOP_SHARE_DARK else AI_FILL_STOP_SHARE_LIGHT
    return Brush.horizontalGradient(stops.map { lerp(surface, it, fraction) })
}
