package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.modifier.sideSafeInsets
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.toAdaptiveDecorativeIconSize
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent

/** The room the capsule keeps above the navigation bar. */
internal val HOME_CAPSULE_BOTTOM_MARGIN = 12.dp

/** The widest the capsule gets. Wider windows keep it this wide and centred. */
internal val HOME_CAPSULE_MAX_WIDTH = 480.dp

/** One text line plus this above and below is the capsule's height, and never less than [TextFieldDefaults.SingleLineHeight]. */
internal val HOME_CAPSULE_VERTICAL_PADDING = 15.dp

/** How opaque the fill is. The shadow does the separating, so the fill is nearly solid and the list behind it shows only faintly. */
internal const val HOME_CAPSULE_FILL_ALPHA = 0.92f

// The glyph's slot starts 8 dp in and is 40 dp wide, so the glyph's centre is 28 dp from the edge and the text starts 52 dp in
// (taj's text field adds its own 4 dp after a leading control).
private val GlyphSlotStart = 8.dp
private val GlyphSlotMinWidth = 40.dp

// The microphone's touch slot is 48 dp square and 4 dp in from the capsule's end. The text stops 16 dp short of it.
private val MicSlot = 48.dp
private val MicEndMargin = 4.dp
private val TextEndGap = 16.dp

// With no microphone the text ends 20 dp from the edge: the same 4 plus 16 the microphone's margin and gap come to.
private val TextEndWithoutMic = MicEndMargin + TextEndGap

// Light: 8 dp down, 24 dp blur, black at 20 percent. Dark: 10 dp down, 30 dp blur, black at 75 percent, the wireframe's values.
private val LightShadowRadius = 24.dp
private val LightShadowOffset = 8.dp
private const val LIGHT_SHADOW_ALPHA = 0.2f
private val DarkShadowRadius = 30.dp
private val DarkShadowOffset = 10.dp
private const val DARK_SHADOW_ALPHA = 0.75f

/** A full pill at every height, so it is still a capsule when the text size makes it tall. */
private val CapsuleShape = RoundedCornerShape(percent = 50)

/**
 * The capsule's fill before it is made translucent: white in light mode, and the raised dark surface taj's bottom sheets use in dark
 * mode. Never the rider's theme colour. Taj's own sheet token is a light grey in light mode, so light mode takes the page surface.
 */
@Composable
internal fun homeCapsuleBase(): Color =
    if (isAppInDarkMode()) KrailTheme.colors.bottomSheetBackground else KrailTheme.colors.surface

/**
 * The capsule's colours in the one place, passed through taj `TextField`'s own `colors`: [homeCapsuleBase] at
 * [HOME_CAPSULE_FILL_ALPHA], and the page's full text colour for the placeholder, the glyph, the microphone and the caret. The
 * placeholder is never dimmed, so it reads on the fill at 4.5 to 1 or more in light and dark (`HomeSearchCapsuleColorsTest`).
 */
@Composable
internal fun homeCapsuleColors(): TextFieldColors {
    val text = KrailTheme.colors.onSurface
    return TextFieldColors(
        containerColor = homeCapsuleBase().copy(alpha = HOME_CAPSULE_FILL_ALPHA),
        contentColor = text,
        placeholderColor = text,
        cursorColor = text,
    )
}

/** The soft shadow that, with the fill, is all that sets the capsule apart: no border, no hairline, no top highlight. */
@Composable
private fun Modifier.capsuleShadow(shape: Shape): Modifier {
    val dark = isAppInDarkMode()
    return dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = if (dark) DarkShadowRadius else LightShadowRadius,
            color = Color.Black,
            offset = DpOffset(x = 0.dp, y = if (dark) DarkShadowOffset else LightShadowOffset),
            alpha = if (dark) DARK_SHADOW_ALPHA else LIGHT_SHADOW_ALPHA,
        ),
    )
}

/**
 * The one search capsule at the bottom of the classic Saved Trips home. It stands alone: no sheet behind it, no pills in it.
 *
 * It is taj's [TextField] as it ships, read only and unable to take focus, drawn as a full pill in [homeCapsuleColors] with a soft
 * drop shadow and nothing else around it: no border, no hairline, no theme colour, no blur, no gradient. Taj's single line height
 * is a minimum here (`minHeight`), so the height is one text line plus [HOME_CAPSULE_VERTICAL_PADDING] above and below and grows
 * with the text size, never under 48 dp. At the start is the search glyph ([HomeSearchGlyph]), always, then "Where are you going?"
 * start aligned and always one line that ends in an ellipsis, however large the text, and at the end taj's plain `MicIcon` only
 * where [isAiAvailable] says Ask KRAIL is there. With the microphone absent the text takes its room and nothing else moves.
 *
 * A tap on the capsule runs [onClick] (Search Stop on the destination field), under the same tags the bar always had. The tap
 * target stops short of the microphone's slot, so the capsule and the microphone are two controls, each with a target of its own,
 * and a screen reader hears the whole question whatever the screen shows. [modifier] carries the capsule's width: it is capped at
 * [HOME_CAPSULE_MAX_WIDTH] and fills what it is given up to that, so the caller centres it.
 */
