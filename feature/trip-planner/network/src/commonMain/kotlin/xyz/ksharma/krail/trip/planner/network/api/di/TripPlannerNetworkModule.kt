package xyz.ksharma.krail.trip.planner.network.api.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.trip.planner.network.api.ratelimit.NetworkRateLimiter
import xyz.ksharma.krail.trip.planner.network.api.ratelimit.RateLimiter
import xyz.ksharma.krail.trip.planner.network.api.service.RealTripPlanningService
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import xyz.ksharma.krail.trip.planner.network.api.service.tripPlannerHttpClient

val tripPlannerNetworkModule = module {
    singleOf(::NetworkRateLimiter) { bind<RateLimiter>() }

    single {
        RealTripPlanningService(
            httpClient = tripPlannerHttpClient(get()),
            ioDispatcher = get(named(IODispatcher)),
        )
    } bind TripPlanningService::class
}
