package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Run on the real default catalogue and the real stop coordinates, with the locations of named
 * stations, so a change to the catalogue or to the data refresh moves these results.
 */
class NearestParkRideStationTest {

    private val stations = BundledStops.defaultStations()

    private fun locationOf(stopId: String): LatLng {
        val stop = BundledStops.byId.getValue(stopId)
        return LatLng(stop.latitude, stop.longitude)
    }

    private fun nearest(
        location: LatLng,
        radiusKm: Double = INTRO_PARK_RIDE_RADIUS_KM,
        among: List<ParkRideStationPickerItem> = stations,
    ) = nearestParkRideStation(location, among, radiusKm)?.stationName

    @Test
    fun `the intro looks five kilometres around the rider`() {
        assertEquals(5.0, INTRO_PARK_RIDE_RADIUS_KM)
    }

    @Test
    fun `a rider at Town Hall, Central or Parramatta has no car park within five kilometres`() {
        assertNull(nearest(locationOf(TOWN_HALL)))
        assertNull(nearest(locationOf(CENTRAL)))
        assertNull(nearest(locationOf(PARRAMATTA)))
    }

    @Test
    fun `those same riders do have one a few kilometres further out`() {
        // Ashfield is about 7.7 km from Town Hall and 7.5 km from Central, Seven Hills about 8 km
        // from Parramatta. A wider radius is the only thing that changes the answer.
        assertNull(nearest(locationOf(TOWN_HALL), radiusKm = 7.0))
        assertEquals("Ashfield", nearest(locationOf(TOWN_HALL), radiusKm = 8.0))
        assertEquals("Ashfield", nearest(locationOf(CENTRAL), radiusKm = 8.0))
        assertEquals("Seven Hills", nearest(locationOf(PARRAMATTA), radiusKm = 8.5))
    }

    @Test
    fun `a rider at an outer Metro station gets that station`() {
        assertEquals("Tallawong", nearest(locationOf(TALLAWONG)))
    }

    @Test
    fun `a rider in a regional centre gets nothing`() {
        val wollongong = LatLng(-34.4278, 150.8931)

        assertNull(nearest(wollongong))
        assertNull(nearest(wollongong, radiusKm = 20.0))
    }

    @Test
    fun `the nearer of two stations in range wins, whatever the list order`() {
        // Schofields and Kellyville are both inside five kilometres of Tallawong, and both sort
        // before it by name, so a first-in-range rule would return the wrong one.
        val atTallawong = locationOf(TALLAWONG)
        val inRange = stations.filter { nearest(atTallawong, among = listOf(it)) != null }
        assertEquals(listOf("Kellyville", "Schofields", "Tallawong"), inRange.map { it.stationName })

        assertEquals("Tallawong", nearest(atTallawong))
        assertEquals("Tallawong", nearest(atTallawong, among = inRange.reversed()))
        assertEquals("Kellyville", nearest(locationOf(KELLYVILLE)))
    }

    @Test
    fun `a station with no coordinates is skipped and the next nearest is offered`() {
        val withoutTallawong = stations.map {
            if (it.stationName == "Tallawong") it.copy(position = null) else it
        }

        assertEquals("Schofields", nearest(locationOf(TALLAWONG), among = withoutTallawong))
    }

    @Test
    fun `the real catalogue has a station with no coordinates and it is never offered`() {
        val unresolved = stations.filter { it.position == null }
        val atWestRyde = LatLng(-33.8064, 151.0906)

        assertEquals(listOf("West Ryde"), unresolved.map { it.stationName })
        // Nothing is nearer than Lindfield Village Green, so only the missing coordinate keeps West Ryde out.
        assertEquals("Lindfield Village Green", nearest(atWestRyde, radiusKm = 20.0))
    }

    @Test
    fun `the radius edge sits at the radius`() {
        val tallawong = locationOf(TALLAWONG)

        // One degree of latitude is about 111.19 km, so moving due north is an exact distance.
        val justInside = tallawong.copy(latitude = tallawong.latitude + kmToDegrees(4.99))
        val justOutside = tallawong.copy(latitude = tallawong.latitude + kmToDegrees(5.01))

        assertEquals("Tallawong", nearest(justInside, radiusKm = 5.0))
        assertNull(nearest(justOutside, radiusKm = 5.0))
    }

    @Test
    fun `an impossible location returns nothing instead of failing`() {
        assertNull(nearest(LatLng(latitude = 200.0, longitude = 151.0), radiusKm = 5000.0))
        assertNull(nearest(LatLng(latitude = Double.NaN, longitude = 151.0), radiusKm = 5000.0))
    }

    private fun kmToDegrees(km: Double): Double = km / (EARTH_RADIUS_KM * PI / 180.0)

    private companion object {
        const val EARTH_RADIUS_KM = 6371.0
        const val TOWN_HALL = "200070"
        const val CENTRAL = "200060"
        const val PARRAMATTA = "215020"
        const val TALLAWONG = "2155384"
        const val KELLYVILLE = "2155382"
    }
}
