package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The rules behind the one-time Park & Ride suggestion: whether it can be offered, where the nearest
 * station is measured from, which station it is, what the card says and what a screen reader hears.
 * Each is driven on its own at its boundary.
 */
class ParkRideSuggestionRulesTest {

    // region whether it can be offered

    private class GateCase(
        val name: String,
        val mapHomeOn: Boolean,
        val alreadyAsked: Boolean,
        val hasSavedStation: Boolean,
        val expected: Boolean,
    )

    @Test
    fun `the suggestion is offered only on the map home, once, to a rider with no saved station`() {
        listOf(
            GateCase("everything allows it", true, false, false, true),
            GateCase("the maps flag is off, so today's home is unchanged", false, false, false, false),
            GateCase("the Intro or an earlier card already asked", true, true, false, false),
            GateCase("a station is already saved", true, false, true, false),
            GateCase("asked and a station saved", true, true, true, false),
            GateCase("flag off and asked", false, true, false, false),
            GateCase("flag off and a station saved", false, false, true, false),
            GateCase("nothing allows it", false, true, true, false),
        ).forEach { case ->
            assertEquals(
                case.expected,
                canSuggest(case.mapHomeOn, case.alreadyAsked, case.hasSavedStation),
                case.name,
            )
        }
    }

    // endregion

    // region where it is measured from

    @Test
    fun `Home is the anchor when it has a position, whatever the current fix`() {
        assertEquals(HOME, suggestionAnchor(home = HOME, currentFix = HERE))
        assertEquals(HOME, suggestionAnchor(home = HOME, currentFix = null))
    }

    @Test
    fun `the current fix is the anchor only before Home is set`() {
        assertEquals(HERE, suggestionAnchor(home = null, currentFix = HERE))
    }

    @Test
    fun `with no Home and no fix there is no anchor`() {
        assertNull(suggestionAnchor(home = null, currentFix = null))
    }

    // endregion

    // region which station

    private class StationCase(
        val name: String,
        val anchor: LatLng?,
        val stations: List<ParkRideStationPickerItem>,
        val radiusKm: Double,
        val expected: String?,
    )

    @Test
    fun `the nearest station inside the radius is the one suggested`() {
        listOf(
            StationCase("the nearer of two", AT_NEAR, listOf(FAR_STATION, NEAR_STATION), 5.0, "Near"),
            StationCase("the nearer of two, list reversed", AT_NEAR, listOf(NEAR_STATION, FAR_STATION), 5.0, "Near"),
            StationCase("only the far one is in range", AT_FAR, listOf(NEAR_STATION, FAR_STATION), 1.0, "Far"),
            StationCase("none in range", AT_NOWHERE, listOf(NEAR_STATION, FAR_STATION), 5.0, null),
            StationCase("no anchor", null, listOf(NEAR_STATION, FAR_STATION), 5.0, null),
            StationCase("no stations", AT_NEAR, emptyList(), 5.0, null),
            StationCase(
                "a station with no coordinates is skipped",
                AT_NEAR,
                listOf(NO_COORDINATES, FAR_STATION),
                50.0,
                "Far",
            ),
            StationCase("a station with no coordinates alone gives none", AT_NEAR, listOf(NO_COORDINATES), 50.0, null),
        ).forEach { case ->
            assertEquals(
                case.expected,
                stationToSuggest(case.anchor, case.stations, case.radiusKm)?.stationName,
                case.name,
            )
        }
    }

    @Test
    fun `a wider radius reaches a station a narrower one does not`() {
        // Far is about 11 km from the near anchor.
        assertNull(stationToSuggest(AT_NEAR, listOf(FAR_STATION), radiusKm = 5.0))
        assertEquals("Far", stationToSuggest(AT_NEAR, listOf(FAR_STATION), radiusKm = 12.0)?.stationName)
    }

    @Test
    fun `two stations at one distance give the one first by name, whatever the list order`() {
        val first = station("1", "Alpha", position = StationPosition(-33.80, 151.00))
        val second = station("2", "Bravo", position = StationPosition(-33.80, 151.00))
        val anchor = LatLng(-33.81, 151.00)

        assertEquals("Alpha", stationToSuggest(anchor, listOf(second, first), 5.0)?.stationName)
        assertEquals("Alpha", stationToSuggest(anchor, listOf(first, second), 5.0)?.stationName)
    }

    @Test
    fun `a tie on the name falls to the smaller station id and capitals do not decide it`() {
        val lower = station("1", "alpha", position = StationPosition(-33.80, 151.00))
        val upper = station("2", "Alpha", position = StationPosition(-33.80, 151.00))
        val anchor = LatLng(-33.81, 151.00)

        assertEquals("1", stationToSuggest(anchor, listOf(upper, lower), 5.0)?.stationId)
    }

