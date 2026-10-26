package xyz.ksharma.krail.feature.track.network.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.network.ApiCredential
import xyz.ksharma.krail.core.network.forApi
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.crowding.CarriageForecastRepository
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.network.BffGtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeService
import xyz.ksharma.krail.feature.track.network.RealGtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.network.RealGtfsRealtimeService
import xyz.ksharma.krail.feature.track.network.crowding.RealCarriageForecastRepository

val trackNetworkModule = module {
    single<CarriageForecastRepository> {
        val isDebug = get<AppInfoProvider>().getAppInfo().isDebug
        val debugStore = get<DebugNetworkConfigStore>()
        val flag = get<Flag>()
        RealCarriageForecastRepository(
            service = get(),
            networkCaller = get(),
            // Read on every load, so a console change or a debug toggle applies to the next
            // results screen without a restart.
            isEnabled = {
                if (isDebug) {
                    debugStore.state.value.crowdingCarriagesEnabled
                } else {
                    flag.getFlagValue(FlagKeys.CROWDING_CARRIAGES_IN_RESULTS_ENABLED.key)
                        .asBoolean(fallback = true)
                }
            },
        )
    }
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
