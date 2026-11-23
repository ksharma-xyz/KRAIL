package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.AnalyticsPane
import xyz.ksharma.krail.core.analytics.AnalyticsPaneTracker
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapDismissedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapDismissedEvent.DurationBucket
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapViewedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.NearbyStopClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopSelectedFromMapEvent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapEventsTest {

    private val sessionId = "0123456789abcdef"

    @AfterTest
    fun resetPane() {
        AnalyticsPaneTracker.set(AnalyticsPane.SINGLE)
    }

    private fun viewed(
        entry: MapViewedEvent.Entry = MapViewedEvent.Entry.SEARCH_STOP,
        hadUserLocation: Boolean = true,
        permission: IntroLetsKrailClickEvent.LocationResult = IntroLetsKrailClickEvent.LocationResult.GRANTED,
    ) = MapViewedEvent(
        entry = entry,
        hadUserLocation = hadUserLocation,
        locationPermission = permission,
        mapSessionId = sessionId,
    )

    private fun dismissed(
        selected: Boolean = false,
        bucket: DurationBucket = DurationBucket.UNDER_5S,
        hadStops: Boolean = true,
    ) = MapDismissedEvent(
        mapSessionId = sessionId,
        selected = selected,
        durationBucket = bucket,
        hadStops = hadStops,
    )

    // region map_viewed

    @Test
    fun `map_viewed is a new name with exactly its five params`() {
        val event = viewed()

        assertEquals("map_viewed", event.name)
        assertEquals(
            mapOf(
                "entry" to "search_stop",
                "hadUserLocation" to 1,
                "locationPermission" to "granted",
                "mapSessionId" to sessionId,
            ),
            event.properties,
        )
    }

    @Test
    fun `the pane is added to map_viewed as it is sent and is not a param of the class`() {
        val event = viewed()
        AnalyticsPaneTracker.set(AnalyticsPane.DETAIL)

        assertFalse(event.properties.orEmpty().containsKey("pane"))
        assertEquals("DETAIL", AnalyticsPaneTracker.decorate(event.properties)["pane"])
        assertEquals("map_viewed", event.name)
    }

    @Test
    fun `entry sends home and search_stop and nothing else`() {
        assertEquals(
            setOf("home", "search_stop"),
            MapViewedEvent.Entry.entries.map { viewed(entry = it).properties?.get("entry") }.toSet(),
        )
    }

    @Test
    fun `location permission reuses the Intro value set`() {
        assertEquals(
            setOf("granted", "denied", "not_asked", "unavailable"),
            IntroLetsKrailClickEvent.LocationResult.entries
                .map { viewed(permission = it).properties?.get("locationPermission") }
                .toSet(),
        )
    }

    @Test
    fun `had user location is the number one or zero and never a boolean`() {
        assertEquals(1, viewed(hadUserLocation = true).properties?.get("hadUserLocation"))
        assertEquals(0, viewed(hadUserLocation = false).properties?.get("hadUserLocation"))
    }

    // endregion

    // region map_dismissed

    @Test
    fun `map_dismissed is a new name with exactly its four params`() {
        val event = dismissed(selected = true, bucket = DurationBucket.FIVE_TO_15S, hadStops = false)

        assertEquals("map_dismissed", event.name)
        assertEquals(
            mapOf(
                "mapSessionId" to sessionId,
                "selected" to 1,
                "durationBucket" to "5_to_15s",
                "hadStops" to 0,
            ),
            event.properties,
        )
    }

    @Test
    fun `selected and had stops are numbers one or zero`() {
        assertEquals(1, dismissed(selected = true).properties?.get("selected"))
        assertEquals(0, dismissed(selected = false).properties?.get("selected"))
        assertEquals(1, dismissed(hadStops = true).properties?.get("hadStops"))
        assertEquals(0, dismissed(hadStops = false).properties?.get("hadStops"))
    }

    @Test
    fun `every duration bucket reaches the event as its own word`() {
        assertEquals(
            setOf("under_5s", "5_to_15s", "15_to_60s", "over_60s"),
            DurationBucket.entries.map { dismissed(bucket = it).properties?.get("durationBucket") }.toSet(),
        )
    }

    @Test
    fun `the duration bucket changes exactly at 5000, 15000 and 60000 milliseconds`() {
        assertEquals(DurationBucket.UNDER_5S, DurationBucket.from(0))
        assertEquals(DurationBucket.UNDER_5S, DurationBucket.from(4_999))
        assertEquals(DurationBucket.FIVE_TO_15S, DurationBucket.from(5_000))
        assertEquals(DurationBucket.FIVE_TO_15S, DurationBucket.from(14_999))
        assertEquals(DurationBucket.FIFTEEN_TO_60S, DurationBucket.from(15_000))
        assertEquals(DurationBucket.FIFTEEN_TO_60S, DurationBucket.from(59_999))
        assertEquals(DurationBucket.OVER_60S, DurationBucket.from(60_000))
        assertEquals(DurationBucket.OVER_60S, DurationBucket.from(Long.MAX_VALUE))
    }

    @Test
    fun `a clock that read backwards is the shortest bucket`() {
        assertEquals(DurationBucket.UNDER_5S, DurationBucket.from(-1))
    }

    // endregion

    // region No stop name, no free text, nothing the sanitizer would change

    @Test
    fun `the two new events send only numbers, enum words and the id, each under the length limit`() {
        val allowedWords = setOf(
            "home", "search_stop",
            "granted", "denied", "not_asked", "unavailable",
            "under_5s", "5_to_15s", "15_to_60s", "over_60s",
        )
        val events = buildList {
            MapViewedEvent.Entry.entries.forEach { entry ->
                IntroLetsKrailClickEvent.LocationResult.entries.forEach { permission ->
                    add(viewed(entry = entry, permission = permission))
                }
            }
            DurationBucket.entries.forEach { add(dismissed(bucket = it)) }
        }

        events.forEach { event ->
            event.properties.orEmpty().forEach { (key, value) ->
                when (value) {
                    is Int -> assertTrue(value == 0 || value == 1, "${event.name}.$key is $value")
                    is String -> {
                        assertTrue(value.length < MAX_PARAM_LENGTH, "${event.name}.$key is ${value.length} long")
                        assertFalse(value.endsWith("~trunc"), "${event.name}.$key was truncated")
                        if (key == "mapSessionId") {
                            assertEquals(sessionId, value)
                        } else {
                            assertTrue(value in allowedWords, "${event.name}.$key sent '$value'")
                        }
                    }
                    else -> error("${event.name}.$key is a ${value::class.simpleName}")
                }
            }
        }
    }

    @Test
    fun `no param name on the two new events can hold a stop id, a stop name or typed text`() {
        val names = (viewed().properties.orEmpty().keys + dismissed().properties.orEmpty().keys)

        assertEquals(
            setOf(
                "entry",
                "hadUserLocation",
                "locationPermission",
                "mapSessionId",
                "selected",
                "durationBucket",
                "hadStops",
            ),
            names,
        )
    }

    // endregion

    // region The id on the two existing events

    @Test
    fun `a map pick carries the map session id and keeps every param it had`() {
        val event = StopSelectedFromMapEvent(
            stopId = "200060",
            searchRadiusKm = 3.0,
            enabledModesCount = 4,
            nearbyStopsCount = 12,
            hadUserLocation = true,
            mapSessionId = sessionId,
        )

        assertEquals("stop_selected_from_map", event.name)
        assertEquals(
            mapOf(
                "stopId" to "200060",
                "searchRadiusKm" to 3.0,
                "radiusSource" to "remote_config",
                "enabledModesCount" to 4,
                "nearbyStopsCount" to 12,
                "hadUserLocation" to true,
                "mapSessionId" to sessionId,
            ),
            event.properties,
        )
    }

    @Test
    fun `a map pick with no open in progress sends no map session id`() {
        val event = StopSelectedFromMapEvent(
            stopId = "200060",
            searchRadiusKm = 3.0,
            enabledModesCount = 4,
            nearbyStopsCount = 12,
            hadUserLocation = false,
        )

        assertFalse(event.properties.orEmpty().containsKey("mapSessionId"))
        assertEquals(
            setOf(
                "stopId",
                "searchRadiusKm",
                "radiusSource",
                "enabledModesCount",
                "nearbyStopsCount",
                "hadUserLocation",
            ),
            event.properties.orEmpty().keys,
        )
    }

    @Test
    fun `a marker tap carries the map session id and keeps every param it had`() {
        val event = NearbyStopClickEvent(stopId = "200060", transportModesCount = 2, mapSessionId = sessionId)

        assertEquals("nearby_stop_click", event.name)
        assertEquals(
            mapOf("stopId" to "200060", "transportModesCount" to 2, "mapSessionId" to sessionId),
            event.properties,
        )
    }

    @Test
    fun `a marker tap with no open in progress sends no map session id`() {
        val event = NearbyStopClickEvent(stopId = "200060", transportModesCount = 2)

        assertEquals(mapOf("stopId" to "200060", "transportModesCount" to 2), event.properties)
    }

    // endregion

    private companion object {
        const val MAX_PARAM_LENGTH = 100
    }
}
