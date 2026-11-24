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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelChipTap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * The Saved Trips screen as the classic home: the label row is the first item of the list, above the Saved Trips heading, and the
 * search capsule stands alone at the bottom, centred, with no sheet and no pills in it. The map, the Park & Ride tiles, the notices
 * and the suggestion are not on the screen.
 *
 * Each test runs in a window of its own size (density 1, so a dp is a pixel) and reads where the real nodes landed. The capsule's
 * own details are in `HomeSearchCapsuleLayoutTest` and `HomeSearchCapsuleLookTest`, the row's in `HomeLabelRowLayoutTest`.
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

    // region the label row, in the list

    @Test
    @Config(qualifiers = PHONE)
    fun `the label row is the first item of the list, above the Saved Trips heading, from the page inset`() {
        setScreen()

        val list = bounds(TripPlannerTestTags.SAVED_TRIPS_LIST)
        val row = bounds(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS)
        val title = textBounds("Saved Trips")
        composeRule.onNode(
            hasTestTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS) and
                hasAnyAncestor(hasTestTag(TripPlannerTestTags.SAVED_TRIPS_LIST)),
            useUnmergedTree = true,
        ).assertExists()
        assertNear("the row is the first thing in the list", list.top, row.top)
        assertTrue(row.bottom <= title.top, "The row $row is above the Saved Trips heading $title.")
        assertNear("Home starts on the page inset", PAGE_PADDING, bounds(TripPlannerTestTags.homeLabelChip("Home")).left)
        assertNear("the row is one 48 dp slot", 48.dp, row.height)
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the row shows every label the rider has, set and unset, the rider's own after Home and Work`() {
        setScreen(labels = CUSTOM_LABELS)

        listOf("Home", "Work", "Gym").forEach { name ->
            composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip(name), useUnmergedTree = true).assertIsDisplayed()
        }
        val lefts = listOf("Home", "Work", "Gym").map { bounds(TripPlannerTestTags.homeLabelChip(it)).left }
        assertEquals(lefts.sorted(), lefts, "Home, Work, Gym run from the left in that order.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the row scrolls away with the list while the capsule and the title bar stay`() {
        setScreen(trips = manyTrips())
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertIsDisplayed()

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST).performScrollToIndex(MANY - 1)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_SETTINGS).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with no labels at all nothing is drawn for the row and no slot is kept, the heading is the first row`() {
        setScreen(labels = persistentListOf())

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertDoesNotExist()
        val list = bounds(TripPlannerTestTags.SAVED_TRIPS_LIST)
        val title = textBounds("Saved Trips")
        assertTrue(
            title.top >= list.top && title.top < list.top + 48.dp,
            "The list opens with its heading $title in $list, with no empty slot above it.",
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `with nothing saved the row is still there and the Saved Trips heading is not`() {
        setScreen(trips = emptyList())

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertIsDisplayed()
        composeRule.onNodeWithText("Saved Trips").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `while the cards are reordered the row and the capsule go, and the list keeps its end clearance`() {
        setScreen(trips = manyTrips())
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertIsDisplayed()
        val barHeight = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR).height

        composeRule.onNodeWithTag(TripPlannerTestTags.savedTripCard(manyTrips().first().tripId))
            .performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_ACTION_DONE).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true).assertDoesNotExist()
        scrollToTheEnd()
        assertTrue(
            textBounds(PARK_RIDE_EMPTY_LINE).bottom <= PHONE_HEIGHT - barHeight,
            "The end of the list keeps the $barHeight the bar had, so it does not jump when reorder starts.",
        )
    }

    // endregion

    // region the capsule, alone at the bottom

    @Test
    @Config(qualifiers = PHONE)
    fun `the capsule stands alone, 16 dp from each side and 12 dp above the bottom, with no sheet and no pills`() {
        setScreen()

        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val bar = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR)
        assertNear("capsule left inset", PAGE_PADDING, capsule.left)
        assertNear("capsule right inset", PHONE_WIDTH - PAGE_PADDING, capsule.right)
        assertNear("capsule bottom", PHONE_HEIGHT - CAPSULE_BOTTOM_MARGIN, capsule.bottom)
        assertNear("the bar is the capsule and its margin", capsule.height + CAPSULE_BOTTOM_MARGIN, bar.height)
        assertNear("the bar reaches the bottom of the window", PHONE_HEIGHT, bar.bottom)
        // The theme coloured sheet that used to hold the search bar and the pills is gone, and so is its tag.
        composeRule.onNodeWithTag(REMOVED_SHEET_TAG).assertDoesNotExist()
        composeRule.onNode(
            hasTestTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS) and
                hasAnyAncestor(hasTestTag(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR)),
            useUnmergedTree = true,
        ).assertDoesNotExist()
        composeRule.onNode(
            hasTestTag(TripPlannerTestTags.homeLabelChip("Home")) and
                hasAnyAncestor(hasTestTag(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR)),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `the capsule is one row of one control, no From and To rows, no go circle and no collapsed Plan a trip button`() {
        setScreen()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_COLLAPSED_PILL).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.HOME_SEARCH_GLYPH, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp")
    fun `phone landscape keeps the same capsule`() {
        setScreen()

        assertTrue(bounds(TripPlannerTestTags.SEARCH_ROW_TO).height >= 48.dp, "The capsule is at least 48 dp in landscape.")
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_COLLAPSED_PILL).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `where Ask KRAIL is allowed the plain microphone sits inside the capsule at its end`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = true))

        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val mic = bounds(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL)
        assertNear("microphone width", 48.dp, mic.width)
        assertNear("microphone height", 48.dp, mic.height)
        assertNear("microphone is 4 dp in from the capsule's end", capsule.right - 4.dp, mic.right)
        assertTrue(textBounds("Where are you going?").right <= mic.left, "The text stops short of the microphone.")
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `without Ask KRAIL the capsule has the glyph and no microphone`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = false))

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.HOME_SEARCH_GLYPH, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = PHONE)
    fun `a device that cannot run Ask KRAIL gets no microphone either`() {
        setScreen(ai = AiSearchInputUiState(isFeatureEnabled = true, isDeviceCapable = false))
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `at 320 dp and 200 percent the capsule is whole, one line, with the microphone inside it`() {
        setScreen(fontScale = 2f, ai = AiSearchInputUiState(isFeatureEnabled = true))

        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        val mic = bounds(TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL)
        assertTrue(mic.left >= capsule.left && mic.right <= capsule.right + 1.dp, "The microphone $mic is inside $capsule.")
        assertTrue(capsule.left >= 0.dp && capsule.right <= NARROW_WIDTH, "The capsule $capsule is inside the window.")
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp")
    fun `on a wide single pane the capsule is capped at 480 dp and centred`() {
        setScreen()

        val capsule = bounds(TripPlannerTestTags.SEARCH_ROW_TO)
        assertNear("width", 480.dp, capsule.width)
        assertNear("equal margins", capsule.left, 700.dp - capsule.right)
    }

    // endregion

    // region the end of the list

    @Test
    @Config(qualifiers = PHONE)
    fun `the end of the list clears the capsule at 100 percent, so the last card scrolls fully into view`() {
        setScreen(trips = manyTrips())

        assertLastRowClearsBar()
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `the end of the list clears the capsule at 320 dp and 200 percent`() {
        setScreen(fontScale = 2f, trips = manyTrips(), ai = AiSearchInputUiState(isFeatureEnabled = true))

        assertLastRowClearsBar()
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `when the text grows the capsule grows and the end of the list follows its measured height`() {
        val fontScale = mutableFloatStateOf(1f)
        setScreen(trips = manyTrips(), fontScaleState = fontScale)
        val barAt100 = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR).height
        assertLastRowClearsBar()

        fontScale.floatValue = LARGEST_TEXT
        composeRule.waitForIdle()
        val barAtLargest = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR).height

        assertTrue(barAtLargest > barAt100 + 10.dp, "The capsule is taller at $LARGEST_TEXT times text: $barAtLargest, it was $barAt100.")
        assertLastRowClearsBar()
    }

    private fun scrollToTheEnd() {
        val list = composeRule.onNodeWithTag(TripPlannerTestTags.SAVED_TRIPS_LIST)
        val itemCount = list.fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount
        list.performScrollToIndex(itemCount - 1)
        composeRule.waitForIdle()
    }

    /** The last row of the list, scrolled to, ends above the top of the bar, by exactly its measured height and the 8 dp gap. */
    private fun assertLastRowClearsBar() {
        scrollToTheEnd()

        val last = textBounds(PARK_RIDE_EMPTY_LINE)
        val bar = bounds(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_BAR)
        assertTrue(last.bottom <= bar.top, "The last row $last ends above the bar $bar.")
        // Exactly the bar's measured height and the 8 dp gap are kept, under the item's own 16 dp of trailing space: not a
        // constant that happens to fit this bar, and not a blank band left under the last card.
        assertNear(
            "the end of the list is the bar's height and the gap",
            END_GAP + LAST_ITEM_TRAILING_SPACE,
            bar.top - last.bottom,
        )
    }

    // endregion

    // region taps

    @Test
    @Config(qualifiers = PHONE)
    fun `a tap on the capsule opens Search Stop on the destination field and is not a tap on the microphone`() {
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

    @Test
    @Config(qualifiers = PHONE)
    fun `a tap on one of the rider's own labels is the same tap, keyed by that label`() {
        setScreenWithTapEffect(labels = CUSTOM_LABELS)

        composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip("Gym"), useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf<SavedTripUiEvent>(SavedTripUiEvent.LabelChipClick("Gym")),
            events.filterIsInstance<SavedTripUiEvent.LabelChipClick>(),
        )
        assertEquals(listOf(SearchStopFieldType.LABEL), navigator.searchStopRequests)
        assertEquals(listOf<String?>("Gym"), navigator.searchStopLabelKeys)
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
            "the capsule spans the list pane less the page insets",
            DUAL_PANE_LIST_WIDTH - PAGE_PADDING * 2,
            bounds(TripPlannerTestTags.SEARCH_ROW_TO).width,
        )
        assertNear(
            "the row starts on the page inset in the list pane",
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
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getVisibleBoundsInRoot()

    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getVisibleBoundsInRoot()

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
        const val REMOVED_SHEET_TAG = "savedtrips.sheet"
        const val MANY = 14
        const val LARGEST_TEXT = 3f
        const val TOLERANCE_DP = 1.5f
        const val PARK_RIDE_EMPTY_LINE = "Save a trip and its station's parking shows here automatically."
        const val HOME_STOP_ID = "200080"
        const val HOME_STOP_NAME = "Wynyard Station"
        val PAGE_PADDING = 16.dp
        val CAPSULE_BOTTOM_MARGIN = 12.dp
        val END_GAP = 8.dp
        val LAST_ITEM_TRAILING_SPACE = 16.dp
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

        val CUSTOM_LABELS = persistentListOf(
            StopLabel(emoji = "H", label = "Home", stopId = HOME_STOP_ID, stopName = HOME_STOP_NAME),
            StopLabel(emoji = "W", label = "Work"),
            StopLabel(emoji = "G", label = "Gym"),
        )
    }
}

private const val HOME_TEST_SDK = 34