    // endregion

    // region Home's position

    private class HomeCase(
        val name: String,
        val labels: List<StopLabel>,
        val coordinates: Map<String, Pair<Double, Double>>,
        val expected: LatLng?,
    )

    @Test
    fun `Home has a position when its stop is in the table`() {
        val set = StopLabel(emoji = "H", label = "Home", stopId = "S1", stopName = "Central")
        val work = StopLabel(emoji = "W", label = "Work", stopId = "S2", stopName = "Town Hall")
        listOf(
            HomeCase("home with a stop", listOf(set), mapOf("S1" to (-33.88 to 151.21)), LatLng(-33.88, 151.21)),
            HomeCase(
                "the label is found whatever its case",
                listOf(set.copy(label = "home")),
                mapOf("S1" to (-33.88 to 151.21)),
                LatLng(-33.88, 151.21),
            ),
            HomeCase(
                "home is not the first label",
                listOf(work, set),
                mapOf("S1" to (-33.88 to 151.21), "S2" to (-33.87 to 151.20)),
                LatLng(-33.88, 151.21),
            ),
            HomeCase("home with no stop", listOf(StopLabel("H", "Home")), mapOf("S1" to (-33.88 to 151.21)), null),
            HomeCase(
                "home with a stop id and no name",
                listOf(set.copy(stopName = null)),
                mapOf("S1" to (-33.88 to 151.21)),
                null,
            ),
            HomeCase("only work is set", listOf(work), mapOf("S2" to (-33.87 to 151.20)), null),
            HomeCase("no labels yet", emptyList(), emptyMap(), null),
            HomeCase("the stop is not in the table", listOf(set), emptyMap(), null),
            HomeCase("the table holds 0.0 and 0.0 for none", listOf(set), mapOf("S1" to (0.0 to 0.0)), null),
            HomeCase("a position off the globe", listOf(set), mapOf("S1" to (95.0 to 151.21)), null),
        ).forEach { case ->
            assertEquals(case.expected, homePosition(case.labels, case.coordinates), case.name)
        }
    }

    // endregion

    // region what the card says

    @Test
    fun `with nothing stored the card is the station's name alone`() {
        val tile = suggestionTile(TALLAWONG, cachedReadings = emptyList())

        assertEquals("Tallawong Station", tile.stationName)
        assertNull(tile.totalFreeSpots)
        assertNull(tile.timeText)
        assertEquals(listOf("26", "27", "28"), tile.carParks.map { it.facilityId })
        assertEquals(listOf("Tallawong P1", "Tallawong P2", "Tallawong P3"), tile.carParks.map { it.name })
    }

    @Test
    fun `stored readings give the total across the car parks and the oldest time`() {
        val tile = suggestionTile(
            TALLAWONG,
            cachedReadings = listOf(
                reading("26", free = 50, time = "8:05 AM", at = 300L),
                reading("27", free = 7, time = "7:50 AM", at = 100L),
                reading("28", free = 98, time = "8:00 AM", at = 200L),
            ),
        )

        assertEquals(155, tile.totalFreeSpots)
        assertEquals("7:50 AM", tile.timeText)
    }

    @Test
    fun `a car park with no stored reading is left out of the total and never read as full`() {
        val tile = suggestionTile(
            TALLAWONG,
            cachedReadings = listOf(reading("26", free = 50, time = "8:05 AM", at = 300L)),
        )

        assertEquals(50, tile.totalFreeSpots)
        assertEquals(3, tile.carParks.size)
        assertNull(tile.carParks.first { it.facilityId == "27" }.freeSpots)
    }

    @Test
    fun `a reading of another car park is ignored`() {
        val tile = suggestionTile(
            TALLAWONG,
            cachedReadings = listOf(reading("99", free = 10, time = "8:00 AM", at = 1L)),
        )

        assertNull(tile.totalFreeSpots)
    }

    @Test
    fun `a car park behind two stops is listed once`() {
        val mona = station(
            id = "210318",
            name = "Mona Vale",
            stopName = "",
            mappings = listOf(
                ParkRideMapping(stopId = "210318", facilityId = "12", facilityName = "Mona Vale"),
                ParkRideMapping(stopId = "2103108", facilityId = "12", facilityName = "Mona Vale"),
            ),
        )

        val tile = suggestionTile(mona, cachedReadings = emptyList())

        assertEquals(1, tile.carParks.size)
        assertEquals("Mona Vale", tile.stationName)
    }

    @Test
    fun `the station's name stands in when the stops table has none`() {
        assertEquals("Mona Vale", suggestionTile(station("1", "Mona Vale", stopName = ""), emptyList()).stationName)
    }

