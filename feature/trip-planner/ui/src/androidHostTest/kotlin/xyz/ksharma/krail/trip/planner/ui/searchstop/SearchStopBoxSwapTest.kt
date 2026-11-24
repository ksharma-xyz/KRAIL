package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The box at the top is the card when two fields show and one bare pill when one does. When the shape changes the two fade into
 * one another and the box takes its new size softly, with no jump, and with reduced motion it is simply the new one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class SearchStopBoxSwapTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var shape by mutableStateOf(FormShape.Single)

    private fun show(reduceMotion: Boolean = false) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        composeRule.setContent {
            PreviewTheme {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    SearchStopGroupCard(
                        form = SearchStopFormModel(
                            shape = shape,
                            editing = ToFromField.To,
                            toPlaceholder = TO_PLACEHOLDER,
                            fromPlaceholder = FROM_PLACEHOLDER,
                            toStop = null,
                            fromStop = null,
                            fieldState = rememberTextFieldState(),
                            focusRequester = remember { FocusRequester() },
                            onFlowEvent = {},
                            onGoClick = {},
                            onTextChange = {},
                        ),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    private fun changeShape(value: FormShape) = composeRule.runOnUiThread {
        shape = value
        Snapshot.sendApplyNotifications()
    }

    private fun cards() = composeRule.onAllNodesWithTag(
        TripPlannerTestTags.SEARCH_STOP_GROUP_CARD,
    ).fetchSemanticsNodes().size

    @Test
    fun `a single field is the bare pill with no card`() {
        show()

        assertEquals(0, cards())
    }

    @Test
    fun `when a second field arrives the card fades in while the pill fades out, then only the card is left`() {
        show()

        changeShape(FormShape.ToAndFrom)
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)
        assertEquals(1, cards(), "The card is already coming in part way through")
        val left = composeRule.onNodeWithTag(
            TripPlannerTestTags.SEARCH_STOP_GROUP_CARD,
        ).fetchSemanticsNode().boundsInRoot.left

        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(1, cards())
        assertTrue(
            composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).fetchSemanticsNode().boundsInRoot.left == left,
            "and the box did not jump sideways",
        )
    }

    @Test
    fun `with reduced motion the new shape is there within a frame`() {
        show(reduceMotion = true)

        changeShape(FormShape.ToAndFrom)
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }

        assertEquals(1, cards())
    }

    private companion object {
        const val FRAME_MS = 16L
        const val SETTLED_MS = 1_000L
    }
}
