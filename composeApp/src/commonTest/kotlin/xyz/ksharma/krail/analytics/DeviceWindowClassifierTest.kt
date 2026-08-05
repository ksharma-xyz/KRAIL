package xyz.ksharma.krail.analytics

import xyz.ksharma.krail.core.appinfo.FoldState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceWindowClassifierTest {

    @Test
    fun `a phone-width window with no hinge is a phone`() {
        assertEquals(
            DeviceFormFactor.PHONE,
            DeviceWindowClassifier.formFactor(smallestWidthDp = 411, foldState = FoldState.NONE),
        )
    }

    @Test
    fun `a folded foldable reports as a phone because the window cannot see the hinge`() {
        // Deliberate: the cover display is phone-shaped and reports no folding feature.
        // Crossed with deviceModel on the analytics side this becomes the used-closed
        // segment; guessing FOLDABLE here would destroy that distinction.
        assertEquals(
            DeviceFormFactor.PHONE,
            DeviceWindowClassifier.formFactor(smallestWidthDp = 374, foldState = FoldState.NONE),
        )
    }

    @Test
    fun `a hinge in the window means the device is open`() {
        assertEquals(
            DeviceFormFactor.FOLDABLE_OPEN,
            DeviceWindowClassifier.formFactor(smallestWidthDp = 674, foldState = FoldState.FLAT),
        )
        assertEquals(
            DeviceFormFactor.FOLDABLE_OPEN,
            DeviceWindowClassifier
                .formFactor(smallestWidthDp = 674, foldState = FoldState.HALF_OPENED),
        )
    }

    @Test
    fun `600dp on the shorter edge is the tablet threshold`() {
        assertEquals(
            DeviceFormFactor.PHONE,
            DeviceWindowClassifier.formFactor(smallestWidthDp = 599, foldState = FoldState.NONE),
        )
        assertEquals(
            DeviceFormFactor.TABLET,
            DeviceWindowClassifier.formFactor(smallestWidthDp = 600, foldState = FoldState.NONE),
        )
    }

    @Test
    fun `orientation follows the longer edge`() {
        assertEquals(ScreenOrientation.PORTRAIT, DeviceWindowClassifier.orientation(411, 914))
        assertEquals(ScreenOrientation.LANDSCAPE, DeviceWindowClassifier.orientation(914, 411))
        // Square windows (split-screen edge case) are not landscape.
        assertEquals(ScreenOrientation.PORTRAIT, DeviceWindowClassifier.orientation(600, 600))
    }

    @Test
    fun `an unchanged window is not a transition`() {
        val state = snapshot()

        assertFalse(DeviceWindowClassifier.isReportableTransition(state, state))
    }

    @Test
    fun `a width class change is a transition`() {
        assertTrue(
            DeviceWindowClassifier.isReportableTransition(
                snapshot(widthSizeClass = "COMPACT"),
                snapshot(widthSizeClass = "EXPANDED"),
            ),
        )
    }

    @Test
    fun `unfolding is a transition even when the width class does not move`() {
        assertTrue(
            DeviceWindowClassifier.isReportableTransition(
                snapshot(foldState = FoldState.HALF_OPENED),
                snapshot(foldState = FoldState.FLAT),
            ),
        )
    }

    @Test
    fun `the layout opening a second pane is a transition on its own`() {
        assertTrue(
            DeviceWindowClassifier.isReportableTransition(
                snapshot(paneMode = PaneMode.SINGLE),
                snapshot(paneMode = PaneMode.DUAL),
            ),
        )
    }

    private fun snapshot(
        widthSizeClass: String = "COMPACT",
        foldState: FoldState = FoldState.NONE,
        paneMode: PaneMode = PaneMode.SINGLE,
    ) = DeviceWindowSnapshot(
        widthSizeClass = widthSizeClass,
        foldState = foldState,
        paneMode = paneMode,
    )
}
