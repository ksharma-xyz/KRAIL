package xyz.ksharma.krail.io.gtfs.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.io.gtfs.GtfsQualifiers
import xyz.ksharma.krail.io.gtfs.nswbusroutes.NswBusRoutesManager
import xyz.ksharma.krail.io.gtfs.nswstops.NswStopsManager
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager

val gtfsModule = module {
    // NSW Stops Manager
    single<StopsManager>(named(GtfsQualifiers.NSW_STOPS_MANAGER)) {
        NswStopsManager(
            ioDispatcher = get(named(IODispatcher)),
            sandook = get(),
            preferences = get(),
        )
    }

    // NSW Bus Routes Manager
    single<StopsManager>(named(GtfsQualifiers.NSW_BUS_ROUTES_MANAGER)) {
        NswBusRoutesManager(
            ioDispatcher = get(named(IODispatcher)),
            nswBusRoutesSandook = get(),
            preferences = get(),
        )
    }
}
