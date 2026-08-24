package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlinx.coroutines.test.runTest
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Where a journey starts when the rider did not say, and in particular what happens when the
 * app knows nothing about where they are.
 *
 * The ordering under test is the whole design: what the rider said beats where they are, where
 * they are beats where they usually start, and a guess never overrules something known. Home
 * sits at the bottom precisely because it is a guess about habit rather than a fact about now.
 */
class RiderOriginLocatorTest {

    private val sandook = FakeSandook()
    private val labelledStopLocator = LabelledStopLocator(sandook)

    // Wynyard and Seven Hills, roughly. Real coordinates, because the locator measures real
    // distances and invented units would pass while meaning something else on a map.
    private val wynyard = Location(latitude = -33.8657, longitude = 151.2065, timestamp = 0L)
    private val sevenHillsLat = -33.7743
    private val sevenHillsLon = 150.9370

    private fun putLabel(label: String, stopId: String, stopName: String, lat: Double, lon: Double) {
        sandook.upsertStopLabel(
            label = label,
            emoji = "📍",
            stopId = stopId,
            stopName = stopName,
            sortOrder = 0L,
        )
        sandook.insertNswStop(
            stopId = stopId,
            stopName = stopName,
            stopLat = lat,
            stopLon = lon,
            isParent = null,
        )
    }

    private fun locator(location: Location?) = RiderOriginLocator(
        resolveCurrentLocation = { location },
        labelledStopLocator = labelledStopLocator,
        nearbyStopsRepository = null,
    )

    @Test
    fun `no location and a Home set falls back to Home`() = runTest {
        // The reported bug: a destination was understood, the rider asked for a trip, and the
        // From field came back blank because nothing could be learned about where they were.
        putLabel("Home", "SEVEN_HILLS", "Seven Hills Station", sevenHillsLat, sevenHillsLon)

        val origin = locator(location = null).originStop(excludeStopId = "TOWN_HALL")

        assertEquals("SEVEN_HILLS", origin?.stopId)
    }

    @Test
    fun `no location and no Home leaves the field blank`() = runTest {
        // Nothing is known and nothing is invented. A rider with location denied and no Home
        // pinned has told the app nothing it can act on.
        putLabel("Work", "WYNYARD", "Wynyard Station", wynyard.latitude, wynyard.longitude)

        assertNull(locator(location = null).originStop(excludeStopId = "TOWN_HALL"))
    }

    @Test
    fun `Home is never used as the origin when it is also the destination`() = runTest {
        // "Get me home" with no location. Home to Home is a journey of no distance and reads as
        // the app not having understood at all.
        putLabel("Home", "SEVEN_HILLS", "Seven Hills Station", sevenHillsLat, sevenHillsLon)

        assertNull(locator(location = null).originStop(excludeStopId = "SEVEN_HILLS"))
    }

    @Test
    fun `a known location beats Home, even when Home is set`() = runTest {
        // The case that decides the ordering. A rider at Wynyard asking for somewhere is served
        // correctly by where they are and wrongly by where they usually start.
        putLabel("Home", "SEVEN_HILLS", "Seven Hills Station", sevenHillsLat, sevenHillsLon)
        putLabel("Work", "WYNYARD", "Wynyard Station", wynyard.latitude, wynyard.longitude)

        val origin = locator(location = wynyard).originStop(excludeStopId = "TOWN_HALL")

        assertEquals("WYNYARD", origin?.stopId)
    }

    @Test
    fun `standing at the destination stays blank rather than falling back to Home`() = runTest {
        // Standing at the destination is something the app knows about right now, so the guess
        // must not overrule it. Filling From with Home here would invent a trip the rider is
        // already at the end of.
        putLabel("Home", "SEVEN_HILLS", "Seven Hills Station", sevenHillsLat, sevenHillsLon)
        putLabel("Work", "WYNYARD", "Wynyard Station", wynyard.latitude, wynyard.longitude)

        assertNull(locator(location = wynyard).originStop(excludeStopId = "WYNYARD"))
    }

    @Test
    fun `a known location with no stop anywhere near does not reach for Home`() = runTest {
        // Location is known and says the rider is nowhere near their Home stop. Home would
        // contradict a fact the app holds, so the field stays blank.
        putLabel("Home", "SEVEN_HILLS", "Seven Hills Station", sevenHillsLat, sevenHillsLon)

        assertNull(locator(location = wynyard).originStop(excludeStopId = "TOWN_HALL"))
    }

    @Test
    fun `the Home label is matched whatever case it was stored in`() = runTest {
        putLabel("home", "SEVEN_HILLS", "Seven Hills Station", sevenHillsLat, sevenHillsLon)

        val origin = locator(location = null).originStop(excludeStopId = null)

        assertEquals("SEVEN_HILLS", origin?.stopId)
    }
}
