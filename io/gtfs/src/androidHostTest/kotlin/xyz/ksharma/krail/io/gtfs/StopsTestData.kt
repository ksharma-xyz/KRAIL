package xyz.ksharma.krail.io.gtfs

import app.krail.kgtfs.proto.NswStop
import app.krail.kgtfs.proto.NswStopList
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager.Companion.MINIMUM_REQUIRED_NSW_STOPS
import xyz.ksharma.krail.io.gtfs.readiness.RealAppDataReadiness.Companion.CANARY_STOP_ID

internal fun stopListBytes(vararg stopIds: String): ByteArray = NswStopList.ADAPTER.encode(
    NswStopList(
        nswStops = stopIds.map { id ->
            NswStop(stopId = id, stopName = "Stop $id", lat = -33.0, lon = 151.0, productClass = listOf(1))
        },
    ),
)

/** A database that already holds current data: the minimum row count, with the canary stop in it. */
internal fun FakeSandook.seedCurrentStops() {
    insertNswStop(CANARY_STOP_ID, "Canary", -33.0, 151.0, isParent = true)
    repeat(MINIMUM_REQUIRED_NSW_STOPS - 1) { index ->
        insertNswStop("seed-$index", "Seed $index", -33.0, 151.0, isParent = true)
    }
}

internal fun FakeSandook.seedStops(vararg stopIds: String) {
    stopIds.forEach { id -> insertNswStop(id, "Stop $id", -33.0, 151.0, isParent = true) }
}

/** Enough rows to clear the minimum, none of them the canary stop. */
internal fun FakeSandook.seedStopsWithoutCanary() {
    repeat(MINIMUM_REQUIRED_NSW_STOPS) { index ->
        insertNswStop("seed-$index", "Seed $index", -33.0, 151.0, isParent = true)
    }
}
