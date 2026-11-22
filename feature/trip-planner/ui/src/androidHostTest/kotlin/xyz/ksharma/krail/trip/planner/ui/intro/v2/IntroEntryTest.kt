package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.navigation.ResultEventBus
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.di.viewModelsModule
import xyz.ksharma.krail.trip.planner.ui.flow.RecordingTripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.entries.IntroEntry
import kotlin.test.assertEquals

/**
 * The real Intro entry, built by the same `entryProvider` the app uses, with the real Koin
 * registration of the view model and fakes at its edges. It holds that the entry finishes to Saved
 * Trips with the back stack cleared, opens the stop search for the right end, receives the pick
 * back, keeps its view model while the search is on screen, and starts fresh on a new visit.
 *
 * The harness plays the part of the navigation decorator that keeps an entry's saved state while it
 * is on the back stack, so a visit's key survives the entry leaving composition.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroEntryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val navigator = IntroRecordingNavigator()
    private val onScreen = mutableStateOf(true)
    private var holder: SaveableStateHolder? = null

    @Before
    fun startKoinWithFakes() {
        startKoin { modules(viewModelsModule, introKoinFakes()) }
    }

    @After
    fun stopKoinAndCleanTheBus() {
        stopKoin()
        ResultEventBus.getInstance().removeResult<StopSelectedResult>()
    }

    private fun launch() {
        composeRule.setContent {
            PreviewTheme {
                val saveable = rememberSaveableStateHolder()
                holder = saveable
                if (onScreen.value) {
                    saveable.SaveableStateProvider(VISIT) { IntroEntryContent(navigator) }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun leave() {
        composeRule.runOnIdle { onScreen.value = false }
        composeRule.waitForIdle()
    }

    private fun comeBack() {
        composeRule.runOnIdle { onScreen.value = true }
        composeRule.waitForIdle()
    }

    private fun answerNo() {
        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `the entry opens on the Park & Ride question`() {
        launch()

        composeRule.onNodeWithText(PARK_RIDE_QUESTION).assertIsDisplayed()
    }

    @Test
    fun `finishing goes to Saved Trips with the back stack cleared`() {
        launch()
        answerNo()

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(SavedTripsRoute), navigator.cleared)
    }

    @Test
    fun `Search another stop opens the stop search for the start`() {
        launch()
        answerNo()

        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.START)).performClick()

        assertEquals(listOf(SearchStopFieldType.FROM), navigator.recorded.searchStopRequests)
    }

    @Test
    fun `a stop picked while the search was on screen is the start when the entry is back`() {
        launch()
        answerNo()
        leave()

        ResultEventBus.getInstance().sendResult(
            result = StopSelectedResult(
                fieldType = SearchStopFieldType.FROM,
                stopId = "215020",
                stopName = "Parramatta Station",
            ),
        )
        comeBack()

        composeRule.onNodeWithContentDescription(
            changeStopDescription(IntroStopRole.START, "Parramatta Station"),
        ).assertIsDisplayed()
    }

    @Test
    fun `leaving for the search and coming back keeps the Intro on step 2`() {
        launch()
        answerNo()

        leave()
        comeBack()

        composeRule.onNodeWithText(FIRST_TRIP_HEADING).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_QUESTION).assertDoesNotExist()
    }

    @Test
    fun `a new visit to the Intro starts on the question again`() {
        launch()
        answerNo()
        composeRule.onNodeWithText(FIRST_TRIP_HEADING).assertIsDisplayed()

        leave()
        composeRule.runOnIdle { holder?.removeState(VISIT) }
        comeBack()

        composeRule.onNodeWithText(PARK_RIDE_QUESTION).assertIsDisplayed()
        composeRule.onNodeWithText(FIRST_TRIP_HEADING).assertDoesNotExist()
    }

    private companion object {
        const val VISIT = "intro-entry"
    }
}

/** The real entry, composed without `NavDisplay`, the way the home flow tests compose theirs. */
@Composable
private fun IntroEntryContent(navigator: TripPlannerNavigator) {
    val provider = entryProvider<NavKey> { IntroEntry(navigator) }
    provider(IntroRoute).Content()
}

/** Records the one call the flow tests' navigator ignores, and leaves the rest to it. */
private class IntroRecordingNavigator(
    val recorded: RecordingTripPlannerNavigator = RecordingTripPlannerNavigator(),
) : TripPlannerNavigator by recorded {
    val cleared = mutableListOf<NavKey>()

    override fun clearBackStackAndNavigate(route: NavKey) {
        cleared += route
    }
}
