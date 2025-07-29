package xyz.ksharma.krail.discover.network.real.di

import org.koin.dsl.module
import xyz.ksharma.krail.discover.network.api.DiscoverSydneyManager
import xyz.ksharma.krail.discover.network.real.RealDiscoverSydneyManager

val discoverModule = module {
    single<DiscoverSydneyManager> {
        RealDiscoverSydneyManager(
            flag = get(),
        )
    }
}
