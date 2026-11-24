package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.MicIcon
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.components.TextFieldColors
import xyz.ksharma.krail.taj.components.TextFieldDefaults
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.toFromBarInsets
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent

/** What the home search bar says while it is waiting. */
internal const val HOME_PILL_PLACEHOLDER = "Where are you going?"

private const val ASK_KRAIL_MICROPHONE_DESCRIPTION = "Ask KRAIL"

/**
 * The one search bar at the bottom of the map home: a tap opens Search Stop for the destination,
 * exactly as the To bar of the old search row did, and its text is one line that ends in an ellipsis. The classic
 * Saved Trips home has its own capsule ([HomeSearchCapsule]) and does not use this bar.
 *
 * The bar is taj's [TextField] as it ships, read only, in taj's own default field colours ([homeSearchPillColors]):
 * it holds no text, it shows the placeholder, it cannot take focus, and the tap over it opens Search Stop. It is
 * 48 dp tall, taj's own height, and a full pill. On the map home it stands over the map, so the page surface fill
 * of the default colours is the pill's edge against the map.
 *
 * The microphone at its end is taj's [MicIcon] as it ships, with no gradient, no circle and no background, in
 * the bar's own content colour, in a 48 dp touch slot flush with the bar's end. It opens Ask KRAIL, and shows
 * only where the feature flag and the device allow it ([isAiAvailable]), so it is never a dead icon. The tap that
 * opens Search Stop covers the field and stops short of that slot, so the bar and the microphone are two
 * controls with a target of their own each.
 */
@Composable
internal fun HomeSearchPill(
    onClick: () -> Unit,
    isAiAvailable: Boolean,
    onAiEvent: (AiSearchInputEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val micSlot = toFromBarInsets().column
    val state = remember { TextFieldState() }
    Box(modifier = modifier.testTag(TripPlannerTestTags.SEARCH_ROW).fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().testTag(TripPlannerTestTags.SEARCH_ROW_TO)) {
            TextField(
                state = state,
                placeholder = HOME_PILL_PLACEHOLDER,
                readOnly = true,
                modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
                colors = homeSearchPillColors(),
                trailingIcon = if (isAiAvailable) {
                    { AskKrailMic(slot = micSlot, onClick = { onAiEvent(AiSearchInputEvent.OpenInput) }) }
                } else {
                    null
                },
                contentPadding = PaddingValues(
                    start = dim.spacingXL,
                    end = if (isAiAvailable) 0.dp else dim.spacingXL,
                ),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = if (isAiAvailable) micSlot else 0.dp)
                    .clip(RoundedCornerShape(50))
                    .klickable(onClick = onClick)
                    .semantics(mergeDescendants = true) {
                        contentDescription = HOME_PILL_PLACEHOLDER
                        role = Role.Button
                    },
            )
        }
    }
}

/**
 * The colours of the bar, in the one place: taj's own default field colours as they ship, the page surface with the
 * page's text colour in both modes, never the inverted set (a cream bar in dark mode and a black one in light mode).
 * The placeholder and the microphone read on that fill at 4.5 to 1 or more in every theme and both modes
 * (`HomeSearchPillColorsTest`).
 */
@Composable
internal fun homeSearchPillColors(): TextFieldColors = TextFieldDefaults.colors()

/**
 * The microphone: taj's plain [MicIcon] in [tint], centred in a square touch [slot]. Shared by the map home's bar and the classic
 * home's capsule, which sizes the glyph by [iconSize] and draws it outside the text field, so it passes its own [tint].
 */
@Composable
internal fun AskKrailMic(
    slot: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = KrailTheme.dimensions.iconDefault,
    tint: Color = LocalTextColor.current,
) {
    Box(
        modifier = modifier
            .size(slot)
            .testTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL)
            .klickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = ASK_KRAIL_MICROPHONE_DESCRIPTION
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = MicIcon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(iconSize),
        )
    }
}

// region Previews

@PreviewComponent
@Composable
private fun HomeSearchPillWithMicPreview() {
    PreviewTheme {
        HomeSearchPill(onClick = {}, isAiAvailable = true, onAiEvent = {})
    }
}

@PreviewComponent
@Composable
private fun HomeSearchPillNoMicPreview() {
    PreviewTheme {
        HomeSearchPill(onClick = {}, isAiAvailable = false, onAiEvent = {})
    }
}

// endregion
