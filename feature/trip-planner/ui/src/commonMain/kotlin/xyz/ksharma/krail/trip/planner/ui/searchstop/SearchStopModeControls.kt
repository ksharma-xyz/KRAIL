package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_filter
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark

/**
 * The mode filter's controls at the bottom of the map state: the [SearchStopModeButton] on the one bottom row and,
 * while it is open, the [SearchStopModeBlock] right above that row. The rider asks for them, so the map is bare
 * until they do.
 *
 * Where they stand (`docs/MAP_HOME_UX.md` rule 9): the bottom row is one [MAP_CONTROL_ROW_HEIGHT] row,
 * [SearchStopGaps.edge] above the system bottom inset, which is the same row the map's own controls use: the info
 * icon at its start and the location control at its end. The button stands in that row right after the info
 * icon ([MAP_ATTRIBUTION_SLOT] in from the edge) and is centred in the row, so its centre line is the one the
 * location control and the info icon share, whatever the text size. The block is one [SearchStopGaps.betweenCards]
 * above the row and spans the same edges as the group card. [clearance] lifts the whole thing above an open stop card
 * the way it lifts the location control.
 */
@Composable
internal fun BoxScope.SearchStopModeControls(
    open: Boolean,
    selectedModes: ImmutableSet<Int>,
    onButtonClick: () -> Unit,
    onToggleMode: (TransportMode) -> Unit,
    modifier: Modifier = Modifier,
    clearance: Dp = 0.dp,
) {
    val gaps = rememberSearchStopGaps()
    val allClasses = remember {
        NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS).map(NswTransportConfig::productClassFor)
    }
    Column(
        modifier = modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .navigationBarsPadding()
            .padding(start = gaps.edge, end = gaps.edge, bottom = modeControlsBottomPadding(gaps, clearance)),
        verticalArrangement = Arrangement.spacedBy(gaps.betweenCards),
    ) {
        if (open) {
            SearchStopModeBlock(selectedModes = selectedModes, onToggle = onToggleMode)
        }
        Row(
            modifier = Modifier
                .heightIn(min = MAP_CONTROL_ROW_HEIGHT)
                .padding(start = MAP_ATTRIBUTION_SLOT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchStopModeButton(
                expanded = open,
                filtered = modeFilterActive(allClasses, selectedModes),
                onClick = onButtonClick,
            )
        }
    }
}

/**
 * The button that opens and shuts the mode block: taj's [Button] at its extra small size reading [MODE_BUTTON_TEXT]
 * after the app's filter icon, in the fixed dark with the fixed light, because it floats over an always light map
 * (the same pair the location notice uses). While any mode is switched off it takes the active look, the two colours
 * swapped, with no count beside it. Its touch target is 48 dp. A screen reader hears its text and whether the block is
 * expanded or collapsed.
 */
@Composable
internal fun SearchStopModeButton(
    expanded: Boolean,
    filtered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = if (filtered) FloatingDark.activeButtonColors() else FloatingDark.buttonColors(),
        dimensions = ButtonDefaults.extraSmallButtonSize(),
        // The tag comes before the size, so the node a screen reader and a test find is the whole 48 dp target.
        modifier = modifier
            .testTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
            .minimumInteractiveComponentSize()
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) MODE_BUTTON_EXPANDED else MODE_BUTTON_COLLAPSED
            },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_filter),
                contentDescription = null,
                colorFilter = ColorFilter.tint(LocalTextColor.current),
                modifier = Modifier.size(KrailTheme.dimensions.iconXS),
            )
            Text(text = MODE_BUTTON_TEXT)
        }
    }
}

internal const val MODE_BUTTON_TEXT = "Options"
internal const val MODE_BUTTON_EXPANDED = "Expanded"
internal const val MODE_BUTTON_COLLAPSED = "Collapsed"
