package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.markerLetterOf
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.markerModeOf
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.stationMarkerStops
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Which stops a stop map marks, and which letter each marker carries. */
class StopMarkerRulesTest {

    private val everyMode = TransportMode.all.map { NswTransportConfig.productClassFor(it) }.toSet()

    private fun stop(stopId: String, vararg modes: TransportMode) = NearbyStopFeature(
        stopId = stopId,
        stopName = stopId,
        position = LatLng(-33.69, 150.9),
        transportModes = modes.toList().toImmutableList(),
    )

    @Test
    fun `a stop with several modes is marked as the one first in priority, whatever order they come in`() {
        assertEquals(TransportMode.Train, markerModeOf(listOf(TransportMode.Bus, TransportMode.Train)))
        assertEquals(TransportMode.Metro, markerModeOf(listOf(TransportMode.Bus, TransportMode.Metro)))
        assertNull(markerModeOf(emptyList()))
    }

    @Test
    fun `each mode has the letter its transport icon has`() {
        assertEquals(
            listOf("T", "M", "B", "L", "F", "C", "B", "O"),
            TransportMode.all.map { markerLetterOf(it) },
        )
    }

    @Test
    fun `a parking station outside the nearby set still has its station marker`() {
        val marked = stationMarkerStops(
            nearby = listOf(stop("near", TransportMode.Bus)),
            parkRide = listOf(stop("far", TransportMode.Metro)),
            selectedProductClasses = everyMode,
        )

        assertEquals(listOf("near", "far"), marked.map { it.stopId })
    }

    @Test
    fun `a parking station already nearby is marked once`() {
        val marked = stationMarkerStops(
            nearby = listOf(stop("both", TransportMode.Metro)),
            parkRide = listOf(stop("both", TransportMode.Metro)),
            selectedProductClasses = everyMode,
        )

        assertEquals(listOf("both"), marked.map { it.stopId })
    }

    @Test
    fun `a parking station whose modes are all turned off has no station marker`() {
        val marked = stationMarkerStops(
            nearby = emptyList(),
            parkRide = listOf(stop("far", TransportMode.Metro)),
            selectedProductClasses = setOf(NswTransportConfig.productClassFor(TransportMode.Bus)),
        )

        assertEquals(emptyList(), marked.map { it.stopId })
    }
}
