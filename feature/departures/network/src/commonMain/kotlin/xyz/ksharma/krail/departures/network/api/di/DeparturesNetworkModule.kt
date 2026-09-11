package xyz.ksharma.krail.departures.network.api.di

import io.ktor.client.HttpClient
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.ksharma.krail.core.network.ApiCredential
import xyz.ksharma.krail.core.network.forApi
import xyz.ksharma.krail.departures.network.api.service.DeparturesService
import xyz.ksharma.krail.departures.network.api.service.RealDeparturesService

val departuresNetworkModule = module {
    single {
        RealDeparturesService(
            httpClient = get<HttpClient>().forApi(ApiCredential.NswApiKey),
            networkCaller = get(),
            resolver = get(),
        )
    } bind DeparturesService::class
}
