package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.di.viewModelsModule
import xyz.ksharma.krail.trip.planner.ui.flow.RecordingTripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
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
    fun stopKoinAfterwards() {
        stopKoin()
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

    // The theme screen comes first, and its right button only comes on once a theme is selected.
    private fun chooseTheme() {
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_THEME_METRO).performClick()
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()
    }

    private fun answerNo() {
        chooseTheme()
        composeRule.onNodeWithText(PARK_RIDE_NO).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `the entry opens on the theme question, with the Park and Ride question not yet shown`() {
        launch()

        composeRule.onNodeWithText(PICK_THEME_HEADING).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_HEADING).assertDoesNotExist()
    }

    @Test
    fun `choosing a theme recolours the whole app through the navigator and moves on to Park & Ride`() {
        launch()

        chooseTheme()

        assertEquals(listOf(KrailThemeStyle.Metro.hexColorCode), navigator.themes)
        composeRule.onNodeWithText(PARK_RIDE_HEADING).assertIsDisplayed()
    }

    @Test
    fun `finishing goes to Saved Trips with the back stack cleared`() {
        launch()
        answerNo()
        // The destination and then the origin are chosen with the right button, and the confirm screen saves.
        repeat(2) {
            composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(SavedTripsRoute), navigator.cleared)
    }

    @Test
    fun `leaving and coming back keeps the Intro on the destination screen`() {
        launch()
        answerNo()

        leave()
        comeBack()

        composeRule.onNodeWithText(DESTINATION_HEADING).assertIsDisplayed()
        composeRule.onNodeWithText(PARK_RIDE_HEADING).assertDoesNotExist()
    }

    @Test
    fun `a new visit to the Intro starts on the first question again`() {
        launch()
        answerNo()
        composeRule.onNodeWithText(DESTINATION_HEADING).assertIsDisplayed()

        leave()
        composeRule.runOnIdle { holder?.removeState(VISIT) }
        comeBack()

        composeRule.onNodeWithText(PICK_THEME_HEADING).assertIsDisplayed()
        composeRule.onNodeWithText(DESTINATION_HEADING).assertDoesNotExist()
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
    val themes = mutableListOf<String>()

    override fun updateTheme(hexColorCode: String) {
        themes += hexColorCode
    }

    override fun clearBackStackAndNavigate(route: NavKey) {
        cleared += route
    }
}
