package xyz.ksharma.krail.core.appstart

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remote_config.RemoteConfig
import xyz.ksharma.krail.io.gtfs.nswstops.ProtoParser
import xyz.ksharma.krail.sandook.Sandook

class RealAppStart(
    private val coroutineScope: CoroutineScope,
    private val remoteConfig: RemoteConfig,
    private val protoParser: ProtoParser,
    private val sandook: Sandook,
) : AppStart {

    init {
        log("RealAppStart created.")
    }

    override fun start() {
        coroutineScope.launch {
            parseAndInsertNswStopsIfNeeded()
            setupRemoteConfig()
        }
    }

    private fun setupRemoteConfig() = runCatching {
        remoteConfig.setup()
    }.getOrElse {
        log("Error setting up remote config: $it")
    }

    /**
     * Parses and inserts NSW_STOPS data in the database if they are not already inserted.
     */
    private suspend fun parseAndInsertNswStopsIfNeeded() = runCatching {
        if (shouldInsertStops()) {
            protoParser.parseAndInsertStops()
        } else {
            log("Stops already inserted in the database.")
        }
    }.getOrElse {
        log("Error reading proto file: $it")
        // TODO - Firebase performance track.
    }

    private fun shouldInsertStops(): Boolean {
        log("Stop count: ${sandook.stopsCount()}, Product class count: ${sandook.productClassCount()}")

        return sandook.stopsCount() == 0 || sandook.productClassCount() == 0
    }
}
