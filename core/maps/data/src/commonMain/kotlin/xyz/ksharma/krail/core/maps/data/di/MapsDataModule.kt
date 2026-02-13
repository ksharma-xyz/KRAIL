package xyz.ksharma.krail.core.maps.data.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.core.maps.data.repository.RealNearbyStopsRepository

val mapsDataModule = module {
    singleOf(::RealNearbyStopsRepository) {
        bind<NearbyStopsRepository>()
    }
}
