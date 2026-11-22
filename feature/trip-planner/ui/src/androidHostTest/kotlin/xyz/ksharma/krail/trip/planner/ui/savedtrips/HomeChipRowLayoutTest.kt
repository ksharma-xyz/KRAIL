package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Rule 41 and the chip row of `docs/MAP_HOME_UX.md` on the real home screen: the chips sit above
 * the search row on the cards' edges, each is 48 dp and a whole pill with no chevron, a short row
 * wraps whole chips, over 130 percent text it scrolls sideways with the first chip fully visible, a
 * long press on a set chip opens Manage and a tap plans.
 *
 * Each test runs in a window of its own width (density 1, so a dp is a pixel) and reads where the
 * real nodes landed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [CHIP_TEST_SDK], manifest = Config.NONE)
class HomeChipRowLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    // region above the search row, on the cards' edges

    @Test
    @Config(qualifiers = PHONE)
    fun `the chips sit directly above the search row with 8 dp between`() {
        setScreen()

        val row = bounds(TripPlannerTestTags.SEARCH_ROW)
        val chips = allChips().map(::bounds)

        assertTrue(chips.all { it.bottom <= row.top }, "The chips $chips must be above the search row $row.")
        assertNear("gap between the last line of chips and the search row", 8.dp, row.top - chips.maxOf { it.bottom })
        assertNear("chip row meets the search row", row.top, bounds(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW).bottom)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `on a phone the chips start and end inside the cards' edges`() = assertChipEdges(window = 411.dp, pane = 411.dp)

    @Test
    @Config(qualifiers = "w700dp-h1000dp")
    fun `in a window past the cap the chips stay in the 560 dp column`() = assertChipEdges(
        window = 700.dp,
        pane = 700.dp,
    )

    @Test
    @Config(qualifiers = "w1024dp-h768dp")
    fun `in the dual pane the chips share the 480 dp list pane's edges`() =
        assertChipEdges(window = 1024.dp, pane = DUAL_PANE_LIST_WIDTH)

    private fun assertChipEdges(window: Dp, pane: Dp) {
        setScreen()
        val column = columnWidthFor(pane)
        val columnLeft = (pane - column) / 2
        val cardLeft = columnLeft + PAGE_PADDING
        val cardRight = columnLeft + column - PAGE_PADDING
        val at = "in a $window window (pane $pane)"

        val rowBounds = bounds(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW)
        assertNear("chip row left edge $at", columnLeft, rowBounds.left)
        assertNear("chip row right edge $at", columnLeft + column, rowBounds.right)
        assertNear("search row left edge $at", columnLeft, bounds(TripPlannerTestTags.SEARCH_ROW).left)

        val chips = allChips().map(::bounds)
        assertNear("first chip left edge $at", cardLeft, chips.minOf { it.left })
        assertTrue(chips.all { it.right <= cardRight + 1.dp }, "Every chip should end by $cardRight $at: $chips")
        val card = bounds(TripPlannerTestTags.savedTripCard(TRIP.tripId))
        assertNear("chips and cards share the left edge $at", card.left, chips.minOf { it.left })
    }

    // endregion

    // region a whole pill, 48 dp, no chevron

    @Test
    @Config(qualifiers = PHONE)
    fun `every chip is 48 dp tall`() {
        setScreen()

        allChips().forEach { tag -> assertNear("height of $tag", 48.dp, bounds(tag).height) }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a chip is its icon, its text and its padding and nothing else, so it has no chevron`() {
        setScreen()

        // 12 dp either side, a 16 dp icon and the 4 dp gap after it, then the text.
        val setHome = bounds(TripPlannerTestTags.homeLabelChip("Home"))
        assertNear("Home chip width", 12.dp + 16.dp + 4.dp + textBounds("Home").width + 12.dp, setHome.width)
        val setWork = bounds(TripPlannerTestTags.homeLabelChip("Work"))
        assertNear("Set Work chip width", 12.dp + 16.dp + 4.dp + textBounds("Set Work").width + 12.dp, setWork.width)
        val parkRide = bounds(TripPlannerTestTags.SAVED_TRIPS_CHIP_PARK_RIDE)
        assertNear("Park & Ride chip width", 12.dp + textBounds("+ Park & Ride").width + 12.dp, parkRide.width)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a set label keeps its chip and reads Home, an unset one reads Set Work`() {
        setScreen()

        composeRule.onNodeWithText("Home").assertExists()
        composeRule.onNodeWithText("Set Work").assertExists()
        composeRule.onNodeWithText("+ Park & Ride").assertExists()
        composeRule.onAllNodesWithTag(TripPlannerTestTags.homeLabelChip("Home")).assertCountEquals(1)
    }

    // endregion

    // region wrapping and scrolling

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 100 percent whole chips wrap onto the next line`() {
        setScreen()

        assertWholeChips(fontScale = 1f)
        assertTrue(lineTops().size >= 2, "Three chips do not fit one 288 dp line, so one should wrap: ${lineTops()}")
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at exactly 130 percent the chips still wrap whole`() {
        setScreen(fontScale = 1.3f)

        assertWholeChips(fontScale = 1.3f)
        assertTrue(lineTops().size >= 2, "At 130 percent the row still wraps, it does not scroll: ${lineTops()}")
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent the chips are one line that scrolls with the first chip fully visible`() {
        setScreen(fontScale = 2f)

        val chips = allChips().map(::bounds)
        val rowLeft = PAGE_PADDING
        val rowRight = 320.dp - PAGE_PADDING

        assertEquals(1, lineTops().size, "Over 130 percent the chips are one sideways line: $chips")
        assertNear("first chip left edge", rowLeft, chips.first().left)
        assertTrue(chips.first().right <= rowRight, "The first chip ${chips.first()} must be fully visible.")
        assertTrue(chips.last().right > rowRight, "There should be more chips off the edge: $chips")
        chips.forEach { assertTrue(it.height >= 48.dp, "A chip is never under 48 dp: $it") }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `on a phone at 200 percent the chips scroll too and none is squeezed`() {
        setScreen(fontScale = 2f)

        val chips = allChips().map(::bounds)
        assertEquals(1, lineTops().size, "Over 130 percent the chips are one sideways line: $chips")
        assertTrue(
            chips.zipWithNext().all { (a, b) -> a.right <= b.left },
            "Chips must not overlap each other: $chips",
        )
    }

    private fun assertWholeChips(fontScale: Float) {
        val chips = allChips().map(::bounds)
        val rowLeft = PAGE_PADDING
        val rowRight = 320.dp - PAGE_PADDING
        val at = "at 320 dp and $fontScale text"

        chips.forEach { assertTrue(it.left >= rowLeft - 1.dp && it.right <= rowRight + 1.dp, "A chip is cut $at: $it") }
        chips.forEachIndexed { i, a ->
            chips.drop(i + 1).forEach { b ->
                assertTrue(
                    a.right <= b.left + 1.dp || b.right <= a.left + 1.dp || a.bottom <= b.top || b.bottom <= a.top,
                    "Two chips overlap $at: $a and $b",
                )
            }
        }
        chips.filter { it.top > chips.minOf { c -> c.top } }.forEach {
            assertTrue(
                abs(it.left.value - rowLeft.value) < 1f || chips.any { c -> c.top == it.top && c.left < it.left },
                "A wrapped chip starts a line at the left edge or follows a chip on its line $at: $it",
            )
        }
    }

    private fun lineTops(): Set<Float> = allChips().map { bounds(it).top.value }.toSet()

    // endregion

    // region taps and long presses

    @Test
    @Config(qualifiers = PHONE)
    fun `a tap on a set chip plans, a tap on an unset chip sets it, both go to the same callback with the label`() {
        val clicked = mutableListOf<String>()
        setScreen(onChipClick = { clicked += it })

        composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip("Home")).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip("Work")).performClick()

        assertEquals(listOf("Home", "Work"), clicked)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a long press on a set chip opens Manage and does not plan`() {
        var manage = 0
        val clicked = mutableListOf<String>()
        setScreen(onChipClick = { clicked += it }, onChipLongClick = { manage++ })

        composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip("Home")).performTouchInput { longClick() }

        assertEquals(1, manage, "A long press on a set chip opens Manage stop labels.")
        assertEquals(emptyList(), clicked, "A long press is not also a tap.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a long press on an unset chip or the Park & Ride chip opens nothing`() {
        var manage = 0
        setScreen(onChipLongClick = { manage++ })

        composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip("Work")).performTouchInput { longClick() }
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_PARK_RIDE).performTouchInput { longClick() }

        assertEquals(0, manage)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the Park & Ride chip opens the Add Park & Ride entry`() {
        var added = 0
        setScreen(onAddParkRide = { added++ })

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_PARK_RIDE).performClick()

        assertEquals(1, added)
    }

    // endregion

    // region when there are no chips

    @Test
    @Config(qualifiers = PHONE)
    fun `no chips show until the labels have loaded`() {
        setScreen(labels = persistentListOf())

        composeRule.onAllNodesWithTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW).assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `no chips show while the saved trips are still loading`() {
        setScreen(loading = true)

        composeRule.onAllNodesWithTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW).assertCountEquals(0)
    }

    // endregion

    // region harness

    private fun setScreen(
        fontScale: Float = 1f,
        labels: kotlinx.collections.immutable.ImmutableList<StopLabel> = LABELS,
        loading: Boolean = false,
        onChipClick: (String) -> Unit = {},
        onChipLongClick: () -> Unit = {},
        onAddParkRide: () -> Unit = {},
    ) {
        composeRule.setContent {
            WithFontScale(fontScale) {
                PreviewTheme {
                    SavedTripsScreen(
                        savedTripsState = SavedTripsState(
                            isSavedTripsLoading = loading,
                            savedTrips = persistentListOf(TRIP),
                            stopLabels = labels,
                        ),
                        onLabelChipClick = onChipClick,
                        onLabelChipLongClick = onChipLongClick,
                        onAddParkRideClick = onAddParkRide,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
            content = content,
        )
    }

    private fun allChips(): List<String> = listOf(
        TripPlannerTestTags.homeLabelChip("Home"),
        TripPlannerTestTags.homeLabelChip("Work"),
        TripPlannerTestTags.SAVED_TRIPS_CHIP_PARK_RIDE,
    )

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE_DP,
            "$what should be at $expected but is at $actual.",
        )
    }

    // endregion

    private companion object {
        const val PHONE = "w411dp-h891dp"
        const val NARROW = "w320dp-h1280dp"
        const val TOLERANCE_DP = 1f
        val PAGE_PADDING = 16.dp

        val TRIP = Trip(
            fromStopId = "200060",
            fromStopName = "Central Station",
            toStopId = "200070",
            toStopName = "Town Hall Station",
        )

        /**
         * Home set, Work not: the one state that shows both chip kinds. Home is a stop the trip
         * does not use, so its name is on the chip and not also on the saved trip card.
         */
        val LABELS = persistentListOf(
            StopLabel(emoji = "H", label = "Home", stopId = "200080", stopName = "Wynyard Station"),
            StopLabel(emoji = "W", label = "Work"),
        )
    }
}

private const val CHIP_TEST_SDK = 34
