package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.LocalThemeController
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface_placeholder
import xyz.ksharma.krail.taj.theme.md_theme_light_outline_subtle
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
 * That is the map state. The list state has no map: the Search Stop box and the notice stand on the cloud gradient, which follows
 * the app mode, so there they follow it too ([CardGroundScope]): light mode is the very same look, and dark mode is the Timetable's
 * own dark tint, [TIMETABLE_ALPHA_DARK] of the theme colour over the dark surface, with the dark palette's text and its
 * `softLabel` hairline. Outside a [CardGroundScope] every accessor here gives the map state's fixed light look.
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
    /** The hairline round every card and chip: the light palette's `softLabel` on the map, the app mode's own on the gradient. */
    val chipEdge: Color,
    /** The prompt of a field on the box: the light palette's placeholder on the map, the box's own text colour in dark mode. */
    val placeholder: Color,
    /** The 1 dp line between the From and To rows: the light palette's `outlineSubtle` on the map, the app mode's own on the gradient. */
    val divider: Color,
)

/**
 * The five colours a box on Search Stop draws with, in one place so the two grounds can be put side by side and blended: its fill,
 * the ink of its text, markers and glyphs, the hairline round it, the prompt of a field in it and the divider inside it.
 */
@Immutable
internal data class CardLook(val tint: Color, val onTint: Color, val edge: Color, val placeholder: Color, val divider: Color)

/**
 * The look of a box in the app mode [darkMode] reads: the Timetable's own tint of [themeColor] over [surface] (the same function
 * [mapCardColorsFor] gives the Timetable's chips, [TIMETABLE_ALPHA_DARK] in dark mode and [TIMETABLE_ALPHA_LIGHT] in light), the
 * mode's `onSurface` for ink, its `softLabel` for the hairline and its `outlineSubtle` for the divider. The prompt is the mode's
 * placeholder token in light mode and the ink in dark mode, because the placeholder token is under 4.5 to 1 on the dark tint.
 * Plain numbers in and out, so the contrast of every theme is read with no composition.
 */
internal fun modeCardLook(
    themeColor: Color,
    darkMode: Boolean,
    surface: Color,
    onSurface: Color,
    softLabel: Color,
    labelPlaceholder: Color,
    outlineSubtle: Color,
): CardLook = CardLook(
    tint = mapCardColorsFor(themeColor, darkMode, surface, onSurface).chip,
    onTint = onSurface,
    edge = softLabel,
    placeholder = if (darkMode) onSurface else labelPlaceholder,
    divider = outlineSubtle,
)

/** The map state's look: [modeCardLook] with the light palette's tokens, so it is the same in light and dark app mode. */
internal fun fixedLightCardLook(themeColor: Color): CardLook = modeCardLook(
    themeColor = themeColor,
    darkMode = false,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    softLabel = md_theme_light_softLabel,
    labelPlaceholder = md_theme_light_onSurface_placeholder,
    outlineSubtle = md_theme_light_outline_subtle,
)

/**
 * [map] at 0 and [list] at 1, and a crossfade in between. The two ends are the exact colours, not a blend of a colour with
 * itself, so a box that is at rest is the very same value in light mode, where the two looks are equal.
 */
internal fun blendCardLook(map: CardLook, list: CardLook, listShare: Float): CardLook = when {
    listShare <= 0f -> map
    listShare >= 1f -> list
    else -> CardLook(
        tint = lerp(map.tint, list.tint, listShare),
        onTint = lerp(map.onTint, list.onTint, listShare),
        edge = lerp(map.edge, list.edge, listShare),
        placeholder = lerp(map.placeholder, list.placeholder, listShare),
        divider = lerp(map.divider, list.divider, listShare),
    )
}

/** The look a [CardGroundScope] gives the boxes under it, or null outside one, which is the map state's fixed light look. */
internal val LocalCardLook = compositionLocalOf<CardLook?> { null }

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

