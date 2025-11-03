package xyz.ksharma.krail.core.appstart

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.remoteconfig.RemoteConfig
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager

class RealAppStart(
    private val coroutineScope: CoroutineScope,
    private val remoteConfig: RemoteConfig,
    private val stopsManager: StopsManager,
) : AppStart {

    override fun start() {
        coroutineScope.launch {
            stopsManager.insertStops()
            setupRemoteConfig()
        }
    }

    private fun setupRemoteConfig() {
        remoteConfig.setup()
    }
}
