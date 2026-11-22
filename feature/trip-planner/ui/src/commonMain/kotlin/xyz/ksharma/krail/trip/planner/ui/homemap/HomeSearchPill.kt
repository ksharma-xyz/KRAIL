package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.TextField
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.AiSheetEntryButton
import xyz.ksharma.krail.trip.planner.ui.components.BarSlot
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.toFromBarInsets
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent

/** What the home pill says while it is waiting, the words the design names it by. */
internal const val HOME_PILL_PLACEHOLDER = "Where are you going?"

/**
 * The home search pill, `docs/MAP_HOME_UX.md` rules 6, 7 and 34 and decision G6: one pill at the bottom of
 * home, in the thumb's reach, that opens Search Stop for the destination exactly as the To bar of the search
 * row always did. Its text is one line that ends in an ellipsis, and its accessible label is the whole of it.
 *
 * The pill is taj's [TextField] as it ships, read only and in the fixed dark ([FloatingDark.fieldColors]), the
 * same field the To and From bars on Search Stop are: it holds no text, it shows the placeholder, it cannot take
 * focus, and a tap over it is the tap that opens Search Stop. The field is 48 dp tall, taj's own height.
 *
 * The microphone at its end is Ask KRAIL's own button, unchanged: it opens Ask KRAIL, shows only where the
 * feature flag and the device allow it ([isAiAvailable], the same gate the search row used), and is never a
 * dead icon. It sits in the field's own trailing slot, a column of the bars' own width, and the tap that opens
 * Search Stop covers the field and stops short of that column, so the pill and the microphone are two controls
 * with a target of their own each. Search Stop has no microphone (rule 7).
 */
@Composable
internal fun HomeSearchPill(
    onClick: () -> Unit,
    isAiAvailable: Boolean,
    onAiEvent: (AiSearchInputEvent) -> Unit,
    isAiHandoffSettling: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val insets = toFromBarInsets()
    val state = remember { TextFieldState() }
    Box(modifier = modifier.testTag(TripPlannerTestTags.SEARCH_ROW).fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().testTag(TripPlannerTestTags.SEARCH_ROW_TO)) {
            TextField(
                state = state,
                placeholder = HOME_PILL_PLACEHOLDER,
                readOnly = true,
                modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
                colors = FloatingDark.fieldColors(),
                trailingIcon = if (isAiAvailable) {
                    {
                        BarSlot(insets.column) {
                            AiSheetEntryButton(onAiEvent = onAiEvent, spinning = isAiHandoffSettling)
                        }
                    }
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
                    .padding(end = if (isAiAvailable) insets.column else 0.dp)
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

// region Previews

@PreviewComponent
@Composable
private fun HomeSearchPillWithMicPreview() {
    PreviewTheme {
        HomeSearchPill(onClick = {}, isAiAvailable = true, onAiEvent = {}, isAiHandoffSettling = false)
    }
}

@PreviewComponent
@Composable
private fun HomeSearchPillNoMicPreview() {
    PreviewTheme {
        HomeSearchPill(onClick = {}, isAiAvailable = false, onAiEvent = {}, isAiHandoffSettling = false)
    }
}

// endregion
