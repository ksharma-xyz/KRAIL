package xyz.ksharma.krail.io.gtfs.nswstops

import app.krail.kgtfs.proto.NswStopList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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
    override suspend fun parseAndInsertStops() = withContext(ioDispatcher) {
        sandook.clearNswStopsTable()
        sandook.clearNswProductClassTable()

        val byteArray = Res.readBytes("files/NSW_STOPS.pb")
        val decodedStops = NswStopList.ADAPTER.decode(byteArray)

        log("Start inserting stops. Currently ${sandook.stopsCount()} stops in the database")
        insertStopsInTransaction(decodedStops)
    }

    private suspend fun insertStopsInTransaction(decoded: NswStopList) = withContext(ioDispatcher) {
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
    }
}
