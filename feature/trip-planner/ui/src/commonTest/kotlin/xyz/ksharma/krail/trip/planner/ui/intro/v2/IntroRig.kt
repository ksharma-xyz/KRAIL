package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.flow.onEach
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.helpers.CallLog
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.TestTimeSource
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal val TALLAWONG = SuggestedStop(stopId = "2155384", name = "Tallawong Station")
internal val CENTRAL = SuggestedStop(stopId = "200060", name = "Central Station")
internal val WYNYARD = SuggestedStop(stopId = "200080", name = "Wynyard Station")

/** Everything the Intro view model touches, as fakes that share one ordered [log]. */
internal class IntroRig(
    private val scope: KrailTestScope,
    initialReadiness: StopsReadiness = StopsReadiness.Ready(stopCount = 100),
    val handle: SavedStateHandle = SavedStateHandle(),
    timeSource: TimeSource? = null,
    stations: List<ParkRideStationPickerItem> = INTRO_STATIONS,
    availabilityLoader: ParkRideAvailabilityLoader? = null,
    val parkRideSandook: FakeNswParkRideSandook = FakeNswParkRideSandook(),
) {
    val log = CallLog()
    val readiness = FakeAppDataReadiness(initial = initialReadiness, callLog = log)
    val sandook = FakeSandook(callLog = log)
    val preferences = FakeSandookPreferences(callLog = log)
    val analytics = FakeAnalytics(callLog = log)
    val flag = FakeFlag()
    val catalogue = FakeParkRideCatalogue(stations = stations)

    /** Records each refresh the Park & Ride step asks for. Unused when the rig was given a loader. */
    val loader = FakeParkRideAvailabilityLoader()

    /** Moves only when a test says so. Ignored when the rig was given its own [timeSource]. */
    val testTime = TestTimeSource()

    private val store = ViewModelStore()

    /** Built on first use, so a test can set the flag before the view model reads it. */
    val viewModel: IntroFlowViewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                IntroFlowViewModel(
                    savedStateHandle = handle,
                    appDataReadiness = readiness,
                    completion = IntroCompletion(sandook, preferences, analytics, scope.ioDispatcher),
                    flag = flag,
                    timeSource = timeSource ?: testTime,
                    parkRide = IntroParkRideGateway(
                        catalogue = catalogue,
                        availabilityLoader = availabilityLoader ?: loader,
                        parkRideSandook = parkRideSandook,
                        ioDispatcher = scope.ioDispatcher,
                    ),
                )
            }
        }
        ViewModelProvider.create(store, factory)[IntroFlowViewModel::class]
    }

    val state: IntroState get() = viewModel.state.value

    fun send(vararg events: IntroUiEvent) {
        events.forEach(viewModel::onEvent)
        scope.runCurrent()
    }

    /** The station on the card, which the test expects to be showing. */
    fun card() = assertIs<ParkRideCardPhase.Card>(state.parkRideCard).station

    /** A rider who answered Yes and is shown the nearest station, Bella Vista here. */
    fun showBellaVista() = send(
        IntroUiEvent.AnswerParkRide(yes = true),
        IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
    )

    fun showTallawong() = send(
        IntroUiEvent.AnswerParkRide(yes = true),
        IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG),
    )

    /** Builds the view model if it is not built yet, then lets everything it started at this instant run. */
    fun settle() = send()

    /** The refresh a real loader does, as far as the Intro can tell: the rows it wrote. */
    fun refreshWritesBellaVista(spots: Long = 120, percentFull: Long = 40, time: String = "8:05 AM") {
        loader.onRefresh = {
            parkRideSandook.insertOrReplaceAll(
                listOf(
                    reading(facilityId = "31", stopId = "2153478", spots = spots, percentFull = percentFull, time = time),
                ),
            )
        }
    }

    /** What the system does when the screen is gone for good: the view model is cleared. */
    fun clearViewModel() = store.clear()

    /** Collects the finish effects, noting each one in [log] at the moment it is emitted. */
    suspend fun withEffects(block: suspend ReceiveTurbine<IntroEffect>.() -> Unit) {
        viewModel.effects.onEach { log.record("effect") }.test {
            block()
            cancelAndIgnoreRemainingEvents()
        }
    }

    fun outcomeEvent(): IntroLetsKrailClickEvent =
        analytics.getTrackedEvents("intro_lets_krail").single() as IntroLetsKrailClickEvent
}

/** The saved state of a view model that had reached [state], as if recreated after process death. */
internal fun savedAt(state: IntroState): SavedStateHandle =
    SavedStateHandle().also { it.saveIntroState(state) }

/**
 * Every reading of elapsed time moves on by [tick], as a real monotonic clock does between two
 * calls: it never reports exactly zero.
 */
internal class TickingTimeSource(private val tick: Duration) : TimeSource {
    private var elapsed = Duration.ZERO

    override fun markNow(): TimeMark = object : TimeMark {
        override fun elapsedNow(): Duration {
            elapsed += tick
            return elapsed
        }
    }
}
