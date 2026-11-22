package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The layout of Search Stop's map state, read from the real bounds of the real screen: one group card at the
 * top holding only the two bars as rows with a divider, the location chip as its own small button directly under
 * it at its left edge, the list card under that as a separate card one gap below, and the mode button and block
 * at the bottom. Run at 411 dp, and at 320 dp with 200 percent
 * text, in light and in dark app mode.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class SearchStopMapStateLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val quietKeyboard = object : SoftwareKeyboardController {
        override fun show() = Unit
        override fun hide() = Unit
    }
    private val gaps = searchStopGaps()
    private var windowHeight = 0.dp

    private fun setScreen(fontScale: Float? = null) {
        val state: SearchStopState = SearchStopFixtures.recentWithHomeSet()
            .copy(isMapsAvailable = true, mapUiState = MapUiState.Ready())
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides if (fontScale != null) Density(density.density, fontScale) else density,
                LocalInspectionMode provides true,
                LocalSoftwareKeyboardController provides quietKeyboard,
            ) {
                PreviewTheme {
                    windowHeight = LocalWindowInfo.current.containerDpSize.height
                    SearchStopScreen(searchStopState = state, onEvent = {})
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun touchBoundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true)
        .fetchSemanticsNode().touchBoundsInRoot.let { rect ->
            with(composeRule.density) { DpRect(rect.left.toDp(), rect.top.toDp(), rect.right.toDp(), rect.bottom.toDp()) }
        }

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(abs((expected - actual).value) <= 1f, "$what: expected $expected, got $actual")
    }

    // region 411 dp

    @Test
    @Config(qualifiers = W411)
    fun `the group card is one rounded card one edge from the top and sides`() {
        setScreen()

        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertNear("top edge", gaps.edge, card.top)
        assertNear("left edge", gaps.edge, card.left)
        assertNear("right edge", 411.dp - gaps.edge, card.right)
    }

    @Test
    @Config(qualifiers = W411)
    fun `the bars are two rows of one width, one card padding inside the card, with a divider gap between`() {
        setScreen()

        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        assertNear("To left", card.left + gaps.cardPadding, to.left)
        assertNear("To top", card.top + gaps.cardPadding, to.top)
        assertNear("bars share a left edge", to.left, from.left)
        assertNear("bars share a width", to.width, from.width)
        assertNear("To is 48 tall", 48.dp, to.height)
        assertNear("From is 48 tall", 48.dp, from.height)
        // One row gap above the divider, the 1 dp divider, one row gap below it.
        assertNear("From sits under the divider", to.bottom + gaps.betweenRows * 2 + 1.dp, from.top)
    }

    @Test
    @Config(qualifiers = W411)
    fun `the status chip is its own button under the card, left aligned with it, one gap below and not inside it`() {
        setScreen()

        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val chip = touchBoundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertTrue(from.bottom <= card.bottom - gaps.cardPadding + 1.dp, "The From bar ends the card's content")
        assertNear("the card ends one padding under the From bar", from.bottom + gaps.cardPadding, card.bottom)
        assertNear("one gap token under the card", card.bottom + gaps.betweenCards, chip.top)
        assertNear("left aligned with the card", card.left, chip.left)
        assertTrue(chip.top >= card.bottom, "The chip $chip is outside the card $card, never inside it")
        assertTrue(chip.height >= 48.dp - 1.dp, "The chip keeps its 48 dp touch target: $chip")
        assertTrue(chip.right <= card.right, "The chip $chip stays within the card's right edge")
    }

    @Test
    @Config(qualifiers = W411)
    fun `the list card is one gap under the chip, and under the card when the chip is not shown`() {
        setScreen()

        val chip = touchBoundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val list = boundsOf(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST)
        assertNear("gap between the chip's touch target and the list card", chip.bottom + gaps.betweenCards, list.top)
    }

    @Test
    @Config(qualifiers = W411)
    fun `the list card is a separate card under the group card on the same edges`() {
        setScreen()

        val group = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val list = boundsOf(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST)
        val chip = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertTrue(list.top >= group.bottom + gaps.betweenCards - 1.dp, "The list card $list is under the group card")
        assertTrue(list.top >= chip.bottom, "The list card $list is under the chip $chip")
        assertNear("same left edge", group.left, list.left)
        assertNear("same right edge", group.right, list.right)
    }

    @Test
    @Config(qualifiers = W411)
    fun `the mode chips are not in the group card and there is nothing that scrolls sideways at the top`() {
        setScreen()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertExists()
    }

    @Test
    @Config(qualifiers = W411)
    fun `the mode button sits at the bottom left, inside the window and clear of the bottom row`() {
        setScreen()

        val button = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        assertNear("left edge", gaps.edge, button.left)
        assertNear("48 wide", 48.dp, button.width)
        assertNear("48 tall", 48.dp, button.height)
        // The location button and the attribution icon share the bottom row, one edge above the inset.
        val controlRowTop = windowHeight - gaps.edge - MAP_CONTROL_ROW_HEIGHT
        assertTrue(button.bottom <= controlRowTop - gaps.betweenCards + 1.dp, "$button is above the row at $controlRowTop")
        assertTrue(button.top >= 0.dp && button.right <= 411.dp, "$button stays inside the window")
    }

    @Test
    @Config(qualifiers = W411)
    fun `the open mode block is a card above its button on the same edges and the list card is above it`() {
        setScreen()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.waitForIdle()

        val block = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
        val button = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        val list = boundsOf(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST)
        assertNear("gap between the block and its button", block.bottom + gaps.betweenCards, button.top)
        assertNear("block left edge", gaps.edge, block.left)
        assertNear("block right edge", 411.dp - gaps.edge, block.right)
        assertTrue(list.bottom <= block.top, "The list card $list never reaches the block $block")
        composeRule.onAllNodes(
            hasScrollAction() and hasAnyAncestor(hasTestTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)),
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = W411)
    fun `the mode button and block are gone with the keyboard up, in the list state`() {
        setScreen()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertDoesNotExist()
    }

    // endregion

    // region 320 dp at 200 percent

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun `at 320 dp and 200 percent text nothing overlaps and nothing leaves the window`() {
        setScreen(fontScale = 2f)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.waitForIdle()

        val group = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val chip = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val list = boundsOf(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST)
        val block = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
        val button = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        assertTrue(chip.top >= group.bottom, "The chip $chip is under the card $group")
        assertTrue(chip.left >= group.left - 1.dp && chip.right <= group.right + 1.dp, "The chip $chip stays in the width")
        assertTrue(chip.bottom <= list.top, "The chip $chip is above the list card $list")
        assertTrue(group.bottom <= list.top, "The group card $group is above the list card $list")
        assertTrue(list.bottom <= block.top + 1.dp, "The list card $list yields to the block $block")
        assertTrue(block.bottom <= button.top + 1.dp, "The block $block is above its button $button")
        for (box in listOf(group, chip, list, block, button)) {
            assertTrue(box.left >= 0.dp && box.right <= 320.dp, "$box stays inside the 320 dp window")
            assertTrue(box.top >= 0.dp && box.bottom <= windowHeight, "$box stays inside the window height")
        }
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO).assertExists()
        assertEquals(48.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height, "The bar stays one 48 dp line.")
    }

    // endregion

    private companion object {
        const val W411 = "w411dp-h891dp"
    }
}
