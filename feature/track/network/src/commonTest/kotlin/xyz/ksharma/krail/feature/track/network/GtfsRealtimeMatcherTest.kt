package xyz.ksharma.krail.feature.track.network

import com.google.transit.realtime.Position
import com.google.transit.realtime.VehiclePosition
import xyz.ksharma.krail.feature.track.VehicleStatus
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeMatcher.findLiveVehiclePosition
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeMatcher.findStopDelays
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `GtfsRealtimeMatcher` is the pure half of live tracking: given a decoded GTFS-RT feed and the
 * identifiers TfNSW handed back for a leg, which vehicle is ours?
 *
 * Getting it wrong does not throw — it draws someone else's train on the map, or draws nothing.
 * The feed selection is equally invisible: Sydney Trains and NSW Trains share productClass 1 but
 * publish to different endpoints, which is the entire reason the lookup takes the raw iconId.
 */
class GtfsRealtimeMatcherTest {

    private val startDate = "20260409"

    // GTFS-RT carries coordinates as float32; widening to Double is not exact.
    private val coordinateTolerance = 1e-4

    // region feed selection

    @Test
    fun `GIVEN productClass 1 WHEN choosing feeds THEN Sydney Trains and NSW Trains split by iconId`() {
        assertEquals(
            listOf("sydneytrains"),
            GtfsRealtimeMatcher.feedNamesForTransportation(iconId = 1, productClass = 1),
        )
        assertEquals(
            listOf("sydneytrains"),
            GtfsRealtimeMatcher.feedNamesForTransportation(iconId = 19, productClass = 1),
        )
        assertEquals(
            listOf("nswtrains"),
            GtfsRealtimeMatcher.feedNamesForTransportation(iconId = 2, productClass = 1),
        )
        assertEquals(
            listOf("nswtrains"),
            GtfsRealtimeMatcher.feedNamesForTransportation(iconId = 3, productClass = 1),
        )
    }

    @Test
    fun `GIVEN every bus icon on productClass 5 WHEN choosing feeds THEN they share the one buses feed`() {
        val busIcons = listOf(4L, 5L, 6L, 9L, 14L, 23L)

        assertEquals(
            List(busIcons.size) { listOf("buses") },
            busIcons.map { GtfsRealtimeMatcher.feedNamesForTransportation(it, productClass = 5) },
        )
    }

    @Test
    fun `GIVEN a region split across numbered files WHEN choosing feeds THEN every file is returned`() {
        assertEquals(
            listOf("regionbuses/northcoast", "regionbuses/northcoast2", "regionbuses/northcoast3"),
            GtfsRealtimeMatcher.feedNamesForTransportation(iconId = 35, productClass = 5),
        )
    }

    @Test
    fun `GIVEN a mode with no realtime coverage WHEN choosing feeds THEN nothing is fetched`() {
        // Coaches, school buses, private ferries and anything unrecognised have no GTFS-RT feed.
        val uncovered = listOf(7L, 22L, 8L, 11L, 18L, 999L)

        assertTrue(
            uncovered.all {
                GtfsRealtimeMatcher.feedNamesForTransportation(it, productClass = 7).isEmpty()
            },
        )
        assertTrue(GtfsRealtimeMatcher.feedNamesForTransportation(iconId = null, productClass = 1).isEmpty())
    }

    // endregion

    // region vehicle matching

    @Test
    fun `GIVEN an exact trip id in the feed WHEN matching THEN that vehicle wins over a route match`() {
        val message = feed(
            vehicleEntity(id = "route-only", routeId = "T1", startDate = startDate),
            vehicleEntity(
                id = "exact",
                tripId = "123.T1.abc",
                position = Position(latitude = -33.90f, longitude = 151.30f),
            ),
        )

        val matched = message.findLiveVehiclePosition(
            realtimeTripId = "123.T1.abc",
            transportationId = "nsw:T1",
            departureAest = "08:00",
            startDate = startDate,
        )

        assertNotNull(matched)
        assertEquals(-33.90, matched.latitude, coordinateTolerance)
        assertEquals(151.30, matched.longitude, coordinateTolerance)
    }

    @Test
    fun `GIVEN a feed that prefixes trip ids WHEN matching THEN the suffix still identifies the trip`() {
        val message = feed(
            vehicleEntity(
                id = "prefixed",
                tripId = "SYD-123.T1.abc",
                position = Position(latitude = -33.91f, longitude = 151.31f),
            ),
        )

        val matched = message.findLiveVehiclePosition(
            realtimeTripId = "123.T1.abc",
            transportationId = "nsw:T1",
            departureAest = "08:00",
            startDate = startDate,
        )

        assertNotNull(matched)
        assertEquals(-33.91, matched.latitude, coordinateTolerance)
    }

    @Test
    fun `GIVEN no trip id WHEN matching THEN the route falls back, ignoring case and punctuation`() {
        val message = feed(vehicleEntity(id = "route", routeId = "t-1", startDate = startDate))

        val matched = message.findLiveVehiclePosition(
            realtimeTripId = null,
            transportationId = "nsw:T1",
            departureAest = "08:00",
            startDate = startDate,
        )

        assertNotNull(matched)
        assertEquals(-33.87, matched.latitude, coordinateTolerance)
    }

    @Test
    fun `GIVEN a route match from another day WHEN matching THEN yesterday's vehicle is not used`() {
        val message = feed(vehicleEntity(id = "route", routeId = "T1", startDate = "20260408"))

        assertNull(
            message.findLiveVehiclePosition(
                realtimeTripId = null,
                transportationId = "nsw:T1",
                departureAest = "08:00",
                startDate = startDate,
            ),
        )
    }

