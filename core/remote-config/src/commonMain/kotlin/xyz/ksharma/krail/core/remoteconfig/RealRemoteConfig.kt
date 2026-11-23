package xyz.ksharma.krail.core.remoteconfig

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.di.DispatchersComponent
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
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
        coroutineScope.launchWithExceptionHandler<RealRemoteConfig>(defaultDispatcher) {
            // The bundled defaults go in before the fetch. A flag read while the fetch is still running, or after it
            // has failed (a first launch with no network), then gets its bundled default and not an empty string. With
            // the defaults after the fetch, a fresh install read every flag as empty until the fetch finished, and a
            // failed fetch skipped the defaults altogether.
            setDefaultConfig()
            remoteConfig.fetchAndActivate()
            setDebugFetchInterval()
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
            remoteConfig.settings { minimumFetchInterval = 1.minutes }
        }
    }
}
