package xyz.ksharma.krail.core.maps.data.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.core.maps.data.repository.RealNearbyStopsRepository

val mapsDataModule = module {
    single<NearbyStopsRepository> {
        RealNearbyStopsRepository(
            nswStopsSandook = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }
}
