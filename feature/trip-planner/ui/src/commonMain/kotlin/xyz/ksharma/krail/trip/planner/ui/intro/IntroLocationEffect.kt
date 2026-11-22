package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult

/**
 * Sends one location result for each yes, and never a second.
 *
 * The state does not say whether the result for the current yes has gone, because the card stays
 * Locating for a moment after a granted result while the view model finds the station. So the one
 * thing kept here is a saved flag, [sent], and the rule is [locateStep]:
 *
 * - While the card awaits a location and nothing is sent, ask for the permission, take one fix and
 *   send it. The flag is set in the same step as the send, so the effect being started again by a
 *   turn of the screen finds it set and does nothing, and a turn that comes before the fix has
 *   landed finds it clear and asks again, because nothing was sent.
 * - When the card stops awaiting, the flag clears, so the next yes sends its own result.
 * - The effect ends in a send whatever happens: a dismissed dialog and a failure are results, and
 *   a wait that goes past [LOCATE_BOUND_MS] is unavailable. A second tap on yes while the card
 *   awaits changes no state, so it starts nothing.
 *
 * [onResult] is the only place the fix goes. It is not held in this composable.
 */
@Composable
internal fun IntroLocationEffect(
    awaiting: Boolean,
    userLocationManager: UserLocationManager,
    onResult: (LocationResult, LatLng?) -> Unit,
) {
    var sent by rememberSaveable { mutableStateOf(false) }
    val latestOnResult by rememberUpdatedState(onResult)

    LaunchedEffect(awaiting, userLocationManager) {
        when (locateStep(awaiting, sent)) {
            LocateStep.Reset -> sent = false
            LocateStep.Wait -> Unit
            LocateStep.Locate -> userLocationManager.locateOnce { result, location ->
                sent = true
                latestOnResult(result, location)
            }
        }
    }
}
