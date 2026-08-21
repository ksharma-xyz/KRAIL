package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager

/**
 * Counts insert calls instead of decoding a protobuf into SQLDelight. Two independent instances
 * are wired into `IntroViewModel` (stops and bus routes), so the call record is per-instance.
 */
internal class FakeStopsManager : StopsManager {

    var insertStopsCallCount = 0
        private set

    override suspend fun insertStops() {
        insertStopsCallCount++
    }
}
