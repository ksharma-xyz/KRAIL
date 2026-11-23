package xyz.ksharma.krail.info.tiles.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.SHADOW_ALPHA
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.borderWidth
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shadowRadius
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shadowSpread
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
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

    /**
     * The soft tint of the rider's theme colour as the fill (the Timetable's own tint over the surface of the mode the tile is
     * drawn in, solid, so nothing shows through it), the tile's 1 dp themed border and no drop shadow. For a tile that sits on a
     * card or over the map, where the glow is the wrong look. The mode is the app mode, so a caller that pins the light palette
     * (the always light map) gets the light tint in dark mode. The layout, the padding and the behaviour are the other styles' own.
     */
    Plain,

    /**
     * Paints nothing of its own: no fill, no border and no shadow, so the surface is the caller's, drawn by the modifier it passes
     * in. It is for a tile that has to wear exactly the surface of another card, so the two take their fill, hairline and corner
     * from one source. The dismiss label takes the caller's text colour ([xyz.ksharma.krail.taj.LocalTextColor]) rather than the
     * theme ink a text button reads, so it is the same colour as the rest of the card's text. The layout, the padding and the
     * behaviour are the other styles' own.
     */
    Bare,
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
internal fun Modifier.infoTileOuter(style: InfoTileStyle, shape: Shape): Modifier = when (style) {
    InfoTileStyle.Standard -> infoTileGlow(shape)

    InfoTileStyle.AiFill, InfoTileStyle.Bare -> this

    InfoTileStyle.Plain -> border(width = borderWidth, color = themeBackgroundColor(), shape = shape)
}

/** The surface, applied after the click target and clip. */
@Composable
internal fun Modifier.infoTileSurface(style: InfoTileStyle, shape: Shape): Modifier = when (style) {
    InfoTileStyle.Standard -> standardSurface(shape)

    InfoTileStyle.AiFill -> background(brush = aiFillBrush(), shape = shape)

    InfoTileStyle.Plain -> background(color = plainFill(), shape = shape)

    InfoTileStyle.Bare -> this
}

/** The Timetable's tint laid over the page surface of the mode the tile is read in, so it is opaque and follows [isAppInDarkMode]. */
@Composable
private fun plainFill(): Color =
    themeBackgroundColor().compositeOver(if (isAppInDarkMode()) md_theme_dark_surface else md_theme_light_surface)

// The fill sits over the glow's shadow, so the shadow colour does not show through. The border is part of the glow.
@Composable
private fun Modifier.standardSurface(shape: Shape): Modifier = background(color = KrailTheme.colors.surface, shape = shape)

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
