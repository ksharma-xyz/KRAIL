package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.trip.planner.ui.intro.finale.IntroFinaleSignal

/**
 * The Intro with its view model: the screen, and the platform glue the screen needs and
 * the view model does not have.
 *
 * - State and readiness are collected with the lifecycle, and the one shot [IntroEffect]s only while
 *   the screen is started, so none is lost and none is replayed after a turn of the screen.
 * - The location permission and the one fix are [IntroLocationEffect], which sends one result for
 *   each yes.
 * - The `view_screen` event is the view model's, asked for once each time the step on screen
 *   changes. It is not by route: `RouteAnalyticsScreen` maps routes for the window events and is
 *   not where `view_screen` is sent.

 * - The theme the rider selects on the first screen is applied to the whole app through [onApplyTheme], so every
 *   screen after it, and the buttons on this one, take its colour at once. The theme is also stored when it is
 *   selected, so the app starts in it however the Intro ends.
 *
 * [onNavigateToSavedTrips] is where the finish goes, through [onIntroFinished]. The finish starts the finale on
 * [finale] and Saved Trips is opened only when the finale asks, at full cover, so the app is composed under a
 * solid colour. This route is the one that navigates, with the callback it has now, and says so to the finale, so a
 * turn of the screen in the middle of it never leaves a callback for a screen that is gone. With no finale host
 * attached, as in a test, the finish goes straight to Saved Trips.
 */
@Composable
internal fun IntroFlowRoute(
    viewModel: IntroFlowViewModel,
    onNavigateToSavedTrips: () -> Unit,
    onApplyTheme: (hexColorCode: String) -> Unit,
    modifier: Modifier = Modifier,
    userLocationManager: UserLocationManager = rememberUserLocationManager(),
    finale: IntroFinaleSignal = remember { IntroFinaleSignal() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val readiness by viewModel.stopsReadiness.collectAsStateWithLifecycle()

    IntroEffectsCollector(
        viewModel = viewModel,
        finale = finale,
        themeHexColorCode = finaleThemeHex(state.selectedThemeId),
        onNavigateToSavedTrips = onNavigateToSavedTrips,
    )
    FinaleNavigationEffect(finale = finale, onNavigateToSavedTrips = onNavigateToSavedTrips)

    LaunchedEffect(viewModel, state.step) { viewModel.onStepShown(state.step) }

    // The whole app takes the colour of the theme that is selected, as the Settings theme screen does, and it
    // does so again each time the Intro is composed with one selected, such as after a turn of the screen.
    val applyTheme by rememberUpdatedState(onApplyTheme)
    LaunchedEffect(state.selectedThemeId) { themeHexFor(state.selectedThemeId)?.let { applyTheme(it) } }

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
    )
}

/**
 * What happens once the Intro has saved or declined the trip, written everything it writes and reported its outcome:
 * it starts the finale on [finale] in the colour [themeHexColorCode], and Saved Trips is opened later, when the finale
 * asks (see [FinaleNavigationEffect]). Nothing is lost if the process is killed meanwhile, because the Intro is already
 * marked seen. With no finale host attached it goes to Saved Trips straight away through [navigateToSavedTrips]. It
 * is the one place the end of the Intro is handed on from.
 */
internal fun onIntroFinished(
    finale: IntroFinaleSignal,
    themeHexColorCode: String,
    navigateToSavedTrips: () -> Unit,
) {
    if (!finale.start(themeHexColorCode)) navigateToSavedTrips()
}

@Composable
private fun IntroEffectsCollector(
    viewModel: IntroFlowViewModel,
    finale: IntroFinaleSignal,
    themeHexColorCode: String,
    onNavigateToSavedTrips: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigate by rememberUpdatedState(onNavigateToSavedTrips)
    val themeHex by rememberUpdatedState(themeHexColorCode)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    IntroEffect.NavigateToSavedTrips -> onIntroFinished(finale, themeHex, navigate)
                }
            }
        }
    }
}

/**
 * Navigates to Saved Trips each time the finale asks, which is once, when the colour covers the screen, and says so.
 * It collects only while the screen is started, like the Intro's other effects, and takes the callback as it is now,
 * so a turn of the screen between the ask and the answer navigates with the screen that is there.
 */
@Composable
private fun FinaleNavigationEffect(finale: IntroFinaleSignal, onNavigateToSavedTrips: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val navigate by rememberUpdatedState(onNavigateToSavedTrips)
    LaunchedEffect(finale, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            finale.navigateRequests.collect {
                navigate()
                finale.onNavigated()
            }
        }
    }
}
