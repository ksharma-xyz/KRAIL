package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.TransformOrigin
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
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark

/**
 * The mode filter's controls at the bottom left of the map state: the [SearchStopModeButton] and, while it is open, the
 * [SearchStopModeBlock] right above it. The rider asks for them, so the map is bare until they do.
 *
 * Where they stand (`docs/MAP_HOME_UX.md` rule 9): one column at the bottom left, left aligned on [SearchStopGaps.edge].
 * The Options button is directly above the map's own info icon, [SearchStopGaps.betweenCards] over it, and the icon is
 * the bottom most element at the left, resting on the system bar (see [modeControlsBottomPadding] and
 * [mapOrnamentPadding]). The block is one [SearchStopGaps.betweenCards] above the button and spans the same edges as the
 * group card. [clearance] lifts the whole stack above an open stop card the way it lifts the location control.
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
    modifier: Modifier = Modifier,
    clearance: Dp = 0.dp,
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
            .padding(start = gaps.edge, end = gaps.edge, bottom = modeControlsBottomPadding(gaps, clearance)),
        horizontalAlignment = Alignment.Start,
    ) {
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.fillMaxWidth(),
            enter = modeBlockEnter(reduceMotion),
            exit = modeBlockExit(reduceMotion),
        ) {
            // The gap to the button is inside the animated content, so it grows and goes with the block.
            Column {
                SearchStopModeBlock(selectedModes = selectedModes, onToggle = onToggleMode)
                Spacer(modifier = Modifier.height(gaps.betweenCards))
            }
        }
        SearchStopModeButton(
            expanded = open,
            filtered = modeFilterActive(allClasses, selectedModes),
            onClick = onButtonClick,
        )
    }
}

/** The block grows out of the button: it fades in while it scales up from its bottom start corner. */
internal fun modeBlockEnter(reduceMotion: Boolean): EnterTransition = if (reduceMotion) {
    EnterTransition.None
} else {
    fadeIn(animationSpec = tween(durationMillis = MODE_BLOCK_FADE_IN_MS, easing = LinearOutSlowInEasing)) +
        scaleIn(
            initialScale = MODE_BLOCK_START_SCALE,
            transformOrigin = MODE_BLOCK_ORIGIN,
            animationSpec = tween(durationMillis = MODE_BLOCK_ENTER_MS, easing = FastOutSlowInEasing),
        )
}

/** And goes back into it, a little quicker: it fades out while it scales down to its bottom start corner. */
internal fun modeBlockExit(reduceMotion: Boolean): ExitTransition = if (reduceMotion) {
    ExitTransition.None
} else {
    fadeOut(animationSpec = tween(durationMillis = MODE_BLOCK_EXIT_MS, easing = FastOutLinearInEasing)) +
        scaleOut(
            targetScale = MODE_BLOCK_START_SCALE,
            transformOrigin = MODE_BLOCK_ORIGIN,
            animationSpec = tween(durationMillis = MODE_BLOCK_EXIT_MS, easing = FastOutSlowInEasing),
        )
}

// The same 200 ms in and 150 ms out the departure board's expanding content uses.
internal const val MODE_BLOCK_ENTER_MS = 200
internal const val MODE_BLOCK_FADE_IN_MS = 150
internal const val MODE_BLOCK_EXIT_MS = 150
internal const val MODE_BLOCK_START_SCALE = 0.8f
private val MODE_BLOCK_ORIGIN = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 1f)

/**
 * The button that opens and shuts the mode block: taj's [Button] at its extra small size reading [MODE_BUTTON_TEXT]
 * after the app's filter icon, in the fixed dark with the fixed light, because it floats over an always light map
 * (the same pair the location notice uses). While any mode is switched off it takes the active look, the two colours
 * swapped, with no count beside it. Its finger target is 48 dp from the automatic minimum touch target and reserves no
 * layout space, so the button is drawn exactly [SearchStopGaps.betweenCards] above the info icon and the extra target
 * never wins a touch over a control it overlaps. A screen reader hears its text and whether the block is expanded or
 * collapsed.
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
        modifier = modifier
            .testTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
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
