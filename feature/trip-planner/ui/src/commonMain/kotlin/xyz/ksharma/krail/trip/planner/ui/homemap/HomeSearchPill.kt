package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextFieldButton
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.AiSheetEntryButton
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent

/** What the home pill says while it is waiting, the words the design names it by. */
internal const val HOME_PILL_PLACEHOLDER = "Where are you going?"

// The microphone is a 48 dp round button at the pill's end and the text stops short of it by its width and
// the 8 dp the other bars keep between their text and an end control.
private val MIC_RESERVE = 56.dp

/**
 * The home search pill, `docs/MAP_HOME_UX.md` rules 6, 7 and 34 and decision G6: one pill at the bottom of
 * home, in the thumb's reach, that opens Search Stop for the destination exactly as the To bar of the search
 * row always did. Its text is one line that ends in an ellipsis, and its accessible label is the whole of it.
 *
 * The microphone at its end is Ask KRAIL's own button, unchanged: it opens Ask KRAIL, shows only where the
 * feature flag and the device allow it ([isAiAvailable], the same gate the search row used), and is never a
 * dead icon. It is a sibling drawn over the pill and not a button inside a button, so the pill and the
 * microphone are two controls with a target of their own each. Search Stop has no microphone (rule 7).
 *
 * The pill is taj's [TextFieldButton] and takes the same surface colour the To and From bars take at rest.
 * That component has no colours parameter, so the dark field of rule 10 is a gap, listed in the ERD.
 */
@Composable
internal fun HomeSearchPill(
    onClick: () -> Unit,
    isAiAvailable: Boolean,
    onAiEvent: (AiSearchInputEvent) -> Unit,
    isAiHandoffSettling: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.testTag(TripPlannerTestTags.SEARCH_ROW).fillMaxWidth()) {
        TextFieldButton(
            onClick = onClick,
            modifier = Modifier
                .testTag(TripPlannerTestTags.SEARCH_ROW_TO)
                .semantics(mergeDescendants = true) {
                    contentDescription = HOME_PILL_PLACEHOLDER
                    role = Role.Button
                },
        ) {
            Text(
                text = HOME_PILL_PLACEHOLDER,
                color = KrailTheme.colors.secondaryLabel,
                maxLines = 1,
                modifier = Modifier.padding(end = if (isAiAvailable) MIC_RESERVE else 0.dp),
            )
        }
        if (isAiAvailable) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                AiSheetEntryButton(onAiEvent = onAiEvent, spinning = isAiHandoffSettling)
            }
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
