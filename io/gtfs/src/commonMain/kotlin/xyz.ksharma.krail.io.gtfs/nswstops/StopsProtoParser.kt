package xyz.ksharma.krail.io.gtfs.nswstops

import app.krail.kgtfs.proto.NswStopList
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.perf.performance
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.until
import krail.io.gtfs.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.sandook.Sandook

class StopsProtoParser(
    private val ioDispatcher: CoroutineDispatcher,
    private val sandook: Sandook,
) : ProtoParser {

    /**
     * Reads and decodes the NSW stops from a protobuf file, then inserts the stops into the database.
     */
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun parseAndInsertStops() = withContext(ioDispatcher) {
        val trace = Firebase.performance.newTrace("parseNswStops")
        trace.start()
        var start = Clock.System.now()

        sandook.clearNswStopsTable()
        sandook.clearNswProductClassTable()

        val byteArray = Res.readBytes("files/NSW_STOPS.pb")
        val decodedStops = NswStopList.ADAPTER.decode(byteArray)
        trace.stop()

        var duration = start.until(
            Clock.System.now(), DateTimeUnit.MILLISECOND,
            TimeZone.currentSystemDefault(),
        )
        log("Decoded #Stops: ${decodedStops.nswStops.size} - duration: $duration ms")

        log("Start inserting stops. Currently ${sandook.stopsCount()} stops in the database")
        start = Clock.System.now()
        insertStopsInTransaction(decodedStops)
        duration = start.until(
            Clock.System.now(), DateTimeUnit.MILLISECOND, TimeZone.currentSystemDefault()
        )
        log("Inserted #Stops: ${decodedStops.nswStops.size} in duration: $duration ms")
    }

    private suspend fun insertStopsInTransaction(decoded: NswStopList) = withContext(ioDispatcher) {
        val trace = Firebase.performance.newTrace("insertNSWStops")
        trace.start()
        val start = Clock.System.now()
        sandook.insertTransaction {
            decoded.nswStops.forEach { nswStop ->
                sandook.insertNswStop(
                    stopId = nswStop.stopId,
                    stopName = nswStop.stopName,
                    stopLat = nswStop.lat,
                    stopLon = nswStop.lon
                )
                nswStop.productClass.forEach { productClass ->
                    sandook.insertNswStopProductClass(
                        stopId = nswStop.stopId,
                        productClass = productClass
                    )
                }
            }
        }

        val duration = start.until(
            Clock.System.now(), DateTimeUnit.MILLISECOND, TimeZone.currentSystemDefault(),
        )
        log("Inserted ${decoded.nswStops.size} stops in a single transaction in $duration ms")
        trace.stop()
    }
}
