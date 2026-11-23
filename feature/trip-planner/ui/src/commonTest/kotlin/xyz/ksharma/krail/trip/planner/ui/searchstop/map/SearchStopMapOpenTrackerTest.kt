package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.LocationResult
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapDismissedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.MapViewedEvent
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

/**
 * One `map_viewed` and exactly one `map_dismissed` for each map open, joined by an id that is minted once per open.
 * The clock is a [TestTimeSource], so a duration is moved by hand and no test waits.
 */
class SearchStopMapOpenTrackerTest {

    private val analytics = FakeAnalytics()
    private val clock = TestTimeSource()
    private val ids = ArrayDeque(listOf("aaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb", "cccccccccccccccc", "dddddddddddddddd"))
    private val tracker = MapOpenTracker(analytics, timeSource = clock, newSessionId = { ids.removeFirst() })

    private fun open(
        entry: MapEntry = MapEntry.SEARCH_STOP,
        hadUserLocation: Boolean = false,
        permission: LocationResult = LocationResult.NOT_ASKED,
        stopCount: Int = 0,
    ) = tracker.viewed(entry, hadUserLocation, permission, stopCount)

    private fun viewedEvents() = analytics.getTrackedEvents("map_viewed").filterIsInstance<MapViewedEvent>()

    private fun dismissedEvents() = analytics.getTrackedEvents("map_dismissed").filterIsInstance<MapDismissedEvent>()

    // region The id

    @Test
    fun `an id is 16 lowercase hex characters from a random 64 bit value`() {
        repeat(ID_SAMPLES) {
            val id = newMapSessionId()

            assertEquals(ID_LENGTH, id.length, id)
            assertTrue(id.all { it in '0'..'9' || it in 'a'..'f' }, id)
        }
    }

    @Test
    fun `an id keeps its leading zeros, so a small value is still 16 characters`() {
        val zero = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        val allOnes = object : Random() {
            override fun nextBits(bitCount: Int): Int = -1
        }

        assertEquals("0000000000000000", newMapSessionId(zero))
        assertEquals(ID_LENGTH, newMapSessionId(allOnes).length)
        assertEquals(
            ID_LENGTH,
            newMapSessionId(Random(1)).length,
        )
    }

    @Test
    fun `ids differ from one draw to the next`() {
        val ids = List(ID_SAMPLES) { newMapSessionId() }.toSet()

        assertEquals(ID_SAMPLES, ids.size)
    }

    @Test
    fun `the id is the same for the open's whole life and is null with no open`() {
        assertNull(tracker.sessionId)

        open()
        val during = tracker.sessionId
        tracker.stopsOnMap(3)
        open() // the screen built again

        assertEquals("aaaaaaaaaaaaaaaa", during)
        assertEquals(during, tracker.sessionId)

        tracker.dismissed()

        assertNull(tracker.sessionId)
    }

    @Test
    fun `a new open has a new id`() {
        open()
        tracker.dismissed()
        open()

        assertEquals("bbbbbbbbbbbbbbbb", tracker.sessionId)
        assertEquals(listOf("aaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb"), viewedEvents().map { it.mapSessionId })
    }

    @Test
    fun `the viewed and the dismissed of one open carry the same id`() {
        open()
        tracker.dismissed()

        assertEquals(viewedEvents().single().mapSessionId, dismissedEvents().single().mapSessionId)
    }

    // endregion

    // region map_viewed once per open

    @Test
    fun `viewed is sent once however many times the screen reports it`() {
        open()
        open()
        open()

        assertEquals(1, viewedEvents().size)
    }

    @Test
    fun `viewed says the entry, the location and the permission it was given`() {
        open(entry = MapEntry.HOME, hadUserLocation = true, permission = LocationResult.GRANTED)

        val event = viewedEvents().single()
        assertEquals(MapViewedEvent.Entry.HOME, event.entry)
        assertEquals(true, event.hadUserLocation)
        assertEquals(LocationResult.GRANTED, event.locationPermission)
    }

    @Test
    fun `both entries reach the event`() {
        open(entry = MapEntry.HOME)
        tracker.dismissed()
        open(entry = MapEntry.SEARCH_STOP)

        assertEquals(
            listOf(MapViewedEvent.Entry.HOME, MapViewedEvent.Entry.SEARCH_STOP),
            viewedEvents().map { it.entry },
        )
    }

    @Test
    fun `nothing is sent while no map has been viewed`() {
        tracker.stopsOnMap(5)
        tracker.stopPicked()
        tracker.dismissed()

        assertEquals(emptyList(), analytics.events)
    }

    // endregion

    // region map_dismissed exactly once per open

    @Test
    fun `dismissed is sent once however many ways the open ends`() {
        open()

        tracker.dismissed()
        tracker.dismissed()
        tracker.stopPicked()
        tracker.dismissed()

        assertEquals(1, dismissedEvents().size)
    }

    @Test
    fun `the first way an open ends is the one that counts`() {
        open()
        tracker.dismissed()
        tracker.stopPicked()
        assertEquals(false, dismissedEvents().single().selected)

        open()
        tracker.stopPicked()
        tracker.dismissed()
        assertEquals(listOf(false, true), dismissedEvents().map { it.selected })
        assertEquals(2, dismissedEvents().size)
    }