    @Test
    fun `a stale reading is still shown with its time`() {
        val tile = suggestionTile(TALLAWONG, listOf(reading("26", free = 12, time = "6:00 AM", at = 1L)))

        assertEquals(12, tile.totalFreeSpots)
        assertEquals("6:00 AM", tile.timeText)
    }

    // endregion

    // region what a screen reader hears

    @Test
    fun `a screen reader hears the station, the spots and the time`() {
        val tile = suggestionTile(
            TALLAWONG,
            listOf(
                reading("26", free = 50, time = "8:05 AM", at = 300L),
                reading("27", free = 7, time = "7:50 AM", at = 100L),
            ),
        )

        assertEquals(
            "Park & Ride suggestion, Tallawong Station, 57 spots, last updated at 7:50 AM",
            suggestionSpokenLabel(tile),
        )
    }

    @Test
    fun `with no number a screen reader hears the station alone`() {
        assertEquals(
            "Park & Ride suggestion, Tallawong Station",
            suggestionSpokenLabel(suggestionTile(TALLAWONG, emptyList())),
        )
    }

    @Test
    fun `one spot is not spoken as spots`() {
        val tile = suggestionTile(TALLAWONG, listOf(reading("26", free = 1, time = "8:05 AM", at = 300L)))

        assertEquals(
            "Park & Ride suggestion, Tallawong Station, 1 spot, last updated at 8:05 AM",
            suggestionSpokenLabel(tile),
        )
    }

    @Test
    fun `the spoken label has no colon, no dash and no arrow`() {
        val tile: ParkRideTile = suggestionTile(TALLAWONG, listOf(reading("26", free = 1, time = "8:05 AM", at = 1L)))
        // A time of day keeps the colon that writes it, and nothing else may have one.
        val spoken = (suggestionSpokenLabel(tile) + addToHomeSpokenLabel(tile) + ADD_TO_HOME_TEXT + NOT_NOW_TEXT)
            .replace(Regex("\\d:\\d\\d"), "")

        listOf(":", "\u2014", "\u2013", "\u2192", "\u00B7").forEach { banned ->
            assertEquals(false, spoken.contains(banned), "found $banned in $spoken")
        }
    }

    @Test
    fun `the primary button names the station it adds`() {
        assertEquals("Add Tallawong Station to home", addToHomeSpokenLabel(suggestionTile(TALLAWONG, emptyList())))
    }

    @Test
    fun `the two buttons read Add to home and Not now`() {
        assertEquals("Add to home", ADD_TO_HOME_TEXT)
        assertEquals("Not now", NOT_NOW_TEXT)
    }

    // endregion

    private companion object {
        val HOME = LatLng(-33.70, 150.90)
        val HERE = LatLng(-33.80, 151.00)

        // About 1 km from AT_NEAR, and about 11 km from it for AT_FAR's station.
        val AT_NEAR = LatLng(-33.800, 151.000)
        val AT_FAR = LatLng(-33.700, 151.000)
        val AT_NOWHERE = LatLng(-34.500, 150.000)

        val NEAR_STATION = station("1", "Near", position = StationPosition(-33.808, 151.000))
        val FAR_STATION = station("2", "Far", position = StationPosition(-33.700, 151.000))
        val NO_COORDINATES = station("3", "Nowhere", position = null)

        val TALLAWONG = station(
            id = "2155384",
            name = "Tallawong",
            stopName = "Tallawong Station",
            mappings = listOf(
                ParkRideMapping("2155384", "26", "Tallawong P1"),
                ParkRideMapping("2155384", "27", "Tallawong P2"),
                ParkRideMapping("2155384", "28", "Tallawong P3"),
            ),
        )

        fun station(
            id: String,
            name: String,
            stopName: String = name,
            position: StationPosition? = StationPosition(-33.0, 151.0),
            mappings: List<ParkRideMapping> = listOf(ParkRideMapping(id, "F$id", name)),
        ) = ParkRideStationPickerItem(
            stationId = id,
            stationName = name,
            stopName = stopName,
            carParkNames = mappings.map { it.facilityName }.distinct(),
            mappings = mappings,
            position = position,
        )

        fun reading(id: String, free: Int, time: String, at: Long) = NSWParkRideFacilityDetail(
            facilityId = id,
            spotsAvailable = free.toLong(),
            totalSpots = 200L,
            facilityName = "Car park $id",
            percentageFull = 80L,
            stopId = "2155384",
            timeText = time,
            suburb = "",
            address = "",
            latitude = 0.0,
            longitude = 0.0,
            stopName = "Tallawong Station",
            timestamp = at,
        )
    }
}
