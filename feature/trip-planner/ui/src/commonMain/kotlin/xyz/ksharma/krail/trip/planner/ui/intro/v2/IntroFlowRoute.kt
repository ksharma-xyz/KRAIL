package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager

/**
 * The two step Intro with its view model: the screen, and the platform glue the screen needs and
 * the view model does not have.
 *
 * - State and readiness are collected with the lifecycle, and the one shot [IntroEffect]s only while
 *   the screen is started, so none is lost and none is replayed after a turn of the screen.
 * - The location permission and the one fix are [IntroLocationEffect], which sends one result for
 *   each yes.
 * - The `view_screen` event is the view model's, asked for once each time the step on screen
 *   changes. It is not by route: `RouteAnalyticsScreen` maps routes for the window events and is
 *   not where `view_screen` is sent.
 *
 * [firstTripStep] is the content of step 2, which the first trip branch supplies, and
 * [onNavigateToSavedTrips] is where the finish goes.
 */
@Composable
internal fun IntroFlowRoute(
    viewModel: IntroFlowViewModel,
    onNavigateToSavedTrips: () -> Unit,
    modifier: Modifier = Modifier,
    userLocationManager: UserLocationManager = rememberUserLocationManager(),
    firstTripStep: IntroStepContent = EmptyIntroStep,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val readiness by viewModel.stopsReadiness.collectAsStateWithLifecycle()

    IntroEffectsCollector(viewModel = viewModel, onNavigateToSavedTrips = onNavigateToSavedTrips)

    LaunchedEffect(viewModel, state.step) { viewModel.onStepShown(state.step) }

    IntroLocationEffect(
        awaiting = state.isAwaitingLocation(),
        userLocationManager = userLocationManager,
        onResult = { result, location ->
            viewModel.onEvent(IntroUiEvent.LocationResultReceived(result, location))
        },
    )

    IntroFlowScreen(
        state = state,
        suggestedStops = viewModel.suggestedStops,
        readiness = readiness,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        firstTripStep = firstTripStep,
    )
}

@Composable
private fun IntroEffectsCollector(viewModel: IntroFlowViewModel, onNavigateToSavedTrips: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigate by rememberUpdatedState(onNavigateToSavedTrips)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    IntroEffect.NavigateToSavedTrips -> navigate()
                }
            }
        }
    }
}
