package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
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
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.savedtrips.CarParkBlock
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The suggestion card of `docs/MAP_HOME_UX.md` rules 13, 22, 27, 45 and 47 on real nodes. Each test runs in a window
 * of its own width at density 1, so a dp is a pixel, and reads where the real nodes landed: Not now left and Add to
 * home right while two whole buttons fit, one under the other with the primary on top when they do not, the same
 * height with and without numbers, every control at least 48 dp, and one region for a screen reader.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [CARD_TEST_SDK], manifest = Config.NONE)
class ParkRideSuggestionCardLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var addTaps = 0
    private var notNowTaps = 0

    // region the two buttons

    @Test
    @Config(qualifiers = PHONE)
    fun `at 411 dp the buttons sit beside each other, Not now at the left and Add to home at the right`() {
        setCard()

        val notNow = bounds(TripPlannerTestTags.ParkRideSuggestion.NOT_NOW)
        val add = bounds(TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME)
        assertNear("Not now starts at the card's padding", 16.dp, notNow.left)
        assertNear("Add to home ends at the card's padding", 411.dp - 16.dp, add.right)
        assertTrue(notNow.right < add.left, "Not now $notNow is left of Add to home $add")
        assertNear("the buttons share one top", notNow.top, add.top)
        assertTrue(add.left - notNow.right >= 8.dp - TOLERANCE_DP.dp, "At least 8 dp between the buttons")
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `at 320 dp and 200 percent text the buttons stack, Add to home on top, each the full width`() {
        setCard(fontScale = 2f)

        val notNow = bounds(TripPlannerTestTags.ParkRideSuggestion.NOT_NOW)
        val add = bounds(TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME)
        assertTrue(add.bottom <= notNow.top, "The primary $add is above Not now $notNow")
        listOf("Add to home" to add, "Not now" to notNow).forEach { (name, box) ->
            assertNear("$name left edge", 16.dp, box.left)
            assertNear("$name right edge", 320.dp - 16.dp, box.right)
        }
        assertNear("the gap between the stacked buttons", 8.dp, notNow.top - add.bottom)
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `a stacked label stays on one line and is never squeezed or cut`() {
        setCard(fontScale = 2f)

        assertLabelInside(TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME, "Add to home")
        assertLabelInside(TripPlannerTestTags.ParkRideSuggestion.NOT_NOW, "Not now")
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp")
    fun `in a wide window the buttons keep their own width and sit at the two ends`() {
        setCard()

        val notNow = bounds(TripPlannerTestTags.ParkRideSuggestion.NOT_NOW)
        val add = bounds(TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME)
        assertNear("Not now starts at the card's padding", 16.dp, notNow.left)
        assertNear("Add to home ends at the card's padding", 840.dp - 16.dp, add.right)
        assertTrue(
            add.width < 240.dp && notNow.width < 240.dp,
            "A button is not stretched across the card: $notNow $add",
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `every control is at least 48 dp tall`() {
        setCard()

        assertEveryControlIsTall()
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `every control is at least 48 dp tall at 320 dp and 200 percent text`() {
        setCard(fontScale = 2f)

        assertEveryControlIsTall()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a tap on each button does only its own thing`() {
        setCard()

        composeRule.onNodeWithTag(TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME).performClick()
        assertEquals(1 to 0, addTaps to notNowTaps)

        composeRule.onNodeWithTag(TripPlannerTestTags.ParkRideSuggestion.NOT_NOW).performClick()
        assertEquals(1 to 1, addTaps to notNowTaps)
    }

    // endregion

    // region one height in every state

    @Test
    @Config(qualifiers = PHONE)
    fun `the card is the same height with its numbers, without them and with an old time`() =
        assertSameHeightInEveryState(fontScale = 1f)

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `the card is the same height at 320 dp and 200 percent in every state`() =
        assertSameHeightInEveryState(fontScale = 2f)

    private fun assertSameHeightInEveryState(fontScale: Float) {
        val shown = mutableStateOf(LOADED)
        composeRule.setContent { WithFontScale(fontScale) { PreviewTheme { Card(shown.value) } } }
        composeRule.waitForIdle()
        val loaded = bounds(TripPlannerTestTags.ParkRideSuggestion.CARD).height

        shown.value = NAME_ALONE
        composeRule.waitForIdle()
        assertNear(
            "a card with no reading is as tall as a loaded one",
            loaded,
            bounds(TripPlannerTestTags.ParkRideSuggestion.CARD).height,
        )

        shown.value = STALE
        composeRule.waitForIdle()
        assertNear(
            "a card with an old reading is as tall as a fresh one",
            loaded,
            bounds(TripPlannerTestTags.ParkRideSuggestion.CARD).height,
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with no reading the card shows the station name alone and no number`() {
        setCard(tile = NAME_ALONE)

        composeRule.onNodeWithText("Tallawong Station", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("spots").assertDoesNotExist()
    }

    // endregion

    // region one region for a screen reader

    @Test
    @Config(qualifiers = PHONE)
    fun `the figures are read as one line and the station is not read a second time`() {
        setCard()

        composeRule.onNodeWithContentDescription(
            "Park & Ride suggestion, Tallawong Station, 155 spots, last updated at 8:05\u00A0AM",
        ).assertExists()
        composeRule.onNodeWithText("Tallawong Station").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the primary button says which station a tap adds`() {
        setCard()

        val node = composeRule.onNodeWithTag(TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME).fetchSemanticsNode()
        val label = node.config.getOrNull(SemanticsActions.OnClick)?.label

        assertEquals("Add Tallawong Station to home", label)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the card has exactly two controls and says nothing beside the figures and the buttons`() {
        setCard()

        val card = composeRule.onNodeWithTag(TripPlannerTestTags.ParkRideSuggestion.CARD, useUnmergedTree = true)
            .fetchSemanticsNode()
        val clickable = card.allNodes().count { it.config.getOrNull(SemanticsActions.OnClick) != null }
        val texts = card.allNodes().flatMap { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        }.toSet()

        assertEquals(2, clickable, "Not now and Add to home, nothing else is a control")
        assertEquals(
            setOf("155", "spots", "Tallawong Station", "Last updated at\u00A08:05\u00A0AM", "Add to home", "Not now"),
            texts,
            "No heading, tip or caption: only the figures and the two buttons",
        )
    }

    // endregion

    // region drawn means shown

    @Test
    @Config(qualifiers = PHONE)
    fun `drawing the item raises shown for its station, once, however often it is recomposed`() {
        var shownCount = 0
        var recompose by mutableIntStateOf(0)
        composeRule.setContent {
            PreviewTheme {
                // A new lambda on every recomposition, as the entry builds one.
                val slot = ParkRideSuggestionSlot(
                    tile = LOADED,
                    onShown = { shownCount++ },
                    onAddToHome = { recompose },
                    onNotNow = {},
                )
                ParkRideSuggestionItem(slot)
            }
        }
        composeRule.waitForIdle()
        recompose++
        composeRule.waitForIdle()
        recompose++
        composeRule.waitForIdle()

        assertEquals(1, shownCount)
    }

    // endregion

    private fun setCard(fontScale: Float = 1f, tile: ParkRideTile = LOADED) {
        composeRule.setContent { WithFontScale(fontScale) { PreviewTheme { Card(tile) } } }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Card(tile: ParkRideTile) {
        ParkRideSuggestionCard(tile = tile, onAddToHome = { addTaps++ }, onNotNow = { notNowTaps++ })
    }

    @Composable
    private fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
            content = content,
        )
    }

    private fun assertEveryControlIsTall() {
        CONTROLS.forEach { tag ->
            val height = bounds(tag).height
            assertTrue(height >= 48.dp - TOLERANCE_DP.dp, "$tag is $height tall")
        }
    }

    private fun assertLabelInside(tag: String, text: String) {
        val button = bounds(tag)
        val label = textBounds(text)
        assertTrue(
            label.left >= button.left && label.right <= button.right,
            "The label $label is inside its button $button",
        )
        assertTrue(button.height < MAX_ONE_LINE_BUTTON_AT_200, "$tag is one line tall, not ${button.height}")
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getVisibleBoundsInRoot()

    private fun textBounds(text: String): DpRect = composeRule.onNodeWithText(text).getVisibleBoundsInRoot()

    private fun SemanticsNode.allNodes(): List<SemanticsNode> = listOf(this) + children.flatMap { it.allNodes() }

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE_DP,
            "$what should be $expected but is $actual.",
        )
    }

    private companion object {
        const val PHONE = "w411dp-h891dp"
        const val TOLERANCE_DP = 1.5f

        // A button at 200 percent text is one line when it is about 70 dp, and two lines when it is over 100.
        val MAX_ONE_LINE_BUTTON_AT_200 = 90.dp

        val CONTROLS = listOf(
            TripPlannerTestTags.ParkRideSuggestion.NOT_NOW,
            TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME,
        )

        const val PERCENT_FULL = 60

        val LOADED = tile(155, "8:05\u00A0AM")
        val NAME_ALONE = tile(null, null)
        val STALE = tile(12, "6:00\u00A0AM")

        fun tile(total: Int?, time: String?) = ParkRideTile(
            stationId = "2155384",
            stationName = "Tallawong Station",
            stopIds = persistentListOf("2155384"),
            totalFreeSpots = total,
            timeText = time,
            carParks = persistentListOf(
                CarParkBlock("26", "Tallawong P1", total, total?.let { PERCENT_FULL }, time, directionsTo = null),
            ),
        )
    }
}

private const val CARD_TEST_SDK = 34
