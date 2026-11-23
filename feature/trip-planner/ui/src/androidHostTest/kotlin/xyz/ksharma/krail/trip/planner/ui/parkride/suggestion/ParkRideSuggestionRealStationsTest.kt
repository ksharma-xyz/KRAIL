package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.intro.BundledStops
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The suggestion on the real default catalogue and the real stop coordinates, with the places of named
 * stops, so a change to the catalogue or to a data refresh moves these results. The 5 km radius is the
 * default of `park_ride_suggestion_radius_km`.
 */
class ParkRideSuggestionRealStationsTest {

    private val stations = BundledStops.defaultStations()

    private fun placeOf(stopId: String): LatLng {
        val stop = BundledStops.byId.getValue(stopId)
        return LatLng(stop.latitude, stop.longitude)
    }

    private fun suggested(anchor: LatLng?, radiusKm: Double = RADIUS_KM) =
        stationToSuggest(anchor, stations, radiusKm)?.stationName

    @Test
    fun `a rider at an outer Metro station is offered that station`() {
        assertEquals("Tallawong", suggested(placeOf(TALLAWONG)))
        assertEquals("Kellyville", suggested(placeOf(KELLYVILLE)))
    }

    @Test
    fun `a rider in the city has no station inside five kilometres`() {
        assertNull(suggested(placeOf(TOWN_HALL)))
        assertNull(suggested(placeOf(CENTRAL)))
        assertNull(suggested(placeOf(PARRAMATTA)))
    }

    @Test
    fun `the same city rider is offered one a few kilometres further out with a wider radius`() {
        assertEquals("Ashfield", suggested(placeOf(CENTRAL), radiusKm = 8.0))
    }

    @Test
    fun `a rider with Home in the city is not offered the station they are standing at`() {
        // Home is the anchor whenever it has a position, so being at Tallawong today does not
        // suggest a station for a home that has none near it.
        val anchor = suggestionAnchor(home = placeOf(TOWN_HALL), currentFix = placeOf(TALLAWONG))

        assertNull(suggested(anchor))
    }

    @Test
    fun `a rider with no Home is offered the station at their current fix`() {
        val anchor = suggestionAnchor(home = null, currentFix = placeOf(TALLAWONG))

        assertEquals("Tallawong", suggested(anchor))
    }

    @Test
    fun `the card for Tallawong lists its three car parks and shows no number from an empty store`() {
        val tallawong = stations.first { it.stationName == "Tallawong" }

        val tile = suggestionTile(tallawong, cachedReadings = emptyList())

        assertEquals(3, tile.carParks.size)
        assertNull(tile.totalFreeSpots)
        assertEquals(BundledStops.name(TALLAWONG), tile.stationName)
    }

    @Test
    fun `a station with no coordinates is never the suggestion`() {
        val unresolved = stations.filter { it.position == null }.map { it.stationName }
        val atWestRyde = LatLng(-33.8064, 151.0906)

        // West Ryde has no coordinates in the table today, so standing on it still offers the next nearest.
        assertEquals(listOf("West Ryde"), unresolved)
        assertEquals("Lindfield Village Green", suggested(atWestRyde, radiusKm = 20.0))
    }

    private companion object {
        const val RADIUS_KM = 5.0
        const val TOWN_HALL = "200070"
        const val CENTRAL = "200060"
        const val PARRAMATTA = "215020"
        const val TALLAWONG = "2155384"
        const val KELLYVILLE = "2155382"
    }
}
