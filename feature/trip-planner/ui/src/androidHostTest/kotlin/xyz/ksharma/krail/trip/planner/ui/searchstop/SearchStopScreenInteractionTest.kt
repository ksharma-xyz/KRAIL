package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.assertEquals

/**
 * Compose UI tests for [SearchStopScreen]. These are deliberately scoped to behaviors
 * that ARE NOT covered by the pure-function tests in `SearchStopRulesTest` /
 * `LabelNameNormalizerTest` — the rules tests cover the math; these tests cover that
 * the rules actually drive the rendering.
 *
 * What's intentionally NOT tested here:
 * - Drag-to-reorder inside `ManageStopLabelsScreen` (gesture coordination with the
 *   reorderable lib is brittle in Robolectric; covered by snapshot tests visually
 *   instead).
 * - The save / add-label sheet flows that require ModalBottomSheet animation
 *   completion — those are ViewModel-side and covered by the upcoming VM tests
 *   in a sibling branch.
 *
 * What IS covered: visibility of the pill row + Home/Work pills under different
 * SearchStopState shapes, the protection-from-deletion invariant on Home,
 * `StopLabelAssignRow`'s icon state (only unassigned rows get a "+"), and
 * direct-tap callbacks (`onStopSelect` for set pills). The pill row itself is
 * tap-only — long-press/edit-mode was removed in favour of `ManageStopLabelsScreen`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SearchStopScreenInteractionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val quietKeyboard = object : SoftwareKeyboardController {
        override fun show() = Unit
        override fun hide() = Unit
    }

    // region Pill row visibility

    @Test
    fun pillRow_isHidden_onFreshInstallWithNoRecents() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.freshInstall(),
                    onEvent = {},
                )
            }
        }

        // Home + Work pills should NOT be in the tree because there are no
        // recents below to interact with, see shouldShowLabelRow().
        composeRule.onNodeWithText("Home").assertDoesNotExist()
        composeRule.onNodeWithText("Work").assertDoesNotExist()
    }

    @Test
    fun pillRow_isShown_whenAtLeastOneRecentExists() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    // The pill row's LazyRow only iterates `isSet` labels (v4 — unset
                    // labels are assigned via each stop's own inline "+ New label"
                    // wall now, not a tap on a top-row pill), so an all-unset fixture
                    // like recentWithDefaults() would render zero label pills here.
                    searchStopState = SearchStopFixtures.recentWithHomeSet(),
                    onEvent = {},
                )
            }
        }

        // The label row leads the card because a label is set, showing the
        // one set label (Home) plus the trailing Manage button. Work stays hidden
        // because it's unset. "Home" renders twice total (pill row + Central
        // Station's own inline pill) — see assignedStop_showsLabelPillInline.
        composeRule.onAllNodesWithText("Home").assertCountEquals(2)
        composeRule.onNodeWithText("Manage").assertIsDisplayed()
    }

    // endregion

    // region The map state and the list state

    /** A map the rider can open on, with the ground drawn as a stand-in because Robolectric has no GPU. */
    private fun withMap(state: SearchStopState = SearchStopFixtures.recentWithDefaults()) =
        state.copy(isMapsAvailable = true, mapUiState = MapUiState.Ready())

    private fun setScreen(
        state: SearchStopState,
        keyboard: SoftwareKeyboardController = quietKeyboard,
        onEvent: (SearchStopUiEvent) -> Unit = {},
        goBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(
                    LocalInspectionMode provides true,
                    LocalSoftwareKeyboardController provides keyboard,
                ) {
                    SearchStopScreen(searchStopState = state, onEvent = onEvent, goBack = goBack)
                }
            }
        }
    }

    @Test
    fun singlePane_opensOnTheMapState_withTheBarAndNoListAndNoKeyboard() {
        val shown = mutableListOf<String>()
        val keyboard = object : SoftwareKeyboardController {
            override fun show() {
                shown += "show"
            }

            override fun hide() = Unit
        }
        setScreen(withMap(), keyboard = keyboard)

        // Rule 39: the map and one dark pill. The rider's recents are one tap away, not on screen.
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertDoesNotExist()
        composeRule.onNodeWithText("Central Station").assertDoesNotExist()
        assert(shown.isEmpty()) { "the map state raises no keyboard, got $shown" }
    }

    @Test
    fun theModeChipsSitInTheMapStateOnly_andATapTogglesThatMode() {
        val events = mutableListOf<SearchStopUiEvent>()
        setScreen(withMap(), onEvent = { events += it })

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertIsDisplayed()
        composeRule.onNodeWithTag(
            TripPlannerTestTags.searchStopModeChip(TransportMode.Ferry.productClass),
        ).performClick()
        assertEquals(
            listOf<SearchStopUiEvent>(SearchStopUiEvent.TransportModeFilterToggled(TransportMode.Ferry)),
            events.filterIsInstance<SearchStopUiEvent.TransportModeFilterToggled>(),
        )

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertDoesNotExist()
    }

    @Test
    fun withNoMap_thereIsNoModeFilterBecauseThereIsNothingToFilter() {
        setScreen(SearchStopFixtures.recentWithDefaults())

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS).assertDoesNotExist()
    }

    @Test
    fun tappingTheBar_raisesTheListState() {
        setScreen(withMap())

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertIsDisplayed()
        composeRule.onNodeWithText("Central Station").assertIsDisplayed()
    }

    @Test
    fun theArrowIsTwoStepsBack_listToMapToHome() {
        var wentBack = 0
        setScreen(withMap(), goBack = { wentBack++ })
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertIsDisplayed()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_BACK).performClick()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertDoesNotExist()
        assert(wentBack == 0) { "the first back returns to the map state and does not leave" }

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_BACK).performClick()
        composeRule.waitForIdle()

        assert(wentBack == 1) { "the second back goes home, got $wentBack" }
    }

    @Test
    fun theX_showsOnlyWithText_clearsItAndKeepsTheRecents() {
        var wentBack = 0
        setScreen(withMap(), goBack = { wentBack++ })
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).assertDoesNotExist()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performTextInput("Cen")
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).assertIsDisplayed()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_CLEAR).assertDoesNotExist()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST).assertIsDisplayed()
        assert(wentBack == 0) { "the X never dismisses the screen" }
    }

    @Test
    fun withNoMapToOpenOn_theListIsTheScreen() {
        // Maps are off for this rider: there is no map state to start in, so the list shows at once.
        setScreen(SearchStopFixtures.recentWithDefaults())

        composeRule.onNodeWithText("Central Station").assertIsDisplayed()
    }

    @Test
    fun theMapStateAsksForTheMapOnOpen() {
        val events = mutableListOf<SearchStopUiEvent>()
        setScreen(SearchStopFixtures.recentWithDefaults().copy(isMapsAvailable = true), onEvent = { events += it })

        assert(events.contains(SearchStopUiEvent.InitializeMap)) { "expected InitializeMap, got $events" }
    }

    @Test
    fun noSelectOnMapRowAnywhere() {
        setScreen(withMap())
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()

        composeRule.onNodeWithText("Select on map").assertDoesNotExist()
    }

    // endregion

    // region The recents card

    @Test
    fun theCardShowsAtMostFiveRecents() {
        val many = (1..8).map { SearchStopState.StopResult("Stop $it", "id_$it") }
        setScreen(SearchStopFixtures.recentWithDefaults().copy(recentStops = many.toImmutableList()))

        for (shown in 1..5) composeRule.onNodeWithText("Stop $shown").assertIsDisplayed()
        for (hidden in 6..8) composeRule.onNodeWithText("Stop $hidden").assertDoesNotExist()
    }

    @Test
    fun withNoRecentsAndNoLocation_theSuggestedStopsShowAsRows() {
        val suggested = persistentListOf(SearchStopState.StopResult("Town Hall Station", "200070"))
        setScreen(
            SearchStopFixtures.freshInstall().copy(
                curatedStartStops = suggested,
                curatedEndStops = suggested,
            ),
        )

        composeRule.onNodeWithText("Town Hall Station").assertIsDisplayed()
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
    }

    @Test
    fun theLabelRowScrollsAndLeadsTheCard() {
        setScreen(SearchStopFixtures.recentWithHomeSet())

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LABEL_ROW).assertIsDisplayed()
    }

    // endregion

    // region The location pill

    @Test
    fun theLocationPillIsOneItemUnderTheForm_onlyWhenPermissionIsNotGranted() {
        // A fresh Robolectric device has no permission decision recorded, so the one pill shows.
        setScreen(SearchStopFixtures.recentWithDefaults())
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL).assertCountEquals(1)
        composeRule.onNodeWithText("Location permission required").assertIsDisplayed()
        // The pill is a taj Button 32 tall inside a 48 tall box (rule 24), and the tag is on the box.
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL).assertHeightIsEqualTo(48.dp)
    }

    // endregion

    // region StopLabelAssignRow icon state

    @Test
    fun assignedStop_hidesAssignIcon() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithHomeSet(),
                    onEvent = {},
                )
            }
        }

        // Central Station is saved as Home; Town Hall isn't. v4: once a stop is
        // assigned its row shows no icon at all (not even a tick) — only the unset
        // Town Hall row gets the "+". recentWithHomeSet has exactly one unset recent
        // stop, so exactly one icon should exist.
        composeRule.onAllNodesWithContentDescription("Assign to a label")
            .assertCountEquals(1)
    }

    @Test
    fun assignedStop_showsLabelPillInline() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithHomeSet(),
                    onEvent = {},
                )
            }
        }

        // "Home" renders twice: the top pill row's shortcut, and Central Station's
        // own inline small pill next to its transport-mode icon — v4 shows the label
        // right there instead of a separate summary block or a tick.
        composeRule.onAllNodesWithText("Home").assertCountEquals(2)
    }

    // endregion

    // region Direct-action callbacks

    @Test
    fun tappingSetPill_invokesOnStopSelectWithUnderlyingStop() {
        var selected: StopItem? = null
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithHomeSet(),
                    onStopSelect = { selected = it },
                    onEvent = {},
                )
            }
        }

        // "Home" now also appears inside Central Station's own row (its
        // AssignedStopSummary pill, EX-B) — the top pill row's "Home" is first in the
        // LazyColumn (pillRowSection is added before recentSearchStopsList).
        composeRule.onAllNodesWithText("Home").onFirst().performClick()

        // Tapping the set Home pill is the fast-path for "use this saved stop as
        // From/To" — onStopSelect should fire with Home's underlying stop.
        val result = checkNotNull(selected) { "expected onStopSelect to fire" }
        assert(result.stopId == "stop_central") { "expected stop_central, got ${result.stopId}" }
        assert(result.stopName == "Central Station")
    }

    // endregion

    // region Recent header

    @Test
    fun recentHeader_showsRecentAndClearAllLabelsWhenRecentsExist() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithDefaults(),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText("Recent").assertIsDisplayed()
        composeRule.onNodeWithText("Clear all").assertIsDisplayed()
    }

    @Test
    fun clearAllButton_firesClearRecentSearchStopsEventWithCount() {
        val captured = mutableListOf<SearchStopUiEvent>()
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithDefaults(),
                    onEvent = { captured += it },
                )
            }
        }

        composeRule.onNodeWithText("Clear all").performClick()

        val clears = captured.filterIsInstance<SearchStopUiEvent.ClearRecentSearchStops>()
        assert(clears.isNotEmpty()) {
            "expected ClearRecentSearchStops event, got: $captured"
        }
        // Two recents in the fixture (centralStop, townHallStop).
        assert(clears.first().recentSearchCount == 2) {
            "expected count 2, got ${clears.first().recentSearchCount}"
        }
    }

    @Test
    fun recentHeader_isHidden_onFreshInstall() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.freshInstall(),
                    onEvent = {},
                )
            }
        }

        // No recents -> no Recent / Clear all header. The whole list is empty so the
        // user falls through to "Select on map" / public-transport note.
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
        composeRule.onNodeWithText("Clear all").assertDoesNotExist()
    }

    // endregion

    // NOTE: the old "tap an unset pill in the top row to enter choose-mode" flow
    // (story A1) tests used to live here. LabelShortcutsRow (SearchStopScreen.kt)
    // filters its LazyRow to `labels.filter { it.isSet }` — unset labels never
    // render as pills there, assignment moved to each stop's own inline
    // "+ New label" wall (v4, StopLabelAssignRow). The choose-mode plumbing
    // (assigningLabel, onUnsetLabelClick, effectiveOnStopSelect) was unreachable
    // dead code and has been removed.

    // region Manage labels affordance

    @Test
    fun manageButton_isVisible_wheneverPillRowRenders() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithHomeSet(),
                    onEvent = {},
                )
            }
        }

        // The trailing "Manage" button is the sole entry point to
        // ManageStopLabelsSheet (change/remove/delete/reorder). It must be present
        // whenever the pill row renders (i.e. at least one label is set).
        composeRule.onNodeWithText("Manage").assertIsDisplayed()
    }

    @Test
    fun manageButton_isHidden_whenNoLabelIsSetYet() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    // Fresh install: Home/Work seeded but unset. No pills to show, so
                    // the trailing Manage button shouldn't float there alone either.
                    searchStopState = SearchStopFixtures.recentWithDefaults(),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText("Manage").assertDoesNotExist()
    }

    @Test
    fun tappingManageButton_invokesOnManageLabelsClick() {
        var manageClicked = false
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithHomeSet(),
                    onEvent = {},
                    onManageLabelsClick = { manageClicked = true },
                )
            }
        }

        // ManageStopLabelsScreen is a real nav destination owned by the caller
        // (ManageStopLabelsEntry), not rendered inline here — this screen only owns
        // firing the callback.
        composeRule.onNodeWithText("Manage").performClick()

        assert(manageClicked) { "expected onManageLabelsClick to fire" }
    }

    @Test
    fun tappingManageButton_hidesKeyboardBeforeInvokingCallback() {
        val events = mutableListOf<String>()
        val spyKeyboardController = object : SoftwareKeyboardController {
            override fun show() = Unit
            override fun hide() {
                events += "hide"
            }
        }
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(LocalSoftwareKeyboardController provides spyKeyboardController) {
                    SearchStopScreen(
                        searchStopState = SearchStopFixtures.recentWithHomeSet(),
                        onEvent = {},
                        onManageLabelsClick = { events += "manageClicked" },
                    )
                }
            }
        }

        // Same "close keyboard before navigating" order as the back button
        // (the back arrow in the bar): the keyboard must hide before the nav
        // callback fires, otherwise it stays up mid-navigation.
        composeRule.onNodeWithText("Manage").performClick()

        // Assert ordering rather than an exact call count: a "hide" must precede
        // "manageClicked". The screen lands on the list, so its showMap effect calls
        // show() on first composition, not hide() — but that is incidental here, and
        // ordering stays the right assertion either way.
        val manageIndex = events.indexOf("manageClicked")
        assert(manageIndex > 0) { "expected onManageLabelsClick to fire, got $events" }
        assert(events.subList(0, manageIndex).contains("hide")) {
            "expected keyboard to hide before onManageLabelsClick fired, got $events"
        }
    }

    // endregion
}
