package xyz.ksharma.krail.core.appstart

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import xyz.ksharma.krail.core.remoteconfig.RemoteConfig
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager

class RealAppStart(
    private val coroutineScope: CoroutineScope,
    private val remoteConfig: RemoteConfig,
    private val stopsManager: StopsManager,
    private val defaultDispatcher: CoroutineDispatcher,
) : AppStart {

    override fun start() {
        coroutineScope.launchWithExceptionHandler<RealAppStart>(defaultDispatcher) {
            stopsManager.insertStops()
            setupRemoteConfig()
        }
    }

    private fun setupRemoteConfig() {
        remoteConfig.setup()
    }
}
