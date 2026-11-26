package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelChipTap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelTapOutcome
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent

/**
 * Plays out a Home or Work chip tap from the ViewModel's [tap], one step each time it changes.
 *
 * Locating takes the one location fix, here because the permission prompt needs the screen, and
 * reports it. Decided acts on the outcome once and reports it handled, which steps the state back
 * to idle so a re-launched effect finds nothing left to do (the second `LaunchedEffect` rule in
 * `docs/FEATURE_QUALITY_CHECKLIST.md`).
 *
 * The state is the ViewModel's, so a rotation during the fix cancels this effect and the new
 * composition starts it again from the same state. A fix that was cancelled reports nothing: it is
 * never reported as "no fix", which would send the rider to the search instead of their trip.
 */
@Composable
internal fun LabelChipTapEffect(
    tap: LabelChipTap,
    locate: suspend () -> LatLng?,
    onEvent: (SavedTripUiEvent) -> Unit,
    tripPlannerNavigator: TripPlannerNavigator,
) {
    LaunchedEffect(tap) {
        when (tap) {
            LabelChipTap.Idle -> Unit
            is LabelChipTap.Locating -> {
                val fix = locate()
                onEvent(SavedTripUiEvent.LabelChipLocated(tap.labelKey, fix?.latitude, fix?.longitude))
            }
            is LabelChipTap.Decided -> {
                act(tap.outcome, tripPlannerNavigator)
                onEvent(SavedTripUiEvent.LabelChipTapHandled)
            }
        }
    }
}

/**
 * Opens what [outcome] says: the trip results, the To and From search holding the labelled stop as the
 * destination, or the stop search to set the label. Planning opens the results the way a saved trip card
 * does. The home holds no stop of its own, so with no fix to plan from the labelled stop goes to Search
 * Stop, where the rider chooses the start and taps go.
 */
private fun act(
    outcome: LabelTapOutcome,
    tripPlannerNavigator: TripPlannerNavigator,
) {
    when (outcome) {
        is LabelTapOutcome.Plan -> tripPlannerNavigator.navigateToTimeTable(
            fromStopId = outcome.from.stopId,
            fromStopName = outcome.from.stopName,
            toStopId = outcome.to.stopId,
            toStopName = outcome.to.stopName,
        )
        is LabelTapOutcome.Search -> tripPlannerNavigator.navigateToSearchStop(
            fieldType = SearchStopFieldType.FROM,
            presetTo = outcome.to,
        )
        is LabelTapOutcome.Assign -> tripPlannerNavigator.navigateToSearchStop(
            fieldType = SearchStopFieldType.LABEL,
            labelKey = outcome.labelKey,
        )
    }
}

/** The rider's position from one fix, or null when there is none: no permission, no signal, timed out. */
internal suspend fun UserLocationManager.currentLatLng(): LatLng? =
    getCurrentLocation(PermissionResultEvent.Source.HOME_LABEL_CHIP)
        .onFailure { log("[HOME_CHIP] no location fix: $it") }
        .getOrNull()
        ?.let { LatLng(it.latitude, it.longitude) }

/** Stop search answered for a label set from the home chips. A result for any other field is not ours. */
internal fun onLabelStopSelected(result: StopSelectedResult, onEvent: (SavedTripUiEvent) -> Unit) {
    val labelKey = result.labelKey ?: return
    onEvent(SavedTripUiEvent.LabelStopAssigned(labelKey, result.stopId, result.stopName, result.locationKind))
}
