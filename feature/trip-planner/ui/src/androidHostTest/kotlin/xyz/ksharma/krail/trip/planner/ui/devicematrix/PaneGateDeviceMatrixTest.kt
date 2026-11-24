package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.core.testing.device.DeviceProfiles
import xyz.ksharma.krail.core.testing.device.visibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapActions
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapHost
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapScreen
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The gate between one pane and two, at its edge, on the device profiles (`docs/testing/DEVICE_PROFILES.md`): two panes from
 * 840 dp wide and 480 dp high, one pane below. The pure function has a unit test in `:core:adaptive-ui`. These tests are about the
 * other half: that the window a real device gives reaches the reader, and that the map home really splits at the edge and not a
 * dp either side of it. The expected answers are written out per profile, not worked out from the constants, so a change to the
 * gate has to change a line here on purpose.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class PaneGateDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    @Test
    fun `every profile reads two panes exactly when it is 840 dp wide and 480 dp high or more`() {
        assertEquals(
            DeviceProfiles.all.map { it.name }.toSet(),
            DUAL_PANE_BY_PROFILE.keys,
            "a profile was added or removed: say here whether it is dual pane",
        )
        matrix.forEachDeviceProfile(screen = "AdaptivePaneGate") { profile ->
            var dualPane: Boolean? = null
            setContent(theme = { BareKrailTheme(content = it) }) {
                val info = rememberAdaptiveLayoutInfo()
                SideEffect { dualPane = info.shouldShowDualPane }
                Box(Modifier.fillMaxSize())
            }
            rule.waitForIdle()

            assertEquals(
                DUAL_PANE_BY_PROFILE.getValue(profile.name),
                dualPane,
                "${profile.name} (${profile.widthDp} x ${profile.heightDp}) reads the wrong pane mode",
            )
        }
    }

    @Test
    fun `the map home puts the map beside the list from 840 dp and under it never`() =
        matrix.forEachDeviceProfile(screen = "AdaptivePaneGate") { profile ->
            setContent(theme = { BareKrailTheme(content = it) }) {
                HomeMapScreen(
                    savedTripsState = SavedTripsState(isSavedTripsLoading = false),
                    host = HomeMapHost(
                        map = { Box(Modifier.fillMaxSize().testTag(MAP_TAG)) },
                        notices = persistentListOf(),
                        locationPill = LocationPill.Hidden,
                        onLocationClick = {},
                        card = null,
                        onCardClose = {},
                    ),
                    actions = HomeMapActions(),
                )
            }

            val map = onNodeWithTag(MAP_TAG).visibleBoundsInRoot()
            if (DUAL_PANE_BY_PROFILE.getValue(profile.name)) {
                assertTrue(
                    abs(map.left.value - DUAL_PANE_LIST_WIDTH.value) <= TOLERANCE,
                    "${profile.name}: the map should start at the list pane's ${DUAL_PANE_LIST_WIDTH} but starts at ${map.left}",
                )
                assertTrue(
                    map.width >= (profile.widthDp - DUAL_PANE_LIST_WIDTH.value - TOLERANCE).dp,
                    "${profile.name}: the map should take the rest of ${profile.widthDp} dp but is ${map.width} wide",
                )
            } else {
                assertTrue(
                    map.left.value <= TOLERANCE && map.width >= (profile.widthDp - TOLERANCE).dp,
                    "${profile.name}: one pane means the map fills the window, but it is ${map.left} to ${map.right}",
                )
            }
        }

    private companion object {
        const val MAP_TAG = "gate.map"
        const val TOLERANCE = 1.5f

        /** Written out on purpose: 840 dp wide and 480 dp high or more, per docs/TABLET_FOLDABLE_UX.md section 1. */
        val DUAL_PANE_BY_PROFILE = mapOf(
            "Narrow320Font2" to false,
            "SmallPhone" to false,
            "LargePhone" to false,
            "PhoneLandscapeCutout" to false,
            "AndroidLandscapeSideNavBar" to false,
            "FoldCover" to false,
            "FoldInner" to true,
            "Medium720" to false,
            "Gate839" to false,
            "Gate840" to true,
            "TabletLandscape" to true,
            "IPhoneSmall" to false,
            "IPhoneProPortrait" to false,
            "IPhoneProLandscape" to false,
            "IPhoneDuoOuter" to false,
            "IPhoneDuoInnerPortrait" to false,
            "IPhoneDuoInnerLandscape" to true,
            "IPadMiniPortrait" to false,
            "IPadMiniLandscape" to true,
            "IPadPro13Portrait" to true,
            "IPadPro13Landscape" to true,
        )
    }
}
