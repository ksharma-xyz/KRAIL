package xyz.ksharma.krail.core.remote_config.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import xyz.ksharma.krail.core.remote_config.RealRemoteConfig
import xyz.ksharma.krail.core.remote_config.RemoteConfig
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.RemoteConfigFlag

val remoteConfigModule = module {
    single<RemoteConfig> {
        RealRemoteConfig(
            appInfoProvider = get(),
            coroutineScope = CoroutineScope(context = SupervisorJob() + Dispatchers.Default),
        )
    }

    single<Flag> {
        RemoteConfigFlag(remoteConfig = get())
    }
}
