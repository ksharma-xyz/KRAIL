package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * The longest a finish waits for the stops, an operator decision. The wait happens under the finale's colour and never on
 * a button. Past it the finish goes ahead without them and the wait is reported as over five seconds.
 */
internal const val FINISH_WAIT_TIMEOUT_MS = 15_000L

/**
 * The Intro. Its state is saved as it changes, so the step, the answer and the picks survive rotation, the system
 * saving its state, and the app being swiped away or force stopped: the last is the [IntroProgress] kept in the app's
 * preferences, which a first run reads once when it opens and a replay from Settings clears and never writes. It
 * never polls: the things it waits on are
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

    // Opened again after the Intro was finished: a replay from Settings, which always starts clean.
    private val replay = completion.hasSeenIntro()

    private val _state = MutableStateFlow(savedStateHandle.openingIntroState(replay, completion.progress))
    val state: StateFlow<IntroState> = _state.asStateFlow()

    private val progressSaver = IntroProgressSaver(viewModelScope, completion.progress, _state.value)

    /** Read once, so the lists never change under the rider. */
    val suggestedStops: SuggestedStops = stops.suggestedStops

    private val effectChannel = Channel<IntroEffect>(Channel.BUFFERED)

    /** Delivered once to whoever collects, never replayed after rotation. */
    val effects: Flow<IntroEffect> = effectChannel.receiveAsFlow()

    private var lastStepShown: IntroStep? = null
    private var suggestionShownWritten = false
    // The finish press was accepted. From here no event changes anything.
    private var finishStarted = false

    // Everything the finish writes is written and the outcome is reported.
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
        // A replay starts at the first screen with nothing left over from before.
        if (replay) progressSaver.clear()
        // After a killed process the saved station id is worked out into a card again.
        parkRideStep.restore()
        stopsStep.loadModes(suggestedStops)
        warmStops()
    }

    /**
     * Gets the stops load going now, on the first screen, so it is normally Ready long before the last one and nothing at
     * the end has to wait for it. The load is the app launch's own job and is usually already running or done, and this
     * is the same call, [AppDataReadiness.retry]: a no-op while Loading or Ready, the first run (with its automatic
     * retries) from Unknown, and one more attempt from Failed. So it adds no network call, because the stops are bundled
     * and the load is a local insert, and no second run, because the claim is atomic. It costs one call when the Intro opens
     * and nothing at all when the load is under way, and it never suspends.
     */
    private fun warmStops() {
        when (appDataReadiness.state.value) {
            StopsReadiness.Unknown, is StopsReadiness.Failed -> appDataReadiness.retry()
            StopsReadiness.Loading, is StopsReadiness.Ready -> Unit
        }
    }

    fun onEvent(event: IntroUiEvent) {
        if (finishStarted) return
        when (event) {
            is IntroUiEvent.PickTheme -> themeStep.pick(event.themeId)
            IntroUiEvent.ConfirmTheme -> if (themeStep.confirm()) finish()
            is IntroUiEvent.AnswerParkRide -> answerParkRide(event.yes)
            is IntroUiEvent.LocationResultReceived -> onLocationResult(event.result, event.location)
            IntroUiEvent.AddParkRideAndContinue -> parkRideStep.addAndContinue()
            IntroUiEvent.DeclineParkRide -> parkRideStep.decline()
            is IntroUiEvent.SelectStop -> selectStop(event.stop)
            IntroUiEvent.ConfirmStop -> _state.value.selectedStop?.let(::chooseStop)
            is IntroUiEvent.QueryChanged -> if (_state.value.step.isStopScreen()) stopsStep.onQueryChanged(event.text)
            is IntroUiEvent.AnswerSaveTrip -> answerSaveTrip(event.yes)
        }
    }

    /**
     * The screen shows [step]. Reported as a screen view once each time the step on screen changes to a
     * different analytics screen, so a turn of the screen that shows the same step again reports nothing and
     * going between two screens that share a name reports nothing more. After the process is killed the restored
     * step is reported once more, as a new run.
     */
    fun onStepShown(step: IntroStep) {
        val previous = lastStepShown
        if (step == previous) return
        lastStepShown = step
        if (shouldTrackScreenView(previous, step)) completion.trackStepView(step)
    }

    // A tap on a row selects the stop and nothing more: it is highlighted, the button appears, and a tap on another row
    // moves the selection. The Intro stays where it is.
    private fun selectStop(stop: IntroStop) {
        if (!_state.value.step.isStopScreen()) return
        update { withStopSelected(stop) }
    }

    // The right button ends here: the selected stop is the end or the start, whichever screen it is, what was typed is
    // dropped, and the Intro moves on in the flow as it stands after the pick.
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

    // Yes, save it and Not now only record the answer and move on to the theme screen. Nothing is written here, and an
    // answer from any other screen does nothing.
    private fun answerSaveTrip(yes: Boolean) {
        if (_state.value.step != IntroStep.CONFIRM) return
        val decision = if (yes) TripDecision.SAVED else TripDecision.DECLINED
        update { copy(tripDecision = decision).movedOn() }
    }

    // The press that ends the Intro. It is never held back and never silent: the finale starts in this same step, before
    // anything is written or waited for, and a second press, or any other event, does nothing from then on. A press on any
    // screen but the theme screen, or with no theme chosen, does nothing.
    private fun finish() {
        if (!_state.value.canFinish() || finishStarted) return
        finishStarted = true
        effectChannel.trySend(IntroEffect.StartFinale)
        viewModelScope.launch { runFinish() }
    }

    /**
     * Everything the finish does after the press, in this order, with the finale already playing over the screen:
     *
     * 1. The trip, when the rider said yes to saving it and there is a complete pair, and the two flags. They come from the
     *    rider's own picks, ids and names, and need no stops table, so they are local writes and wait for nothing. They are
     *    first so that a process killed during the finale, however long it holds, finds the Intro already seen and the trip
     *    already saved. Until they are written a kill only means the rider presses once more, which writes the same trip
     *    again with the same id.
     * 2. The wait for the stops, at most [FINISH_WAIT_TIMEOUT_MS], under the colour the finale holds at full cover. It ends on
     *    Ready, on Failed, or at the bound, and the finish goes ahead whichever it was. Past the bound the load is asked to
     *    carry on in the background, once. A stops load that was warmed when the Intro opened is normally Ready by now and
     *    the wait is nothing.
     * 3. The outcome is reported, and only then does [IntroEffect.NavigateToSavedTrips] let the app be shown. The wait that
     *    is reported is the time from the press until the stops were Ready, Failed or the bound, so it keeps its meaning,
     *    though the first seconds of it now play out under the finale's own animation and not on a dead button.
     */
    private suspend fun runFinish() {
        val wasReady = appDataReadiness.state.value is StopsReadiness.Ready
        val pressed = timeSource.markNow()

        val trip = _state.value.tripToWrite(save = _state.value.tripDecision == TripDecision.SAVED)
        if (trip != null) {
            completion.saveTrip(trip)
            update { copy(saved = true) }
        }
        ensureSuggestionShown()
        completion.writeIntroSeen()

        val readiness = appDataReadiness.awaitReady(FINISH_WAIT_TIMEOUT_MS)
        // A real clock never reads exactly zero, so no wait is decided here, not by the bucket.
        val waited = if (wasReady) Duration.ZERO else pressed.elapsedNow()
        // A no-op while Loading. From Failed it is one more attempt, from Unknown the launch trigger.
        if (readiness !is StopsReadiness.Ready) appDataReadiness.retry()

        completion.track(_state.value.toOutcome(stopsWaitBucket(waited, readiness)), savedTrip = trip)
        finished = true
        effectChannel.send(IntroEffect.NavigateToSavedTrips)
        // Once the finale has been handed on, nothing of the progress is left, and nothing writes it again.
        progressSaver.clear()
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
        // One small write when the progress changed, none on a replay and none once the finish has completed.
        if (!replay && !finished) progressSaver.save(_state.value)
    }
}
