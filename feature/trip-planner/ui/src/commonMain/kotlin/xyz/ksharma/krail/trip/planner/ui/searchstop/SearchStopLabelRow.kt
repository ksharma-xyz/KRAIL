package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The label row, first in the card: Home, Work and any custom label that has a stop, as whole chips
 * in one row that scrolls sideways (the exception rule 4 gives it). The first chip starts fully
 * visible. Keyed the same in every list state so the row stays mounted when the rider types.
 */
internal fun LazyListScope.labelRowSection(
    show: Boolean,
    stopLabels: ImmutableList<StopLabel>,
    actions: StopRowActions,
    onManageClick: () -> Unit,
) {
    if (!show) return
    item(key = "label-shortcuts") {
        LabelShortcutsRow(
            labels = stopLabels,
            onSetLabelClick = { stopItem ->
                actions.keyboard?.hide()
                actions.focusRequester.freeFocus()
                actions.onStopSelect(stopItem)
            },
            onManageClick = onManageClick,
        )
    }
}

@Composable
private fun LabelShortcutsRow(
    labels: ImmutableList<StopLabel>,
    onSetLabelClick: (StopItem) -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    LazyRow(
        modifier = modifier.testTag(TripPlannerTestTags.SEARCH_STOP_LABEL_ROW),
        contentPadding = PaddingValues(
            start = dim.pageHorizontalPadding,
            end = dim.pageHorizontalPadding,
            top = dim.spacingNone,
            bottom = dim.spacingS,
        ),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        items(items = labels.filter { it.isSet }, key = { it.label }) { label ->
            LabelShortcutPill(label = label, onSetLabelClick = onSetLabelClick)
        }
        item(key = "trailing-manage") {
            Button(
                onClick = onManageClick,
                colors = ButtonDefaults.monochromeButtonColors(),
                dimensions = ButtonDefaults.chipButtonSize(),
                modifier = Modifier.padding(start = dim.spacingM),
            ) {
                Text(text = "Manage")
            }
        }
    }
}
