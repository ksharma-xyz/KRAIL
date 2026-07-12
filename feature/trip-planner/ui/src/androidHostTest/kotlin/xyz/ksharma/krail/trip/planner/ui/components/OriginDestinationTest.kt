package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay

/**
 * Compose UI tests for [OriginDestination].
 *
 * These cover the two rendering modes (labelled / unlabelled) and the click
 * callback contract. Animation and shadow rendering are covered by the snapshot
 * suite via [TripPlannerUiSnapshotTest]; they are intentionally not asserted here.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class OriginDestinationTest {

    @get:Rule
    val composeRule = createComposeRule()

    // region Text rendering

    @Test
    fun unlabelled_stopNamesAreDisplayed() {
        composeRule.setContent {
            PreviewTheme {
                OriginDestination(
                    origin = central,
                    destination = townHall,
                    timeLineColor = Color.Black,
                )
            }
        }

        composeRule.onNodeWithText("Central Station").assertIsDisplayed()
        composeRule.onNodeWithText("Town Hall Station").assertIsDisplayed()
    }

    @Test
    fun labelled_showsLabelAndNameInParentheses() {
        composeRule.setContent {
            PreviewTheme {
                OriginDestination(
                    origin = centralLabelled,
                    destination = townHallLabelled,
                    timeLineColor = Color.Black,
                )
            }
        }

        // Label and name render as two separate Text nodes in a FlowRow (so they can
        // wrap independently on narrow widths), not one merged string.
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("(Central Station)").assertIsDisplayed()
        composeRule.onNodeWithText("Work").assertIsDisplayed()
        composeRule.onNodeWithText("(Town Hall Station)").assertIsDisplayed()
    }

    @Test
    fun partialLabel_labelledStopShowsFormat_unlabelledShowsNameOnly() {
        composeRule.setContent {
            PreviewTheme {
                OriginDestination(
                    origin = centralLabelled,
                    destination = townHall,
                    timeLineColor = Color.Black,
                )
            }
        }

        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("(Central Station)").assertIsDisplayed()
        composeRule.onNodeWithText("Town Hall Station").assertIsDisplayed()
    }

    // endregion

    // region Click callbacks

    @Test
    fun tappingOriginRow_invokesOnOriginClick_withCorrectDisplay() {
        var clicked: StopDisplay? = null
        composeRule.setContent {
            PreviewTheme {
                OriginDestination(
                    origin = central,
                    destination = townHall,
                    timeLineColor = Color.Black,
                    onOriginClick = { clicked = it },
                )
            }
        }

        composeRule.onNodeWithText("Central Station").performClick()

        val result = checkNotNull(clicked) { "expected onOriginClick to fire" }
        assert(result.stopId == "200060") { "expected stopId 200060, got ${result.stopId}" }
        assert(result.name == "Central Station") {
            "expected name Central Station, got ${result.name}"
        }
    }

    @Test
    fun tappingDestinationRow_invokesOnDestinationClick_withCorrectDisplay() {
        var clicked: StopDisplay? = null
        composeRule.setContent {
            PreviewTheme {
                OriginDestination(
                    origin = central,
                    destination = townHall,
                    timeLineColor = Color.Black,
                    onDestinationClick = { clicked = it },
                )
            }
        }

        composeRule.onNodeWithText("Town Hall Station").performClick()

        val result = checkNotNull(clicked) { "expected onDestinationClick to fire" }
        assert(result.stopId == "200070") { "expected stopId 200070, got ${result.stopId}" }
        assert(result.name == "Town Hall Station") {
            "expected name Town Hall Station, got ${result.name}"
        }
    }

    @Test
    fun tappingOriginRow_doesNotFireDestinationCallback() {
        var destinationClicked = false
        composeRule.setContent {
            PreviewTheme {
                OriginDestination(
                    origin = central,
                    destination = townHall,
                    timeLineColor = Color.Black,
                    onDestinationClick = { destinationClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Central Station").performClick()

        assert(!destinationClicked) { "destination callback fired when origin was tapped" }
    }

    @Test
    fun noClickHandlers_rendersWithoutError() {
        composeRule.setContent {
            PreviewTheme {
                OriginDestination(
                    origin = central,
                    destination = townHall,
                    timeLineColor = Color.Black,
                )
            }
        }

        composeRule.onNodeWithText("Central Station").assertIsDisplayed()
        composeRule.onNodeWithText("Town Hall Station").assertIsDisplayed()
    }

    // endregion

    // region fixtures

    private val central = StopDisplay(stopId = "200060", name = "Central Station")
    private val townHall = StopDisplay(stopId = "200070", name = "Town Hall Station")
    private val centralLabelled = StopDisplay(stopId = "200060", name = "Central Station", label = "Home")
    private val townHallLabelled = StopDisplay(stopId = "200070", name = "Town Hall Station", label = "Work")

    // endregion
}
