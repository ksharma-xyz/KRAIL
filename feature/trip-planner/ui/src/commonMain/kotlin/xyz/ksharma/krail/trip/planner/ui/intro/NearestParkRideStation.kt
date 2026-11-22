package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.PARK_RIDE_SUGGESTION_DEFAULT_RADIUS_KM
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/**
 * How far from the rider the Intro looks for a Park & Ride station. It is the radius the home suggestion
 * falls back on, so the two ask within the same distance by construction and not by two copies of 5.0.
 */
internal const val INTRO_PARK_RIDE_RADIUS_KM = PARK_RIDE_SUGGESTION_DEFAULT_RADIUS_KM

/**
 * The single nearest station within [radiusKm] of [location], or null. A station with no coordinates
 * is skipped, so it is never offered. The edge is inclusive. Shared with the map-first home stack.
 */
internal fun nearestParkRideStation(
    location: LatLng,
    stations: List<ParkRideStationPickerItem>,
    radiusKm: Double,
): ParkRideStationPickerItem? = stations
    .mapNotNull { station ->
        val position = station.position ?: return@mapNotNull null
        val distanceKm = GeoUtils.haversineDistance(
            lat1 = location.latitude,
            lon1 = location.longitude,
            lat2 = position.latitude,
            lon2 = position.longitude,
        ) ?: return@mapNotNull null
        station to distanceKm
    }
    .filter { (_, distanceKm) -> distanceKm <= radiusKm }
    .minByOrNull { (_, distanceKm) -> distanceKm }
    ?.first

/**
 * The stop to pre-fill as the first trip's start once the rider has added [station], or null when
 * none of the station's stops is in the stops table.
 *
 * The label is the stop's own name from [stopNameLookup], never "<name> Station", because some
 * catalogue rows are bus stops. A station can span several stops and the smallest id may be one the
 * table does not have, so the smallest id that does resolve is used.
 */
internal fun parkRideStartPrefill(
    station: ParkRideStationPickerItem,
    stopNameLookup: (stopId: String) -> String?,
): SuggestedStop? = station.mappings
    .map { it.stopId }
    .distinct()
    .sorted()
    .firstNotNullOfOrNull { stopId ->
        stopNameLookup(stopId)
            ?.takeIf { it.isNotBlank() }
            ?.let { name -> SuggestedStop(stopId = stopId, name = name) }
    }
