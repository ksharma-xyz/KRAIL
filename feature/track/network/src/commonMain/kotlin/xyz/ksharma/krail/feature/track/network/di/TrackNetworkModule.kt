package xyz.ksharma.krail.feature.track.network.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import xyz.ksharma.krail.core.network.ApiCredential
import xyz.ksharma.krail.core.network.forApi
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.network.BffGtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeService
import xyz.ksharma.krail.feature.track.network.RealGtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.network.RealGtfsRealtimeService

val trackNetworkModule = module {
    single<GtfsRealtimeService> {
        RealGtfsRealtimeService(
            httpClient = get<HttpClient>().forApi(ApiCredential.NswApiKey),
            resolver = get(),
        )
    }
    // The BFF snapshot path wraps the direct GTFS-RT path: each poll routes
    // via BffEndpointResolver + the bff_use_for_track kill switch, and any
    // BFF failure falls back to direct polling for that poll.
    single<GtfsRealtimeRepository> {
        BffGtfsRealtimeRepository(
            httpClient = get<HttpClient>().forApi(ApiCredential.NswApiKey),
            resolver = get(),
            flag = get(),
            direct = RealGtfsRealtimeRepository(service = get(), ioDispatcher = Dispatchers.Default),
            ioDispatcher = Dispatchers.Default,
        )
    }
}
