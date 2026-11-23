package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The layout of Search Stop's clean map state, read from the real bounds of the real screen. Only four things float
 * on the map: the box with the two pills at the top, the location chip under it while location is not allowed, at the
 * bottom left the Options button directly above the map's own info icon, and at the bottom right the location control,
 * with the Options card right above Options while it is open. There is no list card, no recents and no near you in
 * this state. Run at 411 dp, and at 320 dp with 200 percent text.
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
    private var windowWidth = 0.dp

    private fun setScreen(fontScale: Float? = null, locationAllowed: Boolean = false) {
        val display = if (locationAllowed) MapDisplay(userLocation = LatLng(-33.8688, 151.2093)) else MapDisplay()
        val state: SearchStopState = SearchStopFixtures.recentWithHomeSet()
            .copy(isMapsAvailable = true, mapUiState = MapUiState.Ready(mapDisplay = display))
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides if (fontScale != null) Density(density.density, fontScale) else density,
                LocalInspectionMode provides true,
                LocalSoftwareKeyboardController provides quietKeyboard,
            ) {
                PreviewTheme {
                    windowHeight = LocalWindowInfo.current.containerDpSize.height
                    windowWidth = LocalWindowInfo.current.containerDpSize.width
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

    private fun DpRect.centreY(): Dp = top + height / 2f

    // region the box and the chip

    @Test
    @Config(qualifiers = W411)
    fun `the box is one rounded box one edge from the top and sides`() {
        setScreen()

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertNear("top edge", gaps.edge, box.top)
        assertNear("left edge", gaps.edge, box.left)
        assertNear("right edge", 411.dp - gaps.edge, box.right)
    }

    @Test
    @Config(qualifiers = W411)
    fun `the box holds two pills of one width, 48 tall, one padding inside and one gap apart`() {
        setScreen()

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        assertNear("To left", box.left + gaps.cardPadding, to.left)
        assertNear("To top", box.top + gaps.cardPadding, to.top)
        assertNear("pills share a left edge", to.left, from.left)
        assertNear("pills share a width", to.width, from.width)
        assertNear("To is 48 tall", 48.dp, to.height)
        assertNear("From is 48 tall", 48.dp, from.height)
        assertNear("From sits one gap under To", to.bottom + gaps.betweenRows, from.top)
        assertNear("the box ends one padding under From", from.bottom + gaps.cardPadding, box.bottom)
    }

    @Test
    @Config(qualifiers = W411)
    fun `the chip is its own button under the box, left aligned with it, one gap below and never inside it`() {
        setScreen()

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val chip = touchBoundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertNear("one gap token under the box", box.bottom + gaps.betweenCards, chip.top)
        assertNear("left aligned with the box", box.left, chip.left)
        assertTrue(chip.height >= 48.dp - 1.dp, "The chip keeps its 48 dp touch target: $chip")
        assertTrue(chip.right <= box.right, "The chip $chip stays within the box's right edge")
    }

    @Test
    @Config(qualifiers = W411)
    fun `the chip is not there while location is allowed`() {
        setScreen(locationAllowed = true)

        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL).assertCountEquals(0)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).assertExists()
    }

    // endregion

    // region a clean map

    @Test
    @Config(qualifiers = W411)
    fun `the map state has no list card, no recents, no near you and no label row`() {
        setScreen()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RESULTS_LIST).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LABEL_ROW).assertDoesNotExist()
        composeRule.onNodeWithText("Central Station").assertDoesNotExist()
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
        composeRule.onNodeWithText("Near you").assertDoesNotExist()
        composeRule.onNodeWithText("Select on map").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = W411)
    fun `the map state shows the box, the Options button and the location control`() {
        setScreen()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON).assertExists()
        composeRule.onNodeWithText("Options").assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertDoesNotExist()
    }

    // endregion

    // region the bottom left stack

    @Test
    @Config(qualifiers = W411)
    fun `at 411 dp and 100 percent text Options is one gap above the info icon, left aligned`() = assertStack(null)

    @Test
    @Config(qualifiers = W411)
    fun `at 411 dp and 150 percent text Options is one gap above the info icon, left aligned`() = assertStack(1.5f)

    @Test
    @Config(qualifiers = W411)
    fun `at 411 dp and 200 percent text Options is one gap above the info icon, left aligned`() = assertStack(2f)

    @Test
    @Config(qualifiers = W320)
    fun `at 320 dp and 100 percent text Options is one gap above the info icon, left aligned`() = assertStack(null)

    @Test
    @Config(qualifiers = W320)
    fun `at 320 dp and 150 percent text Options is one gap above the info icon, left aligned`() = assertStack(1.5f)

    @Test
    @Config(qualifiers = W320)
    fun `at 320 dp and 200 percent text Options is one gap above the info icon, left aligned`() = assertStack(2f)

    /**
     * The info icon is the library's own native view, which a host test does not draw, so its slot is worked out from the
     * ornament padding the screen passes (zero over the bottom inset, which is zero here) and the library's 8 dp margin:
     * 21 dp square, 8 dp above the bottom. Options stands 8 dp above that slot, on the same left edge as the box and the
     * visible i, and the location control is not touched.
     */
    private fun assertStack(fontScale: Float?) {
        setScreen(fontScale = fontScale)

        val padding = mapOrnamentPadding(gaps, statusBarTop = 0.dp, navigationBarBottom = 0.dp, headerHeight = 0.dp)
        assertEquals(0.dp, padding.calculateBottomPadding(), "The icon's own bottom padding is zero over the inset.")
        val iconBottom = windowHeight - padding.calculateBottomPadding() - MAP_ORNAMENT_LIBRARY_MARGIN
        val iconTop = iconBottom - MAP_ATTRIBUTION_ICON_SIZE
        val iconGlyphLeft = padding.calculateLeftPadding(LayoutDirection.Ltr) + MAP_ORNAMENT_LIBRARY_MARGIN +
            MAP_ATTRIBUTION_GLYPH_INSET
        val options = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        val location = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON)

        assertNear("Options is 8 dp above the info icon", iconTop - 8.dp, options.bottom)
        assertNear("Options left edge is the visible i's left edge", iconGlyphLeft, options.left)
        assertNear("and the box's left edge", boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).left, options.left)
        val touch = touchBoundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        assertTrue(touch.height >= 47.dp && touch.width >= 47.dp, "Options keeps its 48 dp finger target: $touch")
        assertTrue(options.bottom <= iconTop, "Options $options does not reach the icon's slot, which starts at $iconTop")
        assertNear("the location control is 48 wide", 48.dp, location.width)
        assertNear("the location control ends one edge in", windowWidth - gaps.edge, location.right)
        assertNear("the location control stays one edge above the bottom", windowHeight - gaps.edge, location.bottom)
        assertTrue(
            options.right <= location.left || options.bottom <= location.top,
            "Options $options never overlaps the location control $location",
        )
        assertNear("the control is 8 dp above the icon's bottom", iconBottom - 8.dp, location.bottom)
    }

    // endregion

    // region the Options card

    @Test
    @Config(qualifiers = W411)
    fun `the open Options card is 8 dp above Options, on the box's edges, and the map is still clean`() {
        setScreen()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.waitForIdle()

        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
        val options = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertNear("8 dp between the card and Options", options.top - 8.dp, card.bottom)
        assertEquals(8.dp, gaps.betweenCards)
        assertNear("card left edge", box.left, card.left)
        assertNear("card right edge", box.right, card.right)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertDoesNotExist()
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = W320)
    fun `at 320 dp and 200 percent text the mode chips wrap whole and never scroll sideways`() {
        setScreen(fontScale = 2f)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.waitForIdle()

        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
        val modes = NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS)
            .map { TripPlannerTestTags.searchStopModeChip(NswTransportConfig.productClassFor(it)) }
        val chips = modes.map { boundsOf(it) }
        assertTrue(chips.map { it.top }.distinct().size > 1, "At 200 percent the chips wrap onto more than one line.")
        for ((tag, chip) in modes.zip(chips)) {
            assertTrue(
                chip.left >= card.left - 1.dp && chip.right <= card.right + 1.dp,
                "$tag $chip stays whole inside the card $card",
            )
        }
        composeRule.onAllNodes(
            hasScrollAction() and hasAnyAncestor(hasTestTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)),
            useUnmergedTree = true,
        ).assertCountEquals(0)
        val options = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        assertNear("8 dp above Options", options.top - 8.dp, card.bottom)
        assertTrue(card.top > boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).bottom, "The card stays under the box.")
    }

    @Test
    @Config(qualifiers = W411)
    fun `the Options button opens and shuts the card, and leaving to the list state puts both away`() {
        setScreen()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertDoesNotExist()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertDoesNotExist()
    }

    // endregion

    // region 320 dp at 200 percent

    @Test
    @Config(qualifiers = W320)
    fun `at 320 dp and 200 percent text nothing overlaps and nothing leaves the window`() {
        setScreen(fontScale = 2f)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.waitForIdle()

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val chip = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
        val options = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        val location = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON)
        assertTrue(chip.top >= box.bottom, "The chip $chip is under the box $box")
        assertTrue(box.bottom <= card.top, "The box $box is above the Options card $card")
        assertTrue(card.bottom <= options.top + 1.dp, "The card $card is above its button $options")
        for (item in listOf(box, chip, card, options, location)) {
            assertTrue(item.left >= 0.dp && item.right <= 320.dp, "$item stays inside the 320 dp window")
            assertTrue(item.top >= 0.dp && item.bottom <= windowHeight, "$item stays inside the window height")
        }
        assertNotEquals(0.dp, options.width, "Options is drawn.")
        assertEquals(48.dp, boundsOf(TripPlannerTestTags.SEARCH_ROW_TO).height, "The pill stays one 48 dp line.")
    }

    // endregion

    private companion object {
        const val W411 = "w411dp-h891dp"
        const val W320 = "w320dp-h640dp"
    }
}
