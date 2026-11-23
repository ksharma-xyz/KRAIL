package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.adaptiveui.DUAL_PANE_LIST_WIDTH
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.flow.RecordingTripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelChipTap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Saved Trips screen as the classic home: the theme coloured sheet `main` drew the search row in is pinned at the
 * bottom and holds, from the top, the Home and Work pills, an 8 dp gap and the one search bar with the microphone
 * inside it. The list is main's list with no pills in it, and the map, the Park & Ride tiles, the notices and the
 * suggestion are not on the screen.
 *
 * Each test runs in a window of its own size (density 1, so a dp is a pixel) and reads where the real nodes
 * landed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [HOME_TEST_SDK], manifest = Config.NONE)
class SavedTripsHomeLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = RecordingTripPlannerNavigator()
    private val events = mutableListOf<SavedTripUiEvent>()
    private val aiEvents = mutableListOf<AiSearchInputEvent>()
    private var toClicks = 0

    // region the pills, in the sheet above the bar

    @Test
    @Config(qualifiers = PHONE)
    fun `the pills are in the sheet directly above the bar, 8 dp apart, on the sheet's own side inset`() {
        setScreen()

        val sheet = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET)
        val pills = bounds(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS)
        val bar = bounds(TripPlannerTestTags.SEARCH_ROW_TO)

        assertNear(
            "the pill row starts 28 dp under the sheet's top, as the bar did",
            SHEET_TOP_PADDING,
            pills.top - sheet.top,
        )
        assertNear("the pill row is one 48 dp row", 48.dp, pills.height)
        assertNear("the gap from the pill row to the bar", PILLS_GAP, bar.top - pills.bottom)
        assertNear("the pill row starts on the page inset", PAGE_PADDING, pills.left)
        assertNear("the pill row ends on the page inset", PHONE_WIDTH - PAGE_PADDING, pills.right)
        assertNear("the bar is as wide as the pill row", pills.width, bar.width)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the pills are not in the list, and the list is main's, its first row the Saved Trips title`() {
        setScreen()

        composeRule.onNode(
            hasTestTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS) and
                hasAnyAncestor(hasTestTag(TripPlannerTestTags.SAVED_TRIPS_LIST)),
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNode(
            hasTestTag(TripPlannerTestTags.homeLabelChip("Home")) and
                hasAnyAncestor(hasTestTag(TripPlannerTestTags.SAVED_TRIPS_LIST)),
            useUnmergedTree = true,
        ).assertDoesNotExist()
        val list = bounds(TripPlannerTestTags.SAVED_TRIPS_LIST)
        val title = textBounds("Saved Trips")
        assertTrue(
            title.top >= list.top && title.top < list.top + 48.dp,
            "The list opens with its title $title in $list.",
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the pills stay in the sheet while the list scrolls and the title bar stays`() {
        setScreen(trips = manyTrips())
        val before = bounds(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS)

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST).performScrollToIndex(MANY - 1)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertIsDisplayed()
        assertNear("the pills did not move", before.top, bounds(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).top)
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `while the cards are reordered the sheet goes, pills with it, and the list keeps its end clearance`() {
        setScreen(trips = manyTrips())
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertIsDisplayed()
        val sheetHeight = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET).height

        composeRule.onNodeWithTag(TripPlannerTestTags.savedTripCard(manyTrips().first().tripId))
            .performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET).assertDoesNotExist()
        scrollToTheEnd()
        assertTrue(
            textBounds(PARK_RIDE_EMPTY_LINE).bottom <= PHONE_HEIGHT - sheetHeight,
            "The end of the list keeps the $sheetHeight the sheet had, so it does not jump when reorder starts.",
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `both pills fit on one row at 411 dp and 100 percent`() = assertBothFit(fontScale = 1f)

    @Test
    @Config(qualifiers = PHONE)
    fun `both pills fit on one row at 411 dp and 150 percent`() = assertBothFit(fontScale = 1.5f)

    private fun assertBothFit(fontScale: Float) {
        setScreen(fontScale = fontScale, labels = UNSET_LABELS)

        val home = bounds(TripPlannerTestTags.homeLabelChip("Home"))
        val work = bounds(TripPlannerTestTags.homeLabelChip("Work"))
        assertNear("Home starts on the page inset", PAGE_PADDING, home.left)
        assertNear("Work is on the same row", home.top, work.top)
        assertTrue(home.right < work.left, "Work follows Home: $home $work")
        assertTrue(work.right <= PHONE_WIDTH - PAGE_PADDING + 1.dp, "Work ends inside the end inset: $work")
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `when the pills do not fit one line the whole pill wraps to the next line and nothing scrolls sideways`() {
        // Three whole pills at 200 percent text cannot share a 320 dp line, so the last one goes onto the next.
        setPills(fontScale = 2f, pills = listOf(unset("Home"), unset("Work"), unset("Gym")))

        val home = bounds(TripPlannerTestTags.homeLabelChip("Home"))
        val work = bounds(TripPlannerTestTags.homeLabelChip("Work"))
        val gym = bounds(TripPlannerTestTags.homeLabelChip("Gym"))
        assertNear("Work stays on Home's line", home.top, work.top)
        assertTrue(gym.top >= home.bottom, "Gym wraps onto a line of its own: $home $gym")
        assertNear("the wrapped pill starts at the page inset", PAGE_PADDING, gym.left)
        listOf(home, work, gym).forEach { pill ->
            assertTrue(
                pill.left >= PAGE_PADDING - 1.dp && pill.right <= NARROW_WIDTH - PAGE_PADDING + 1.dp,
                "$pill is whole in the row",
            )
        }
        listOf("Home", "Work", "Gym").forEach { name -> assertEquals(1, pillLineCount(name), "$name is one line") }
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `a pill wider than the row stays one line and ends in an ellipsis, for a set pill and an unset one`() {
        setPills(fontScale = 2f, pills = listOf(unset(LONG_NAME), set(LONG_SET_NAME)))

        listOf(LONG_NAME, LONG_SET_NAME).forEach { name ->
            val pill = bounds(TripPlannerTestTags.homeLabelChip(name))
            assertTrue(
                pill.left >= PAGE_PADDING - 1.dp && pill.right <= NARROW_WIDTH - PAGE_PADDING + 1.dp,
                "The pill for $name is not wider than the row: $pill",
            )
            assertEquals(1, pillLineCount(name), "$name is one line")
            assertTrue(pillTextOverflows(name), "$name ends in an ellipsis")
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `every pill has a 48 dp touch target and draws smaller than that`() {
        setScreen()

        listOf("Home", "Work").map(TripPlannerTestTags::homeLabelChip).forEach { tag ->
            assertNear("touch height of $tag", 48.dp, touchBounds(tag).height)
            assertTrue(bounds(tag).height < 48.dp, "taj's small button draws less than 48 dp: ${bounds(tag)}")
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a set label is the near black pill reading Home, an unset one is the outlined button reading plus Work`() {
        setScreen()

        // A trip card that starts at the Home stop draws the label too, so the text is found inside the pill.
        fun inPill(label: String, text: String) =
            hasText(text) and hasAnyAncestor(hasTestTag(TripPlannerTestTags.homeLabelChip(label)))
        composeRule.onNode(inPill("Home", "Home"), useUnmergedTree = true).assertExists()
        composeRule.onNode(inPill("Work", "+ Work"), useUnmergedTree = true).assertExists()
        composeRule.onNode(hasText("Set Work"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a set pill and an unset pill are the same height, so the row does not jump when a label is saved`() {
        setScreen(labels = LABELS)
        val unsetHeight = bounds(TripPlannerTestTags.homeLabelChip("Work")).height
        val setHeight = bounds(TripPlannerTestTags.homeLabelChip("Home")).height

        assertNear("set and unset pill heights", setHeight, unsetHeight)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `no pills show until the labels have loaded`() {
        setScreen(labels = persistentListOf())
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertDoesNotExist()
    }

    // endregion

    // region the one search bar

    @Test
    @Config(qualifiers = PHONE)
    fun `the bar is one 48 dp row, full width less the page insets, 20 dp above the bottom of the sheet`() {
        setScreen()

        val bar = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("bar height", 48.dp, bar.height)
        assertNear("bar left inset", PAGE_PADDING, bar.left)
        assertNear("bar width", PHONE_WIDTH - PAGE_PADDING * 2, bar.width)
        assertNear("bar bottom", PHONE_HEIGHT - SHEET_BOTTOM_PADDING, bar.bottom)
        val text = textBounds("Where are you going?")
        assertTrue(text.top >= bar.top && text.bottom <= bar.bottom, "The placeholder is one line inside the bar.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the bar has no From and To rows and no collapsed Plan a trip button`() {
        setScreen()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_COLLAPSED_PILL).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp")
    fun `phone landscape keeps the same one row bar`() {
        setScreen()

        assertNear("bar height in landscape", 48.dp, bounds(TripPlannerTestTags.SEARCH_ROW_TO).height)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_COLLAPSED_PILL).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `where Ask KRAIL is allowed the plain microphone sits inside the bar at its end`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = true))

        val bar = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val mic = bounds(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL)
        assertNear("microphone width", 48.dp, mic.width)
        assertNear("microphone height", 48.dp, mic.height)
        assertNear("microphone is flush with the bar's end", bar.right, mic.right)
        assertNear("microphone fills the bar's height", bar.top, mic.top)
        assertTrue(textBounds("Where are you going?").right <= mic.left, "The text stops short of the microphone.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `without Ask KRAIL the bar has no microphone`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = false))
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a device that cannot run Ask KRAIL gets no microphone either`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = true, isDeviceCapable = false))
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent the bar is still one 48 dp row with the microphone whole`() {
        setScreen(fontScale = 2f, ai = AiSearchInputUiState(isFeatureEnabled = true))

        val bar = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val mic = bounds(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL)
        assertNear("bar height at 200 percent", 48.dp, bar.height)
        assertTrue(mic.left >= bar.left && mic.right <= bar.right + 1.dp, "The microphone $mic is inside $bar.")
    }

    // endregion

    // region the sheet

    @Test
    @Config(qualifiers = PHONE)
    fun `the sheet is 152 dp, 28 above the pills, the 48 dp pill row, 8, the 48 dp bar and 20 below`() {
        setScreen()

        val sheet = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET)
        val bar = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("sheet height", SHEET_HEIGHT_WITH_PILLS, sheet.height)
        assertNear("sheet left", 0.dp, sheet.left)
        assertNear("sheet width", PHONE_WIDTH, sheet.width)
        assertNear("sheet bottom", PHONE_HEIGHT, sheet.bottom)
        assertNear("space below the bar", SHEET_BOTTOM_PADDING, sheet.bottom - bar.bottom)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `until the labels have loaded the sheet is main's 96 dp with the bar 28 dp from its top and no gap above it`() {
        setScreen(labels = persistentListOf())

        val sheet = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET)
        val bar = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("sheet height", SHEET_HEIGHT, sheet.height)
        assertNear("space above the bar", SHEET_TOP_PADDING, bar.top - sheet.top)
        assertNear("space below the bar", SHEET_BOTTOM_PADDING, sheet.bottom - bar.bottom)
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent the sheet is as tall as its parts, however many lines the pills take`() {
        setScreen(fontScale = 2f)

        val sheet = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET)
        val pills = bounds(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS)
        val bar = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("the bar is still 48 dp", 48.dp, bar.height)
        assertNear(
            "the sheet is its padding, the pills, the gap and the bar",
            SHEET_TOP_PADDING + pills.height + PILLS_GAP + 48.dp + SHEET_BOTTOM_PADDING,
            sheet.height,
        )
        assertNear("the gap from the pills to the bar", PILLS_GAP, bar.top - pills.bottom)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the end of the list clears the sheet at 100 percent, so the last card scrolls fully into view`() {
        setScreen(trips = manyTrips())

        assertLastRowClearsSheet()
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `the end of the list clears the sheet at 320 dp and 200 percent`() {
        setScreen(fontScale = 2f, trips = manyTrips(), ai = AiSearchInputUiState(isFeatureEnabled = true))

        assertLastRowClearsSheet()
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `the end of the list clears the sheet at 320 dp and 200 percent when the pills have not loaded`() {
        setScreen(fontScale = 2f, trips = manyTrips(), labels = persistentListOf())

        assertLastRowClearsSheet()
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `when the text grows past what the pills fit on one line the sheet grows and the end of the list follows`() {
        val fontScale = mutableFloatStateOf(1f)
        setScreen(trips = manyTrips(), fontScaleState = fontScale)
        val sheetAt100 = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET).height
        assertLastRowClearsSheet()

        fontScale.floatValue = LARGEST_TEXT
        composeRule.waitForIdle()
        val sheetAtLargest = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET).height

        assertTrue(
            sheetAtLargest >= sheetAt100 + 48.dp,
            "The pills wrap onto a second line at $LARGEST_TEXT times text: the sheet is $sheetAtLargest, it was $sheetAt100.",
        )
        assertLastRowClearsSheet()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `when the labels load after the sheet is up the sheet grows by the pill row and the end of the list follows`() {
        val labels = mutableStateOf<ImmutableList<StopLabel>>(persistentListOf())
        setScreen(trips = manyTrips(), labelsState = labels)
        assertNear(
            "sheet height before the labels",
            SHEET_HEIGHT,
            bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET).height,
        )
        assertLastRowClearsSheet()

        labels.value = LABELS
        composeRule.waitForIdle()

        assertNear(
            "sheet height with the pills",
            SHEET_HEIGHT_WITH_PILLS,
            bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET).height,
        )
        assertLastRowClearsSheet()
    }

    private fun scrollToTheEnd() {
        val list = composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST)
        val itemCount = list.fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount
        list.performScrollToIndex(itemCount - 1)
        composeRule.waitForIdle()
    }

    /** The last row of the list, scrolled to, ends above the top of the sheet, whatever the sheet is made of. */
    private fun assertLastRowClearsSheet() {
        scrollToTheEnd()

        val last = textBounds(PARK_RIDE_EMPTY_LINE)
        val sheet = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET)
        assertTrue(last.bottom <= sheet.top, "The last row $last ends above the sheet $sheet.")
        // Exactly the sheet's measured height and the 8 dp gap are kept, under the item's own 16 dp of trailing space:
        // not a constant that happens to fit this sheet, and not a blank band left under the last card.
        assertNear(
            "the end of the list is the sheet's height and the gap",
            PILLS_GAP + LAST_ITEM_TRAILING_SPACE,
            sheet.top - last.bottom,
        )
    }

    // endregion

    // region taps

    @Test
    @Config(qualifiers = PHONE)
    fun `a tap on the bar opens Search Stop on the destination field and is not a tap on the microphone`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = true))

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true).performClick()

        assertEquals(1, toClicks)
        assertEquals(listOf(SearchStopFieldType.TO), navigator.searchStopRequests)
        assertEquals(listOf<Pair<StopItem?, StopItem?>>(null to null), navigator.searchStopPresets)
        assertTrue(aiEvents.isEmpty())
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a tap on the microphone opens Ask KRAIL and never Search Stop`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = true))

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL, useUnmergedTree = true).performClick()

        assertEquals(listOf<AiSearchInputEvent>(AiSearchInputEvent.OpenInput), aiEvents)
        assertEquals(0, toClicks)
        assertTrue(navigator.searchStopRequests.isEmpty())
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a tap on an unset pill opens Search Stop to set that label`() {
        setScreenWithTapEffect(labels = UNSET_LABELS)

        composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip("Work"), useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf<SavedTripUiEvent>(SavedTripUiEvent.LabelChipClick("Work")),
            events.filterIsInstance<SavedTripUiEvent.LabelChipClick>(),
        )
        assertEquals(listOf(SearchStopFieldType.LABEL), navigator.searchStopRequests)
        assertEquals(listOf<String?>("Work"), navigator.searchStopLabelKeys)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a tap on a set pill with no location fix opens Search Stop holding that stop as the destination`() {
        setScreenWithTapEffect(labels = LABELS)

        composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip("Home"), useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(SearchStopFieldType.FROM), navigator.searchStopRequests)
        assertEquals(
            listOf<Pair<StopItem?, StopItem?>>(null to StopItem(stopId = HOME_STOP_ID, stopName = HOME_STOP_NAME)),
            navigator.searchStopPresets,
        )
    }

    // endregion

    // region what is not on the home

    @Test
    @Config(qualifiers = PHONE)
    fun `on a phone no map, Park and Ride tile, notice, suggestion or chip row is composed`() {
        setScreen(rightPane = { Box(Modifier.fillMaxSize().testTag(MAP_TAG)) })

        listOf(
            MAP_TAG,
            TripPlannerTestTags.HOME_MAP_TOP_ROW,
            TripPlannerTestTags.HOME_MAP_BOTTOM,
            TripPlannerTestTags.HOME_MAP_LOGO,
            TripPlannerTestTags.HOME_MAP_OFFLINE_CHIP,
            TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL,
            TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW,
            TripPlannerTestTags.SAVED_TRIPS_CHIP_PARK_RIDE,
            TripPlannerTestTags.SAVED_TRIPS_PARK_RIDE_ROW,
            TripPlannerTestTags.SAVED_TRIPS_REORDER_TOP_ROW,
        ).forEach { tag -> composeRule.onNodeWithTag(tag).assertDoesNotExist() }
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp")
    fun `a wide window keeps the 480 dp list pane with the map beside it`() {
        setScreen(rightPane = { Box(Modifier.fillMaxSize().testTag(MAP_TAG)) })

        val map = bounds(MAP_TAG)
        assertNear("the map starts where the list pane ends", DUAL_PANE_LIST_WIDTH, map.left)
        assertNear(
            "the bar spans the list pane less the page insets",
            DUAL_PANE_LIST_WIDTH - PAGE_PADDING * 2,
            bounds(TripPlannerTestTags.SEARCH_ROW_TO).width,
        )
        assertNear(
            "the pills start on the page inset in the list pane",
            PAGE_PADDING,
            bounds(TripPlannerTestTags.homeLabelChip("Home")).left,
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with nothing saved the Park and Ride add card and its line are main's own`() {
        setScreen(trips = emptyList())

        composeRule.onNodeWithText("Park & Ride").assertIsDisplayed()
        composeRule.onNodeWithText("Add Park & Ride").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Save a trip and its station's parking shows here automatically.",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertIsDisplayed()
    }

    // endregion

    // region harness

    private fun setScreen(
        fontScale: Float = 1f,
        trips: List<Trip> = TRIPS,
        labels: ImmutableList<StopLabel> = LABELS,
        ai: AiSearchInputUiState = AiSearchInputUiState(),
        rightPane: @Composable BoxScope.() -> Unit = {},
        // A text size and labels that change while the screen is up, in place of [fontScale] and [labels].
        fontScaleState: State<Float>? = null,
        labelsState: State<ImmutableList<StopLabel>>? = null,
    ) {
        composeRule.setContent {
            WithFontScale(fontScaleState?.value ?: fontScale) {
                PreviewTheme {
                    SavedTripsScreen(
                        savedTripsState = SavedTripsState(
                            isSavedTripsLoading = false,
                            savedTrips = trips.toImmutableList(),
                            stopLabels = labelsState?.value ?: labels,
                        ),
                        toButtonClick = {
                            toClicks++
                            navigator.navigateToSearchStop(SearchStopFieldType.TO)
                        },
                        aiState = ai,
                        onAiEvent = { aiEvents += it },
                        onEvent = { events += it },
                        rightPane = rightPane,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * The screen with the entry's half of a pill tap played out the way the ViewModel and the entry do it: the
     * click starts the tap, no fix comes back, and the tap's decision is acted on by [LabelChipTapEffect] against
     * the recording navigator.
     */
    private fun setScreenWithTapEffect(labels: ImmutableList<StopLabel>) {
        composeRule.setContent {
            val tap = remember { mutableStateOf<LabelChipTap>(LabelChipTap.Idle) }
            LabelChipTapEffect(
                tap = tap.value,
                locate = { null },
                onEvent = { event ->
                    events += event
                    when (event) {
                        is SavedTripUiEvent.LabelChipLocated -> {
                            val destination = homeLabelFor(event.labelKey, labels)?.toStopItem() ?: return@LabelChipTapEffect
                            tap.value = LabelChipTap.Decided(
                                decideLabelTap(destination, rider = null, candidates = emptyList()),
                            )
                        }
                        SavedTripUiEvent.LabelChipTapHandled -> tap.value = LabelChipTap.Idle
                        else -> Unit
                    }
                },
                tripPlannerNavigator = navigator,
            )
            PreviewTheme {
                SavedTripsScreen(
                    savedTripsState = SavedTripsState(
                        isSavedTripsLoading = false,
                        savedTrips = TRIPS.toImmutableList(),
                        stopLabels = labels,
                    ),
                    onEvent = { event ->
                        events += event
                        if (event is SavedTripUiEvent.LabelChipClick) {
                            tap.value = startLabelChipTap(homeLabelFor(event.labelKey, labels)!!)
                        }
                    },
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * The sheet with just the pills and the bar in it, in a window of its own, so a pill can be made wider than the
     * row, which the real home never does because its pills are only Home and Work.
     */
    private fun setPills(fontScale: Float, pills: List<HomeChip.Label>) {
        composeRule.setContent {
            WithFontScale(fontScale) {
                PreviewTheme {
                    Box(Modifier.fillMaxSize()) {
                        HomeSearchSheet(modifier = Modifier.align(Alignment.BottomCenter)) {
                            HomeLabelPills(
                                pills = pills.toImmutableList(),
                                onLabelClick = { events += SavedTripUiEvent.LabelChipClick(it) },
                            )
                            HomeSearchPill(onClick = {}, isAiAvailable = false, onAiEvent = {})
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun unset(name: String) = HomeChip.Label(StopLabel(emoji = "x", label = name))

    private fun set(name: String) =
        HomeChip.Label(StopLabel(emoji = "x", label = name, stopId = "1", stopName = "Some Station"))

    /** The text layout of the one text inside the pill for [name], where its lines and ellipsis can be read. */
    private fun pillText(name: String): TextLayoutResult {
        val inPill = hasAnyAncestor(hasTestTag(TripPlannerTestTags.homeLabelChip(name))) and hasText(name, substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNode(inPill, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(layouts)
        return layouts.first()
    }

    private fun pillLineCount(name: String): Int = pillText(name).lineCount

    private fun pillTextOverflows(name: String): Boolean = pillText(name).isLineEllipsized(0)

    @Composable
    private fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
            content = content,
        )
    }

    private fun manyTrips(): List<Trip> = (1..MANY).map { index ->
        Trip(
            fromStopId = "F$index",
            fromStopName = "From $index Station",
            toStopId = "T$index",
            toStopName = "To $index Station",
        )
    }

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    /** The whole touch target of a control, which the 48 dp interactive size gives a button that draws smaller. */
    private fun touchBounds(tag: String): DpRect {
        val rect = composeRule.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .touchBoundsInRoot
        return with(composeRule.density) {
            DpRect(rect.left.toDp(), rect.top.toDp(), rect.right.toDp(), rect.bottom.toDp())
        }
    }

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
        const val MAP_TAG = "test.map"
        const val MANY = 14
        const val LARGEST_TEXT = 3f
        const val TOLERANCE_DP = 1.5f
        const val LONG_NAME = "Grandparents house far away"
        const val LONG_SET_NAME = "Weekend place by the lake"
        const val PARK_RIDE_EMPTY_LINE = "Save a trip and its station's parking shows here automatically."
        const val HOME_STOP_ID = "200080"
        const val HOME_STOP_NAME = "Wynyard Station"
        val PAGE_PADDING = 16.dp
        val SHEET_BOTTOM_PADDING = 20.dp
        val SHEET_TOP_PADDING = 28.dp
        val SHEET_HEIGHT = 96.dp
        val PILLS_GAP = 8.dp
        val LAST_ITEM_TRAILING_SPACE = 16.dp
        val SHEET_HEIGHT_WITH_PILLS = SHEET_HEIGHT + 48.dp + PILLS_GAP
        val PHONE_WIDTH = 411.dp
        val PHONE_HEIGHT = 891.dp
        val NARROW_WIDTH = 320.dp

        val TRIPS = listOf(
            Trip(
                fromStopId = "200060",
                fromStopName = "Central Station",
                toStopId = "200070",
                toStopName = "Town Hall Station",
            ),
            Trip(
                fromStopId = "200080",
                fromStopName = "Wynyard Station",
                toStopId = "200090",
                toStopName = "Seven Hills Station",
            ),
        )

        val LABELS = persistentListOf(
            StopLabel(emoji = "H", label = "Home", stopId = HOME_STOP_ID, stopName = HOME_STOP_NAME),
            StopLabel(emoji = "W", label = "Work"),
        )

        val UNSET_LABELS = persistentListOf(
            StopLabel(emoji = "H", label = "Home"),
            StopLabel(emoji = "W", label = "Work"),
        )
    }
}

private const val HOME_TEST_SDK = 34
