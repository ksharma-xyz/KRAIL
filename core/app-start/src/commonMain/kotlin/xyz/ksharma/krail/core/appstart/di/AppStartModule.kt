package xyz.ksharma.krail.core.appstart.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import xyz.ksharma.krail.core.appstart.AppStart
import xyz.ksharma.krail.core.appstart.RealAppStart

val appStartModule = module {
    single<AppStart> {
        RealAppStart(
            coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            remoteConfig = get(),
            protoParser = get(),
            preferences = get(),
        )
    }
}
