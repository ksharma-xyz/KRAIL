package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The primary action of the stop card on Search Stop says what the rider is doing: Set as destination, Set as starting
 * point, Set as Home, Set as Work, or Set as and the label's saved name exactly as saved. It is the same button with the
 * same behaviour, one line that ends in an ellipsis if it is too long, and a screen reader hears the words drawn. When the
 * context changes with the card open the words cross fade, and plainly with reduced motion.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class StopCardContextActionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var picked = 0
    private var role by mutableStateOf(PickRole.Destination)
    private var labelName: String? by mutableStateOf(null)

    private fun show(fontScale: Float = 1f, reduceMotion: Boolean = false, hostWords: Boolean = true) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PreviewTheme {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        StopCardPickActionsFor(reduceMotion, hostWords)
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun StopCardPickActionsFor(reduceMotion: Boolean, hostWords: Boolean) {
        StopCardPickActions(
            stop = StopItem(stopId = "200060", stopName = "Central Station"),
            options = StopCardOptions(
                pickRole = role,
                labels = persistentListOf(),
                pickText = if (hostWords) contextPickText(role, labelName) else null,
            ),
            onPick = { picked++ },
            reduceMotion = reduceMotion,
        )
    }

    private fun words() = composeRule.onNodeWithTag(StopCard.PICK).fetchSemanticsNode()
        .config[SemanticsProperties.Text].joinToString { it.text }

    private fun change(newRole: PickRole, newLabel: String? = null) = composeRule.runOnUiThread {
        role = newRole
        labelName = newLabel
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun `choosing the destination reads Set as destination and the same tap still picks the stop`() {
        show()

        assertEquals("Set as destination", words())
        composeRule.onNodeWithTag(StopCard.PICK).performClick()
        assertEquals(1, picked)
    }

    @Test
    fun `choosing the start reads Set as starting point`() {
        role = PickRole.Start
        show()

        assertEquals("Set as starting point", words())
    }

    @Test
    fun `choosing a stop for Home, Work or a rider's own label reads the label as it was saved`() {
        show()

        for (name in listOf("Home", "Work", "Gym", "Mum's place")) {
            change(PickRole.Choose, name)
            composeRule.waitForIdle()
            assertEquals("Set as $name", words())
        }
    }

    @Test
    fun `a host with no field to name keeps its own words`() {
        role = PickRole.Choose
        show(hostWords = false)

        assertEquals("Select stop", words())
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun `a long custom label at 320 dp and 200 percent is one line ending in an ellipsis, and is read whole`() {
        role = PickRole.Choose
        labelName = "Grandma and Grandpa's holiday house by the sea"
        show(fontScale = 2f)

        assertEquals("Set as Grandma and Grandpa's holiday house by the sea", words(), "A screen reader hears the whole label")
        val layout = textLayout()
        assertEquals(1, layout.lineCount, "One line, never a second")
        assertTrue(layout.isLineEllipsized(0), "ending in an ellipsis")
        val button = composeRule.onNodeWithTag(StopCard.PICK).getUnclippedBoundsInRoot()
        val text = composeRule.onNodeWithText(words(), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(text.width <= button.width, "The words stay inside the button, ${text.width} in ${button.width}")
        assertTrue(button.height >= 48.dp, "and the button keeps its 48 dp, it is ${button.height}")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun `a short label at 320 dp and 200 percent is not cut`() {
        role = PickRole.Choose
        labelName = "Home"
        show(fontScale = 2f)

        assertEquals("Set as Home", words())
        assertTrue(!textLayout().isLineEllipsized(0), "Nothing is cut when it fits")
    }

    @Test
    fun `the words cross fade when the context changes, with both on screen part way and one at the end`() {
        show()
        composeRule.mainClock.autoAdvance = false

        change(PickRole.Start)
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 4)
        assertEquals(1, composeRule.onAllNodesWithText("Set as destination", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(1, composeRule.onAllNodesWithText("Set as starting point", useUnmergedTree = true).fetchSemanticsNodes().size)

        composeRule.mainClock.advanceTimeBy(SETTLED_MS)
        assertEquals(0, composeRule.onAllNodesWithText("Set as destination", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals("Set as starting point", words())
    }

    @Test
    fun `with reduced motion the words change within a frame, with no cross fade`() {
        show(reduceMotion = true)
        composeRule.mainClock.autoAdvance = false

        change(PickRole.Start)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(0, composeRule.onAllNodesWithText("Set as destination", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals("Set as starting point", words())
    }

    private fun textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = composeRule.onNodeWithText(words(), useUnmergedTree = true).fetchSemanticsNode()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first()
    }

    private companion object {
        const val FRAME_MS = 16L
        const val SETTLED_MS = 1_000L
    }
}
