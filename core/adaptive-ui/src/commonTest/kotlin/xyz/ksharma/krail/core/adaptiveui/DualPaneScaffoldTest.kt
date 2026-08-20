package xyz.ksharma.krail.core.adaptiveui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The list pane's width is a fixed number, and it has to stay one.
 *
 * This looks like a test of a constant, and it is — but the value is not the point. The point
 * is that it is a [androidx.compose.ui.unit.Dp] at all rather than a `widthIn(min..max)` range.
 *
 * `DualPaneScaffold` puts the list pane beside a `weight(1f)` right pane that, on the screens
 * that matter, holds a MapLibre `MLNMapView` behind a `UIKitView`. A flexible-width sibling
 * makes that weight slot resolve over several measure passes, and iOS interop hands the native
 * view a frame that never settles: the map renders blank, on iOS only, with no error anywhere.
 * A hard width makes the remaining space deterministic in a single pass.
 *
 * `docs/TABLET_FOLDABLE_UX.md` §2 described the flexible form for a while after the code had
 * stopped using it, which is the drift this pins shut. Widening or narrowing the pane is fine;
 * turning it back into a range is the regression. Changing the number means changing it here
 * too, deliberately, with the doc in the same commit.
 */
class DualPaneScaffoldTest {

    @Test
    fun listPaneWidthIsFixed() {
        assertEquals(
            480.dp,
            DUAL_PANE_LIST_WIDTH,
            "DUAL_PANE_LIST_WIDTH is the fixed left-pane width every dual-pane screen shares. " +
                "See docs/TABLET_FOLDABLE_UX.md §2 and the iOS compositing note on " +
                "DualPaneScaffold before changing it.",
        )
    }
}