/**
 * The one accessor of the card colours, for the current theme and mode. [MapCardColors.tint], [MapCardColors.onTint],
 * [MapCardColors.chipEdge], [MapCardColors.placeholder] and [MapCardColors.divider] are the box's look: the map state's fixed light
 * one, or, under a [CardGroundScope], the one that scope is showing.
 */
@Composable
internal fun mapCardColors(): MapCardColors {
    val theme = themeDecorColor()
    val inMode = mapCardColorsFor(
        themeColor = theme,
        darkMode = isAppInDarkMode(),
        surface = KrailTheme.colors.surface,
        onSurface = KrailTheme.colors.onSurface,
    )
    // The map state's fixed light look unless a scope says the box stands on the gradient: the same family function with the light
    // palette's own tokens, so it is the Timetable's light tint over white and the light text colour even in dark app mode.
    val look = LocalCardLook.current ?: fixedLightCardLook(theme)
    val tint = look.tint
    val onTint = look.onTint
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
            contentColor = md_theme_light_onSurface,
            disabledContainerColor = md_theme_light_surface.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = md_theme_light_onSurface.copy(alpha = DISABLED_ALPHA),
        ),
        chipEdge = look.edge,
        placeholder = look.placeholder,
        divider = look.divider,
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

/**
 * What a box on Search Stop stands on, which is the one thing that decides its colours. [onGradient] is true in the list state, where
 * the cloud gradient covers the map and follows the app mode, and false in the map state, where the map is always light. On the map
 * this is [MapLightScope]'s fixed light look. On the gradient the boxes under it follow the app mode: the same light look in light
 * mode, and in dark mode the Timetable's dark tint with the dark palette's text, hairline and divider ([modeCardLook]).
 *
 * The colours are read here, above the pinned light palette, so the dark look is the real app mode's even while the map state is
 * leaving, and they move between the two looks on the card spec ([mapCardFadeSpec]), a crossfade that is a jump with reduced
 * motion. It starts at where the ground is on first composition, so a screen that opens on the gradient in dark mode is dark from
 * its first frame. Under the scope [mapCardColors] gives the look shown, the text and content colours are its ink, and the app
 * mode the theme reads is the real one on the gradient and light on the map. A box outside every scope stays the map look.
 */
@Composable
internal fun CardGroundScope(onGradient: Boolean, content: @Composable () -> Unit) {
    val realController = LocalThemeController.current
    val theme = themeDecorColor()
    val mapLook = fixedLightCardLook(theme)
    val listLook = modeCardLook(
        themeColor = theme,
        darkMode = isAppInDarkMode(),
        surface = KrailTheme.colors.surface,
        onSurface = KrailTheme.colors.onSurface,
        softLabel = KrailTheme.colors.softLabel,
        labelPlaceholder = KrailTheme.colors.labelPlaceholder,
        outlineSubtle = KrailTheme.colors.outlineSubtle,
    )
    val listShare by animateFloatAsState(
        targetValue = if (onGradient) 1f else 0f,
        animationSpec = mapCardFadeSpec(isReduceMotionEnabled()),
        label = "search-stop-card-ground",
    )
    val look = blendCardLook(map = mapLook, list = listLook, listShare = listShare)
    val lightController = remember { ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {}) }
    CompositionLocalProvider(
        LocalCardLook provides look,
        LocalThemeController provides if (onGradient) realController else lightController,
        LocalTextColor provides look.onTint,
        LocalContentColor provides look.onTint,
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
 * The 1 dp hairline round a chip or card that floats over the map, in [MapCardColors.chipEdge] (the light palette's `softLabel`,
 * or the app mode's own under a [CardGroundScope] on the gradient), drawn over the fill so the edge shows even where its tint is
 * close to the ground. It is drawn in both app modes.
 */
@Composable
internal fun Modifier.mapChipEdge(shape: Shape, color: Color = mapCardColors().chipEdge): Modifier =
    drawWithContent {
        drawContent()
        drawOutline(outline = shape.createOutline(size, layoutDirection, this), color = color, style = Stroke(width = 1.dp.toPx()))
    }
