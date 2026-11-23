package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Search Stop's floating surfaces on a display with a side safe area, read from the real screen with a real horizontal inset
 * dispatched to it: the outer screen of a foldable iPhone, where a status pill runs down the right edge, and a camera cutout on
 * both sides. The box with From and To, its swap and go controls, the location notice, the list state and the Options card
 * must stand wholly inside the window the inset leaves, and a touch on the swap or the go circle must reach it. Where a side
 * system element is drawn the system takes the touch, so a control under it cannot be pressed at all.
 *
 * The iOS shape is a side inset that only `safeDrawing` carries: the display cutout, the status bars and the navigation bars are all
 * zero at the sides, which is what Compose on iOS reports on that screen. On Android the one system bar type no other padding call
 * on the screen reads is the caption bar, so the inset is given as that: a screen that pads by the cutout, by the status bar or by
 * the navigation bar alone would leave it unpadded, exactly as it does on the iPhone. The map is not padded: it stays the full
 * window, which the map test already reads.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class SearchStopSideInsetLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val quietKeyboard = object : SoftwareKeyboardController {
        override fun show() = Unit
        override fun hide() = Unit
    }
    private val gaps = searchStopGaps()
    private val central = StopItem(stopName = "Central Station", stopId = "stop_central")
    private val townHall = StopItem(stopName = "Town Hall", stopId = "stop_town_hall")

    private var tripsChosen = 0
    private var systemTaps = 0
    private var lastSystemTap: Offset? = null

    /**
     * The screen with both stops held, so the swap and the go circle are drawn. With [systemColumn] a strip at the right edge,
     * [SIDE_DP] wide and [PILL_HEIGHT_DP] tall, stands over the screen and takes every touch, as the status pill does.
     */
    private fun setScreen(locationAllowed: Boolean, systemColumn: Boolean = false) {
        val display = if (locationAllowed) MapDisplay(userLocation = LatLng(-33.8688, 151.2093)) else MapDisplay()
        val state = SearchStopFixtures.recentWithHomeSet()
            .copy(isMapsAvailable = true, mapUiState = MapUiState.Ready(mapDisplay = display))
        // The app draws edge to edge and handles its own insets. Left to the default the window pads its content for the system
        // bars, so the screen would be handed a window already shrunk and the test would pass whatever the screen does with insets.
        composeRule.runOnUiThread { WindowCompat.setDecorFitsSystemWindows(composeRule.activity.window, false) }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalSoftwareKeyboardController provides quietKeyboard,
            ) {
                BareTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SearchStopScreen(
                            searchStopState = state,
                            presetFrom = central,
                            presetTo = townHall,
                            onTripChosen = { _, _ -> tripsChosen++ },
                            onEvent = {},
                        )
                        if (systemColumn) SystemColumn()
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Hands the window the insets the way the system does, and keeps handing them: the system dispatches again whenever the
     * layout or the focus changes, so the insets are held on the decor view and every dispatch carries them, and the screen is
     * never left on zero insets after a tap. Then it lets the screen settle on them.
     */
    private fun applyInsets(left: Int = 0, right: Int = 0, kind: InsetKind) {
        val density = composeRule.density
        val l = with(density) { left.dp.roundToPx() }
        val r = with(density) { right.dp.roundToPx() }
        val builder = WindowInsetsCompat.Builder()
        when (kind) {
            InsetKind.SystemBar -> builder.setInsets(WindowInsetsCompat.Type.captionBar(), Insets.of(l, 0, r, 0))
            InsetKind.Cutout -> builder.setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(l, 0, r, 0))
        }
        val insets = builder.build()
        composeRule.runOnUiThread {
            val decor = composeRule.activity.window.decorView
            ViewCompat.setOnApplyWindowInsetsListener(decor) { _, _ -> insets }
            ViewCompat.dispatchApplyWindowInsets(decor, insets)
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getVisibleBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(abs((expected - actual).value) <= 1f, "$what: expected $expected, got $actual")
    }

    /** [tag] is wholly inside the window the inset leaves, from [left] to [WINDOW_DP] less [right]. */
    private fun assertInside(what: String, tag: String, left: Int, right: Int) {
        val b = boundsOf(tag)
        assertTrue(
            b.left >= left.dp - 1.dp,
            "$what starts at ${b.left}, inside the ${left}dp the system keeps on the left",
        )
        assertTrue(
            b.right <= (WINDOW_DP - right).dp + 1.dp,
            "$what ends at ${b.right}, past ${WINDOW_DP - right}dp where the system's ${right}dp on the right starts",
        )
    }

    private fun rowHas(rowTag: String, text: String) = composeRule.onAllNodes(
        hasText(text, substring = true) and hasAnyAncestor(hasTestTag(rowTag)),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().isNotEmpty()

    // region the iOS shape: a side system bar on the right, no cutout

    @Test
    fun `with a side system bar on the right the box, its rows and its controls stand inside the window`() {
        setScreen(locationAllowed = false)
        applyInsets(right = SIDE_DP, kind = InsetKind.SystemBar)

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertNear("the box's left edge", gaps.edge, box.left)
        assertNear(
            "the box's right edge is one edge in from the system's side",
            (WINDOW_DP - SIDE_DP).dp - gaps.edge,
            box.right,
        )
        for (tag in listOf(
            TripPlannerTestTags.SEARCH_ROW_FROM,
            TripPlannerTestTags.SEARCH_ROW_TO,
            TripPlannerTestTags.SEARCH_ROW_REVERSE,
            TripPlannerTestTags.SEARCH_ROW_SEARCH,
        )) {
            assertInside(tag, tag, left = 0, right = SIDE_DP)
        }
    }

    @Test
    fun `with a side system bar on the right the location notice and its buttons stand inside the window`() {
        setScreen(locationAllowed = false)
        applyInsets(right = SIDE_DP, kind = InsetKind.SystemBar)

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val notice = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertNear("the notice keeps the box's left edge", box.left, notice.left)
        assertNear("the notice keeps the box's right edge", box.right, notice.right)
        assertInside("the notice", TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, left = 0, right = SIDE_DP)
        composeRule.onNodeWithText("Not now", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Allow", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `a touch on the swap and on the go circle reaches them and not the system column over the right edge`() {
        setScreen(locationAllowed = false, systemColumn = true)
        applyInsets(right = SIDE_DP, kind = InsetKind.SystemBar)
        assertTrue(rowHas(TripPlannerTestTags.SEARCH_ROW_FROM, "Central Station"), "From holds Central Station")
        assertTrue(rowHas(TripPlannerTestTags.SEARCH_ROW_TO, "Town Hall"), "To holds Town Hall")

        touchCentreOf(TripPlannerTestTags.SEARCH_ROW_REVERSE)
        assertEquals(
            0,
            systemTaps,
            "the swap was pressed, so the system column was not (it got a tap at $lastSystemTap)",
        )
        assertTrue(rowHas(TripPlannerTestTags.SEARCH_ROW_FROM, "Town Hall"), "the swap put Town Hall in From")
        assertTrue(rowHas(TripPlannerTestTags.SEARCH_ROW_TO, "Central Station"), "and Central Station in To")

        touchCentreOf(TripPlannerTestTags.SEARCH_ROW_SEARCH)
        assertEquals(
            0,
            systemTaps,
            "the go circle was pressed, so the system column was not (it got a tap at $lastSystemTap)",
        )
        assertEquals(1, tripsChosen, "the go circle sent the trip")
    }

    /** A finger on the middle of [tag], at the window's own coordinates, so whatever stands over that point gets the touch. */
    private fun touchCentreOf(tag: String) {
        val b = boundsOf(tag)
        val density = composeRule.density
        val point = with(density) { Offset(((b.left + b.right) / 2f).toPx(), ((b.top + b.bottom) / 2f).toPx()) }
        composeRule.onRoot().performTouchInput { click(point) }
        composeRule.waitForIdle()
    }

    @Test
    fun `in the list state the box and the list stand inside the window`() {
        setScreen(locationAllowed = false)
        applyInsets(right = SIDE_DP, kind = InsetKind.SystemBar)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).performClick()
        composeRule.waitForIdle()

        assertInside("the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, left = 0, right = SIDE_DP)
        assertInside("the recents list", TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST, left = 0, right = SIDE_DP)
        assertInside("Select on map", TripPlannerTestTags.SEARCH_STOP_SELECT_ON_MAP, left = 0, right = SIDE_DP)
    }

    @Test
    fun `with a side system bar on the left Options and its open card stand inside the window`() {
        setScreen(locationAllowed = true)
        applyInsets(left = SIDE_DP, kind = InsetKind.SystemBar)

        assertInside("the Options button", TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON, left = SIDE_DP, right = 0)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.waitForIdle()

        assertInside("the Options card", TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS, left = SIDE_DP, right = 0)
    }

    // endregion

    // region a camera cutout on both sides

    @Test
    fun `with a cutout on both sides the box and the notice keep inside it`() {
        setScreen(locationAllowed = false)
        applyInsets(left = CUTOUT_DP, right = CUTOUT_DP, kind = InsetKind.Cutout)

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertNear("the box's left edge", CUTOUT_DP.dp + gaps.edge, box.left)
        assertNear("the box's right edge", (WINDOW_DP - CUTOUT_DP).dp - gaps.edge, box.right)
        assertInside("the notice", TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, left = CUTOUT_DP, right = CUTOUT_DP)
        assertInside("the swap", TripPlannerTestTags.SEARCH_ROW_REVERSE, left = CUTOUT_DP, right = CUTOUT_DP)
        assertInside("the go circle", TripPlannerTestTags.SEARCH_ROW_SEARCH, left = CUTOUT_DP, right = CUTOUT_DP)
    }

    // endregion

    // region no inset

    @Test
    fun `with no side inset the box is the one edge from both sides, as before`() {
        setScreen(locationAllowed = false)

        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        assertNear("left", gaps.edge, box.left)
        assertNear("right", WINDOW_DP.dp - gaps.edge, box.right)
    }

    // endregion

    /** The strip the system draws over the edge of the display: [SIDE_DP] wide, [PILL_HEIGHT_DP] tall, and it takes every touch. */
    @Composable
    private fun BoxScope.SystemColumn() {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(SIDE_DP.dp)
                .height(PILL_HEIGHT_DP.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        systemTaps++
                        lastSystemTap = offset
                    }
                },
        )
    }

    /**
     * The app's theme and nothing else. `PreviewTheme` pads for the system bars itself, which would hand the screen insets that are
     * already used up, so a screen that ignores them would pass.
     */
    @Composable
    private fun BareTheme(content: @Composable () -> Unit) {
        KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
            CompositionLocalProvider(
                LocalThemeColor provides remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) },
            ) {
                content()
            }
        }
    }

    private enum class InsetKind { SystemBar, Cutout }

    private companion object {
        const val WINDOW_DP = 411

        /** The width the system keeps on the right of the outer screen of a foldable iPhone, which the status pill stands in. */
        const val SIDE_DP = 84
        const val CUTOUT_DP = 48
        const val PILL_HEIGHT_DP = 200
    }
}
