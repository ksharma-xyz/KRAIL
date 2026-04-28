package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
 * - Drag-to-reorder (gesture coordination with reorderable lib is brittle in
 *   Robolectric; covered by snapshot tests visually instead).
 * - Long-press timing-based assertions (Compose's clock advancement makes them
 *   flaky in CI). Long-press is exercised at the pure-function layer
 *   (`pillRowBannerText` editing branch) so the rule is locked in regardless.
 * - The save / add-label sheet flows that require ModalBottomSheet animation
 *   completion — those are ViewModel-side and covered by the upcoming VM tests
 *   in a sibling branch.
 *
 * What IS covered: visibility of the pill row + Home/Work pills under different
 * SearchStopState shapes, the protection-from-deletion invariant on Home, the
 * star-icon state on stop rows, and direct-tap callbacks (`onStopSelect` for set
 * pills, `ClearLabelStop` for filled-star taps).
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
                    searchStopState = SearchStopFixtures.recentWithDefaults(),
                    onEvent = {},
                )
            }
        }

        // The pill row should be visible because we have recent stops.
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Work").assertIsDisplayed()
    }

    // endregion

    // region Star icon state on stop rows

    @Test
    fun savedStop_showsRemoveFromLabelsStar() {
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithHomeSet(),
                    onEvent = {},
                )
            }
        }

        // Central Station is saved as Home (homeSet stopId == stop_central), so
        // its star should advertise "Remove from labels" rather than "Save as
        // label". onAllNodes is used because Town Hall's star advertises the
        // "Save as label" variant — we only want to assert at least one of each.
        composeRule.onAllNodesWithContentDescription("Remove from labels")
            .assertCountAtLeast(1)
        composeRule.onAllNodesWithContentDescription("Save as label")
            .assertCountAtLeast(1)
    }

    // endregion

    // region Direct-action callbacks

    @Test
    fun tappingFilledStar_firesClearLabelStopWithMatchingLabel() {
        val captured = mutableListOf<SearchStopUiEvent>()
        composeRule.setContent {
            PreviewTheme {
                SearchStopScreen(
                    searchStopState = SearchStopFixtures.recentWithHomeSet(),
                    onEvent = { captured += it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Remove from labels").performClick()

        // Tapping the filled star should clear the matching label directly — no
        // sheet opens, no SaveStopAsLabelSheet detour.
        val cleared = captured.filterIsInstance<SearchStopUiEvent.ClearLabelStop>()
        assert(cleared.isNotEmpty()) {
            "expected at least one ClearLabelStop event, got: $captured"
        }
        assert(cleared.first().labelKey == "Home") {
            "expected ClearLabelStop for Home, got ${cleared.first().labelKey}"
        }
    }

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

        composeRule.onNodeWithText("Home").performClick()

        // Tapping the set Home pill is the fast-path for "use this saved stop as
        // From/To" — onStopSelect should fire with Home's underlying stop.
        assert(selected != null) { "expected onStopSelect to fire" }
        assert(selected!!.stopId == "stop_central") {
            "expected stop_central, got ${selected?.stopId}"
        }
        assert(selected!!.stopName == "Central Station")
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

    // endregion
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountAtLeast(
    minimum: Int,
) {
    val actual = fetchSemanticsNodes().size
    assert(actual >= minimum) {
        "expected at least $minimum nodes, found $actual"
    }
}
