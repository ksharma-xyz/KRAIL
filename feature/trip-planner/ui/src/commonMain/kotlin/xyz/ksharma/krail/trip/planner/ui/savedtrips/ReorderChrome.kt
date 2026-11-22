package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.contrast.ContrastAnalyzer.Companion.UI_COMPONENT_CONTRAST_AA
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeInkColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.PARK_RIDE_ROW_KEY

/**
 * The chrome reorder mode adds, `docs/MAP_HOME_UX.md` rule 31. While it is on the logo, the gear,
 * the chips and the search bar are gone and the stack takes their height; what is left is one dark
 * Done at the top right, a grip on every card and an outline round the Park & Ride row.
 */

/** A trip card's grip: a small bar centred on its top edge. The Park & Ride row's is the wider one. */
@Composable
internal fun ReorderGrip(key: String, wide: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            // Half the bar sits above the edge it is centred on.
            .offset(y = -GRIP_HEIGHT / 2)
            .size(width = if (wide) GRIP_WIDE_WIDTH else GRIP_WIDTH, height = GRIP_HEIGHT)
            // After the offset, so the tag's bounds are where the bar is drawn.
            .testTag(TripPlannerTestTags.Reorder.grip(key))
            .clip(RoundedCornerShape(GRIP_HEIGHT / 2))
            .background(themeInkColor(UI_COMPONENT_CONTRAST_AA))
            // Decoration. The whole card is the drag target, and a screen reader has no drag to offer.
            .clearAndSetSemantics {},
    )
}

private val GRIP_WIDTH = 32.dp
private val GRIP_WIDE_WIDTH = 64.dp
private val GRIP_HEIGHT = 4.dp

/**
 * The top row of reorder mode: one dark Done at the right, 48 dp tall and 16 dp from the top and the
 * right edge, below the status bar. Done is taj's [Button] in its monochrome colours, the one the
 * title bar's Done used.
 */
@Composable
internal fun ReorderTopRow(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .testTag(TripPlannerTestTags.SAVED_TRIPS_REORDER_TOP_ROW)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = dim.spacingXL, end = dim.spacingXL),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(
            onClick = onDone,
            colors = ButtonDefaults.monochromeButtonColors(),
            dimensions = ButtonDefaults.chipButtonSize(),
            modifier = Modifier
                .testTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE)
                .heightIn(min = dim.textFieldHeight),
        ) {
            Text(text = "Done")
        }
    }
}

/**
 * The Park & Ride row in reorder mode: one outlined block with the wider grip on its top edge, so it
 * reads as one thing that moves up or down among the trips as a unit. The row inside it is drawn
 * shut and does not scroll or open, so a touch anywhere on the block is a drag.
 */
@Composable
internal fun ParkRideReorderBlock(
    lifted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dim = KrailTheme.dimensions
    Box(modifier = modifier.padding(horizontal = dim.pageHorizontalPadding).liftedLook(lifted)) {
        Box(
            modifier = Modifier
                .testTag(TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_BLOCK)
                .fillMaxWidth()
                .border(
                    width = dim.strokeThin,
                    color = themeInkColor(UI_COMPONENT_CONTRAST_AA),
                    shape = CardShape,
                )
                .padding(vertical = dim.spacingXL),
        ) {
            content()
        }
        ReorderGrip(
            key = PARK_RIDE_ROW_KEY,
            wide = true,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * The look of a card that is lifted, by the long press that began the mode or by a drag in
 * progress: a little larger, with a shadow under it.
 */
@Composable
internal fun Modifier.liftedLook(lifted: Boolean): Modifier = if (lifted) {
    this
        .graphicsLayer {
            scaleX = LIFT_SCALE
            scaleY = LIFT_SCALE
        }
        .shadow(elevation = KrailTheme.dimensions.spacingM, shape = CardShape)
} else {
    this
}

private const val LIFT_SCALE = 1.03f

/**
 * Calls [onTouch] on the first touch down on whatever this wraps, without consuming it, while
 * [enabled]. Used to cancel reorder mode when the rider turns to the map instead of the cards. The
 * map is a separate pane, so a touch on it can never be a drag of a card.
 */
internal fun Modifier.onFirstTouch(enabled: Boolean, onTouch: () -> Unit): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(onTouch) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                onTouch()
            }
        }
    }
