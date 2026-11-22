package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CompletableDeferred
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.trip.planner.ui.parkride.RealParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCarPark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val TALLAWONG_CAR_PARKS = listOf("Tallawong P1", "Tallawong P2", "Tallawong P3")

class IntroFlowViewModelParkRideAvailabilityTest {

    @Test
    fun `a fix inside five kilometres settles the nearest station with its car park readings`() = krailRunTest {
        val rig = IntroRig(this)
        rig.refreshWritesBellaVista(spots = 120, percentFull = 40, time = "8:05 AM")

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )

        val card = rig.card()
        assertEquals("2153478", card.stationId)
        assertEquals("Bella Vista", card.stationName)
        assertEquals(ParkRideAvailabilityState.Content, card.availability)
        assertEquals(
            listOf(
                ParkRideCarPark("31", "Bella Vista", spotsAvailable = 120, percentageFull = 40, timeText = "8:05 AM"),
            ),
            card.carParks,
        )
        assertEquals(LocationResult.GRANTED, rig.state.locationResult)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step, "the card is on the question step")
        assertFalse(rig.state.addedParkRide)
    }

    @Test
    fun `nothing is fetched until a station is found`() = krailRunTest {
        val cases = listOf(
            "denied" to IntroRig(this).also {
                it.send(
                    IntroUiEvent.AnswerParkRide(yes = true),
                    IntroUiEvent.LocationResultReceived(LocationResult.DENIED, null),
                )
            },
            "outside five kilometres" to IntroRig(this).also {
                it.send(
                    IntroUiEvent.AnswerParkRide(yes = true),
                    IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
                )
            },
            "stops failed" to IntroRig(this, initialReadiness = StopsReadiness.Failed(attempt = 1, reason = "disk")).also {
                it.showBellaVista()
            },
            "only asked" to IntroRig(this).also { it.send(IntroUiEvent.AnswerParkRide(yes = true)) },
        )

        cases.forEach { (case, rig) -> assertTrue(rig.loader.refreshedMappings.isEmpty(), case) }
    }

    @Test
    fun `the card shows at once with names only and settles when the one call is back`() = krailRunTest {
        val rig = IntroRig(this)
        val release = CompletableDeferred<Unit>()
        rig.loader.onRefresh = { release.await() }

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG),
        )

        val loading = rig.card()
        assertEquals(ParkRideAvailabilityState.Loading, loading.availability)
        assertEquals(TALLAWONG_CAR_PARKS, loading.carParks.map { it.name })
        assertTrue(loading.carParks.none { it.hasReading })

        rig.parkRideSandook.insertOrReplaceAll(
            listOf(
                reading("26", "2155384", spots = 0, percentFull = 100),
                reading("28", "2155384", spots = 30, percentFull = 85),
            ),
        )
        release.complete(Unit)
        runCurrent()

        val settled = rig.card()
        assertEquals(ParkRideAvailabilityState.Content, settled.availability)
        // A full car park is a reading, and a car park with no row is a name.
        assertEquals(listOf(0, null, 30), settled.carParks.map { it.spotsAvailable })
        assertEquals(listOf(100, null, 85), settled.carParks.map { it.percentageFull })
    }

    @Test
    fun `a call that fails leaves the station name and car parks with no numbers`() = krailRunTest {
        val rig = IntroRig(this)
        rig.loader.onRefresh = { error("offline") }

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG),
        )

        val card = rig.card()
        assertEquals("Tallawong", card.stationName)
        assertEquals(ParkRideAvailabilityState.Unavailable, card.availability)
        assertEquals(TALLAWONG_CAR_PARKS, card.carParks.map { it.name })
        assertTrue(card.carParks.none { it.hasReading })
    }

    @Test
    fun `a call that writes nothing is the same as one that fails`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )

        assertEquals(ParkRideAvailabilityState.Unavailable, rig.card().availability)
        assertEquals(listOf("Bella Vista"), rig.card().carParks.map { it.name })
    }

    @Test
    fun `availability is asked for once for every car park of the station and never again`() = krailRunTest {
        val rig = IntroRig(this)
        rig.refreshWritesBellaVista()

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG),
        )
        assertEquals(listOf(TALLAWONG_STATION.mappings), rig.loader.refreshedMappings)

        pumpOnce(60 * 60 * 1_000L)
        rig.send(IntroUiEvent.Next, IntroUiEvent.Back)

        assertEquals(1, rig.loader.refreshedMappings.size, "no timer, no poll and no retry, only the one call")
    }

    @Test
    fun `a second intro inside the cooldown makes no second call and shows the stored readings`() = krailRunTest {
        val shared = FakeNswParkRideSandook()
        val service = CountingParkRideService()
        val loader = RealParkRideAvailabilityLoader(
            parkRideSandook = shared,
            parkRideService = service,
            flag = FakeFlag(),
        )

        listOf(1, 2).forEach { run ->
            val rig = IntroRig(this, availabilityLoader = loader, parkRideSandook = shared)
            rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
            )

            assertEquals(1, service.callCount, "run $run: one call for the one car park, none once it is warm")
            val carPark = rig.card().carParks.single()
            assertEquals(574, carPark.spotsAvailable, "run $run: 774 spots less 200 taken")
            assertEquals(25, carPark.percentageFull)
        }
    }

    @Test
    fun `clearing the view model cancels an availability call that is still in flight`() = krailRunTest {
        val rig = IntroRig(this)
        val never = CompletableDeferred<Unit>()
        rig.loader.onRefresh = { never.await() }
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )
        assertEquals(ParkRideAvailabilityState.Loading, rig.card().availability)

        rig.clearViewModel()
        rig.parkRideSandook.insertOrReplaceAll(listOf(reading("31", "2153478")))
        never.complete(Unit)
        runCurrent()

        assertEquals(ParkRideAvailabilityState.Loading, rig.card().availability, "the cancelled look wrote nothing")
    }

    /** Wraps the shared fake purely to count network hits, as the loader's own test does. */
    private class CountingParkRideService(
        private val delegate: ParkRideService = FakeParkRideService(),
    ) : ParkRideService {
        var callCount = 0
            private set

        override suspend fun fetchCarParkFacilities(facilityId: String): Result<CarParkFacilityDetailResponse> {
            callCount++
            return delegate.fetchCarParkFacilities(facilityId)
        }

        override suspend fun fetchCarParkFacilities(): Result<Map<String, String>> = delegate.fetchCarParkFacilities()

        override suspend fun fetchAvailabilityForStops(stopIds: List<String>): ParkingStopBatchResponse? =
            delegate.fetchAvailabilityForStops(stopIds)
    }
}
