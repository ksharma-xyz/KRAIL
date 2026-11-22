package xyz.ksharma.krail.trip.planner.ui.homemap

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.savedtrips.CarParkBlock
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Which pins the home map draws and what a tap on each opens, from real Sydney stops and the real
 * Tallawong and Penrith stations, with no map, layout or database.
 */
class HomeMapPinsTest {

    private val townHall = NearbyStopFeature(
        stopId = "200070",
        stopName = "Town Hall Station",
        position = LatLng(-33.8732, 151.2070),
        transportModes = persistentListOf(TransportMode.Train),
    )
    private val wynyard = townHall.copy(stopId = "200080", stopName = "Wynyard Station")
    private val tallawongStop = townHall.copy(stopId = "2155384", stopName = "Tallawong Station")

    private val coordinates = mapOf(
        "200070" to (-33.8732 to 151.2070),
        "200080" to (-33.8662 to 151.2060),
        "2155384" to (-33.7056 to 150.9082),
        "2750430" to (-33.7515 to 150.7203),
        "0" to (0.0 to 0.0),
        "off" to (95.0 to 200.0),
    )

    private val pins = homePinsFor(
        nearby = listOf(townHall, wynyard),
        parkRide = parkRidePinsFor(listOf(tile("2155384", "Tallawong", listOf("2155384"))), coordinates),
        labels = labelPinsFor(listOf(StopLabel("🏠", "Home", "200080", "Wynyard Station")), coordinates),
    )

    private fun tile(
        stationId: String,
        name: String,
        stopIds: List<String>,
        carParkAt: LatLng? = null,
    ) = ParkRideTile(
        stationId = stationId,
        stationName = name,
        stopIds = persistentListOf(*stopIds.toTypedArray()),
        totalFreeSpots = 10,
        timeText = "8:05 AM",
        carParks = persistentListOf(
            CarParkBlock(
                facilityId = "$stationId-1",
                name = "$name P1",
                freeSpots = 10,
                percentFull = 50,
                timeText = "8:05 AM",
                directionsTo = carParkAt,
            ),
        ),
    )

    // region label pins

    @Test
    fun `a label with a stop and a position is a pin with its own emoji`() {
        val pins = labelPinsFor(
            labels = listOf(StopLabel("🏠", "Home", "200070", "Town Hall Station")),
            coordinates = coordinates,
        )

        assertEquals(1, pins.size)
        assertEquals("Home", pins.single().labelKey)
        assertEquals("🏠", pins.single().emoji)
        assertEquals(LatLng(-33.8732, 151.2070), pins.single().position)
    }

    @Test
    fun `a label with no stop has no pin`() {
        assertTrue(labelPinsFor(StopLabel.defaults, coordinates).isEmpty())
    }

    @Test
    fun `a label whose stop has no position has no pin`() {
        val labels = listOf(
            StopLabel("🏠", "Home", "999999", "Nowhere"),
            StopLabel("💼", "Work", "0", "At the origin"),
            StopLabel("🎓", "Uni", "off", "Off the globe"),
        )

        assertTrue(labelPinsFor(labels, coordinates).isEmpty())
    }

    @Test
    fun `a stop that is two labels keeps the first label`() {
        val labels = listOf(
            StopLabel("🏠", "Home", "200070", "Town Hall Station"),
            StopLabel("💼", "Work", "200070", "Town Hall Station"),
        )

        assertEquals(listOf("Home"), labelPinsFor(labels, coordinates).map { it.labelKey })
    }

    @Test
    fun `label pins keep the order the labels have`() {
        val labels = listOf(
            StopLabel("💼", "Work", "200080", "Wynyard Station"),
            StopLabel("🏠", "Home", "200070", "Town Hall Station"),
        )

        assertEquals(listOf("Work", "Home"), labelPinsFor(labels, coordinates).map { it.labelKey })
    }

    // endregion

    // region park and ride pins

    @Test
    fun `a station is plotted at its own stop`() {
        val pins = parkRidePinsFor(listOf(tile("2155384", "Tallawong", listOf("2155384"))), coordinates)

        assertEquals(LatLng(-33.7056, 150.9082), pins.single().position)
        assertEquals("Tallawong", pins.single().stationName)
    }

    @Test
    fun `a station with no stop position falls back to a car park with its own`() {
        val carPark = LatLng(-33.75, 150.72)
        val pins = parkRidePinsFor(listOf(tile("1", "Somewhere", listOf("unknown"), carPark)), coordinates)

        assertEquals(carPark, pins.single().position)
    }

    @Test
    fun `a station with no position anywhere has no pin`() {
        assertTrue(parkRidePinsFor(listOf(tile("1", "Nowhere", listOf("unknown"))), coordinates).isEmpty())
    }

