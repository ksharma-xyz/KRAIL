package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition

/**
 * Three stations of the default catalogue with their real stop ids, car park ids and positions
 * (Tallawong has three car parks behind one stop, the others fewer), so the Intro is run on shapes
 * riders get and not on invented ones.
 */
internal val TALLAWONG_STATION = ParkRideStationPickerItem(
    stationId = "2155384",
    stationName = "Tallawong",
    stopName = "Tallawong Station",
    carParkNames = listOf("Tallawong P1", "Tallawong P2", "Tallawong P3"),
    mappings = listOf(
        ParkRideMapping(stopId = "2155384", facilityId = "26", facilityName = "Tallawong P1"),
        ParkRideMapping(stopId = "2155384", facilityId = "27", facilityName = "Tallawong P2"),
        ParkRideMapping(stopId = "2155384", facilityId = "28", facilityName = "Tallawong P3"),
    ),
    position = StationPosition(latitude = -33.69163, longitude = 150.906022),
)

internal val KELLYVILLE_STATION = ParkRideStationPickerItem(
    stationId = "2155382",
    stationName = "Kellyville",
    stopName = "Kellyville Station",
    carParkNames = listOf("Kellyville (north)", "Kellyville (south)"),
    mappings = listOf(
        ParkRideMapping(stopId = "2155382", facilityId = "29", facilityName = "Kellyville (north)"),
        ParkRideMapping(stopId = "2155382", facilityId = "30", facilityName = "Kellyville (south)"),
    ),
    position = StationPosition(latitude = -33.713514, longitude = 150.935304),
)

internal val BELLA_VISTA_STATION = ParkRideStationPickerItem(
    stationId = "2153478",
    stationName = "Bella Vista",
    stopName = "Bella Vista Station",
    carParkNames = listOf("Bella Vista"),
    mappings = listOf(
        ParkRideMapping(stopId = "2153478", facilityId = "31", facilityName = "Bella Vista"),
    ),
    position = StationPosition(latitude = -33.730592, longitude = 150.944024),
)

internal val INTRO_STATIONS = listOf(KELLYVILLE_STATION, TALLAWONG_STATION, BELLA_VISTA_STATION)

/** The names the stops table gives those stops. They are stop names, never built from a car park name. */
internal val INTRO_STOP_NAMES = mapOf(
    "2155384" to "Tallawong Station",
    "2155382" to "Kellyville Station",
    "2153478" to "Bella Vista Station",
)

/** Where the rider stands, none of them a station. A point is named by its distance from one. */
internal val AT_TALLAWONG = LatLng(latitude = -33.69163, longitude = 150.906022)
internal val AT_CENTRAL = LatLng(latitude = -33.883852, longitude = 151.206272)

/** Due south of Bella Vista, and the nearest station is Bella Vista in both cases. */
internal val FOUR_POINT_SIX_KM_FROM_BELLA_VISTA = LatLng(latitude = -33.771961, longitude = 150.944024)
internal val FIVE_POINT_FOUR_KM_FROM_BELLA_VISTA = LatLng(latitude = -33.779155, longitude = 150.944024)

private const val TOTAL_SPOTS = 200L

/** A reading as the availability loader stores it: [stopName] is the stop id until the station is saved. */
internal fun reading(
    facilityId: String,
    stopId: String,
    spots: Long = 120,
    percentFull: Long = 40,
    time: String = "8:05 AM",
    stopName: String = stopId,
) = NSWParkRideFacilityDetail(
    facilityId = facilityId,
    spotsAvailable = spots,
    totalSpots = TOTAL_SPOTS,
    facilityName = "facility $facilityId",
    percentageFull = percentFull,
    stopId = stopId,
    stopName = stopName,
    timeText = time,
    suburb = "",
    address = "",
    latitude = 0.0,
    longitude = 0.0,
    timestamp = 1_000,
)
