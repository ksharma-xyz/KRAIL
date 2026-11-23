package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * What the suggestion reads, every bit of it from this phone: whether a station is already saved, where
 * Home is, the stations of the catalogue with their positions, and the readings already stored. Nothing
 * here is a network call, so the suggestion costs no request however often it is worked out.
 */
internal class ParkRideSuggestionSources(
    private val catalogue: ParkRideCatalogue,
    private val sandook: Sandook,
    private val parkRideSandook: NswParkRideSandook,
) {

    /** Whether any Park & Ride row is saved, from the picker, a saved trip or the card itself. It follows the table. */
    fun hasSavedStation(): Flow<Boolean> = parkRideSandook.observeSavedParkRides()
        .map { rows -> rows.isNotEmpty() }
        .distinctUntilChanged()

    /**
     * Where Home is, following the labels. It looks the stop up only while Home has one, so a rider with
     * no Home costs no query.
     */
    fun homePosition(): Flow<LatLng?> = sandook.observeStopLabels()
        .map { rows ->
            val labels = rows.map { row ->
                StopLabel(emoji = row.emoji, label = row.label, stopId = row.stop_id, stopName = row.stop_name)
            }
            homeStopId(labels)?.let { stopId ->
                homePosition(labels, sandook.selectStopCoordinatesBatch(listOf(stopId)))
            }
        }
        .distinctUntilChanged()

    /** One entry per station, with the position the local stops table holds for it. */
    fun stations(): List<ParkRideStationPickerItem> = catalogue.stations()

    /** The readings stored on the phone, once. A station never fetched has none and the card says so by showing no number. */
    suspend fun cachedReadings(): List<NSWParkRideFacilityDetail> =
        parkRideSandook.getAllParkRideFacilityDetail().first()
}
