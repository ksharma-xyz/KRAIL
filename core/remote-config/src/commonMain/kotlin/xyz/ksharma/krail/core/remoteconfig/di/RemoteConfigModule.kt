package xyz.ksharma.krail.core.remoteconfig.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import xyz.ksharma.krail.core.remoteconfig.RealRemoteConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfig
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.RemoteConfigFlag

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
