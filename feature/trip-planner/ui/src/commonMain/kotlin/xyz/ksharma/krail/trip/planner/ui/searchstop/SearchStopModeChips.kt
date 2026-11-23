package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import kotlinx.collections.immutable.ImmutableSet
import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.info.tiles.ui.infoTileGlow
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip

/**
 * The mode filter block of the map state: a rounded card on taj's own surface, white in light mode and dark in dark
 * mode, with the info tile's glow around it, holding one whole chip per mode the map can show. It opens from the Options
 * button at the bottom of the map and never sits in the group card at the top.
 *
 * Whole chips wrap onto the next line when width is short, at every text size, so it is never a row that
 * scrolls sideways and never has a fade at an edge. A chip is never shrunk, clipped or broken across lines.
 *
 * A chip is on when its mode is in [selectedModes] (product classes) and a tap turns it off or on through
 * [onToggle]. Nothing is saved: the filter is the map's, for this visit, and it is kept while the block is shut.
 */
@Composable
internal fun SearchStopModeBlock(
    selectedModes: ImmutableSet<Int>,
    onToggle: (TransportMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = remember { NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS) }
    val gaps = rememberSearchStopGaps()
    val shape = RoundedCornerShape(gaps.cardCorner)
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
            .infoTileGlow(shape)
            .clip(shape)
            .background(KrailTheme.colors.surface)
            .padding(gaps.cardPadding),
        horizontalArrangement = Arrangement.spacedBy(gaps.betweenRows),
        verticalArrangement = Arrangement.spacedBy(gaps.betweenRows),
    ) {
        modes.forEach { mode ->
            ModeChip(
                mode = mode,
                selected = NswTransportConfig.productClassFor(mode) in selectedModes,
                onToggle = onToggle,
            )
        }
    }
}

/** One mode as the app's own chip, with the on or off state announced the way a toggle is. */
@Composable
private fun ModeChip(
    mode: TransportMode,
    selected: Boolean,
    onToggle: (TransportMode) -> Unit,
) {
    TransportModeChip(
        transportMode = mode,
        selected = selected,
        onClick = { onToggle(mode) },
        modifier = Modifier
            .testTag(TripPlannerTestTags.searchStopModeChip(NswTransportConfig.productClassFor(mode)))
            .semantics {
                role = Role.Checkbox
                toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
            },
    )
}
