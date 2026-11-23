package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.taj.themeDecorColor

/**
 * Where every card that floats over the map takes its colours from. The map is always light, in both app modes, but the cards that
 * hold the rider's own content on it follow the app mode, so a card is one colour wherever it stands.
 *
 * That is the Search Stop box, the bare single pill, the location notice, the Options card and the home map's notices. Light mode is
 * the Timetable's tint, [TIMETABLE_ALPHA_LIGHT] of the theme colour over white, with the light palette's `onSurface` for text and a
 * 1 dp hairline in its `softLabel` (3.81 to 1 against the map ground). Dark mode is the Timetable's own dark tint,
 * [TIMETABLE_ALPHA_DARK] of the theme colour over the dark surface, with the dark palette's `onSurface` (cream) for text, markers and
 * glyphs, its `outlineSubtle` for the divider and its `softLabel` for the hairline. The hairline is the one thing that depends on
 * what the card stands on: a dark card on the light map is already well clear of it and wears none there, and on the cloud
 * gradient of the list state it wears the hairline, which it needs ([CardGroundScope], [cardEdgeShown]). Every colour is a taj
 * token, none is typed here. `MapCardColorsTest` pins them.
 *
 * Outside a [CardGroundScope] a card stands on the map, so it takes the look in the app mode with no hairline in dark mode. The
 * controls on the map are not cards and keep one look in both modes: the Options pill is white ([pill], with the light hairline
 * [pillEdge]), and the info icon, the location control and the go circle are not here.
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
internal data class MapCardColors(
    /** The stop card's body: the soft tint, in the mode the app is in. */
    val body: Color,
    /** A band on the stop card (its header): a visibly different step of the same family. */
    val header: Color,
    /** Text and icons on [body] and [header]: the page's own text colour, in the mode the app is in. */
    val onCard: Color,
    /** The fill of the Search Stop box and of every card that wears its look: the Timetable's tint of the app mode, opaque. */
    val tint: Color,
    /** Text, markers and icons on [tint]: the app mode's `onSurface`. */
    val onTint: Color,
    /** A small chip on the map: [tint] with [onTint] on it. */
    val chip: ButtonColors,
    /**
     * The Options pill at rest: white (the light palette's `surface`) with [onTint] on it, whatever the app mode. White is neutral
     * beside the pale card, the blue location control and the theme coloured go circle, and 20.87 to 1 under its text. It is
     * the pill's resting look, and its solid theme colour ([mapActiveChipColors]) stays the look of an active filter.
     */
    val pill: ButtonColors,
    /** The hairline of the white Options pill: the light palette's `softLabel`, whatever the app mode. */
    val pillEdge: Color,
    /**
     * The hairline round every card and chip: the app mode's `softLabel`. In dark mode it has no alpha where the card stands on the
     * map, which is outside a [CardGroundScope] and under one with `onGradient` false, and it is drawn on the gradient.
     */
    val chipEdge: Color,
    /** The prompt of a field on the box: the mode's placeholder in light mode and the box's own text colour in dark mode. */
    val placeholder: Color,
    /** The 1 dp line between the From and To rows: the app mode's `outlineSubtle`. */
    val divider: Color,
)

/**
 * The five colours a box on Search Stop draws with, in one place so the two grounds can be put side by side and blended: its fill,
 * the ink of its text, markers and glyphs, the hairline round it, the prompt of a field in it and the divider inside it.
 */
@Immutable
internal data class CardLook(
    val tint: Color,
    val onTint: Color,
    val edge: Color,
    val placeholder: Color,
    val divider: Color,
)

/**
 * The look of a box in the app mode [darkMode] reads: the Timetable's own tint of [themeColor] over [surface] (the same function
 * [mapCardColorsFor] gives the Timetable's chips, [TIMETABLE_ALPHA_DARK] in dark mode and [TIMETABLE_ALPHA_LIGHT] in light), the
 * mode's `onSurface` for ink, its `softLabel` for the hairline and its `outlineSubtle` for the divider. The prompt is the mode's
 * placeholder token in light mode and the ink in dark mode, because the placeholder token is under 4.5 to 1 on the dark tint.
 * Plain numbers in and out, so the contrast of every theme is read with no composition.
 */
internal fun modeCardLook(themeColor: Color, darkMode: Boolean, tokens: ModeTokens): CardLook = CardLook(
    tint = mapCardColorsFor(themeColor, darkMode, tokens.surface, tokens.onSurface).chip,
    onTint = tokens.onSurface,
    edge = tokens.softLabel,
    placeholder = if (darkMode) tokens.onSurface else tokens.labelPlaceholder,
    divider = tokens.outlineSubtle,
)

/** The five tokens of one app mode that a box's look is made from, so [modeCardLook] takes one thing and not seven. */
@Immutable
internal data class ModeTokens(
    val surface: Color,
    val onSurface: Color,
    val softLabel: Color,
    val labelPlaceholder: Color,
    val outlineSubtle: Color,
)

/** The tokens of the app mode that is showing now. */
@Composable
internal fun currentModeTokens(): ModeTokens = ModeTokens(
    surface = KrailTheme.colors.surface,
    onSurface = KrailTheme.colors.onSurface,
    softLabel = KrailTheme.colors.softLabel,
    labelPlaceholder = KrailTheme.colors.labelPlaceholder,
    outlineSubtle = KrailTheme.colors.outlineSubtle,
)

