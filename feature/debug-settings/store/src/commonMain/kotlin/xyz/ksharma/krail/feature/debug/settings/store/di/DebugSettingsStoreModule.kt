package xyz.ksharma.krail.feature.debug.settings.store.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import xyz.ksharma.krail.feature.debug.settings.store.RealDebugNetworkConfigStore

val debugSettingsStoreModule = module {
    single<DebugNetworkConfigStore> {
        RealDebugNetworkConfigStore(
            preferences = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }
}