@Composable
internal fun HomeSearchCapsule(
    onClick: () -> Unit,
    isAiAvailable: Boolean,
    onAiEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = homeCapsuleColors()
    val state = remember { TextFieldState() }
    val micReserve = MicEndMargin + MicSlot
    Box(
        modifier = modifier
            .widthIn(max = HOME_CAPSULE_MAX_WIDTH)
            .fillMaxWidth()
            .testTag(TripPlannerTestTags.SEARCH_ROW),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TripPlannerTestTags.SEARCH_ROW_TO)
                // The shadow comes before the fill and spreads outside the shape, so nothing clips it.
                .capsuleShadow(CapsuleShape),
        ) {
            TextField(
                state = state,
                placeholder = HOME_PILL_PLACEHOLDER,
                readOnly = true,
                modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
                colors = colors,
                shape = CapsuleShape,
                minHeight = TextFieldDefaults.SingleLineHeight,
                leadingIcon = {
                    Box(modifier = Modifier.defaultMinSize(minWidth = GlyphSlotMinWidth), contentAlignment = Alignment.Center) {
                        HomeSearchGlyph(tint = colors.contentColor)
                    }
                },
                contentPadding = PaddingValues(
                    start = GlyphSlotStart,
                    top = HOME_CAPSULE_VERTICAL_PADDING,
                    end = if (isAiAvailable) micReserve + TextEndGap else TextEndWithoutMic,
                    bottom = HOME_CAPSULE_VERTICAL_PADDING,
                ),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = if (isAiAvailable) micReserve else 0.dp)
                    .clip(CapsuleShape)
                    .klickable(onClick = onClick)
                    .semantics(mergeDescendants = true) {
                        contentDescription = HOME_PILL_PLACEHOLDER
                        role = Role.Button
                    },
            )
            if (isAiAvailable) {
                AskKrailMic(
                    slot = MicSlot,
                    onClick = { onAiEvent(AiSearchInputEvent.OpenInput) },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = MicEndMargin),
                    iconSize = KrailTheme.dimensions.iconDefault.toAdaptiveDecorativeIconSize(),
                    tint = colors.contentColor,
                )
            }
        }
    }
}

/**
 * The bar at the bottom of the classic home: the capsule, and nothing else, centred, with the same 16 dp at each side inside the side
 * safe inset ([sideSafeInsets], never `displayCutout`) and [HOME_CAPSULE_BOTTOM_MARGIN] above the navigation bar. It has no fill of its
 * own and no sheet behind it. The slide up and fade in once the saved trips have loaded is the caller's, and so is reading its height:
 * [modifier] can carry an `onSizeChanged`, which sees the whole bar with its margins and the navigation bar, so the end of the list
 * is kept clear by the bar's real height at every text size and never by an assumed one.
 */
@Composable
internal fun HomeSearchBar(
    onClick: () -> Unit,
    isAiAvailable: Boolean,
    onAiEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR)
            .sideSafeInsets()
            .navigationBarsPadding()
            .padding(start = dim.pageHorizontalPadding, end = dim.pageHorizontalPadding, bottom = HOME_CAPSULE_BOTTOM_MARGIN),
        contentAlignment = Alignment.BottomCenter,
    ) {
        HomeSearchCapsule(onClick = onClick, isAiAvailable = isAiAvailable, onAiEvent = onAiEvent)
    }
}

// region Previews

// The shadow reaches 32 dp past the capsule (24 dp blur and 8 dp down), so the preview leaves more than that around it, or the
// picture would cut the shadow off in a hard line.
private val PREVIEW_SHADOW_ROOM = 40.dp

/** The capsule as a rider sees it, with the microphone, on the page with room around it for the shadow. */
@ScreenshotTest
@PreviewComponent
@Composable
private fun HomeSearchCapsuleWithMicPreview() {
    PreviewTheme {
        HomeSearchCapsule(
            onClick = {},
            isAiAvailable = true,
            onAiEvent = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = PREVIEW_SHADOW_ROOM),
        )
    }
}

/** With no microphone nothing else moves: the glyph, the text start and the height are the same. */
@ScreenshotTest
@PreviewComponent
@Composable
private fun HomeSearchCapsuleNoMicPreview() {
    PreviewTheme {
        HomeSearchCapsule(
            onClick = {},
            isAiAvailable = false,
            onAiEvent = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = PREVIEW_SHADOW_ROOM),
        )
    }
}

// endregion
