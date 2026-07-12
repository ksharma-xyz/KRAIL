package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

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
        // recents below to interact with — see shouldShowPillRow().
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

        // The pill row should be visible because we have recent stops, showing the
        // one set label (Home) plus the trailing Manage button. Work stays hidden
        // because it's unset. "Home" renders twice total (pill row + Central
        // Station's own inline pill) — see assignedStop_showsLabelPillInline.
        composeRule.onAllNodesWithText("Home").assertCountEquals(2)
        composeRule.onNodeWithText("Manage").assertIsDisplayed()
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
                    searchStopState = SearchStopFixtures.recentWithDefaults(),
                    onEvent = {},
                )
            }
        }

        // The trailing "Manage" button is the sole entry point to
        // ManageStopLabelsSheet (change/remove/delete/reorder). It must be present
        // whenever the pill row renders.
        composeRule.onNodeWithText("Manage").assertIsDisplayed()
    }

    @Test
    fun tappingManageButton_invokesOnManageLabelsClick() {
        var manageClicked = false
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithDefaults(),
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
            override fun show() {}
            override fun hide() {
                events += "hide"
            }
        }
        composeRule.setContent {
            PreviewTheme {
                CompositionLocalProvider(LocalSoftwareKeyboardController provides spyKeyboardController) {
                    SearchStopScreen(
                        searchStopState = SearchStopFixtures.recentWithDefaults(),
                        onEvent = {},
                        onManageLabelsClick = { events += "manageClicked" },
                    )
                }
            }
        }

        // Same "close keyboard before navigating" order as the back button
        // (SearchTopBar's NavActionButton) — the keyboard must hide before the nav
        // callback fires, otherwise it stays up mid-navigation.
        composeRule.onNodeWithText("Manage").performClick()

        // The screen also hides the keyboard once on first composition (the
        // showMap LaunchedEffect, unrelated to this click), so assert ordering
        // rather than an exact call count: a "hide" must precede "manageClicked".
        val manageIndex = events.indexOf("manageClicked")
        assert(manageIndex > 0) { "expected onManageLabelsClick to fire, got $events" }
        assert(events.subList(0, manageIndex).contains("hide")) {
            "expected keyboard to hide before onManageLabelsClick fired, got $events"
        }
    }

    // endregion
}
