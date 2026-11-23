package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.LocalThemeController
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.taj.themeDecorColor

/**
 * Where every card that floats over the map takes its colours from. The map is always light, in both app modes, so there are
 * two families here and which one a surface wears is decided by what it is, not by the app mode.
 *
 * The cards that hold the rider's own content on the map are one fixed light look ([tint], [onTint], [chip], [chipEdge]): the
 * Search Stop box, the bare single pill, the location notice, the Options card and the Options pill. The fill is the theme
 * colour at [TIMETABLE_ALPHA_LIGHT] over white (the Timetable's own tint function, evaluated with the light palette even in dark
 * mode), the text is the light palette's `onSurface`, and the edge is a 1 dp hairline in the light palette's `softLabel` (3.81 to
 * 1 against the map ground, where the dark mode `softLabel` would be 1.82). Every colour is a taj token, none is typed here.
 * `MapCardColorsTest` pins them equal in light and dark app mode.
 *
 * The stop card is not part of that look and is not changed: its [body], [header] and [onCard] are the older mode aware family,
 * the theme colour at a few strengths laid over the page's own surface, which makes each step an opaque colour (over the
 * light map a translucent wash would show the map through it). There are two steps and which is stronger is the same in both
 * modes, the header:
 * - Light mode: the body is the Timetable's tint, [TIMETABLE_ALPHA_LIGHT] of the theme colour, and the header a stronger
 *   step, [HEADER_ALPHA_LIGHT]. A stronger step reads clearly as a band and still keeps the text far above 4.5 to 1.
 * - Dark mode: the header is the Timetable's tint, [TIMETABLE_ALPHA_DARK], and the body is half of it, [BODY_ALPHA_DARK].
 *   A stronger step than the Timetable's drops the text under 4.5 to 1 on some themes, so the body steps down instead.
 *
 * `MapCardColorsTest` prints every theme in both modes and holds the header against the body at 1.15 to 1 or better and
 * the text on both at 4.5 to 1 or better.
 */
@Immutable
internal class MapCardColors(
    /** The stop card's body: the soft tint, in the mode the app is in. */
    val body: Color,
    /** A band on the stop card (its header): a visibly different step of the same family. */
    val header: Color,
    /** Text and icons on [body] and [header]: the page's own text colour, in the mode the app is in. */
    val onCard: Color,
    /**
     * The fill of the Search Stop card and of every surface that wears its look: the theme colour at 15 percent over white, the
     * same in light and dark app mode, opaque.
     */
    val tint: Color,
    /** Text, markers and icons on [tint]: the light palette's `onSurface`, whatever the app mode. */
    val onTint: Color,
    /** A small chip on the map: [tint] with [onTint] on it. */
    val chip: ButtonColors,
    /**
     * The Options pill at rest: white (the light palette's `surface`) with [onTint] on it, whatever the app mode. White is neutral
     * beside the pale card, the blue location control and the theme coloured go circle, and 20.87 to 1 under its text. It is
     * the pill's resting look, and its solid theme colour ([mapActiveChipColors]) stays the look of an active filter.
     */
    val pill: ButtonColors,
    /** The hairline round every light card and chip: the light palette's `softLabel`, whatever the app mode. */
    val chipEdge: Color,
)

internal const val TIMETABLE_ALPHA_LIGHT = 0.15f
internal const val TIMETABLE_ALPHA_DARK = 0.45f
internal const val HEADER_ALPHA_LIGHT = 0.35f
internal const val BODY_ALPHA_DARK = 0.225f

/**
 * The card colours for [themeColor] in light or dark mode ([darkMode]), over the page's [surface], with [onSurface] as the
 * text. Plain numbers in and out, so the contrast of every theme in both modes is read with no composition.
 */
internal fun mapCardColorsFor(themeColor: Color, darkMode: Boolean, surface: Color, onSurface: Color): MapCardTints {
    val timetable = themeColor.copy(alpha = if (darkMode) TIMETABLE_ALPHA_DARK else TIMETABLE_ALPHA_LIGHT).compositeOver(surface)
    return if (darkMode) {
        MapCardTints(
            header = timetable,
            body = themeColor.copy(alpha = BODY_ALPHA_DARK).compositeOver(surface),
            chip = timetable,
            onCard = onSurface,
        )
    } else {
        MapCardTints(
            header = themeColor.copy(alpha = HEADER_ALPHA_LIGHT).compositeOver(surface),
            body = timetable,
            chip = timetable,
            onCard = onSurface,
        )
    }
}

/** The fills [mapCardColorsFor] gives: [chip] is the Timetable's own tint, the same colour a Timetable chip is. */
internal data class MapCardTints(val header: Color, val body: Color, val chip: Color, val onCard: Color)

/** The one accessor of the card colours, for the current theme and mode. */
@Composable
internal fun mapCardColors(): MapCardColors {
    val theme = themeDecorColor()
    val inMode = mapCardColorsFor(
        themeColor = theme,
        darkMode = isAppInDarkMode(),
        surface = KrailTheme.colors.surface,
        onSurface = KrailTheme.colors.onSurface,
    )
    // The fixed light look: the same family function with the light palette's own tokens, so it is the Timetable's light tint over
    // white and the light text colour even while the app is in dark mode.
    val light = mapCardColorsFor(
        themeColor = theme,
        darkMode = false,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
    )
    val tint = light.chip
    val onTint = light.onCard
    return MapCardColors(
        body = inMode.body,
        header = inMode.header,
        onCard = inMode.onCard,
        tint = tint,
        onTint = onTint,
        chip = ButtonColors(
            containerColor = tint,
            contentColor = onTint,
            disabledContainerColor = tint.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = onTint.copy(alpha = DISABLED_ALPHA),
        ),
        pill = ButtonColors(
            containerColor = md_theme_light_surface,
            contentColor = onTint,
            disabledContainerColor = md_theme_light_surface.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = onTint.copy(alpha = DISABLED_ALPHA),
        ),
        chipEdge = md_theme_light_softLabel,
    )
}

/**
 * Everything inside draws in the light palette whatever the app mode is: taj's own text and content colours are the light
 * `onSurface`, and the app mode the theme colour's ink and tint are read in is light. It is for the surfaces that stand on the
 * always light map (the Search Stop box, the Options card and the location notice) and reuse a component that reads the mode
 * inside and takes no colours (a text button's ink, the info tile's border and tint). A colour that a component takes is passed
 * to it from [mapCardColors] instead.
 */
@Composable
internal fun MapLightScope(content: @Composable () -> Unit) {
    val controller = remember { ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {}) }
    CompositionLocalProvider(
        LocalThemeController provides controller,
        LocalTextColor provides md_theme_light_onSurface,
        LocalContentColor provides md_theme_light_onSurface,
        content = content,
    )
}

private const val DISABLED_ALPHA = 0.38f

/**
 * taj's own active look for a chip: the button's default colours, the solid theme colour with its foreground. It is the
 * Options button while a mode is switched off, against the soft tint it has when none is.
 */
@Composable
internal fun mapActiveChipColors(): ButtonColors = ButtonDefaults.buttonColors()

/**
 * The 1 dp hairline round a chip or card that floats over the map, in the light palette's `softLabel` ([MapCardColors.chipEdge]),
 * drawn over the fill so the edge shows even where its tint is close to the map's ground. It is drawn in both app modes.
 */
@Composable
internal fun Modifier.mapChipEdge(shape: Shape, color: Color = md_theme_light_softLabel): Modifier =
    drawWithContent {
        drawContent()
        drawOutline(outline = shape.createOutline(size, layoutDirection, this), color = color, style = Stroke(width = 1.dp.toPx()))
    }
