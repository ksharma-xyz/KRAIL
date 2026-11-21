package xyz.ksharma.krail.io.gtfs.nswstops

import app.krail.kgtfs.proto.NswStopList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import krail.io.gtfs.generated.resources.Res
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager.Companion.MINIMUM_REQUIRED_NSW_STOPS
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_STOPS_VERSION
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

internal class NswStopsManager(
    private val ioDispatcher: CoroutineDispatcher,
    private val sandook: Sandook,
    private val preferences: SandookPreferences,
    // The bundled file is the only thing a test cannot reach, so it is the one seam.
    private val readStopsBytes: suspend () -> ByteArray = { Res.readBytes("files/NSW_STOPS.pb") },
) : StopsManager {

    // insertStops() is invoked from both RealAppStart and IntroViewModel and they can run
    // concurrently on first launch. Without this lock both pass shouldInsertNswStops() (the stored
    // version is only written after the transaction commits) and each runs the full ~37k-row
    // insert, doubling the work and the time the DB write lock is held. Serialising here means the
    // second caller re-checks after the first commits and skips.
    private val insertMutex = Mutex()

    init {
        log("NswStopsManager Initialized with NSW Stops version: $NSW_STOPS_VERSION: $this")
    }

    override suspend fun insertStops(): Result<Unit> = runCatching {
        insertMutex.withLock {
            log("NswStopsManager Inserting NSW Stops data if not already inserted: $this")
            if (shouldInsertNswStops()) {
                insertNswStops()
            } else {
                log("NswStopsManager Stops already inserted in the database.")
            }
        }
    }.onFailure { error ->
        if (error is CancellationException) throw error
        logError("NswStopsManager Error inserting stops: ${error.message}")
    }

    private suspend fun insertNswStops() {
        parseAndInsertStops()
        log("[NswStopsManager] NswStops parsed and inserted successfully.")
        preferences.setLong(
            key = SandookPreferences.KEY_NSW_STOPS_VERSION,
            value = NSW_STOPS_VERSION,
        )
        log("NswStopsManager NswStops inserted in the database, new version: $NSW_STOPS_VERSION.")
    }

    private fun shouldInsertNswStops(): Boolean {
        val storedVersion = preferences.getLong(SandookPreferences.KEY_NSW_STOPS_VERSION) ?: 0
        log("RealAppStart Current NSW Stops data version: $NSW_STOPS_VERSION, Stored version: $storedVersion")
        val insertedStopsCount = sandook.stopsCount()
        return storedVersion < NSW_STOPS_VERSION || insertedStopsCount < MINIMUM_REQUIRED_NSW_STOPS
    }

    /**
     * Reads and decodes the NSW stops from a protobuf file, then replaces the stored rows in one
     * transaction. The file is read and decoded first so the write lock is held only for the insert.
     */
    private suspend fun parseAndInsertStops() = withContext(ioDispatcher) {
        val totalMark = TimeSource.Monotonic.markNow()

        val readMark = TimeSource.Monotonic.markNow()
        val byteArray = readStopsBytes()
        log("NswStopsManager ⏱️ File read time: ${readMark.elapsedNow().inWholeMilliseconds}ms")

        val decodeMark = TimeSource.Monotonic.markNow()
        val decodedStops = NswStopList.ADAPTER.decode(byteArray)
        log("NswStopsManager ⏱️ Proto decode time: ${decodeMark.elapsedNow().inWholeMilliseconds}ms")

        log("Start inserting stops. Currently ${sandook.stopsCount()} stops in the database")

        val insertMark = TimeSource.Monotonic.markNow()
        replaceStopsInTransaction(decodedStops)
        log("NswStopsManager ⏱️ Insert time: ${insertMark.elapsedNow().inWholeMilliseconds}ms")
        log(
            "NswStopsManager ⏱️ TOTAL time: ${totalMark.elapsedNow().inWholeMilliseconds}ms " +
                "(${decodedStops.nswStops.size} stops)",
        )
    }

    /**
     * The clear is inside the transaction on purpose: committed on its own it empties the table
     * for the whole insert, and a failed insert would leave it empty for good.
     */
    private fun replaceStopsInTransaction(decoded: NswStopList) {
        sandook.insertTransaction {
            sandook.clearNswStopsTable()
            sandook.clearNswProductClassTable()
            decoded.nswStops.forEach { nswStop ->
                sandook.insertNswStop(
                    stopId = nswStop.stopId,
                    stopName = nswStop.stopName,
                    stopLat = nswStop.lat,
                    stopLon = nswStop.lon,
                    isParent = nswStop.isParent,
                )
                nswStop.productClass.forEach { productClass ->
                    sandook.insertNswStopProductClass(
                        stopId = nswStop.stopId,
                        productClass = productClass,
                    )
                }
            }
            log("Inserted ${decoded.nswStops.size} stops into the database.")
        }
    }
}
