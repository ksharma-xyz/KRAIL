package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * The longest a finish waits for the stops, an operator decision. Past it the finish goes ahead
 * without them and the wait is reported as over five seconds.
 */
internal const val FINISH_WAIT_TIMEOUT_MS = 15_000L

/**
 * The Intro. Its state is saved as it changes, so the step, the answer and the picks survive rotation
 * and the process being killed. It never polls: the things it waits on are
 * [AppDataReadiness.awaitReady], once for the nearest station and once at the finish, and neither
 * wait blocks for good.
 *
 * The Park & Ride step is [IntroParkRideStep]. The screen asks for the location permission and sends
 * the result, and this class never asks for it.
 */
internal class IntroFlowViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val appDataReadiness: AppDataReadiness,
    private val completion: IntroCompletion,
    flag: Flag,
    private val timeSource: TimeSource,
    parkRide: IntroParkRideGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(savedStateHandle.restoreIntroState())
    val state: StateFlow<IntroState> = _state.asStateFlow()

    /** The stops the last button waits on, for the screen to read. The view model adds nothing to it. */
    val stopsReadiness: StateFlow<StopsReadiness> get() = appDataReadiness.state

    /** Read once, so the lists never change under the rider. */
    val suggestedStops: SuggestedStops = flag.introSuggestedStops()

    private val effectChannel = Channel<IntroEffect>(Channel.BUFFERED)

    /** Delivered once to whoever collects, never replayed after rotation. */
    val effects: Flow<IntroEffect> = effectChannel.receiveAsFlow()

    private var lastStepShown: IntroStep? = null
    private var suggestionShownWritten = false
    private var finishJob: Job? = null
    private var finished = false

    private val parkRideStep = IntroParkRideStep(
        scope = viewModelScope,
        readiness = appDataReadiness,
        gateway = parkRide,
        read = { _state.value },
        write = ::update,
        onLeftQuestion = ::markSuggestionShown,
    )

    init {
        // After a killed process the saved station id is worked out into a card again.
        parkRideStep.restore()
    }

    fun onEvent(event: IntroUiEvent) {
        when (event) {
            // Back goes one screen back, and does nothing on the first.
            IntroUiEvent.Back -> update { movedBack() }
            is IntroUiEvent.AnswerParkRide -> answerParkRide(event.yes)
            is IntroUiEvent.LocationResultReceived -> parkRideStep.onLocationResult(event.result, event.location)
            IntroUiEvent.AddParkRideAndContinue -> parkRideStep.addAndContinue()
            IntroUiEvent.DeclineParkRide -> parkRideStep.decline()
            is IntroUiEvent.PickStart -> update { withPick(isStart = true, event.stop.stopId, event.stop.name) }
            is IntroUiEvent.PickEnd -> update { withPick(isStart = false, event.stop.stopId, event.stop.name) }
            is IntroUiEvent.StopPickedFromSearch -> update {
                withPick(event.isStart, event.stopId, event.stopName).copy(usedSearch = true)
            }
            IntroUiEvent.FinishPressed -> finish(save = true)
            IntroUiEvent.FinishWithoutTrip -> finish(save = false)
        }
    }

    /**
     * The screen shows [step]. Reported as a screen view once each time the step on screen changes,
     * so a turn of the screen that shows the same step again reports nothing, and going back to an
     * earlier screen is a new visit. After the process is killed the restored step is reported once more, as a new run.
     */
    fun onStepShown(step: IntroStep) {
        if (step == lastStepShown) return
        lastStepShown = step
        completion.trackStepView(step)
    }

    // Yes starts the location step, and the screen then sends LocationResultReceived. No ends it and moves on.
    private fun answerParkRide(yes: Boolean) {
        if (yes) {
            parkRideStep.askLocation()
        } else {
            parkRideStep.cancel()
            update {
                copy(parkRideAnswer = ParkRideAnswer.NO, parkRideCard = ParkRideCardPhase.None).leavingParkRide()
            }
            markSuggestionShown()
        }
    }

    private fun markSuggestionShown() {
        viewModelScope.launch { ensureSuggestionShown() }
    }

    // A press before the stops are ready is not dropped: the finish waits for them, up to
    // FINISH_WAIT_TIMEOUT_MS, and then goes ahead. A press on a screen that does not ask about a trip
    // does nothing.
    private fun finish(save: Boolean) {
        if (!_state.value.step.isTripStep()) return
        if (finished || finishJob?.isActive == true) return
        finishJob = viewModelScope.launch { runFinish(save) }
    }

    /**
     * Waits for the stops, writes the trip if there is one, marks the Intro seen, reports the
     * outcome and only then tells the screen to leave. The wait ends on Ready, on Failed, or after
     * FINISH_WAIT_TIMEOUT_MS, and the finish goes ahead whichever it was. When the stops are not
     * Ready by then the load is asked to carry on in the background, once. A press that no longer has
     * a screen to finish, because the rider went back while it waited, is abandoned. [save] is the
     * positive answer: the trip is written when there is a complete pair, and with the negative answer
     * none is.
     */
    private suspend fun runFinish(save: Boolean) {
        val wasReady = appDataReadiness.state.value is StopsReadiness.Ready
        val waitStarted = timeSource.markNow()
        val readiness = appDataReadiness.awaitReady(FINISH_WAIT_TIMEOUT_MS)
        // A real clock never reads exactly zero, so no wait is decided here, not by the bucket.
        val waited = if (wasReady) Duration.ZERO else waitStarted.elapsedNow()

        if (!_state.value.step.isTripStep()) return
        // A no-op while Loading. From Failed it is one more attempt, from Unknown the launch trigger.
        if (readiness !is StopsReadiness.Ready) appDataReadiness.retry()

        val trip = _state.value.tripToWrite(save)
        if (trip != null) {
            completion.saveTrip(trip)
            update { copy(saved = true) }
        }
        ensureSuggestionShown()
        completion.writeIntroSeen()
        completion.track(_state.value.toOutcome(stopsWaitBucket(waited, readiness)), savedTrip = trip)
        finished = true
        effectChannel.send(IntroEffect.NavigateToSavedTrips)
    }

    /** Written once, at the first of: the question answered or passed, or the finish. */
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
