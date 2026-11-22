package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.assertEquals
import kotlin.time.TimeSource

/**
 * The theme screen through the route, with the real view model and the fakes at the edges: a badge applies the
 * theme to the whole app at once and stores it, a turn of the screen applies it again, and confirming moves on
 * to Park & Ride, reports the choice once, and sends one screen view for the two screens that are the Intro.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroFlowRouteThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }
    private val analytics = FakeAnalytics()
    private val sandook = FakeSandook()
    private val appliedThemes = mutableListOf<String>()

    private val viewModel = IntroFlowViewModel(
        savedStateHandle = savedAt(IntroState(step = IntroStep.THEME)),
        appDataReadiness = FakeAppDataReadiness(initial = StopsReadiness.Ready(stopCount = 100)),
        completion = IntroCompletion(sandook, FakeSandookPreferences(), analytics, Dispatchers.Unconfined),
        timeSource = TimeSource.Monotonic,
        parkRide = IntroParkRideGateway(
            catalogue = FakeParkRideCatalogue(stations = listOf(BELLA_VISTA_STATION)),
            availabilityLoader = FakeParkRideAvailabilityLoader(),
            parkRideSandook = FakeNswParkRideSandook(),
            ioDispatcher = Dispatchers.Unconfined,
            stopNameLookup = { null },
            analytics = analytics,
        ),
        stops = fakeStopsGateway(),
    )

    private fun show() {
        restorationTester.setContent {
            PreviewTheme {
                IntroFlowRoute(
                    viewModel = viewModel,
                    onNavigateToSavedTrips = {},
                    onApplyTheme = { appliedThemes += it },
                    userLocationManager = FakeUserLocationManager(),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun badge(style: KrailThemeStyle) =
        composeRule.onNodeWithTag(requireNotNull(introThemeChoices().first { it.style == style }.tag))

    private fun primary() = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)

    private fun screenViews() = analytics.getTrackedEvents("view_screen").map { it.properties?.get("name") }

    @Test
    fun `no theme is applied before the rider selects one, and the right button waits`() {
        show()

        composeRule.onNodeWithText(PICK_THEME_HEADING).assertIsDisplayed()
        assertEquals(emptyList(), appliedThemes)
        primary().assertIsNotEnabled()
    }

    @Test
    fun `a badge applies its theme to the app at once, stores it, and turns the right button on`() {
        show()

        badge(KrailThemeStyle.Metro).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(KrailThemeStyle.Metro.hexColorCode), appliedThemes)
        assertEquals(KrailThemeStyle.Metro.id.toLong(), sandook.getProductClass())
        primary().assertIsEnabled()
        composeRule.onNodeWithText(KrailThemeStyle.Metro.tagLine).assertIsDisplayed()
    }

    @Test
    fun `tapping one badge after another applies each theme in turn, and the right button stays on`() {
        show()

        listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro).forEach {
            badge(it).performClick()
            composeRule.waitForIdle()
        }

        assertEquals(listOf(KrailThemeStyle.Train.hexColorCode, KrailThemeStyle.Metro.hexColorCode), appliedThemes)
        primary().assertIsEnabled()
    }

    @Test
    fun `a turn of the screen applies the selected theme again, so the colour is never the default`() {
        show()
        badge(KrailThemeStyle.Ferry).performClick()
        composeRule.waitForIdle()
        appliedThemes.clear()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertEquals(listOf(KrailThemeStyle.Ferry.hexColorCode), appliedThemes)
        composeRule.onNodeWithText(KrailThemeStyle.Ferry.tagLine).assertIsDisplayed()
    }

    @Test
    fun `yes moves to Park and Ride, reports the choice once, and the Intro screen is viewed once`() {
        show()
        badge(KrailThemeStyle.Bus).performClick()
        composeRule.waitForIdle()

        primary().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(PARK_RIDE_HEADING).assertIsDisplayed()
        assertEquals(
            listOf<Any?>(KrailThemeStyle.Bus.id.toString()),
            analytics.getTrackedEvents("theme_selected").map { it.properties?.get("themeId") },
        )
        assertEquals(listOf<Any?>("Intro"), screenViews())
    }
}
