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
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
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
    private val timeSource: TimeSource,
    parkRide: IntroParkRideGateway,
    stops: IntroStopsGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(savedStateHandle.restoreIntroState())
    val state: StateFlow<IntroState> = _state.asStateFlow()

    /** The stops the last button waits on, for the screen to read. The view model adds nothing to it. */
    val stopsReadiness: StateFlow<StopsReadiness> get() = appDataReadiness.state

    /** Read once, so the lists never change under the rider. */
    val suggestedStops: SuggestedStops = stops.suggestedStops

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

    private val themeStep = IntroThemeStep(
        scope = viewModelScope,
        completion = completion,
        read = { _state.value },
        write = ::update,
    )

    // The stations near the rider and the badges of the stops the screens list, worked out in the background.
    private val stopsStep = IntroStopsStep(
        scope = viewModelScope,
        readiness = appDataReadiness,
        gateway = stops,
        read = { _state.value },
        write = ::update,
    )

    init {
        // After a killed process the saved station id is worked out into a card again.
        parkRideStep.restore()
        stopsStep.loadModes(suggestedStops)
    }

    fun onEvent(event: IntroUiEvent) {
        when (event) {
            // Back goes one screen back, and does nothing on the first. What was typed on a screen is not kept.
            IntroUiEvent.Back -> {
                stopsStep.cancelSearch()
                update { movedBack().withoutSearch() }
            }
            is IntroUiEvent.PickTheme -> themeStep.pick(event.themeId)
            IntroUiEvent.ConfirmTheme -> themeStep.confirm()
            is IntroUiEvent.AnswerParkRide -> answerParkRide(event.yes)
            is IntroUiEvent.LocationResultReceived -> onLocationResult(event.result, event.location)
            IntroUiEvent.AddParkRideAndContinue -> parkRideStep.addAndContinue()
            IntroUiEvent.DeclineParkRide -> parkRideStep.decline()
            is IntroUiEvent.ChooseStop -> chooseStop(event.stop)
            IntroUiEvent.UseHighlightedStop -> _state.value.highlightedStop(suggestedStops)?.let(::chooseStop)
            // The screen has the search field, so it answers this one. It never chooses and never leaves a screen.
            IntroUiEvent.FocusSearch -> Unit
            is IntroUiEvent.QueryChanged -> if (_state.value.step.isStopScreen()) stopsStep.onQueryChanged(event.text)
            IntroUiEvent.FinishPressed -> finish(save = true)
            IntroUiEvent.FinishWithoutTrip -> finish(save = false)
        }
    }

    /**
     * The screen shows [step]. Reported as a screen view once each time the step on screen changes to a
     * different analytics screen, so a turn of the screen that shows the same step again reports nothing,
     * going from the theme screen to Park & Ride (both the Intro) reports nothing more, and going back to
     * an earlier screen with a different name is a new visit. After the process is killed the restored step
     * is reported once more, as a new run.
     */
    fun onStepShown(step: IntroStep) {
        val previous = lastStepShown
        if (step == previous) return
        lastStepShown = step
        if (shouldTrackScreenView(previous, step)) completion.trackStepView(step)
    }

    // A tap on a row and the right button both end here: the stop is the end or the start, whichever screen it is,
    // what was typed is dropped, and the Intro moves on in the flow as it stands after the pick.
    private fun chooseStop(stop: IntroStop) {
        if (!_state.value.step.isStopScreen()) return
        stopsStep.cancelSearch()
        update { withStopChosen(stop) }
    }

    // The fix the Park & Ride question took is the only one, and the stations near the rider come from it too. It is
    // handed on in the same call and held by neither.
    private fun onLocationResult(result: LocationResult, location: LatLng?) {
        val taken = parkRideStep.onLocationResult(result, location)
        if (taken && result == LocationResult.GRANTED && location != null) stopsStep.onFix(location)
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
    // FINISH_WAIT_TIMEOUT_MS, and then goes ahead. A press on any screen but the confirm screen does nothing.
    private fun finish(save: Boolean) {
        if (!_state.value.step.canFinish()) return
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

        if (!_state.value.step.canFinish()) return
        // A no-op while Loading. From Failed it is one more attempt, from Unknown the launch trigger.
        if (readiness !is StopsReadiness.Ready) appDataReadiness.retry()

        val trip = _state.value.tripToWrite(save)
        if (trip != null) {
            completion.saveTrip(trip)
            update { copy(saved = true) }
        }
        ensureSuggestionShown()
        completion.writeIntroSeen()
        completion.track(_state.value.toOutcome(stopsWaitBucket(waited, readiness), declined = !save), savedTrip = trip)
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
