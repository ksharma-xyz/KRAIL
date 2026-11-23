package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeDecorColor

/**
 * Where every card that floats over the map takes its fill from: the soft tint of the rider's theme colour, the family the
 * Timetable's From and To card and the saved trip cards are drawn in (`themeBackgroundColor`), so a card is a light peach on
 * Train in light mode and a dark brown on Train in dark mode, and never a near black slab or a pure white one. The family is
 * in this one place, so it is tuned here and nowhere else.
 *
 * The family is the theme colour at a few strengths laid over the page's own surface, which makes each step an opaque
 * colour: what the Timetable draws is the same tint over the same surface, and over the light map a translucent wash would
 * show the map through it. There are two steps and which is stronger is the same in both modes, the header:
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
    /** The body of a card: the soft tint. */
    val body: Color,
    /** A band on a card (the stop card's header): a visibly different step of the same family. */
    val header: Color,
    /** Text and icons on [body] and [header]: the page's own text colour. */
    val onCard: Color,
    /** A small chip on the map: the Timetable's tint, opaque, with [onCard] on it. */
    val chip: ButtonColors,
    /** The hairline round a chip, so a pale chip does not vanish into the light map. */
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
    val tints = mapCardColorsFor(
        themeColor = themeDecorColor(),
        darkMode = isAppInDarkMode(),
        surface = KrailTheme.colors.surface,
        onSurface = KrailTheme.colors.onSurface,
    )
    // The Timetable's own function, read here so the chip can never drift from it: the same tint over the same surface.
    val timetable = themeBackgroundColor().compositeOver(KrailTheme.colors.surface)
    val onCard = tints.onCard
    return MapCardColors(
        body = tints.body,
        header = tints.header,
        onCard = onCard,
        chip = ButtonColors(
            containerColor = timetable,
            contentColor = onCard,
            disabledContainerColor = timetable.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = onCard.copy(alpha = DISABLED_ALPHA),
        ),
        chipEdge = KrailTheme.colors.softLabel,
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
 * The 1 dp hairline round a chip that floats over the map, in `softLabel`, drawn over the chip's own fill so the chip's
 * edge shows even where its tint is close to the map's ground. It is the one place that edge is drawn.
 */
@Composable
internal fun Modifier.mapChipEdge(shape: Shape, color: Color = mapCardColors().chipEdge): Modifier =
    drawWithContent {
        drawContent()
        drawOutline(outline = shape.createOutline(size, layoutDirection, this), color = color, style = Stroke(width = 1.dp.toPx()))
    }
