package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFinishAction
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStops
import xyz.ksharma.krail.trip.planner.ui.intro.introFinishAction
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer

/**
 * The two step Intro. Its state is saved as it changes, so the step, the answer and the picks
 * survive rotation and the process being killed. It never polls.
 *
 * The finish, and the Park & Ride step, come in later branches. Until then the bottom button only
 * handles Next and Try again, an answer of yes is only recorded, a location result is only noted,
 * and add to home does nothing.
 */
internal class IntroFlowViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val appDataReadiness: AppDataReadiness,
    private val completion: IntroCompletion,
    flag: Flag,
) : ViewModel() {

    private val _state = MutableStateFlow(savedStateHandle.restoreIntroState())
    val state: StateFlow<IntroState> = _state.asStateFlow()

    /** Read once, so the lists never change under the rider. */
    val suggestedStops: SuggestedStops = flag.introSuggestedStops()

    private var suggestionShownWritten = false

    fun onEvent(event: IntroUiEvent) {
        when (event) {
            IntroUiEvent.Back -> update { copy(step = IntroStep.PARK_RIDE) }
            IntroUiEvent.Next -> next()
            is IntroUiEvent.AnswerParkRide -> answerParkRide(event.yes)
            // The park and ride step completes these two. Until then the result is only recorded.
            is IntroUiEvent.LocationResultReceived -> update { copy(locationResult = event.result) }
            IntroUiEvent.ToggleParkRide -> Unit
            is IntroUiEvent.PickStart -> update { withPick(isStart = true, event.stop.stopId, event.stop.name) }
            is IntroUiEvent.PickEnd -> update { withPick(isStart = false, event.stop.stopId, event.stop.name) }
            is IntroUiEvent.StopPickedFromSearch -> update {
                withPick(event.isStart, event.stopId, event.stopName).copy(usedSearch = true)
            }
            IntroUiEvent.FinishPressed -> finishPressed()
        }
    }

    private fun next() {
        if (_state.value.step != IntroStep.PARK_RIDE) return
        update { copy(step = IntroStep.FIRST_TRIP) }
        viewModelScope.launch { ensureSuggestionShown() }
    }

    private fun answerParkRide(yes: Boolean) {
        if (yes) {
            update { copy(parkRideAnswer = ParkRideAnswer.YES) }
        } else {
            update { copy(parkRideAnswer = ParkRideAnswer.NO, step = IntroStep.FIRST_TRIP) }
            viewModelScope.launch { ensureSuggestionShown() }
        }
    }

    private fun finishPressed() {
        when (introFinishAction(_state.value, appDataReadiness.state.value)) {
            IntroFinishAction.Next -> next()
            IntroFinishAction.TryAgain -> appDataReadiness.retry()
            // The next branch completes the finish: wait for the stops, save the trip, write the
            // flags, track the outcome and leave. Until then these presses do nothing.
            IntroFinishAction.GettingReady,
            IntroFinishAction.SaveAndStart,
            IntroFinishAction.SkipForNow,
            IntroFinishAction.LetsKrail,
            -> Unit
        }
    }

    /** Written once, when the question is answered or passed. */
    private suspend fun ensureSuggestionShown() {
        if (suggestionShownWritten) return
        suggestionShownWritten = true
        completion.writeSuggestionShown()
    }

    // Always on the main thread, so a stale snapshot can never be saved over a newer one.
    private fun update(transform: IntroState.() -> IntroState) {
        _state.update(transform)
        savedStateHandle.saveIntroState(_state.value)
    }
}
