package xyz.ksharma.krail.core.appstart.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.appstart.AppStart
import xyz.ksharma.krail.core.appstart.RealAppStart
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.DefaultDispatcher

val appStartModule = module {
    single<AppStart> {
        val dispatcher = get<CoroutineDispatcher>(named(DefaultDispatcher))
        RealAppStart(
            coroutineScope = CoroutineScope(dispatcher + SupervisorJob()),
            remoteConfig = get(),
            stopsManager = get(),
            defaultDispatcher = dispatcher,
        )
    }
}
