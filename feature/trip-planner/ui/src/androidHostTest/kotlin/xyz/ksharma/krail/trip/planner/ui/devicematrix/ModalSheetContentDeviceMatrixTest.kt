package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.core.testing.device.DeviceMatrixScope
import xyz.ksharma.krail.core.testing.device.DeviceProfile
import xyz.ksharma.krail.core.testing.device.visibleBoundsInRoot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.fail

/**
 * The container of every `ModalBottomSheet`, `SheetContent`, on every device profile (`docs/testing/DEVICE_PROFILES.md`). Three
 * things are held: a control at either end of a full width row stands clear of a side strip and takes a real touch; the
 * content is padded by exactly the part of each side inset that the sheet's own edge does not already clear; and a body that
 * pads the vertical system bars itself leaves the sides to the container. The second keeps the first from being met by padding everywhere:
 * a centred sheet in a wide window, an Android phone in landscape for one, does not reach the cutout, and content padded there
 * would sit off centre for nothing.
 *
 * It lives here and not in `:taj` because `:taj`'s host tests have no `:core:testing` dependency, and this module's do.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class ModalSheetContentDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    @Test
    fun `sheet content keeps a control at each end of a full width row clear of a side strip on every device`() =
        matrix.forEachDeviceProfile(screen = "ModalBottomSheet") {
            setContent(theme = { BareKrailTheme(content = it) }) {
                SheetHost {
                    // The bottom bar is the body's own business, as it is for every sheet body in the app, so the row clears it.
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Probe(START)
                        Probe(END)
                    }
                }
            }

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(START))
            assertTouchReaches(onNodeWithTag(END))
        }

    @Test
    fun `sheet content is padded only by the part of a side inset the sheet's own edge does not clear`() =
        matrix.forEachDeviceProfile(screen = "ModalBottomSheet") { profile ->
            setContent(theme = { BareKrailTheme(content = it) }) {
                SheetHost { Box(modifier = Modifier.fillMaxWidth().height(48.dp).testTag(BODY)) }
            }

            assertBodyStandsWhereTheInsetLeavesIt(profile)
        }

    /**
     * `StopDetailsBottomSheet` pads its own content by the vertical system bars only, so a body like it leaves the sides to the
     * container. It used to pad by the whole of `systemBarsPadding()`: the host model says the container's consumption keeps
     * that from doubling the side inset, but on the real iOS sheet it doubled (the content was 84 pt narrower on the outer
     * screen of the iPhone Duo), so no sheet body pads the sides itself and `scripts/check_layout_invariants.py` fails the
     * build on one that does.
     */
    @Test
    fun `a sheet body that pads the vertical system bars itself leaves the sides to the container`() =
        matrix.forEachDeviceProfile(screen = "ModalBottomSheet") { profile ->
            setContent(theme = { BareKrailTheme(content = it) }) {
                SheetHost {
                    Box(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical))
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(BODY),
                    )
                }
            }

            assertBodyStandsWhereTheInsetLeavesIt(profile)
        }

    /**
     * The content of a sheet runs from the sheet's left edge plus whatever of the left inset the window's left edge does not
     * already clear, to the sheet's right edge minus the same on the right. The sheet is centred and no wider than Material 3's
     * cap, so its own edge is `(window - sheet) / 2` in from the window's.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    private fun DeviceMatrixScope.assertBodyStandsWhereTheInsetLeavesIt(profile: DeviceProfile) {
        val sheetWidth = min(profile.widthDp.toFloat(), BottomSheetDefaults.SheetMaxWidth.value)
        val sheetLeft = (profile.widthDp - sheetWidth) / 2
        val sheetRight = sheetLeft + sheetWidth
        val expectedLeft = sheetLeft + max(0f, profile.insets.left - sheetLeft)
        val expectedRight = sheetRight - max(0f, profile.insets.right - (profile.widthDp - sheetRight))

        val body = onNodeWithTag(BODY).visibleBoundsInRoot()
        if (abs(body.left.value - expectedLeft) > TOLERANCE_DP || abs(body.right.value - expectedRight) > TOLERANCE_DP) {
            fail(
                "${profile.name}: the sheet runs from $sheetLeft to $sheetRight dp in a ${profile.widthDp} dp window with " +
                    "side insets L${profile.insets.left} R${profile.insets.right}, so its content should run from " +
                    "$expectedLeft to $expectedRight dp, and it runs from ${body.left.value} to ${body.right.value} dp",
            )
        }
    }

    /** A 48 dp control, the smallest touch target, so it is its own width and the row has two ends. */
    @Composable
    private fun Probe(tag: String) {
        Box(modifier = Modifier.size(48.dp).testTag(tag).clickable {})
    }

    private companion object {
        const val START = "sheet_start"
        const val END = "sheet_end"
        const val BODY = "sheet_body"
        const val TOLERANCE_DP = 1f
    }
}
