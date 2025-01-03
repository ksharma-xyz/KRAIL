package xyz.ksharma.krail.core.remote_config

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.di.DispatchersComponent
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import kotlin.time.Duration.Companion.minutes

internal class RealRemoteConfig(
    private val appInfoProvider: AppInfoProvider,
    private val coroutineScope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher = DispatchersComponent().defaultDispatcher,
) : RemoteConfig {

    private val remoteConfig = Firebase.remoteConfig

    override fun getConfigValue(key: String): RemoteConfigValue {
        return RemoteConfigValue(remoteConfig.getValue(key))
    }

    override fun setup() {
        coroutineScope.launch(defaultDispatcher) {
            remoteConfig.fetchAndActivate()
            setDebugFetchInterval()
            setDefaultConfig()
        }
    }

    private suspend fun setDefaultConfig() {
        remoteConfig.setDefaults(*RemoteConfigDefaults.getDefaults())
    }

    /**
     * How often the client can fetch the remote config from server.
     * https://firebase.google.com/docs/remote-config/get-started?platform=android&authuser=0#throttling
     */
    private suspend fun setDebugFetchInterval() {
        if (appInfoProvider.getAppInfo().isDebug) {
            remoteConfig.settings { minimumFetchInterval = 10.minutes }
        }
    }
}