    @Test
    fun `GIVEN a feed that omits start_date WHEN matching THEN the route match is still accepted`() {
        val message = feed(vehicleEntity(id = "route", routeId = "T1", startDate = null))

        val matched = message.findLiveVehiclePosition(
            realtimeTripId = null,
            transportationId = "nsw:T1",
            departureAest = "08:00",
            startDate = startDate,
        )

        assertNotNull(matched)
        assertEquals(-33.87, matched.latitude, coordinateTolerance)
    }

    @Test
    fun `GIVEN a matched vehicle with no coordinates WHEN matching THEN nothing is drawn`() {
        val message = feed(vehicleEntity(id = "no-position", tripId = "trip-1", position = null))

        assertNull(
            message.findLiveVehiclePosition(
                realtimeTripId = "trip-1",
                transportationId = "nsw:T1",
                departureAest = "08:00",
                startDate = startDate,
            ),
        )
    }

    @Test
    fun `GIVEN an empty feed WHEN matching THEN nothing is drawn`() {
        assertNull(
            feed().findLiveVehiclePosition(
                realtimeTripId = "trip-1",
                transportationId = "nsw:T1",
                departureAest = "08:00",
                startDate = startDate,
            ),
        )
    }

    @Test
    fun `GIVEN each vehicle stop status WHEN mapped THEN only stopped and incoming keep their meaning`() {
        val statuses = listOf(
            VehiclePosition.VehicleStopStatus.INCOMING_AT to VehicleStatus.INCOMING_AT,
            VehiclePosition.VehicleStopStatus.STOPPED_AT to VehicleStatus.STOPPED_AT,
            VehiclePosition.VehicleStopStatus.IN_TRANSIT_TO to VehicleStatus.IN_TRANSIT_TO,
            null to VehicleStatus.IN_TRANSIT_TO,
        )

        val mapped = statuses.map { (feedStatus, _) ->
            val message = feed(vehicleEntity(tripId = "trip-1", status = feedStatus))
            message.findLiveVehiclePosition(
                realtimeTripId = "trip-1",
                transportationId = "nsw:T1",
                departureAest = "08:00",
                startDate = startDate,
            )?.status
        }

        assertEquals(statuses.map { it.second }, mapped)
    }

    @Test
    fun `GIVEN a vehicle with no bearing or timestamp WHEN mapped THEN both default rather than crash`() {
        val matched = feed(vehicleEntity(tripId = "trip-1", timestamp = null))
            .findLiveVehiclePosition(
                realtimeTripId = "trip-1",
                transportationId = "nsw:T1",
                departureAest = "08:00",
                startDate = startDate,
            )

        assertEquals(0f, matched?.bearing)
        assertEquals(0L, matched?.lastUpdatedEpochSec)
    }

    // endregion

    // region stop delays

    @Test
    fun `GIVEN stop updates WHEN reading delays THEN arrival wins and departure fills the gap`() {
        val message = feed(
            tripUpdateEntity(
                tripId = "trip-1",
                stopTimeUpdates = listOf(
                    stopTimeUpdate(stopId = "a", arrivalDelay = 120, departureDelay = 300),
                    stopTimeUpdate(stopId = "b", departureDelay = -60),
                ),
            ),
        )

        val delays = message.findStopDelays(
            realtimeTripId = "trip-1",
            transportationId = "nsw:T1",
            departureAest = "08:00",
            startDate = startDate,
        )

        assertEquals(mapOf("a" to 120, "b" to -60), delays)
    }

    @Test
    fun `GIVEN updates with no stop or no delay WHEN reading delays THEN they are dropped`() {
        val message = feed(
            tripUpdateEntity(
                tripId = "trip-1",
                stopTimeUpdates = listOf(
                    stopTimeUpdate(stopId = null, arrivalDelay = 120),
                    stopTimeUpdate(stopId = "b"),
                    stopTimeUpdate(stopId = "c", arrivalDelay = 30),
                ),
            ),
        )

        val delays = message.findStopDelays(
            realtimeTripId = "trip-1",
            transportationId = "nsw:T1",
            departureAest = "08:00",
            startDate = startDate,
        )

        assertEquals(mapOf("c" to 30), delays)
    }

    @Test
    fun `GIVEN no trip id WHEN reading delays THEN the route match supplies them`() {
        val message = feed(
            tripUpdateEntity(
                tripId = "someone-else",
                routeId = "T1",
                startDate = startDate,
                stopTimeUpdates = listOf(stopTimeUpdate(stopId = "a", arrivalDelay = 60)),
            ),
        )

        assertEquals(
            mapOf("a" to 60),
            message.findStopDelays(
                realtimeTripId = null,
                transportationId = "nsw:T1",
                departureAest = "08:00",
                startDate = startDate,
            ),
        )
    }

    @Test
    fun `GIVEN nothing matching in the feed WHEN reading delays THEN the map is empty, not stale`() {
        val message = feed(
            tripUpdateEntity(
                tripId = "someone-else",
                routeId = "T4",
                startDate = startDate,
                stopTimeUpdates = listOf(stopTimeUpdate(stopId = "a", arrivalDelay = 60)),
            ),
        )

        assertTrue(
            message.findStopDelays(
                realtimeTripId = "trip-1",
                transportationId = "nsw:T1",
                departureAest = "08:00",
                startDate = startDate,
            ).isEmpty(),
        )
    }

    // endregion
}
