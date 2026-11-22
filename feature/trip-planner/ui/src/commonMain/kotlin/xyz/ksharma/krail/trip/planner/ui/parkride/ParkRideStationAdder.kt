package xyz.ksharma.krail.trip.planner.ui.parkride

import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideUserFacilityEvent
import xyz.ksharma.krail.core.appreview.AppReviewManager
import xyz.ksharma.krail.core.appreview.DelightMoment
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.db.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/**
 * The one way a rider adds a Park & Ride station to home. The Park & Ride picker and the one-time
 * suggestion card on the map home both go through it, so a station added from either is stored the
 * same way, is reported the same way and arms the same review moment, and the two can never drift.
 *
 * Adding stores every car park at the station as a `UserAdded` row, so the home card lists them
 * together. It never fetches availability: that stays owned by the home cards' own lifecycle (see
 * `docs/POLLING_LIFECYCLE.md`), which is why this costs a local write and no network call.
 */
internal class ParkRideStationAdder(
    private val parkRideSandook: NswParkRideSandook,
    private val analytics: Analytics,
    private val appReviewManager: AppReviewManager,
) {

    /**
     * Stores [station]'s car parks, arms the Park & Ride added review moment (it fires once the rider
     * is on home) and reports the add with [source], which says which surface it came from.
     */
    suspend fun add(station: ParkRideStationPickerItem, source: ParkRideUserFacilityEvent.Source) {
        parkRideSandook.insertOrReplaceSavedParkRides(
            parkRideInfoList = station.mappings.map { mapping ->
                SavedParkRide(
                    stopId = mapping.stopId,
                    facilityId = mapping.facilityId,
                    stopName = station.stopName.ifBlank { station.stationName },
                    facilityName = mapping.facilityName,
                    source = UserAdded.value,
                )
            }.toSet(),
            source = UserAdded,
        )
        appReviewManager.onDelightMoment(DelightMoment.PARK_RIDE_ADDED)
        analytics.track(
            ParkRideUserFacilityEvent(
                facilityId = station.mappings.joinToString { it.facilityId },
                stopId = station.stationId,
                action = ParkRideUserFacilityEvent.Action.ADD,
                source = source,
            ),
        )
    }
}
