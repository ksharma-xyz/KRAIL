package xyz.ksharma.krail.feature.track

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackingManagerTest {

    private fun makeManager() = TrackingManager()

    private fun makeDeepLink(
        fromStopId: String = "10101100",
        toStopId: String = "10102099",
        departureUtcDateTime: String = "2025-04-19T22:26:00Z",
    ) = TripDeepLink(
        fromStopId = fromStopId,
        toStopId = toStopId,
        fromStopName = "Seven Hills",
        toStopName = "Wynyard",
        departureUtcDateTime = departureUtcDateTime,
        legs = listOf(TripDeepLink.DeepLinkLeg(transportationId = "nsw:020T1:W:R:sj2", productClass = 1)),
    )

    private fun makeDisplay() = TrackedJourneyDisplay(
        fromStopId = "10101100",
        toStopId = "10102099",
        fromStopName = "Seven Hills",
        toStopName = "Wynyard",
        originTime = "8:26 AM",
        scheduledOriginTime = null,
        destinationTime = "9:15 AM",
        originUtcDateTime = "2025-04-19T22:26:00Z",
        destinationUtcDateTime = "2025-04-19T23:15:00Z",
        travelTime = "49 min",
        legs = persistentListOf(),
    )

    // region initial state

    @Test
    fun `tracked is null initially`() = runTest {
        val manager = makeManager()
        assertNull(manager.tracked.first())
    }

    @Test
    fun `isTracking returns false when nothing tracked`() {
        val manager = makeManager()
        assertFalse(manager.isTracking(makeDeepLink()))
    }

    // endregion

    // region start

    @Test
    fun `start sets tracked journey with no display`() = runTest {
        val manager = makeManager()
        val deepLink = makeDeepLink()

        manager.start(deepLink)

        val tracked = manager.tracked.first()
        assertNotNull(tracked)
        assertEquals(deepLink, tracked.deepLink)
        assertNull(tracked.display)
        assertFalse(tracked.isArrived)
    }

    @Test
    fun `start overwrites a previous tracked journey`() = runTest {
        val manager = makeManager()
        val first = makeDeepLink(fromStopId = "AAA", departureUtcDateTime = "2025-04-19T08:20:00Z")
        val second = makeDeepLink(fromStopId = "BBB", departureUtcDateTime = "2025-04-19T08:26:00Z")

        manager.start(first)
        manager.start(second)

        val tracked = manager.tracked.first()
        assertNotNull(tracked)
        assertEquals(second, tracked.deepLink)
    }

    // endregion

    // region isTracking

    @Test
    fun `isTracking returns true for the active deep link`() {
        val manager = makeManager()
        val deepLink = makeDeepLink()

        manager.start(deepLink)
        assertTrue(manager.isTracking(deepLink))
    }

    @Test
    fun `isTracking returns false for a different deep link`() {
        val manager = makeManager()
        val active = makeDeepLink(departureUtcDateTime = "2025-04-19T08:26:00Z")
        val other = makeDeepLink(departureUtcDateTime = "2025-04-19T08:35:00Z")

        manager.start(active)
        assertFalse(manager.isTracking(other))
    }

    // endregion

    // region update

    @Test
    fun `update sets display on tracked journey`() = runTest {
        val manager = makeManager()
        val deepLink = makeDeepLink()
        val display = makeDisplay()

        manager.start(deepLink)
        manager.update(display)

        val tracked = manager.tracked.first()
        assertNotNull(tracked)
        assertEquals(display, tracked.display)
        assertEquals(deepLink, tracked.deepLink)
    }

    @Test
    fun `update does nothing when nothing is tracked`() = runTest {
        val manager = makeManager()
        manager.update(makeDisplay())
        assertNull(manager.tracked.first())
    }

    @Test
    fun `update preserves isArrived flag`() = runTest {
        val manager = makeManager()
        manager.start(makeDeepLink())
        manager.markArrived()
        manager.update(makeDisplay())

        val tracked = manager.tracked.first()
        assertNotNull(tracked)
        assertTrue(tracked.isArrived)
    }

    // endregion

    // region markArrived

    @Test
    fun `markArrived sets isArrived to true`() = runTest {
        val manager = makeManager()
        manager.start(makeDeepLink())
        manager.markArrived()

        val tracked = manager.tracked.first()
        assertNotNull(tracked)
        assertTrue(tracked.isArrived)
    }

    @Test
    fun `markArrived preserves deepLink and display`() = runTest {
        val manager = makeManager()
        val deepLink = makeDeepLink()
        val display = makeDisplay()

        manager.start(deepLink)
        manager.update(display)
        manager.markArrived()

        val tracked = manager.tracked.first()
        assertNotNull(tracked)
        assertEquals(deepLink, tracked.deepLink)
        assertEquals(display, tracked.display)
        assertTrue(tracked.isArrived)
    }

    @Test
    fun `markArrived does nothing when nothing is tracked`() = runTest {
        val manager = makeManager()
        manager.markArrived()
        assertNull(manager.tracked.first())
    }

    // endregion

    // region stop

    @Test
    fun `stop clears tracked journey`() = runTest {
        val manager = makeManager()
        manager.start(makeDeepLink())
        manager.stop()

        assertNull(manager.tracked.first())
    }

    @Test
    fun `isTracking returns false after stop`() {
        val manager = makeManager()
        val deepLink = makeDeepLink()

        manager.start(deepLink)
        manager.stop()

        assertFalse(manager.isTracking(deepLink))
    }

    @Test
    fun `stop is idempotent when nothing is tracked`() = runTest {
        val manager = makeManager()
        manager.stop()
        manager.stop()
        assertNull(manager.tracked.first())
    }

    // endregion

    // region full lifecycle

    @Test
    fun `full lifecycle start → update → markArrived → stop`() = runTest {
        val manager = makeManager()
        val deepLink = makeDeepLink()
        val display = makeDisplay()

        manager.start(deepLink)
        assertTrue(manager.isTracking(deepLink))

        manager.update(display)
        assertEquals(display, manager.tracked.first()?.display)

        manager.markArrived()
        assertTrue(manager.tracked.first()?.isArrived == true)

        manager.stop()
        assertNull(manager.tracked.first())
        assertFalse(manager.isTracking(deepLink))
    }

    // endregion
}
