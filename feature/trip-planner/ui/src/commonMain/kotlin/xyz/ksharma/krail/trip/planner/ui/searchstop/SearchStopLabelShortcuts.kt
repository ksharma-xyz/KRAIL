package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * A single label pill in the idle pill row — tap-only, no long-press, set labels only
 * (the row filters to `isSet` before rendering). Tapping selects the pill's stop as
 * From/To. Change / remove / delete / reorder all live in `ManageStopLabelsScreen`
 * (opened from the row's trailing "Manage" button) — see `SEARCH_STOP_UX.md`.
 */
@Composable
internal fun LabelShortcutPill(
    label: StopLabel,
    onSetLabelClick: (StopItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val shape = RoundedCornerShape(dim.radiusFull)
    // clip BEFORE klickable so the ripple is contained inside the rounded shape.
    val tapModifier = modifier
        .clip(shape)
        .klickable { label.toStopItem()?.let(onSetLabelClick) }

    SetLabelPill(label = label, modifier = tapModifier)
}
