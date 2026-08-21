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
 * It composes the screen, not the window around it, so anything that only happens to a real
 * Activity is out of its reach: which pane the layout picks, and what other callbacks fire
 * while the window is being rebuilt.
 *
 * An earlier version of this note claimed the sheet also closes on device because
 * `ModalBottomSheet`'s dialog fires `onDismissRequest` as it is disposed during recreation.
 * Measured on an emulator, that does not happen: a probe logging every `onDismissRequest`
 * with its lifecycle state recorded none at all across a rotation, and the sheets whose
 * visibility is saveable came back on screen. The dismissals it did record were scrim taps at
 * `RESUMED`. What actually closed sheets was ordinary lost state, and on the search-stop map a
 * text callback that re-fired with restored text and switched the screen back to the list.
 * See `docs/learning/2026-08-21-sheet-closed-by-a-text-callback.md`.
 *
 * One case remains, and this test cannot reach it either: on a phone, rotating swaps single
 * pane for dual pane, which are different composables, so a sheet open in one does not carry
 * into the other.
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
