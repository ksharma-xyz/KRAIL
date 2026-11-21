package xyz.ksharma.krail.io.gtfs.readiness

import app.krail.kgtfs.proto.NswStopList
import xyz.ksharma.krail.io.gtfs.readiness.RealAppDataReadiness.Companion.CANARY_STOP_ID
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Readiness is Ready only when a read of [CANARY_STOP_ID] returns a row. The insert guard checks the
 * version and the count and knows nothing about the canary, so if a data refresh ever drops that
 * stop, every rider would sit in Failed and no retry could clear it. This test reads the real
 * bundled file and fails the refresh in CI instead.
 */
class BundledStopsCanaryTest {

    @Test
    fun `the bundled stops data still contains the canary stop`() {
        // Gradle runs a module's host tests with the module directory as the working directory.
        val file = File("src/commonMain/composeResources/files/NSW_STOPS.pb")
        assertTrue(file.exists(), "Cannot find the bundled stops file at ${file.absolutePath}")

        val stopIds = NswStopList.ADAPTER.decode(file.readBytes()).nswStops.map { it.stopId }.toSet()

        assertTrue(
            CANARY_STOP_ID in stopIds,
            "Stop $CANARY_STOP_ID is not in NSW_STOPS.pb. Change CANARY_STOP_ID in " +
                "RealAppDataReadiness to a stop that exists in the new data, or readiness can never reach Ready.",
        )
    }
}
