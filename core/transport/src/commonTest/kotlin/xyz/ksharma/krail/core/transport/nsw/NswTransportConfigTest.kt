package xyz.ksharma.krail.core.transport.nsw

import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.core.transport.TransportMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NswTransportConfigTest {

    // Train productClass = 1, Metro = 2, Bus = 5, Ferry = 9, LightRail = 4, Coach = 7

    // region: Train (cls=1)

    @Test
    fun `Given Train productClass with destination When resolving display text Then returns towards destination`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 1,
            destinationName = "Bondi Junction via Central",
            description = "Waterfall or Cronulla to Bondi Junction",
        )
        assertEquals("towards Bondi Junction via Central", result)
    }

    @Test
    fun `Given Train productClass with null destination When resolving display text Then falls back to description`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 1,
            destinationName = null,
            description = "Waterfall or Cronulla to Bondi Junction",
        )
        assertEquals("Waterfall or Cronulla to Bondi Junction", result)
    }

    @Test
    fun `Given Train productClass with both null When resolving display text Then returns null`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 1,
            destinationName = null,
            description = null,
        )
        assertNull(result)
    }

    // region: Metro (cls=2)

    @Test
    fun `Given Metro productClass with destination When resolving display text Then returns towards destination`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 2,
            destinationName = "Tallawong",
            description = "Sydenham to Tallawong",
        )
        assertEquals("towards Tallawong", result)
    }

    @Test
    fun `Given Metro productClass with null destination When resolving display text Then falls back to description`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 2,
            destinationName = null,
            description = "Sydenham to Tallawong",
        )
        assertEquals("Sydenham to Tallawong", result)
    }

    // region: Bus (cls=5)

    @Test
    fun `Given Bus productClass with description When resolving display text Then returns description as-is`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 5,
            destinationName = "Rouse Hill Station",
            description = "Seven Hills to Rouse Hill Station via Norwest & Kellyville",
        )
        assertEquals("Seven Hills to Rouse Hill Station via Norwest & Kellyville", result)
    }

    @Test
    fun `Given Bus productClass with null description When resolving display text Then falls back to destination name`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 5,
            destinationName = "Rouse Hill Station",
            description = null,
        )
        assertEquals("Rouse Hill Station", result)
    }

    @Test
    fun `Given Bus productClass with both null When resolving display text Then returns null`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 5,
            destinationName = null,
            description = null,
        )
        assertNull(result)
    }

    // region: Ferry (cls=9)

    @Test
    fun `Given Ferry productClass with description When resolving display text Then returns description`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = 9,
            destinationName = "Manly Wharf",
            description = "Circular Quay to Manly",
        )
        assertEquals("Circular Quay to Manly", result)
    }

    // region: null productClass

    @Test
    fun `Given null productClass with description When resolving display text Then returns description`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = null,
            destinationName = "Liverpool",
            description = "Parramatta to Liverpool",
        )
        assertEquals("Parramatta to Liverpool", result)
    }

    @Test
    fun `Given null productClass with null description When resolving display text Then falls back to destination name`() {
        val result = NswTransportConfig.resolveServiceDisplayText(
            productClass = null,
            destinationName = "Liverpool",
            description = null,
        )
        assertEquals("Liverpool", result)
    }

    // region: per-surface mode availability

    @Test
    fun `Given NEARBY_STOPS surface When resolving product classes Then School Bus is excluded`() {
        val result = NswTransportConfig.productClassesFor(ModeSelectionSurface.NEARBY_STOPS)

        assertFalse(result.contains(TransportMode.SchoolBus.productClass))
        assertTrue(
            result.containsAll(
                listOf(
                    TransportMode.Train.productClass,
                    TransportMode.Metro.productClass,
                    TransportMode.Bus.productClass,
                    TransportMode.LightRail.productClass,
                    TransportMode.Ferry.productClass,
                    TransportMode.Coach.productClass,
                ),
            ),
        )
    }

    @Test
    fun `Given TRIP_PLANNER surface When resolving product classes Then all modes including School Bus are present`() {
        val result = NswTransportConfig.productClassesFor(ModeSelectionSurface.TRIP_PLANNER)

        assertEquals(NswTransportConfig.allProductClasses(), result)
        assertTrue(result.contains(TransportMode.SchoolBus.productClass))
    }

    @Test
    fun `Given NEARBY_STOPS surface When resolving modes Then School Bus is not in the list`() {
        val result = NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS)

        assertFalse(result.contains(TransportMode.SchoolBus))
    }

    @Test
    fun `Given TRIP_PLANNER surface When resolving modes Then School Bus is in the list`() {
        val result = NswTransportConfig.modesFor(ModeSelectionSurface.TRIP_PLANNER)

        assertTrue(result.contains(TransportMode.SchoolBus))
    }
}

