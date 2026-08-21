package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val MAP_OPTIONS_RESTORE_TEST_SDK = 34

/**
 * Map options a rider has picked but not yet saved have to survive Activity recreation.
 *
 * The sheet stays open across a rotation — its visibility flag is already saveable — so the
 * only way a rider can tell the pending edits were dropped is by saving and getting the old
 * values back. Held in a plain `remember`, every chip and toggle silently reverted to what
 * was already committed, on a sheet that looked untouched.
 *
 * The assertion is what Save commits rather than which chip draws selected: the committed
 * value is the thing the rider loses, and it does not depend on how selection is painted.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [MAP_OPTIONS_RESTORE_TEST_SDK],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class MapOptionsSheetRestoreTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    private val events = mutableListOf<SearchStopUiEvent>()

    @Test
    fun `a radius picked before recreation is the one Save commits after it`() {
        setSheetContent()

        composeRule.onNodeWithText(FIVE_KM_LABEL).performClick()
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(SAVE_LABEL).performClick()
        composeRule.waitForIdle()

        val radiusChanges = events.filterIsInstance<SearchStopUiEvent.SearchRadiusChanged>()
        assertEquals(
            listOf(FIVE_KM),
            radiusChanges.map { it.radiusKm },
            "Save committed ${radiusChanges.map { it.radiusKm }} instead of the picked ${FIVE_KM}km",
        )
    }

    @Test
    fun `a toggle flipped before recreation is still flipped after it`() {
        setSheetContent()

        // Only the switch is clickable, and the distance scale is the one that starts off:
        // the compass starts on, so "the off switch" identifies it without a test tag.
        composeRule.onAllNodes(isToggleable() and isOff()).onFirst().performClick()
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onAllNodes(isToggleable()).onFirst().assertIsOn()

        composeRule.onNodeWithText(SAVE_LABEL).performClick()
        composeRule.waitForIdle()

        val toggles = events.filterIsInstance<SearchStopUiEvent.ShowDistanceScaleToggled>()
        assertTrue(
            toggles.singleOrNull()?.enabled == true,
            "Save committed $toggles instead of the distance scale the rider switched on",
        )
    }

    private fun setSheetContent() {
        restorationTester.setContent {
            PreviewTheme {
                MapOptionsBottomSheet(
                    searchRadiusKm = ONE_KM,
                    selectedTransportModes = persistentSetOf(TRAIN_PRODUCT_CLASS),
                    showDistanceScale = false,
                    showCompass = true,
                    onDismiss = {},
                    onEvent = { event -> events += event },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val ONE_KM = 1.0
        const val FIVE_KM = 5.0
        const val FIVE_KM_LABEL = "5km"
        const val SAVE_LABEL = "Save"
        const val TRAIN_PRODUCT_CLASS = 1
    }
}
