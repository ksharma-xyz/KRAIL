package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeNswStopsSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.sandook.db.SelectStopsNearby
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Real stops with their real positions, so the query is asked about places riders stand at.
private val AT_CENTRAL_STATION = LatLng(latitude = -33.883852, longitude = 151.206272)

private fun nearbyRow(stop: IntroStop, lat: Double, lon: Double, classes: String, distanceSquared: Double = 0.0) =
    SelectStopsNearby(
        stopId = stop.stopId,
        stopName = stop.name,
        stopLat = lat,
        stopLon = lon,
        isParent = null,
        productClasses = classes,
        distanceSquared = distanceSquared,
    )

class IntroStopsGatewayTest {

    private val nswStops = FakeNswStopsSandook(
        nearbyRows = listOf(
            nearbyRow(CENTRAL_STOP, -33.883852, 151.206272, classes = "1,2,4"),
            nearbyRow(REDFERN_STOP, -33.8923698959, 151.198484708, classes = "1", distanceSquared = 1.1),
            nearbyRow(JUNIORS_KINGSFORD, -33.924978, 151.22927, classes = "4", distanceSquared = 9.0),
        ),
    )
    private val sandook = FakeSandook()
    private val stopResults = FakeStopResultsManager()

    private fun gateway() = fakeStopsGateway(
        ioDispatcher = Dispatchers.Unconfined,
        nswStops = nswStops,
        sandook = sandook,
        stopResults = stopResults,
    )

    // region nearby stations

    @Test
    fun `the nearest stations are the rows of the stops table's nearby query, as ids, names and modes`() = krailRunTest {
        val found = gateway().nearbyStations(AT_CENTRAL_STATION)

        assertEquals(
            listOf(
                ListedStop(CENTRAL_STOP, listOf(TransportMode.Train, TransportMode.Metro, TransportMode.LightRail)),
                ListedStop(REDFERN_STOP, listOf(TransportMode.Train)),
                ListedStop(JUNIORS_KINGSFORD, listOf(TransportMode.LightRail)),
            ),
            found,
        )
    }

    @Test
    fun `one query is made, for train, metro and light rail within the radius, around the fix`() = krailRunTest {
        gateway().nearbyStations(AT_CENTRAL_STATION)

        val call = nswStops.nearbyCalls.single()
        assertEquals(AT_CENTRAL_STATION.latitude, call.centerLat)
        assertEquals(AT_CENTRAL_STATION.longitude, call.centerLon)
        assertEquals(INTRO_NEARBY_RADIUS_KM, call.radiusKm)
        assertEquals(1L, call.filterByModes, "the modes are filtered")
        assertEquals(
            listOf(TransportMode.Train, TransportMode.Metro, TransportMode.LightRail).map { it.productClass.toLong() },
            call.productClasses,
            "stations only: a bus stop is not offered as a start",
        )
        assertEquals(INTRO_NEARBY_QUERY_LIMIT, call.maxResults)
    }

    @Test
    fun `the query's box surrounds the fix on all sides and is no wider than the radius needs`() = krailRunTest {
        gateway().nearbyStations(AT_CENTRAL_STATION)

        val call = nswStops.nearbyCalls.single()
        assertTrue(call.minLat < call.centerLat && call.centerLat < call.maxLat)
        assertTrue(call.minLon < call.centerLon && call.centerLon < call.maxLon)
        // A radius in kilometres is about 0.009 degrees of latitude for each kilometre.
        assertTrue(call.maxLat - call.minLat < 2 * INTRO_NEARBY_RADIUS_KM * 0.0095, "${call.maxLat - call.minLat}")
    }

    @Test
    fun `a position that cannot be searched from is no stations, and no query is made`() = krailRunTest {
        val found = gateway().nearbyStations(LatLng(latitude = 500.0, longitude = 151.0))

        assertEquals(emptyList(), found)
        assertTrue(nswStops.nearbyCalls.isEmpty())
    }

    @Test
    fun `a stop with a mode the app does not know is listed with the modes it does`() = krailRunTest {
        nswStops.nearbyRows = listOf(nearbyRow(CENTRAL_STOP, -33.88, 151.2, classes = "1, 999,x,4"))

        val found = gateway().nearbyStations(AT_CENTRAL_STATION)

        assertEquals(listOf(TransportMode.Train, TransportMode.LightRail), found.single().modes)
    }

    @Test
    fun `no rows is no stations`() = krailRunTest {
        nswStops.nearbyRows = emptyList()

        assertEquals(emptyList(), gateway().nearbyStations(AT_CENTRAL_STATION))
    }

    // endregion

    // region modes

    @Test
    fun `the modes of the stops asked for come from the stops table, and a stop it does not have is left out`() =
        krailRunTest {
            sandook.insertNswStop("200060", "Central Station", -33.883852, 151.206272, isParent = null)
            sandook.insertNswStopProductClass("200060", TransportMode.Train.productClass)
            sandook.insertNswStopProductClass("200060", TransportMode.Metro.productClass)
            sandook.insertNswStop("201510", "Redfern Station", -33.8923698959, 151.198484708, isParent = null)
            sandook.insertNswStopProductClass("201510", TransportMode.Train.productClass)

            val modes = gateway().modesFor(listOf("200060", "201510", "999999"))

            assertEquals(
                mapOf(
                    "200060" to listOf(TransportMode.Train, TransportMode.Metro),
                    "201510" to listOf(TransportMode.Train),
                ),
                modes,
            )
        }

    @Test
    fun `no ids is no modes`() = krailRunTest {
        assertEquals(emptyMap(), gateway().modesFor(emptyList()))
    }

    // endregion

    // region the typed search

    private fun stopResult(stop: IntroStop, vararg modes: TransportMode) =
        SearchStopState.SearchResult.Stop(stop.name, stop.stopId, persistentListOf(*modes))

    @Test
    fun `the typed search is the local stop search, with routes left out, and answers with stops only`() =
        krailRunTest {
            stopResults.answer = listOf(
                stopResult(CHATSWOOD_STOP, TransportMode.Train),
                SearchStopState.SearchResult.Trip(
                    tripId = "t1",
                    routeShortName = "702",
                    headsign = "Blacktown",
                    stops = persistentListOf(),
                    transportMode = TransportMode.Bus,
                ),
                stopResult(JUNIORS_KINGSFORD, TransportMode.LightRail),
            )

            val found = gateway().search("ch")

            assertEquals(listOf("ch" to false), stopResults.searches, "routes are not searched")
            assertEquals(
                listOf(
                    ListedStop(CHATSWOOD_STOP, listOf(TransportMode.Train)),
                    ListedStop(JUNIORS_KINGSFORD, listOf(TransportMode.LightRail)),
                ),
                found,
            )
        }

    @Test
    fun `the typed search shows no more than the limit, best first`() = krailRunTest {
        stopResults.answer = (1..INTRO_SEARCH_RESULT_LIMIT + 5).map { n ->
            stopResult(IntroStop("id$n", "Stop $n"))
        }

        val found = gateway().search("stop")

        assertEquals(INTRO_SEARCH_RESULT_LIMIT, found.size)
        assertEquals("id1", found.first().stop.stopId)
    }

    // endregion

    @Test
    fun `the suggested lists are the bundled ones when the flag has none`() {
        assertEquals(DEFAULT_SUGGESTED_STOPS, gateway().suggestedStops)
    }
}
