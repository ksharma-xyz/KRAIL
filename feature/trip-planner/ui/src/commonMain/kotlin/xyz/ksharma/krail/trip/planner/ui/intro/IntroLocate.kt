package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.withTimeoutOrNull
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationError
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.coroutines.cancellation.CancellationException

/**
 * The longest the screen waits for the permission and the one fix together, so a card is never
 * left Locating for good. The location manager already bounds the fix at ten seconds. This bound
 * is for what it does not cover, the rider reading the permission dialog, and is long enough that
 * nobody doing that reaches it.
 */
internal const val LOCATE_BOUND_MS = 120_000L

/** What the screen does about the location, given whether the card awaits it and whether it has sent it. */
internal enum class LocateStep {
    /** Not awaiting, so the next time the card awaits a location is a new yes. */
    Reset,

    /** Awaiting and nothing sent for this yes: ask and take the one fix. */
    Locate,

    /** Awaiting and already sent for this yes. The view model is working it out, so do nothing. */
    Wait,
}

internal fun locateStep(awaiting: Boolean, sent: Boolean): LocateStep = when {
    !awaiting -> LocateStep.Reset
    sent -> LocateStep.Wait
    else -> LocateStep.Locate
}

/**
 * The card waits for the permission and the fix. A Locating card that carries a station id is one
 * being worked out again after a killed process, which asks for nothing.
 */
internal fun IntroState.isAwaitingLocation(): Boolean {
    val card = parkRideCard
    return card is ParkRideCardPhase.Locating && card.stationId == null
}

/** Only a refusal is a denial. Every other failure is no fix, which the Intro treats alike. */
internal fun locationResultOf(error: Throwable): LocationResult =
    if (error is LocationError.PermissionDenied) LocationResult.DENIED else LocationResult.UNAVAILABLE

/**
 * Asks for the permission if it is not decided, takes one fix, and calls [onResult] exactly once with
 * the answer: granted with the fix, denied, or unavailable for a failure of any kind and for a wait
 * past [LOCATE_BOUND_MS]. A cancelled caller gets no call.
 *
 * The fix goes from the location manager straight to [onResult]. It is not stored, not logged and
 * not put in any state here.
 */
internal suspend fun UserLocationManager.locateOnce(onResult: (LocationResult, LatLng?) -> Unit) {
    val outcome: Result<Location>? = runCatching { withTimeoutOrNull(LOCATE_BOUND_MS) { getCurrentLocation() } }
        .onFailure { if (it is CancellationException) throw it }
        .getOrElse { Result.failure(it) }

    if (outcome == null) {
        onResult(LocationResult.UNAVAILABLE, null)
    } else {
        outcome.fold(
            onSuccess = { onResult(LocationResult.GRANTED, LatLng(it.latitude, it.longitude)) },
            onFailure = { onResult(locationResultOf(it), null) },
        )
    }
}
