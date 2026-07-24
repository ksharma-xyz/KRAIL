package xyz.ksharma.krail.core.appstart

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.remoteconfig.RemoteConfig
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager
import xyz.ksharma.krail.sandook.UserLifecycleStore

internal class RealAppStart(
    private val coroutineScope: CoroutineScope,
    private val remoteConfig: RemoteConfig,
    private val nswStopsManager: StopsManager,
    private val nswBusRoutesManager: StopsManager,
    private val userLifecycleStore: UserLifecycleStore,
) : AppStart {

    override fun start() {
        coroutineScope.launch {
            // First, so that anything later in the launch can already read the install age.
            userLifecycleStore.recordFirstInstallIfAbsent()
            nswStopsManager.insertStops()
            nswBusRoutesManager.insertStops()
            setupRemoteConfig()
        }
    }

    private fun setupRemoteConfig() {
        remoteConfig.setup()
    }
}
