package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDarkTheme
import xyz.ksharma.krail.trip.planner.ui.components.ToFromBlockStyle
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/** Everything the form needs, in one holder so the two layouts take it as one argument. */
internal data class SearchStopFormModel(
    val shape: FormShape,
    val editing: ToFromField?,
    val toPlaceholder: String,
    val fromPlaceholder: String,
    val toStop: StopItem?,
    val fromStop: StopItem?,
    val fieldState: TextFieldState,
    val focusRequester: FocusRequester,
    val onFlowEvent: (SearchStopFlowEvent) -> Unit,
    val onGoClick: () -> Unit,
    val onTextChange: (String) -> Unit,
)

/**
 * The group card at the top of Search Stop (rules 33, 35 and 36 of `docs/MAP_HOME_UX.md`): one rounded card, a
 * fixed dark surface with fixed light content in light and dark app modes, because the map under it is always
 * light. Inside, from the top and in the order [groupCardRows] gives: the To and From bars as two rows with a thin
 * divider between them, then the location status chip, only while it is shown. Every gap inside is the one
 * [SearchStopGaps.betweenRows], and the card's padding and corner are the card's own tokens.
 *
 * The bars are rows on this one surface and have no second fill each, and the recents or results card is a
 * separate card under this one, never inside it.
 */
@Composable
internal fun SearchStopGroupCard(
    form: SearchStopFormModel,
    location: LocationPillState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gaps = rememberSearchStopGaps()
    FloatingDarkTheme {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .testTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
                .clip(RoundedCornerShape(gaps.cardCorner))
                .background(FloatingDark.container)
                .padding(gaps.cardPadding),
            verticalArrangement = Arrangement.spacedBy(gaps.betweenRows),
        ) {
            SearchStopForm(
                shape = form.shape,
                editing = form.editing,
                toPlaceholder = form.toPlaceholder,
                fromPlaceholder = form.fromPlaceholder,
                toStop = form.toStop,
                fromStop = form.fromStop,
                fieldState = form.fieldState,
                focusRequester = form.focusRequester,
                onFlowEvent = form.onFlowEvent,
                onBack = onBack,
                onGoClick = form.onGoClick,
                onTextChange = form.onTextChange,
                style = ToFromBlockStyle.FloatingRows(rowGap = gaps.betweenRows),
            )
            LocationStatusChip(pill = location.pill, onClick = location.onClick)
        }
    }
}
