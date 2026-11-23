package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
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
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.mapActiveChipColors
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.components.mapChipEdge
import xyz.ksharma.krail.trip.planner.ui.components.MapControlVisibility
import xyz.ksharma.krail.trip.planner.ui.components.MapMotion
import xyz.ksharma.krail.trip.planner.ui.components.mapPopIn
import xyz.ksharma.krail.trip.planner.ui.components.mapPopOut

/**
 * The mode filter's controls at the bottom left of the map state: the [SearchStopModeButton] and, while it is open, the
 * [SearchStopModeBlock] right above it. The rider asks for them, so the map is bare until they do.
 *
 * Where they stand (`docs/MAP_HOME_UX.md` rule 9): the bottom of the map is two lines. The lower one is the map's own info
 * icon alone, at the bottom most left. The upper one is the control line: Options here at the start and the location
 * control at the end, on one centre line (see [controlLineCentre]). Options stands over the icon in z order: its finger
 * target ends where the visible "i" begins, so it reaches over the icon's own empty edge and never over the "i", and the
 * pill is [SearchStopGaps.aboveIcon] above the "i". The block is [SearchStopGaps.betweenCards] above the line's visible
 * top and spans the same edges as the group card. [clearance] lifts the line above an open stop card the way it lifts
 * the location control.
 *
 * While a stop card is open, or the location permission is not granted, [visible] is false and the button and its card
 * are gone: the card is told to shut by the screen, and the button fades and scales away from its bottom start corner
 * the way the card does, then comes back the same way. Neither takes a tap while it is on its way out or gone.
 *
 * [pillHeight] is the drawn height of the Options pill, which the screen holds because the location control stands on
 * the same line, and [onPillHeight] reports it as it is measured.
 *
 * The block grows out of the button and goes back into it: it fades and scales from its bottom start corner, with its
 * layout final from the first frame, so its wrapped chips never reflow or clip while it moves, and it leaves the tree
 * only once it has gone. It reverses from wherever it is when the button is tapped again. With [reduceMotion] it simply
 * appears and disappears.
 */
@Composable
internal fun BoxScope.SearchStopModeControls(
    open: Boolean,
    selectedModes: ImmutableSet<Int>,
    onButtonClick: () -> Unit,
    onToggleMode: (TransportMode) -> Unit,
    pillHeight: Dp,
    onPillHeight: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    clearance: Dp = 0.dp,
    visible: Boolean = true,
    reduceMotion: Boolean = isReduceMotionEnabled(),
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
            .padding(start = gaps.edge, end = gaps.edge, bottom = optionsBottomPadding(gaps, pillHeight, clearance)),
        horizontalAlignment = Alignment.Start,
    ) {
        AnimatedVisibility(
            visible = open && visible,
            modifier = Modifier.fillMaxWidth().offset(y = optionsCardOverlap(gaps, pillHeight)),
            enter = modeBlockEnter(reduceMotion),
            exit = modeBlockExit(reduceMotion),
        ) {
            // The gap to the line is inside the animated content, so it grows and goes with the block.
            Column {
                SearchStopModeBlock(selectedModes = selectedModes, onToggle = onToggleMode)
                Spacer(modifier = Modifier.height(gaps.betweenCards))
            }
        }
        MapControlVisibility(
            visible = visible,
            enter = mapPopIn(reduceMotion),
            exit = mapPopOut(reduceMotion),
        ) {
            SearchStopModeButton(
                expanded = open,
                filtered = modeFilterActive(allClasses, selectedModes),
                onClick = onButtonClick,
                pillBottomGap = gaps.aboveIcon,
                onPillHeight = onPillHeight,
            )
        }
    }
}

/** The block grows out of the button: it fades in while it scales up from its bottom start corner. */
internal fun modeBlockEnter(reduceMotion: Boolean): EnterTransition = mapPopIn(reduceMotion)

/** And goes back into it, a little quicker: it fades out while it scales down to its bottom start corner. */
internal fun modeBlockExit(reduceMotion: Boolean): ExitTransition = mapPopOut(reduceMotion)

// The one motion spec's numbers (`MapMotion`), by the names the block's tests have always read.
internal const val MODE_BLOCK_ENTER_MS = MapMotion.ENTER_MS
internal const val MODE_BLOCK_FADE_IN_MS = MapMotion.FADE_IN_MS
internal const val MODE_BLOCK_EXIT_MS = MapMotion.EXIT_MS
internal const val MODE_BLOCK_START_SCALE = MapMotion.POP_START_SCALE

/**
 * The button that opens and shuts the mode block: taj's [Button] at its medium size, as shipped (32 dp at 100 percent text and
 * growing with it), reading [MODE_BUTTON_TEXT] after the app's filter icon. It is a control on the always light map, not a card, so
 * it keeps one look in both app modes. At rest it is white with the light palette's text and a 1 dp light `softLabel` hairline
 * ([MapCardColors.pillEdge]) so it holds against the light map ([MapCardColors.pill], through the button's own `colors`). While any mode is switched off it takes taj's default button look, the solid theme colour, with no
 * count beside it.
 *
 * The finger target is the node tagged [TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON]: a real 48 dp box that is itself
 * the clickable, not the touch slop Compose adds around a small clickable, which loses to the map under it (a hit on
 * the map outranks that slop). The pill, tagged [TripPlannerTestTags.SEARCH_STOP_MODE_PILL], sits in the box's bottom
 * start corner, [pillBottomGap] above its bottom edge, and reports its own height through [onPillHeight]. A screen
 * reader hears the one node: its text and whether the block is expanded or collapsed.
 */
@Composable
internal fun SearchStopModeButton(
    expanded: Boolean,
    filtered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pillBottomGap: Dp = 0.dp,
    onPillHeight: (Dp) -> Unit = {},
) {
    val density = LocalDensity.current
    val dimensions = ButtonDefaults.mediumButtonSize()
    Box(
        modifier = modifier
            .testTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
            .heightIn(min = MAP_CONTROL_ROW_HEIGHT)
            .widthIn(min = MAP_CONTROL_ROW_HEIGHT)
            .semantics(mergeDescendants = true) {
                stateDescription = if (expanded) MODE_BUTTON_EXPANDED else MODE_BUTTON_COLLAPSED
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Button(
            onClick = onClick,
            colors = if (filtered) mapActiveChipColors() else mapCardColors().pill,
            dimensions = dimensions,
            modifier = Modifier
                .padding(bottom = pillBottomGap)
                .onSizeChanged { onPillHeight(with(density) { it.height.toDp() }) }
                .mapChipEdge(dimensions.shape, mapCardColors().pillEdge)
                .testTag(TripPlannerTestTags.SEARCH_STOP_MODE_PILL),
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
}

internal const val MODE_BUTTON_TEXT = "Options"
internal const val MODE_BUTTON_EXPANDED = "Expanded"
internal const val MODE_BUTTON_COLLAPSED = "Collapsed"
