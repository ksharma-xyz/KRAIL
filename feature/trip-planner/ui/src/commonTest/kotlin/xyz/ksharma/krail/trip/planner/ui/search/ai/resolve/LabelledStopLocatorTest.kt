package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A rider standing near a stop they named should leave from it.
 *
 * Distances here are in real Sydney degrees rather than round numbers, because the locator uses
 * an equirectangular approximation and a test built on invented units would pass while the
 * radius meant something different on a map.
 */
class LabelledStopLocatorTest {

    private val sandook = FakeSandook()
    private val locator = LabelledStopLocator(sandook)

    // Wynyard, roughly.
    private val workLat = -33.8657
    private val workLon = 151.2065

    // Seven Hills, roughly 30km west. Nowhere near Wynyard by any radius.
    private val homeLat = -33.7743
    private val homeLon = 150.9370

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

    @Test
    fun `a rider standing at work leaves from work`() = runTest {
        putLabel("Work", "10101", "Wynyard Station", workLat, workLon)
        putLabel("Home", "20202", "Seven Hills Station", homeLat, homeLon)

        // Two hundred metres from Wynyard, going home.
        val origin = locator.nearestLabelledStop(
            latitude = workLat + 0.0018,
            longitude = workLon,
            excludeStopId = "20202",
        )

        assertEquals("10101", origin?.stopId)
    }

    @Test
    fun `standing at home and asking for home resolves to nothing`() = runTest {
        // Home to Home is a journey of no distance and reads as the app not having understood.
        // The destination is excluded, and there is nothing else in range.
        putLabel("Home", "20202", "Seven Hills Station", homeLat, homeLon)

        val origin = locator.nearestLabelledStop(
            latitude = homeLat,
            longitude = homeLon,
            excludeStopId = "20202",
        )

        assertNull(origin)
    }

    @Test
    fun `a labelled stop outside walking distance is not used`() = runTest {
        putLabel("Work", "10101", "Wynyard Station", workLat, workLon)

        // Roughly three kilometres north. The caller keeps its nearest-stop behaviour.
        val origin = locator.nearestLabelledStop(
            latitude = workLat + 0.027,
            longitude = workLon,
            excludeStopId = null,
        )

        assertNull(origin)
    }

    @Test
    fun `the nearest label wins when two are in range`() = runTest {
        putLabel("Work", "10101", "Wynyard Station", workLat, workLon)
        putLabel("Gym", "10102", "Town Hall Station", workLat + 0.005, workLon)

        val origin = locator.nearestLabelledStop(
            latitude = workLat + 0.0045,
            longitude = workLon,
            excludeStopId = null,
        )

        assertEquals("10102", origin?.stopId, "the closer of the two labels should win")
    }

    @Test
    fun `a label with no stop pinned to it is ignored`() = runTest {
        sandook.upsertStopLabel(label = "Work", emoji = "💼", stopId = null, stopName = null, sortOrder = 0L)

        val origin = locator.nearestLabelledStop(
            latitude = workLat,
            longitude = workLon,
            excludeStopId = null,
        )

        assertNull(origin)
    }

    @Test
    fun `a rider standing at the destination is already there`() = runTest {
        // "To home by 9pm" while at home. The caller uses this to leave the origin blank rather
        // than filling it with the stop next door, which would be a trip to a neighbour.
        putLabel("Home", "20202", "Seven Hills Station", homeLat, homeLon)

        val there = locator.isStandingAt("20202", latitude = homeLat, longitude = homeLon)

        assertEquals(true, there)
    }

    @Test
    fun `a rider well away from the destination is not already there`() = runTest {
        putLabel("Home", "20202", "Seven Hills Station", homeLat, homeLon)

        val there = locator.isStandingAt("20202", latitude = workLat, longitude = workLon)

        assertEquals(false, there)
    }

    @Test
    fun `a rider with no labels at all resolves to nothing`() = runTest {
        val origin = locator.nearestLabelledStop(
            latitude = workLat,
            longitude = workLon,
            excludeStopId = null,
        )

        assertNull(origin)
    }
}
