package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.appreview.DelightMoment
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.helpers.CallLog
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_PARK_RIDE_SUGGESTION_SHOWN
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.sandook.db.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideStationAdder
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * What the suggestion's ViewModel decides, on the shared fakes: when the card appears, which station, that
 * it is marked shown once and for good, that a rotation keeps it, and that Add to home and Not now end it.
 * The real stops table, labels and saved stations go through the fake database, so each lookup is the real
 * call and not a stub of it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParkRideSuggestionViewModelTest {

    private val callLog = CallLog()
    private val preferences = FakeSandookPreferences(callLog)
    private val sandook = FakeSandook().apply {
        insertNswStop("200070", "Town Hall Station", TOWN_HALL.latitude, TOWN_HALL.longitude, isParent = null)
        insertNswStop("2155384", "Tallawong Station", TALLAWONG_AT.latitude, TALLAWONG_AT.longitude, isParent = null)
    }
    private val parkRideSandook = FakeNswParkRideSandook()
    private val catalogue = FakeParkRideCatalogue(stations = listOf(TALLAWONG, PENRITH))
    private val analytics = FakeAnalytics()
    private val appReviewManager = FakeAppReviewManager()
    private val flag = FakeFlag()

    // Every screen the test puts in front is taken away again when the test ends, as the screen leaving would.
    private val screens = mutableListOf<Job>()

    private fun KrailTestScope.buildViewModel() = ParkRideSuggestionViewModel(
        preferences = preferences,
        sources = ParkRideSuggestionSources(catalogue, sandook, parkRideSandook),
        adder = ParkRideStationAdder(parkRideSandook, analytics, appReviewManager),
        flag = flag,
        ioDispatcher = ioDispatcher,
    )

    private fun viewModelTest(body: suspend KrailTestScope.() -> Unit) = krailRunTest {
        try {
            body()
        } finally {
            screens.forEach { it.cancel() }
        }
    }

    private fun KrailTestScope.watch(viewModel: ParkRideSuggestionViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        screens += job
        runCurrent()
        return job
    }

    private fun mapsOn() = flag.setFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key, FlagValue.BooleanValue(true))

    private fun ParkRideSuggestionViewModel.at(position: LatLng?) =
        onEvent(ParkRideSuggestionEvent.LocationChanged(position))

    private fun ParkRideSuggestionViewModel.shown() = uiState.value.tile?.let {
        onEvent(ParkRideSuggestionEvent.Shown(it.stationId))
    }

    private fun shownWrites() = callLog.count("pref:$KEY_PARK_RIDE_SUGGESTION_SHOWN=true")

    private fun setHome(stopId: String?, stopName: String? = null) =
        sandook.upsertStopLabel("Home", "H", stopId, stopName, 0L)

    // region when the card appears

    @Test
    fun `with the maps flag off there is no card and nothing is read or written`() = viewModelTest {
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        assertNull(viewModel.uiState.value.tile)
        assertTrue(callLog.entries.isEmpty(), "A build without the map home touches no preference: ${callLog.entries}")
    }

    @Test
    fun `with no Home and no position there is no card`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)

        assertNull(viewModel.uiState.value.tile)
    }

    @Test
    fun `a rider with a station near their position is offered it`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        val tile = assertNotNull(viewModel.uiState.value.tile)
        assertEquals("Tallawong Station", tile.stationName)
        assertEquals(listOf("26", "27", "28"), tile.carParks.map { it.facilityId })
    }

    @Test
    fun `a position with no station near offers nothing`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(OUT_AT_SEA)
        runCurrent()

        assertNull(viewModel.uiState.value.tile)
    }

    @Test
    fun `Home is measured from before the position when it has a stop`() = viewModelTest {
        mapsOn()
        setHome("2155384", "Tallawong Station")
        val viewModel = buildViewModel()
        watch(viewModel)

        // The rider is in the city, and Home is at Tallawong.
        viewModel.at(TOWN_HALL)
        runCurrent()

        assertEquals("Tallawong Station", viewModel.uiState.value.tile?.stationName)
    }

    @Test
    fun `a Home with no station near gives nothing even when the rider stands at one`() = viewModelTest {
        mapsOn()
        setHome("200070", "Town Hall Station")
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        assertNull(viewModel.uiState.value.tile)
    }

    @Test
    fun `a Home with no stop, or a stop the table does not hold, falls back to the position`() = viewModelTest {
        mapsOn()
        val unset = buildViewModel()
        watch(unset)
        unset.at(TALLAWONG_AT)
        runCurrent()
        assertNotNull(unset.uiState.value.tile)

        setHome("999999", "Nowhere Station")
        val missing = buildViewModel()
        watch(missing)
        missing.at(TALLAWONG_AT)
        runCurrent()
        assertNotNull(missing.uiState.value.tile)
    }

    @Test
    fun `a rider the Intro or an earlier card already asked is never offered it`() = viewModelTest {
        mapsOn()
        preferences.setBoolean(KEY_PARK_RIDE_SUGGESTION_SHOWN, true)
        callLog.clear()
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        assertNull(viewModel.uiState.value.tile)
        assertEquals(0, shownWrites())
    }

    @Test
    fun `a rider with a saved station is not offered one, and a station saved later takes the card away`() =
        viewModelTest {
            mapsOn()
            val viewModel = buildViewModel()
            watch(viewModel)
            viewModel.at(TALLAWONG_AT)
            runCurrent()
            assertNotNull(viewModel.uiState.value.tile)

            parkRideSandook.insertOrReplaceSavedParkRides(
                setOf(savedRow("26", source = SavedTrips)),
                source = SavedTrips,
            )
            runCurrent()

            assertNull(viewModel.uiState.value.tile)
        }

    @Test
    fun `a rider who already has a saved station is offered nothing from the start`() = viewModelTest {
        mapsOn()
        parkRideSandook.insertOrReplaceSavedParkRides(setOf(savedRow("26", source = UserAdded)), source = UserAdded)
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        assertNull(viewModel.uiState.value.tile)
    }

    @Test
    fun `an empty catalogue offers nothing`() = viewModelTest {
        mapsOn()
        catalogue.stations = emptyList()
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        assertNull(viewModel.uiState.value.tile)
    }

    @Test
    fun `before the card is drawn the offer follows the position`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()
        assertNotNull(viewModel.uiState.value.tile)

        viewModel.at(OUT_AT_SEA)
        runCurrent()

        assertNull(viewModel.uiState.value.tile)
        assertEquals(0, shownWrites(), "A card that was never drawn is never marked shown.")
    }

    // endregion

    // region the radius

    @Test
    fun `the radius is the Remote Config value, 5 km by default`() = viewModelTest {
        mapsOn()
        catalogue.stations = listOf(SCHOFIELDS)

        val byDefault = buildViewModel()
        watch(byDefault)
        byDefault.at(TALLAWONG_AT)
        runCurrent()
        assertEquals("Schofields Station", byDefault.uiState.value.tile?.stationName)

        flag.setFlagValue(FlagKeys.PARK_RIDE_SUGGESTION_RADIUS_KM.key, FlagValue.StringValue("1.0"))
        val narrow = buildViewModel()
        watch(narrow)
        narrow.at(TALLAWONG_AT)
        runCurrent()
        assertNull(narrow.uiState.value.tile)
    }

    @Test
    fun `a radius that is out of range is ignored and 5 km applies`() = viewModelTest {
        mapsOn()
        catalogue.stations = listOf(SCHOFIELDS)
        flag.setFlagValue(FlagKeys.PARK_RIDE_SUGGESTION_RADIUS_KM.key, FlagValue.StringValue("0.1"))
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        assertEquals("Schofields Station", viewModel.uiState.value.tile?.stationName)
    }

    // endregion

    // region what the card says

    @Test
    fun `the card shows the stored reading and its time, and nothing is fetched to fill it`() = viewModelTest {
        mapsOn()
        parkRideSandook.insertOrReplace(reading("26", free = 50, time = "8:05 AM", at = 300L))
        parkRideSandook.insertOrReplace(reading("27", free = 7, time = "7:50 AM", at = 100L))
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        val tile = assertNotNull(viewModel.uiState.value.tile)
        assertEquals(57, tile.totalFreeSpots)
        assertEquals("7:50 AM", tile.timeText)
    }

    @Test
    fun `with no stored reading the card is the station's name alone`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)

        viewModel.at(TALLAWONG_AT)
        runCurrent()

        val tile = assertNotNull(viewModel.uiState.value.tile)
        assertNull(tile.totalFreeSpots)
        assertNull(tile.timeText)
    }

    // endregion

    // region shown once, for good

    @Test
    fun `drawing the card writes the shown flag once, however often it is drawn`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)
        viewModel.at(TALLAWONG_AT)
        runCurrent()

        repeat(3) { viewModel.shown() }
        runCurrent()

        assertEquals(1, shownWrites())
        assertTrue(preferences.getBoolean(KEY_PARK_RIDE_SUGGESTION_SHOWN) == true)
    }

    @Test
    fun `a card for a station that is not on offer marks nothing as shown`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)
        viewModel.at(TALLAWONG_AT)
        runCurrent()

        viewModel.onEvent(ParkRideSuggestionEvent.Shown("not-a-station"))
        runCurrent()

        assertEquals(0, shownWrites())
    }

    @Test
    fun `once drawn the station is held and a position that jitters never swaps it`() = viewModelTest {
        mapsOn()
        catalogue.stations = listOf(TALLAWONG, SCHOFIELDS)
        val viewModel = buildViewModel()
        watch(viewModel)
        viewModel.at(TALLAWONG_AT)
        runCurrent()
        viewModel.shown()
        runCurrent()

        viewModel.at(SCHOFIELDS_AT)
        runCurrent()
        viewModel.at(OUT_AT_SEA)
        runCurrent()

        assertEquals("Tallawong Station", viewModel.uiState.value.tile?.stationName)
    }

    @Test
    fun `a rotation keeps the card and its decision`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        var screen = watch(viewModel)
        viewModel.at(TALLAWONG_AT)
        runCurrent()
        viewModel.shown()
        runCurrent()

        // The screen is destroyed and recreated: the ViewModel stays and the collector comes back, inside
        // the grace and then well after it.
        screen.cancel()
        pumpOnce(1.seconds)
        screen = watch(viewModel)
        assertEquals("Tallawong Station", viewModel.uiState.value.tile?.stationName)

        screen.cancel()
        pumpOnce(30.seconds)
        screen = watch(viewModel)

        assertEquals("Tallawong Station", viewModel.uiState.value.tile?.stationName)
        assertEquals(1, shownWrites())
        screen.cancel()
    }

    @Test
    fun `a process that dies with the card up never offers it again`() = viewModelTest {
        mapsOn()
        val first = buildViewModel()
        watch(first)
        first.at(TALLAWONG_AT)
        runCurrent()
        first.shown()
        runCurrent()

        val relaunched = buildViewModel()
        watch(relaunched)
        relaunched.at(TALLAWONG_AT)
        runCurrent()

        assertNull(relaunched.uiState.value.tile)
    }

    // endregion

    // region Add to home and Not now

    @Test
    fun `Add to home adds every car park through the pickers add, reports it and ends the card`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)
        viewModel.at(TALLAWONG_AT)
        runCurrent()
        viewModel.shown()
        runCurrent()

        viewModel.onEvent(ParkRideSuggestionEvent.AddToHome)
        runCurrent()

        val rows = parkRideSandook.observeSavedParkRidesBySource(UserAdded).first()
        assertEquals(setOf("26", "27", "28"), rows.map { it.facilityId }.toSet())
        assertEquals(setOf("Tallawong Station"), rows.map { it.stopName }.toSet())
        assertEquals(listOf(DelightMoment.PARK_RIDE_ADDED), appReviewManager.armedMoments)
        val reported = analytics.getTrackedEvents("park_ride_user_facility").mapNotNull { it.properties }
        assertEquals(listOf("add"), reported.map { it["action"] })
        assertEquals(listOf("home_suggestion"), reported.map { it["source"] })
        assertNull(viewModel.uiState.value.tile)
    }

    @Test
    fun `a second tap on Add to home adds nothing more`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)
        viewModel.at(TALLAWONG_AT)
        runCurrent()
        viewModel.shown()
        runCurrent()

        repeat(2) { viewModel.onEvent(ParkRideSuggestionEvent.AddToHome) }
        runCurrent()

        assertEquals(1, analytics.getTrackedEvents("park_ride_user_facility").size)
        assertEquals(1, appReviewManager.armedMoments.size)
    }

    @Test
    fun `Add to home before the card was drawn does nothing`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)
        viewModel.at(TALLAWONG_AT)
        runCurrent()

        viewModel.onEvent(ParkRideSuggestionEvent.AddToHome)
        runCurrent()

        assertTrue(parkRideSandook.observeSavedParkRides().first().isEmpty())
        assertTrue(analytics.getTrackedEvents("park_ride_user_facility").isEmpty())
    }

    @Test
    fun `Not now only dismisses, and it stays gone`() = viewModelTest {
        mapsOn()
        val viewModel = buildViewModel()
        watch(viewModel)
        viewModel.at(TALLAWONG_AT)
        runCurrent()
        viewModel.shown()
        runCurrent()

        viewModel.onEvent(ParkRideSuggestionEvent.NotNow)
        runCurrent()
        viewModel.at(TALLAWONG_AT)
        runCurrent()

        assertNull(viewModel.uiState.value.tile)
        assertTrue(parkRideSandook.observeSavedParkRides().first().isEmpty(), "Not now saves nothing.")
        assertTrue(analytics.getTrackedEvents("park_ride_user_facility").isEmpty(), "Not now reports nothing.")
        assertTrue(appReviewManager.armedMoments.isEmpty())
        assertFalse(preferences.getBoolean(KEY_PARK_RIDE_SUGGESTION_SHOWN) != true, "The flag stays true.")
    }

    // endregion

    private fun savedRow(facilityId: String, source: SavedParkRideSource) =
        SavedParkRide(
            stopId = "2155384",
            facilityId = facilityId,
            stopName = "Tallawong Station",
            facilityName = "Tallawong P1",
            source = source.value,
        )

    private fun reading(id: String, free: Int, time: String, at: Long) = NSWParkRideFacilityDetail(
        facilityId = id,
        spotsAvailable = free.toLong(),
        totalSpots = 200L,
        facilityName = "Tallawong P$id",
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

    private fun <T : Any> assertNotNull(value: T?): T {
        kotlin.test.assertNotNull(value)
        return value
    }

    private companion object {
        val TALLAWONG_AT = LatLng(-33.7056, 150.9082)
        val SCHOFIELDS_AT = LatLng(-33.7050, 150.8750)
        val TOWN_HALL = LatLng(-33.8732, 151.2070)
        val OUT_AT_SEA = LatLng(-34.9, 152.9)

        val TALLAWONG = station(
            id = "2155384",
            name = "Tallawong",
            stopName = "Tallawong Station",
            position = StationPosition(TALLAWONG_AT.latitude, TALLAWONG_AT.longitude),
            mappings = listOf(
                ParkRideMapping("2155384", "26", "Tallawong P1"),
                ParkRideMapping("2155384", "27", "Tallawong P2"),
                ParkRideMapping("2155384", "28", "Tallawong P3"),
            ),
        )

        // About 3 km west of Tallawong, so it is inside the default 5 km and outside 1 km.
        val SCHOFIELDS = station(
            id = "2155382",
            name = "Schofields",
            stopName = "Schofields Station",
            position = StationPosition(SCHOFIELDS_AT.latitude, SCHOFIELDS_AT.longitude),
            mappings = listOf(ParkRideMapping("2155382", "31", "Schofields")),
        )

        // Far from Tallawong and from the city, so it is never the nearest to either.
        val PENRITH = station(
            id = "2750001",
            name = "Penrith",
            stopName = "Penrith Station",
            position = StationPosition(-33.7510, 150.6940),
            mappings = listOf(ParkRideMapping("2750001", "41", "Penrith")),
        )

        fun station(
            id: String,
            name: String,
            stopName: String,
            position: StationPosition,
            mappings: List<ParkRideMapping>,
        ) = ParkRideStationPickerItem(
            stationId = id,
            stationName = name,
            stopName = stopName,
            carParkNames = mappings.map { it.facilityName },
            mappings = mappings,
            position = position,
        )
    }
}
