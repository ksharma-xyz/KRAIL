package xyz.ksharma.krail.discover.network.real.di

import org.koin.dsl.module
import xyz.ksharma.krail.discover.network.api.DiscoverSydneyManager
import xyz.ksharma.krail.discover.network.api.db.DiscoverCardOrderingEngine
import xyz.ksharma.krail.discover.network.real.RealDiscoverSydneyManager
import xyz.ksharma.krail.discover.network.real.db.RealDiscoverCardOrderingEngine

val discoverModule = module {
    single<DiscoverSydneyManager> {
        RealDiscoverSydneyManager(
            flag = get(),
            discoverCardOrderingEngine = get(),
        )
    }

    single<DiscoverCardOrderingEngine> {
        RealDiscoverCardOrderingEngine(
            discoverCardQueries = get(),
        )
    }
}
