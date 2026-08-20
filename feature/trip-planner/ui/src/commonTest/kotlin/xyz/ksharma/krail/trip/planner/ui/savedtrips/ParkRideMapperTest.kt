package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.Location
import xyz.ksharma.krail.park.ride.network.model.Occupancy
import xyz.ksharma.krail.park.ride.network.model.Zone
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the home Park & Ride card's display behaviour, which must not change.
 *
 * Keying `SavedParkRide` by source lets one station be held by both a saved trip and an
 * explicit add, so the merge feeding this mapper can now contain the same facility twice.
 * These cases prove that extra row stays invisible: the card list is still one entry per
 * stop, with each facility appearing once.
 */
class ParkRideMapperTest {

    @Test
    fun `a facility held by two sources still produces a single card`() {
        // The same facility arrives twice, as it does when a station is both saved-trip
        // derived and user added.
        val details = listOf(
            facilityDetail(facilityId = "26", stopId = "2155384"),
            facilityDetail(facilityId = "26", stopId = "2155384"),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(1, uiState.size)
        assertEquals(1, uiState.first().facilities.size)
        assertEquals("26", uiState.first().facilities.first().facilityId)
    }

    @Test
    fun `several car parks at one stop stay on one card`() {
        // Tallawong: three car parks behind a single stop.
        val details = listOf(
            facilityDetail(facilityId = "26", stopId = "2155384", facilityName = "Tallawong P1"),
            facilityDetail(facilityId = "27", stopId = "2155384", facilityName = "Tallawong P2"),
            facilityDetail(facilityId = "28", stopId = "2155384", facilityName = "Tallawong P3"),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(1, uiState.size)
        assertEquals(3, uiState.first().facilities.size)
    }

    @Test
    fun `one facility reached from two stops is not shown twice`() {
        // Mona Vale: one car park, two stop IDs. The second stop has nothing left to show
        // once the facility is claimed, so it must not produce an empty card.
        val details = listOf(
            facilityDetail(facilityId = "12", stopId = "210318"),
            facilityDetail(facilityId = "12", stopId = "2103108"),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(1, uiState.size)
        assertEquals(1, uiState.first().facilities.size)
    }

    // region occupancy above capacity

    @Test
    fun `a car park reported beyond capacity is shown as full, not over full`() {
        // Gosford, seen in production: the feed reported occupancy above the recorded
        // capacity and the card read "0 spots available, 135% full".
        val details = listOf(
            facilityDetail(facilityId = "26", stopId = "2155384", percentageFull = 135),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(100, uiState.first().facilities.first().percentageFull)
    }

    @Test
    fun `a reading just over capacity is capped too`() {
        // Ashfield, same day: 101%.
        val details = listOf(
            facilityDetail(facilityId = "5", stopId = "205710", percentageFull = 101),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(100, uiState.first().facilities.first().percentageFull)
    }

    @Test
    fun `an ordinary reading passes through untouched`() {
        val details = listOf(
            facilityDetail(facilityId = "26", stopId = "2155384", percentageFull = 60),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(60, uiState.first().facilities.first().percentageFull)
    }

    @Test
    fun `a genuinely full car park still reads 100`() {
        val details = listOf(
            facilityDetail(facilityId = "26", stopId = "2155384", percentageFull = 100),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(100, uiState.first().facilities.first().percentageFull)
    }

    @Test
    fun `the loading sentinel survives the cap`() {
        // -1 across every number means "no reading yet". Clamping it into 0..100 would
        // render an empty car park where there is simply no data.
        val details = listOf(
            facilityDetail(facilityId = "26", stopId = "2155384", percentageFull = -1),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(-1, uiState.first().facilities.first().percentageFull)
    }

    // endregion

    // region occupancy above capacity, at the point it is calculated

    @Test
    fun `occupancy above capacity is stored as 100, never more`() {
        // 135 cars recorded against 100 spots, which is what produced the 135% card.
        val response = facilityResponse(totalSpots = "100", occupied = "135")

        val stored = response.toNSWParkRideFacilityDetail(stopName = "Gosford", stopId = "2250")

        assertEquals(100L, stored.percentageFull)
    }

    @Test
    fun `spots available and percentage full agree once capacity is exceeded`() {
        // The pair has to be consistent: clamping one and not the other is what put
        // "0 spots available" next to "135% full" on the same card.
        val response = facilityResponse(totalSpots = "100", occupied = "135")

        val stored = response.toNSWParkRideFacilityDetail(stopName = "Gosford", stopId = "2250")

        assertEquals(0L, stored.spotsAvailable)
        assertEquals(100L, stored.percentageFull)
    }

    @Test
    fun `a normal car park is unaffected`() {
        val response = facilityResponse(totalSpots = "100", occupied = "60")

        val stored = response.toNSWParkRideFacilityDetail(stopName = "Gosford", stopId = "2250")

        assertEquals(60L, stored.percentageFull)
        assertEquals(40L, stored.spotsAvailable)
    }

    @Test
    fun `an empty car park reads zero, not clamped upwards`() {
        val response = facilityResponse(totalSpots = "100", occupied = "0")

        val stored = response.toNSWParkRideFacilityDetail(stopName = "Gosford", stopId = "2250")

        assertEquals(0L, stored.percentageFull)
        assertEquals(100L, stored.spotsAvailable)
    }

    @Test
    fun `a facility reporting no capacity does not divide by zero`() {
        val response = facilityResponse(totalSpots = "0", occupied = "12")

        val stored = response.toNSWParkRideFacilityDetail(stopName = "Gosford", stopId = "2250")

        assertEquals(0L, stored.percentageFull)
        assertEquals(0L, stored.spotsAvailable)
    }

    // endregion

    private fun facilityResponse(
        totalSpots: String,
        occupied: String,
    ) = CarParkFacilityDetailResponse(
        tsn = "2250",
        time = "0",
        spots = totalSpots,
        zones = listOf(
            Zone(
                spots = totalSpots,
                zoneId = "1",
                occupancy = Occupancy(
                    loop = null,
                    total = occupied,
                    monthlies = null,
                    openGate = null,
                    transients = null,
                ),
                zoneName = "Zone 1",
                parentZoneId = "0",
            ),
        ),
        parkID = 1,
        location = Location(suburb = "Gosford", address = "", latitude = "0.0", longitude = "0.0"),
        occupancy = Occupancy(
            loop = null,
            total = occupied,
            monthlies = null,
            openGate = null,
            transients = null,
        ),
        messageDate = "2026-08-06T08:16:00",
        facilityId = "26",
        facilityName = "Park&Ride - Gosford",
        tfnswFacilityId = "2250TPR001",
    )

    private fun facilityDetail(
        facilityId: String,
        stopId: String,
        facilityName: String = "Car park $facilityId",
        percentageFull: Long = 60,
    ) = NSWParkRideFacilityDetail(
        facilityId = facilityId,
        spotsAvailable = 40,
        totalSpots = 100,
        facilityName = facilityName,
        percentageFull = percentageFull,
        stopId = stopId,
        stopName = "Station $stopId",
        timeText = "8:00 AM",
        suburb = "",
        address = "",
        latitude = 0.0,
        longitude = 0.0,
        timestamp = 0,
    )
}