/**
 * Whether a box draws its hairline. In light mode it always does, on the map and on the gradient. In dark mode it does on the
 * cloud gradient, where it separates the box from the page, and not on the map: the dark box is already well clear of the light map
 * (5.9 to 7.4 to 1 on the four offered themes) and the dark `softLabel` would be only 1.8 to 1 against it.
 */
internal fun cardEdgeShown(darkMode: Boolean, onGradient: Boolean): Boolean = !darkMode || onGradient

/**
 * This look with its hairline drawn at [share] of its own alpha, 1 for fully drawn and 0 for not at all. At 1 it is this very look,
 * so light mode, where the hairline is always drawn, is the same value at every frame. Only the hairline moves, never the fill,
 * the text or the glyphs.
 */
internal fun CardLook.withEdgeShare(share: Float): CardLook =
    if (share >= 1f) this else copy(edge = edge.copy(alpha = edge.alpha * share.coerceAtLeast(0f)))

/** The look a [CardGroundScope] gives the boxes under it, or null outside one, where a card stands on the map. */
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
    val timetable = themeColor.copy(
        alpha = if (darkMode) TIMETABLE_ALPHA_DARK else TIMETABLE_ALPHA_LIGHT,
    ).compositeOver(surface)
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
 * [MapCardColors.chipEdge], [MapCardColors.placeholder] and [MapCardColors.divider] are the box's look: the one a
 * [CardGroundScope] is showing, or, outside a scope, where a card stands on the map, the same look with no hairline in dark mode.
 * It follows the app mode either way.
 */
@Composable
internal fun mapCardColors(): MapCardColors {
    val theme = themeDecorColor()
    val darkMode = isAppInDarkMode()
    val inMode = mapCardColorsFor(
        themeColor = theme,
        darkMode = darkMode,
        surface = KrailTheme.colors.surface,
        onSurface = KrailTheme.colors.onSurface,
    )
    val look = LocalCardLook.current
        ?: modeCardLook(themeColor = theme, darkMode = darkMode, tokens = currentModeTokens())
            .withEdgeShare(if (cardEdgeShown(darkMode, onGradient = false)) 1f else 0f)
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
        pillEdge = md_theme_light_softLabel,
        chipEdge = look.edge,
        placeholder = look.placeholder,
        divider = look.divider,
    )
}

/**
 * What a box on Search Stop stands on. The box, the bare pill and the notice follow the app mode on both grounds: the Timetable's
 * tint of the theme colour over the app mode's surface, the mode's `onSurface` for text, markers, glyphs and the prompt, and its
 * `outlineSubtle` for the divider ([modeCardLook]), so the map state and the list state draw the very same fill, text and glyphs.
 * The theme controller is the real one and the text and content colours are the box's ink, so a taj component inside reads the
 * app mode as well.
 *
 * [onGradient] is true in the list state, where the cloud gradient covers the map, and false in the map state, where the map is
 * always light. It decides one thing, the hairline ([cardEdgeShown]): always drawn in light mode, and in dark mode drawn on the
 * gradient and not on the map. The hairline fades in and out on the card spec ([mapCardFadeSpec]) when the ground changes, and
 * jumps with reduced motion; nothing else moves. A scope that opens on the gradient has its hairline from its first frame. A card
 * outside every scope stands on the map, so it has the same look as one in a scope with [onGradient] false.
 */
@Composable
internal fun CardGroundScope(onGradient: Boolean, content: @Composable () -> Unit) {
    val darkMode = isAppInDarkMode()
    val look = modeCardLook(themeColor = themeDecorColor(), darkMode = darkMode, tokens = currentModeTokens())
    val edgeShare by animateFloatAsState(
        targetValue = if (cardEdgeShown(darkMode, onGradient)) 1f else 0f,
        animationSpec = mapCardFadeSpec(isReduceMotionEnabled()),
        label = "search-stop-card-edge",
    )
    CompositionLocalProvider(
        LocalCardLook provides look.withEdgeShare(edgeShare),
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
 * The 1 dp hairline round a chip or card that floats over the map, in [MapCardColors.chipEdge] (the app mode's `softLabel` under a
 * [CardGroundScope], the light palette's outside one), drawn over the fill so the edge shows even where its tint is close to the
 * ground. Nothing is drawn while the colour has no alpha, which is the dark box on the map.
 */
@Composable
internal fun Modifier.mapChipEdge(shape: Shape, color: Color = mapCardColors().chipEdge): Modifier =
    drawWithContent {
        drawContent()
        if (color.alpha > 0f) {
            drawOutline(
                outline = shape.createOutline(size, layoutDirection, this),
                color = color,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }

/**
 * The surface of a card on Search Stop: [shape] with the hairline ([mapChipEdge]) over it, its content clipped to it and its fill,
 * all from the look [mapCardColors] gives. The From and To box and the location notice both take their surface from here, so the
 * fill, the hairline and the corner of the two are one source and cannot drift apart.
 */
@Composable
internal fun Modifier.searchStopCardSurface(shape: Shape): Modifier {
    val card = mapCardColors()
    return mapChipEdge(shape, card.chipEdge).clip(shape).background(card.tint)
}
