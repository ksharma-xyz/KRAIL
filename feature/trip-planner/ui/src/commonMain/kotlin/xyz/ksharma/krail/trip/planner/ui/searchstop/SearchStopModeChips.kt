package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ChipRowMode
import xyz.ksharma.krail.trip.planner.ui.savedtrips.chipRowModeFor

/**
 * The mode filter of the map state: one whole chip per mode the map can show, directly under the pill,
 * floating on the map. It replaces the Options sheet, so a rider filters the map without leaving it.
 *
 * Whole chips wrap onto the next line when width is short. Over 130 percent text, [chipRowModeFor],
 * the same rule the home chip row follows, it is one line that scrolls sideways with the first chip
 * fully visible, under a fade at the right edge. A chip is never shrunk, clipped or broken across lines
 * to fit.
 *
 * A chip is on when its mode is in [selectedModes] (product classes) and a tap turns it off or on
 * through [onToggle]. Nothing is saved: the filter is the map's, for this visit.
 */
@Composable
internal fun SearchStopModeChips(
    selectedModes: ImmutableSet<Int>,
    onToggle: (TransportMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = remember { NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS) }
    val spacing = KrailTheme.dimensions.spacingM
    val rowModifier = modifier
        .fillMaxWidth()
        .testTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
    val chips: @Composable () -> Unit = {
        modes.forEach { mode ->
            ModeChip(
                mode = mode,
                selected = NswTransportConfig.productClassFor(mode) in selectedModes,
                onToggle = onToggle,
            )
        }
    }

    when (chipRowModeFor(LocalDensity.current.fontScale)) {
        ChipRowMode.Wrap -> FlowRow(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) { chips() }

        ChipRowMode.Scroll -> ScrollingModeChips(modifier = rowModifier, content = chips)
    }
}

/**
 * One line of chips that scrolls sideways, with a fade at the right edge while there is more to see
 * (decision A7). The fade runs into the surface colour, the same one the home chip row fades into.
 */
@Composable
private fun ScrollingModeChips(modifier: Modifier, content: @Composable () -> Unit) {
    val dim = KrailTheme.dimensions
    val scroll = rememberScrollState()
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) { content() }
        if (scroll.canScrollForward) {
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(dim.spacingXXXXL)
                        .testTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS_FADE)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, KrailTheme.colors.surface)),
                        ),
                )
            }
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
