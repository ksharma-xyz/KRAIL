package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.lifecycle.SavedStateHandle
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.helpers.CallLog
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState

internal val TALLAWONG = SuggestedStop(stopId = "2155384", name = "Tallawong Station")
internal val CENTRAL = SuggestedStop(stopId = "200060", name = "Central Station")
internal val WYNYARD = SuggestedStop(stopId = "200080", name = "Wynyard Station")

/** Everything the Intro view model touches, as fakes that share one ordered [log]. */
internal class IntroRig(
    private val scope: KrailTestScope,
    initialReadiness: StopsReadiness = StopsReadiness.Ready(stopCount = 100),
    val handle: SavedStateHandle = SavedStateHandle(),
) {
    val log = CallLog()
    val readiness = FakeAppDataReadiness(initial = initialReadiness, callLog = log)
    val preferences = FakeSandookPreferences(callLog = log)
    val flag = FakeFlag()

    /** Built on first use, so a test can set the flag before the view model reads it. */
    val viewModel: IntroFlowViewModel by lazy {
        IntroFlowViewModel(
            savedStateHandle = handle,
            appDataReadiness = readiness,
            completion = IntroCompletion(preferences, scope.ioDispatcher),
            flag = flag,
        )
    }

    val state: IntroState get() = viewModel.state.value

    fun send(vararg events: IntroUiEvent) {
        events.forEach(viewModel::onEvent)
        scope.runCurrent()
    }
}

/** The saved state of a view model that had reached [state], as if recreated after process death. */
internal fun savedAt(state: IntroState): SavedStateHandle =
    SavedStateHandle().also { it.saveIntroState(state) }
