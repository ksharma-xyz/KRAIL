package xyz.ksharma.krail.core.appstart.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.appstart.AppStart
import xyz.ksharma.krail.core.appstart.RealAppStart
import xyz.ksharma.krail.io.gtfs.GtfsQualifiers

val appStartModule = module {
    single<AppStart> {
        RealAppStart(
            coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            remoteConfig = get(),
            nswStopsManager = get(named(GtfsQualifiers.NSW_STOPS_MANAGER)),
            nswBusRoutesManager = get(named(GtfsQualifiers.NSW_BUS_ROUTES_MANAGER)),
        )
    }
}
