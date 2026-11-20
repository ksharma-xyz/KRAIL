package xyz.ksharma.krail.trip.planner.network.api.di

import io.ktor.client.HttpClient
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.ksharma.krail.core.network.ApiCredential
import xyz.ksharma.krail.core.network.forApi
import xyz.ksharma.krail.trip.planner.network.api.ratelimit.NetworkRateLimiter
import xyz.ksharma.krail.trip.planner.network.api.ratelimit.RateLimiter
import xyz.ksharma.krail.trip.planner.network.api.service.RealTripPlanningService
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import xyz.ksharma.krail.trip.planner.network.disruption.RealDisruptionRepository
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionRepository

val tripPlannerNetworkModule = module {
    // factory, NOT single: a limiter's trigger channel is a broadcast, so every consumer
    // sharing one instance fetches whenever any of them refreshes. See NetworkRateLimiter's
    // KDoc and RateLimiterScopeTest.
    factoryOf(::NetworkRateLimiter) { bind<RateLimiter>() }

    single {
        RealTripPlanningService(
            httpClient = get<HttpClient>().forApi(ApiCredential.NswApiKey),
            networkCaller = get(),
            resolver = get(),
        )
    } bind TripPlanningService::class

    // single: the cache and the one-request-at-a-time lock live on the instance, so every screen
    // that asks for disruptions shares them.
    single<DisruptionRepository> { RealDisruptionRepository(service = get()) }
}
