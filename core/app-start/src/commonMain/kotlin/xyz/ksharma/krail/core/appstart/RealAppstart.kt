package xyz.ksharma.krail.core.appstart

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.remoteconfig.RemoteConfig
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.sandook.UserLifecycleStore

internal class RealAppStart(
    private val coroutineScope: CoroutineScope,
    private val remoteConfig: RemoteConfig,
    private val appDataReadiness: AppDataReadiness,
    private val userLifecycleStore: UserLifecycleStore,
) : AppStart {

    override fun start() {
        coroutineScope.launch {
            // First, so that anything later in the launch can already read the install age.
            userLifecycleStore.recordFirstInstallIfAbsent()
            userLifecycleStore.recordActiveDay()
            // Starts the stops and bus routes inserts and tracks them. Not awaited, so remote
            // config set up below no longer waits behind the insert.
            appDataReadiness.retry()
            setupRemoteConfig()
        }
    }

    private fun setupRemoteConfig() {
        remoteConfig.setup()
    }
}