    @Test
    fun `a station behind two stops uses the first stop that has a position`() {
        val pins = parkRidePinsFor(listOf(tile("12", "Mona Vale", listOf("unknown", "2750430"))), coordinates)

        assertEquals(LatLng(-33.7515, 150.7203), pins.single().position)
    }

    @Test
    fun `a station is one pin however many times it is listed`() {
        val twice = listOf(
            tile("2155384", "Tallawong", listOf("2155384")),
            tile("2155384", "Tallawong", listOf("2155384")),
        )

        assertEquals(1, parkRidePinsFor(twice, coordinates).size)
    }

    // endregion

    // region one pin per place

    @Test
    fun `nearby stops pass through when nothing else sits on them`() {
        val pins = homePinsFor(listOf(townHall, wynyard), parkRide = emptyList(), labels = emptyList())

        assertEquals(listOf("200070", "200080"), pins.stops.map { it.stopId })
    }

    @Test
    fun `a stop that is a label pin is drawn once, as the label`() {
        val label = labelPinsFor(listOf(StopLabel("🏠", "Home", "200070", "Town Hall Station")), coordinates)
        val pins = homePinsFor(listOf(townHall, wynyard), parkRide = emptyList(), labels = label)

        assertEquals(listOf("200080"), pins.stops.map { it.stopId })
        assertEquals(listOf("Home"), pins.labels.map { it.labelKey })
    }

    @Test
    fun `a stop a station sits behind gives way to the P`() {
        val station = parkRidePinsFor(listOf(tile("2155384", "Tallawong", listOf("2155384"))), coordinates)
        val pins = homePinsFor(listOf(townHall, tallawongStop), parkRide = station, labels = emptyList())

        assertEquals(listOf("200070"), pins.stops.map { it.stopId })
        assertEquals(listOf("2155384"), pins.parkRide.map { it.stationId })
    }

    @Test
    fun `a station behind a labelled stop gives way to the label`() {
        val station = parkRidePinsFor(listOf(tile("2155384", "Tallawong", listOf("2155384"))), coordinates)
        val label = labelPinsFor(listOf(StopLabel("🏠", "Home", "2155384", "Tallawong Station")), coordinates)
        val pins = homePinsFor(listOf(tallawongStop), parkRide = station, labels = label)

        assertTrue(pins.stops.isEmpty())
        assertTrue(pins.parkRide.isEmpty())
        assertEquals(listOf("Home"), pins.labels.map { it.labelKey })
    }

    @Test
    fun `no sources at all is no pins`() {
        val pins = homePinsFor(emptyList(), emptyList(), emptyList())

        assertTrue(pins.stops.isEmpty() && pins.parkRide.isEmpty() && pins.labels.isEmpty())
    }

    @Test
    fun `the stop ids a lookup needs are each listed once`() {
        val labels = listOf(
            StopLabel("🏠", "Home", "200070", "Town Hall Station"),
            StopLabel("💼", "Work", null, null),
        )
        val tiles = listOf(
            tile("2155384", "Tallawong", listOf("2155384", "200070")),
            tile("2750430", "Penrith", listOf("2750430")),
        )

        assertEquals(listOf("200070", "2155384", "2750430"), pinStopIdsFor(labels, tiles))
    }

    // endregion

    // region what a tap opens

    @Test
    fun `a stop pin opens the stop card for that stop`() {
        val card = homeCardFor(stopCardKey("200070"), pins)

        assertIs<HomeCard.Stop>(card)
        assertEquals("Town Hall Station", card.stop.stopName)
    }

    @Test
    fun `a P pin opens the Park and Ride card for that station`() {
        val card = homeCardFor(parkRideCardKey("2155384"), pins)

        assertIs<HomeCard.ParkRide>(card)
        assertEquals("Tallawong", card.pin.stationName)
    }

    @Test
    fun `a label pin opens the stop card of its stop with no modes held`() {
        val card = homeCardFor(stopCardKey("200080"), pins)

        assertIs<HomeCard.Stop>(card)
        assertEquals("Wynyard Station", card.stop.stopName)
        assertTrue(card.stop.transportModes.isEmpty())
    }

    @Test
    fun `no key leaves the card closed`() {
        assertNull(homeCardFor(null, pins))
    }

    @Test
    fun `a pin that left the map leaves the card closed`() {
        assertNull(homeCardFor(stopCardKey("gone"), pins))
        assertNull(homeCardFor(parkRideCardKey("gone"), pins))
    }

    @Test
    fun `a key of no known kind leaves the card closed`() {
        assertNull(homeCardFor("something:200070", pins))
    }

    @Test
    fun `a stop key never opens a station and a station key never opens a stop`() {
        assertNull(homeCardFor(stopCardKey("2155384"), pins))
        assertNull(homeCardFor(parkRideCardKey("200070"), pins))
    }

    // endregion
}
