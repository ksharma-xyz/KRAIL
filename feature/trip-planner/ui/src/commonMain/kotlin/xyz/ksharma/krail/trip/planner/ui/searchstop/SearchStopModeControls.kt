package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
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
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDarkTheme

/**
 * The mode filter's controls at the bottom of the map state: a small round button at the bottom left and, while
 * it is open, the [SearchStopModeBlock] right above it. The rider asks for them, so the map is bare until they do.
 *
 * Where they stand (`docs/MAP_HOME_UX.md` rule 9): [SearchStopGaps.edge] in from the left and right, and
 * [modeControlsBottomPadding] above the system bottom inset, which is one 48 dp control row and one gap above the
 * map's own bottom row. That row holds the attribution icon at the left and the location button at the right, so
 * nothing here is ever over either. [clearance] lifts the controls above an open stop card the way it lifts the
 * location button. [onTopChanged] reports where the controls start, measured from the top of the map, so the
 * list card at the top can yield height to them and scroll inside.
 */
@Composable
internal fun BoxScope.SearchStopModeControls(
    open: Boolean,
    selectedModes: ImmutableSet<Int>,
    onButtonClick: () -> Unit,
    onToggleMode: (TransportMode) -> Unit,
    onTopChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    clearance: Dp = 0.dp,
) {
    val gaps = rememberSearchStopGaps()
    val density = LocalDensity.current
    val allClasses = remember {
        NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS).map(NswTransportConfig::productClassFor)
    }
    Column(
        modifier = modifier
            .align(Alignment.BottomStart)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .navigationBarsPadding()
            .padding(start = gaps.edge, end = gaps.edge, bottom = modeControlsBottomPadding(gaps, clearance))
            .onGloballyPositioned { onTopChanged(with(density) { it.positionInParent().y.toDp() }) },
        verticalArrangement = Arrangement.spacedBy(gaps.betweenCards),
    ) {
        if (open) {
            SearchStopModeBlock(selectedModes = selectedModes, onToggle = onToggleMode)
        }
        SearchStopModeButton(
            expanded = open,
            filtered = modeFilterActive(allClasses, selectedModes),
            onClick = onButtonClick,
        )
    }
}

/**
 * The button that opens and shuts the mode block: taj's [RoundIconButton], 48 across, in the fixed dark with
 * the app's filter icon in the fixed light. While any mode is switched off it takes the active look, the
 * go circle's pattern of light fill and dark icon, with no text and no count beside it. A screen reader hears
 * its name and whether the block is expanded or collapsed.
 */
@Composable
internal fun SearchStopModeButton(
    expanded: Boolean,
    filtered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingDarkTheme {
        val fill = if (filtered) FloatingDark.content else FloatingDark.container
        val ink = if (filtered) FloatingDark.container else FloatingDark.content
        RoundIconButton(
            onClick = onClick,
            color = fill,
            modifier = modifier
                .testTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
                .semantics {
                    role = Role.Button
                    stateDescription = if (expanded) MODE_BUTTON_EXPANDED else MODE_BUTTON_COLLAPSED
                },
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_filter),
                contentDescription = MODE_BUTTON_LABEL,
                colorFilter = ColorFilter.tint(ink),
                modifier = Modifier.size(KrailTheme.dimensions.iconM),
            )
        }
    }
}

internal const val MODE_BUTTON_LABEL = "Transport modes"
internal const val MODE_BUTTON_EXPANDED = "Expanded"
internal const val MODE_BUTTON_COLLAPSED = "Collapsed"
