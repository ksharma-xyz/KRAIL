package xyz.ksharma.krail.trip.planner.ui.components.ai

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * The rider's own places, on the surface the moment it opens, so the fastest route through
 * this sheet involves no speech at all.
 *
 * Only labels with a stop assigned appear. An unassigned "Work" cannot answer "take me to
 * work", and offering it as a chip that silently does nothing is worse than not offering it.
 *
 * Reuses [SetLabelPill] rather than drawing a chip of its own, so a place looks the same here
 * as it does everywhere else the rider has already seen it.
 */
@Composable
internal fun AiLabelChipRow(
    stopLabels: ImmutableList<StopLabel>,
    onLabelClick: (StopLabel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val assigned = stopLabels.filter { it.isSet }
    if (assigned.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dim.spacingXS),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingS, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        assigned.forEach { label ->
            SetLabelPill(
                label = label,
                modifier = Modifier.klickable(onClick = { onLabelClick(label) }),
            )
        }
    }
}
