package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.fakes.FakeDeparturesService
import xyz.ksharma.krail.departures.ui.DepartureBoardRepository
import xyz.ksharma.krail.departures.ui.DeparturesViewModel
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

private const val TIMETABLE_RESTORE_TEST_SDK = 34

/**
 * The stop-details sheet a rider opens from the timetable header has to survive rotation.
 *
 * Which stop is open is held by the screen, not the ViewModel, so it is the screen's job to
 * make it saveable. Held in a plain `remember`, the sheet closes itself the moment the Activity
 * is recreated — a rider reading a departure board in portrait and turning the phone sideways
 * to see more of it is left staring at the timetable instead.
 *
 * ## What this test cannot see
 *
 * On a device the sheet still closes, for a second and separate reason: `ModalBottomSheet`
 * hosts its content in a real `Dialog`, and that dialog fires `onDismissRequest` as it is
 * disposed during recreation — which runs the caller's dismiss handler and clears the state
 * this test is about. `StateRestorationTester` saves and restores the composition without ever
 * building that dialog window, so it does not reproduce it. Confirmed by hand: after a theme
 * switch the sheet is gone and one tap on the same stop reopens it, which it would not do if
 * the state had survived. Every sheet in the app is wired the same way, so the fix belongs
 * with the sheet rather than here. Saving the state is still required either way — without it
 * there would be nothing left to restore once that is fixed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [TIMETABLE_RESTORE_TEST_SDK],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class TimeTableStopSheetRestoreTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    @Before
    fun setUp() {
        val departuresViewModel = DeparturesViewModel(
            repository = DepartureBoardRepository(
                departuresService = FakeDeparturesService(),
                ioDispatcher = Dispatchers.Main,
            ),
            analytics = NoOpAnalytics,
            ioDispatcher = Dispatchers.Main,
        )
        startKoin {
            modules(module { viewModel<DeparturesViewModel> { departuresViewModel } })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `the stop details sheet is still open after the screen is recreated`() {
        restorationTester.setContent {
            PreviewTheme {
                TimeTableScreen(
                    timeTableState = STATE_WITH_TRIP,
                    expandedJourneyId = null,
                    dateTimeSelectionItem = null,
                    onEvent = {},
                    onAlertClick = {},
                    onBackClick = {},
                    onJourneyLegClick = { _, _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Departures from $ORIGIN_NAME").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(STOP_ID_LABEL).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(STOP_ID_LABEL).assertIsDisplayed()
    }

    private companion object {
        const val ORIGIN_ID = "200060"
        const val ORIGIN_NAME = "Central Station"

        /** Only the sheet renders this, so finding it means the sheet is up. */
        const val STOP_ID_LABEL = "Stop ID - $ORIGIN_ID"

        val STATE_WITH_TRIP = TimeTableState(
            isLoading = false,
            trip = Trip(
                fromStopId = ORIGIN_ID,
                fromStopName = ORIGIN_NAME,
                toStopId = "200070",
                toStopName = "Town Hall Station",
            ),
        )
    }
}

/** Discards events; this test is about state survival, not attribution. */
private object NoOpAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserId(userId: String) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
}