    @Test
    fun `an open that ends a second time is a different open with its own pair of events`() {
        open()
        tracker.dismissed()
        open()
        tracker.dismissed()

        assertEquals(2, viewedEvents().size)
        assertEquals(2, dismissedEvents().size)
        assertNotEquals(dismissedEvents()[0].mapSessionId, dismissedEvents()[1].mapSessionId)
    }

    @Test
    fun `viewed always comes before its dismissed`() {
        open()
        tracker.dismissed()

        assertEquals(listOf("map_viewed", "map_dismissed"), analytics.events.map { it.name })
    }

    // endregion

    // region selected and hadStops

    @Test
    fun `selected is false until a stop is picked, then true`() {
        open()
        tracker.dismissed()
        open()
        tracker.stopPicked()

        assertEquals(listOf(false, true), dismissedEvents().map { it.selected })
    }

    @Test
    fun `a pick ends the open`() {
        open()
        tracker.stopPicked()

        assertNull(tracker.sessionId)
        assertEquals(1, dismissedEvents().size)
    }

    @Test
    fun `had stops is true when there were markers as the map opened`() {
        open(stopCount = 7)
        tracker.dismissed()

        assertEquals(true, dismissedEvents().single().hadStops)
    }

    @Test
    fun `had stops turns true when markers arrive later in the open`() {
        open(stopCount = 0)
        tracker.stopsOnMap(0)
        tracker.stopsOnMap(4)
        tracker.stopsOnMap(0)
        tracker.dismissed()

        assertEquals(true, dismissedEvents().single().hadStops)
    }

    @Test
    fun `had stops is false when the map was empty the whole open`() {
        open(stopCount = 0)
        tracker.stopsOnMap(0)
        tracker.dismissed()

        assertEquals(false, dismissedEvents().single().hadStops)
    }

    @Test
    fun `markers seen in one open do not make the next open had stops`() {
        open(stopCount = 3)
        tracker.dismissed()
        open(stopCount = 0)
        tracker.dismissed()

        assertEquals(listOf(true, false), dismissedEvents().map { it.hadStops })
    }

    // endregion

    // region durationBucket, from the clock difference at dismiss

    private fun bucketAfter(millis: Long): MapDismissedEvent.DurationBucket {
        val before = dismissedEvents().size
        open()
        clock += millis.milliseconds
        tracker.dismissed()
        return dismissedEvents()[before].durationBucket
    }

    @Test
    fun `the bucket changes at 5000, 15000 and 60000 milliseconds exactly`() {
        ids.addAll(List(BUCKET_CASES) { "ffffffffffff000$it" })

        assertEquals(MapDismissedEvent.DurationBucket.UNDER_5S, bucketAfter(4_999))
        assertEquals(MapDismissedEvent.DurationBucket.FIVE_TO_15S, bucketAfter(5_000))
        assertEquals(MapDismissedEvent.DurationBucket.FIVE_TO_15S, bucketAfter(14_999))
        assertEquals(MapDismissedEvent.DurationBucket.FIFTEEN_TO_60S, bucketAfter(15_000))
        assertEquals(MapDismissedEvent.DurationBucket.FIFTEEN_TO_60S, bucketAfter(59_999))
        assertEquals(MapDismissedEvent.DurationBucket.OVER_60S, bucketAfter(60_000))
    }

    @Test
    fun `the duration is measured from the open, and not from an earlier one`() {
        ids.addAll(List(2) { "eeeeeeeeeeeeeee$it" })
        open()
        clock += 70_000.milliseconds
        tracker.dismissed()

        open()
        clock += 1_000.milliseconds
        tracker.dismissed()

        assertEquals(
            listOf(MapDismissedEvent.DurationBucket.OVER_60S, MapDismissedEvent.DurationBucket.UNDER_5S),
            dismissedEvents().map { it.durationBucket },
        )
    }

    @Test
    fun `time before the map was viewed does not count`() {
        clock += 120_000.milliseconds
        open()
        clock += 100.milliseconds
        tracker.dismissed()

        assertEquals(MapDismissedEvent.DurationBucket.UNDER_5S, dismissedEvents().single().durationBucket)
    }

    // endregion

    // region No stop name, no text

    @Test
    fun `no param of either event holds a stop name or text`() {
        open(entry = MapEntry.SEARCH_STOP, hadUserLocation = true, permission = LocationResult.DENIED, stopCount = 2)
        tracker.stopPicked()

        val words = setOf(
            "home", "search_stop", "granted", "denied", "not_asked", "unavailable",
            "under_5s", "5_to_15s", "15_to_60s", "over_60s",
        )
        analytics.events.forEach { event: AnalyticsEvent ->
            event.properties.orEmpty().forEach { (key, value) ->
                when (value) {
                    is Int -> assertTrue(value == 0 || value == 1, "${event.name}.$key")
                    is String -> assertTrue(
                        value in words || key == "mapSessionId",
                        "${event.name}.$key holds '$value'",
                    )
                    else -> error("${event.name}.$key is ${value::class.simpleName}")
                }
            }
        }
    }

    // endregion

    private companion object {
        const val ID_LENGTH = 16
        const val ID_SAMPLES = 2_000
        const val BUCKET_CASES = 6
    }
}
