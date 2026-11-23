package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopSelectedFromMapEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopSelectedFromMapEvent.RadiusSource
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `searchRadiusKm` changed meaning under one name: it was the rider's choice of 1, 3 or 5, and it is now the Remote
 * Config number. A version cannot identify which, so the event says it with `radiusSource`.
 */
class RadiusSourceEventTest {

    private fun pick(radiusKm: Double = 3.0, source: RadiusSource? = null) = if (source == null) {
        StopSelectedFromMapEvent(
            stopId = "200060",
            searchRadiusKm = radiusKm,
            enabledModesCount = 4,
            nearbyStopsCount = 12,
            hadUserLocation = true,
        )
    } else {
        StopSelectedFromMapEvent(
            stopId = "200060",
            searchRadiusKm = radiusKm,
            enabledModesCount = 4,
            nearbyStopsCount = 12,
            hadUserLocation = true,
            radiusSource = source,
        )
    }

    @Test
    fun `this build always sends remote_config`() {
        assertEquals("remote_config", pick().properties?.get("radiusSource"))
    }

    @Test
    fun `the value set is fixed and remote_config`() {
        assertEquals(
            setOf("fixed", "remote_config"),
            RadiusSource.entries.map { pick(source = it).properties?.get("radiusSource") }.toSet(),
        )
    }

    @Test
    fun `the radius stays a number that carries the config value`() {
        assertEquals(2.5, pick(radiusKm = 2.5).properties?.get("searchRadiusKm"))
        assertEquals(10.0, pick(radiusKm = 10.0).properties?.get("searchRadiusKm"))
    }

    @Test
    fun `the event keeps its name and every param it had, with the marker added`() {
        val event = pick(radiusKm = 3.0)

        assertEquals("stop_selected_from_map", event.name)
        assertEquals(
            mapOf(
                "stopId" to "200060",
                "searchRadiusKm" to 3.0,
                "radiusSource" to "remote_config",
                "enabledModesCount" to 4,
                "nearbyStopsCount" to 12,
                "hadUserLocation" to true,
            ),
            event.properties,
        )
    }

    @Test
    fun `the marker is a short word, never a number or free text`() {
        RadiusSource.entries.forEach { source ->
            val value = pick(source = source).properties?.get("radiusSource")

            assertEquals(true, value is String && value.length < 100 && !value.endsWith("~trunc"))
        }
    }
}
