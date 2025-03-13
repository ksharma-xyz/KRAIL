package xyz.ksharma.krail.core.appstart

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remote_config.RemoteConfig
import xyz.ksharma.krail.io.gtfs.nswstops.ProtoParser
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_STOPS_VERSION

class RealAppStart(
    private val coroutineScope: CoroutineScope,
    private val remoteConfig: RemoteConfig,
    private val protoParser: ProtoParser,
    private val preferences: SandookPreferences,
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
        if (shouldInsertNswStops()) {
            protoParser.parseAndInsertStops()
            preferences.setLong(
                key = SandookPreferences.KEY_NSW_STOPS_VERSION,
                value = NSW_STOPS_VERSION,
            )
            log("NswStops inserted in the database, new version: $NSW_STOPS_VERSION.")
        } else {
            log("Stops already inserted in the database.")
        }
    }.getOrElse {
        log("Error reading proto file: $it")
        // TODO - Firebase performance track.
    }

    private fun shouldInsertNswStops(): Boolean {
        val storedVersion = preferences.getLong(SandookPreferences.KEY_NSW_STOPS_VERSION) ?: 0
        log("Current NSW Stops data version: $NSW_STOPS_VERSION, Stored version: $storedVersion")

        return storedVersion < NSW_STOPS_VERSION
    }
}
