package xyz.ksharma.krail.trip.planner.ui.parkride

import kotlinx.coroutines.flow.first
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideUserFacilityEvent
import xyz.ksharma.krail.core.appreview.DelightMoment
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The one add that the Park & Ride picker and the suggestion card on home share. It is the real adder on
 * the shared fake stores, so the rows, the review moment and the report are what a rider's tap produces.
 */
class ParkRideStationAdderTest {

    private val parkRideSandook = FakeNswParkRideSandook()
    private val analytics = FakeAnalytics()
    private val appReviewManager = FakeAppReviewManager()
    private val adder = ParkRideStationAdder(parkRideSandook, analytics, appReviewManager)

    private suspend fun savedRows() = parkRideSandook.observeSavedParkRides().first()

    private fun reported() = analytics.getTrackedEvents("park_ride_user_facility").mapNotNull { it.properties }

    @Test
    fun `adding stores every car park of the station as the riders own`() = krailRunTest {
        adder.add(TALLAWONG, ParkRideUserFacilityEvent.Source.HOME_SUGGESTION)

        val rows = savedRows()
        assertEquals(listOf("26", "27", "28"), rows.map { it.facilityId })
        assertEquals(setOf(UserAdded.value), rows.map { it.source }.toSet())
        assertEquals(setOf("Tallawong Station"), rows.map { it.stopName }.toSet())
        assertEquals(listOf("Tallawong P1", "Tallawong P2", "Tallawong P3"), rows.map { it.facilityName })
    }

    @Test
    fun `the stop's name is stored and the catalogue's name stands in when it has none`() = krailRunTest {
        adder.add(TALLAWONG.copy(stopName = ""), ParkRideUserFacilityEvent.Source.HOME_SUGGESTION)

        assertEquals(setOf("Tallawong"), savedRows().map { it.stopName }.toSet())
    }

    @Test
    fun `adding arms the Park and Ride added review moment once`() = krailRunTest {
        adder.add(TALLAWONG, ParkRideUserFacilityEvent.Source.HOME_SUGGESTION)

        assertEquals(listOf(DelightMoment.PARK_RIDE_ADDED), appReviewManager.armedMoments)
    }

    @Test
    fun `adding is reported once, as an add from the surface that asked`() = krailRunTest {
        adder.add(TALLAWONG, ParkRideUserFacilityEvent.Source.HOME_SUGGESTION)

        assertEquals(
            listOf(
                mapOf(
                    "facilityId" to "26, 27, 28",
                    "stopId" to "2155384",
                    "action" to "add",
                    "source" to "home_suggestion",
                ),
            ),
            reported(),
        )
    }

    @Test
    fun `the picker reports its own source through the same add`() = krailRunTest {
        adder.add(TALLAWONG, ParkRideUserFacilityEvent.Source.ADD_PARK_RIDE_SCREEN)

        assertEquals(listOf("add_park_ride_screen"), reported().map { it["source"] })
    }

    @Test
    fun `adding the same station twice leaves one row per car park`() = krailRunTest {
        adder.add(TALLAWONG, ParkRideUserFacilityEvent.Source.ADD_PARK_RIDE_SCREEN)
        adder.add(TALLAWONG, ParkRideUserFacilityEvent.Source.HOME_SUGGESTION)

        assertEquals(3, savedRows().size)
    }

    @Test
    fun `a station with one car park is one row`() = krailRunTest {
        adder.add(MONA_VALE, ParkRideUserFacilityEvent.Source.HOME_SUGGESTION)

        assertEquals(listOf("12"), savedRows().map { it.facilityId })
    }

    private companion object {
        val TALLAWONG = ParkRideStationPickerItem(
            stationId = "2155384",
            stationName = "Tallawong",
            stopName = "Tallawong Station",
            carParkNames = listOf("Tallawong P1", "Tallawong P2", "Tallawong P3"),
            mappings = listOf(
                ParkRideMapping("2155384", "26", "Tallawong P1"),
                ParkRideMapping("2155384", "27", "Tallawong P2"),
                ParkRideMapping("2155384", "28", "Tallawong P3"),
            ),
        )

        val MONA_VALE = ParkRideStationPickerItem(
            stationId = "210318",
            stationName = "Mona Vale",
            stopName = "Mona Vale",
            carParkNames = listOf("Mona Vale"),
            mappings = listOf(ParkRideMapping("210318", "12", "Mona Vale")),
        )
    }
}
