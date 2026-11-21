package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.flow.onEach
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.helpers.CallLog
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
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
) {
    val log = CallLog()
    val readiness = FakeAppDataReadiness(initial = initialReadiness, callLog = log)
    val sandook = FakeSandook(callLog = log)
    val preferences = FakeSandookPreferences(callLog = log)
    val analytics = FakeAnalytics(callLog = log)
    val flag = FakeFlag()

    /** Moves only when a test says so. Ignored when the rig was given its own [timeSource]. */
    val testTime = TestTimeSource()

    /** Built on first use, so a test can set the flag before the view model reads it. */
    val viewModel: IntroFlowViewModel by lazy {
        IntroFlowViewModel(
            savedStateHandle = handle,
            appDataReadiness = readiness,
            completion = IntroCompletion(sandook, preferences, analytics, scope.ioDispatcher),
            flag = flag,
            timeSource = timeSource ?: testTime,
        )
    }

    val state: IntroState get() = viewModel.state.value

    fun send(vararg events: IntroUiEvent) {
        events.forEach(viewModel::onEvent)
        scope.runCurrent()
    }

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
