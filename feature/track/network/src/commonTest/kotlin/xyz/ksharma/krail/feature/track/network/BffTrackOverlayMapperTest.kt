package xyz.ksharma.krail.feature.track.network

import app.krail.bff.proto.LegTracking
import app.krail.bff.proto.StopProgress
import app.krail.bff.proto.TrackResponse
import app.krail.bff.proto.VehicleLive
import xyz.ksharma.krail.feature.track.VehicleStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BffTrackOverlayMapperTest {

    private fun vehicle(
        lat: Double = -33.87,
        lon: Double = 151.21,
        bearing: Float? = 42f,
        relation: VehicleLive.StopRelation = VehicleLive.StopRelation.IN_TRANSIT_TO,
    ) = VehicleLive(
        latitude = lat,
        longitude = lon,
        bearing_degrees = bearing ?: 0f,
        has_bearing = bearing != null,
        measured_at_epoch_sec = 1_700_000_000L,
        stop_relation = relation,
    )

    @Test
    fun `maps vehicles back onto their legIndex via leg_ref`() {
        val response = TrackResponse(
            fetched_at_epoch_sec = 1_700_000_123L,
            legs = listOf(
                LegTracking(leg_ref = "0", status = LegTracking.Status.TRACKING, vehicle = vehicle()),
                LegTracking(leg_ref = "2", status = LegTracking.Status.NOT_STARTED),
            ),
        )
        val overlay = response.toOverlay()

        assertEquals(setOf(0), overlay.vehiclePositions.keys, "leg without vehicle contributes nothing")
        val pos = overlay.vehiclePositions.getValue(0)
        assertEquals(-33.87, pos.latitude)
        assertEquals(42f, pos.bearing)
        assertEquals(VehicleStatus.IN_TRANSIT_TO, pos.status)
        assertEquals(1_700_000_000L, pos.lastUpdatedEpochSec)
        assertEquals("1700000123", overlay.lastModified)
    }

    @Test
    fun `missing bearing maps to zero, stop relations map across`() {
        val response = TrackResponse(
            legs = listOf(
                LegTracking(
                    leg_ref = "0",
                    status = LegTracking.Status.TRACKING,
                    vehicle = vehicle(bearing = null, relation = VehicleLive.StopRelation.STOPPED_AT),
                ),
            ),
        )
        val pos = response.toOverlay().vehiclePositions.getValue(0)
        assertEquals(0f, pos.bearing)
        assertEquals(VehicleStatus.STOPPED_AT, pos.status)
    }

    @Test
    fun `leg delay fans out to every stop id only when has_delay is set`() {
        val response = TrackResponse(
            legs = listOf(
                LegTracking(
                    leg_ref = "0",
                    status = LegTracking.Status.TRACKING,
                    delay_seconds = 120,
                    has_delay = true,
                    stops = listOf(
                        StopProgress(stop_id = "S1", state = StopProgress.State.DEPARTED),
                        StopProgress(stop_id = "S2", state = StopProgress.State.UPCOMING),
                    ),
                ),
                LegTracking(
                    leg_ref = "1",
                    status = LegTracking.Status.TRACKING,
                    delay_seconds = 0,
                    has_delay = false,
                    stops = listOf(StopProgress(stop_id = "S3", state = StopProgress.State.UPCOMING)),
                ),
            ),
        )
        val delays = response.toOverlay().stopDelays
        assertEquals(mapOf("S1" to 120, "S2" to 120), delays, "no-delay leg contributes nothing")
    }

    @Test
    fun `non-numeric leg_ref is skipped, empty response maps to empty overlay`() {
        val overlay = TrackResponse(
            legs = listOf(
                LegTracking(leg_ref = "weird", status = LegTracking.Status.TRACKING, vehicle = vehicle()),
            ),
        ).toOverlay()
        assertTrue(overlay.vehiclePositions.isEmpty())
        assertNull(overlay.lastModified)
    }

    @Test
    fun `service date converts UTC to the Sydney calendar day`() {
        // 18:05Z on the 11th is 04:05 AEST on the 12th.
        assertEquals("20260612", "2026-06-11T18:05:30Z".toServiceDate())
        // Garbage input falls back to the raw date portion.
        assertEquals("20260611", "2026-06-11Tgarbage".toServiceDate())
    }
}
