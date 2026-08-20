package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.fakes.FakeDeparturesService
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.ui.DepartureBoardConfig
import xyz.ksharma.krail.departures.ui.DepartureBoardRepository
import xyz.ksharma.krail.departures.ui.DeparturesViewModel
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The departure board's countdown has to keep counting down.
 *
 * [DeparturesViewModel.isActive] is the 10-second ticker that recomputes "in X mins" for every
 * row on screen. It is a `SharingStarted.WhileSubscribed` flow, so it runs only while something
 * collects it — and the surfaces that show a departure board are the only things that can.
 * This test opens the real map stop sheet and checks the number still moves once the sheet has
 * been sitting there a while, which is the whole point of the ticker and is invisible to every
 * ViewModel-level test (they subscribe to the flow themselves and so can never see it unwired).
 *
 * The clock is injected rather than waited on: "now" is the only input to the recomputation,
 * and a test that advanced real time by a minute to watch one string change would be a minute
 * of CI for one assertion.
 */
@OptIn(ExperimentalTime::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [TICKER_TEST_SDK],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class DeparturesRelativeTimeTickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val service = FakeDeparturesService()

    private val departureAt: Instant = Clock.System.now() + FAR_OUT

    private var clockTheRiderIsWatching: Instant = Clock.System.now()

    private lateinit var departuresViewModel: DeparturesViewModel

    @Before
    fun setUp() {
        service.response = responseDepartingAt(departureAt)
        departuresViewModel = DeparturesViewModel(
            repository = DepartureBoardRepository(
                departuresService = service,
                // Robolectric's main looper, so every fetch and every tick lands on the queue
                // the test drives rather than on a real background thread.
                ioDispatcher = Dispatchers.Main,
                // Far longer than anything this test advances: a re-fetch would recompute the
                // text through the mapper and hide whether the ticker ever ran.
                config = DepartureBoardConfig(refreshIntervalMs = NO_REFETCH_INTERVAL_MS),
            ),
            analytics = NoOpAnalytics,
            ioDispatcher = Dispatchers.Main,
            now = { clockTheRiderIsWatching },
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
    fun `the countdown keeps ticking while the stop sheet stays open`() {
        composeRule.setContent {
            PreviewTheme {
                StopDetailsBottomSheet(stop = CENTRAL, onDismiss = {})
            }
        }
        composeRule.waitForIdle()

        departuresViewModel.onEvent(
            DeparturesUiEvent.LoadDepartures(
                stopId = CENTRAL.stopId,
                stopName = CENTRAL.stopName,
                source = DepartureBoardSource.MAP_SHEET,
            ),
        )
        letTimePass(SETTLE_MILLIS)

        assertTrue(
            departuresViewModel.uiState.value.departures.isNotEmpty(),
            "The sheet's own uiState collection should have started the poll and loaded a row",
        )

        // The rider keeps the sheet open and the departure draws nearer.
        clockTheRiderIsWatching = departureAt - CLOSE_IN
        letTimePass(TICKER_INTERVAL_MILLIS)

        assertEquals(
            "in 2 mins",
            departuresViewModel.uiState.value.departures.first().relativeTimeText,
            "Relative time froze at whatever the fetch computed — nothing is collecting " +
                "DeparturesViewModel.isActive, so the 10s ticker never runs",
        )
    }

    /** Advance the frame clock and the looper together, the way `FlowTest.letTimePass` does. */
    private fun letTimePass(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS)
        composeRule.waitForIdle()
    }

    private fun responseDepartingAt(instant: Instant) = DepartureMonitorResponse(
        stopEvents = listOf(
            DepartureMonitorResponse.StopEvent(
                departureTimePlanned = instant.toString(),
                departureTimeEstimated = null,
                transportation = DepartureMonitorResponse.Transportation(
                    id = "T1",
                    disassembledName = "T1",
                    destination = DepartureMonitorResponse.Destination(
                        id = "dest",
                        name = "Berowra",
                    ),
                    product = DepartureMonitorResponse.Product(cls = 1, iconId = 1, name = "Train"),
                ),
                location = null,
            ),
        ),
    )

    private companion object {

        val CENTRAL = NearbyStopFeature(
            stopId = "200060",
            stopName = "Central Station",
            position = LatLng(-33.8830, 151.2070),
            transportModes = persistentListOf(TransportMode.Train),
        )

        /** Far enough out that the fetch-time text cannot be mistaken for the ticked one. */
        val FAR_OUT = 45.minutes

        /** Where the rider's clock is moved to, expressed as "in 2 mins". */
        val CLOSE_IN = 2.minutes

        const val NO_REFETCH_INTERVAL_MS = 600_000L
        const val SETTLE_MILLIS = 500L
        const val TICKER_INTERVAL_MILLIS = 10_000L
    }
}

internal const val TICKER_TEST_SDK = 34

/** Discards events; this test is about the ticker, not attribution. */
private object NoOpAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserId(userId: String) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
}
