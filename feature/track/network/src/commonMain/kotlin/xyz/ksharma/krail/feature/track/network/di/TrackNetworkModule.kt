package xyz.ksharma.krail.feature.track.network.di

import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeService
import xyz.ksharma.krail.feature.track.network.RealGtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.network.RealGtfsRealtimeService
import xyz.ksharma.krail.feature.track.network.gtfsRealtimeHttpClient

val trackNetworkModule = module {
    single<GtfsRealtimeService> { RealGtfsRealtimeService(httpClient = gtfsRealtimeHttpClient(get())) }
    single<GtfsRealtimeRepository> {
        RealGtfsRealtimeRepository(service = get(), ioDispatcher = Dispatchers.Default)
    }
}
