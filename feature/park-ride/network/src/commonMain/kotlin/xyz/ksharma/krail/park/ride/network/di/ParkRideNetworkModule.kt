package xyz.ksharma.krail.park.ride.network.di

import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.network.ApiCredential
import xyz.ksharma.krail.core.network.forApi
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.RealNswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.park.ride.network.service.RealParkRideService

val parkRideNetworkModule = module {
    single {
        RealParkRideService(
            httpClient = get<HttpClient>().forApi(ApiCredential.NswApiKey),
            ioDispatcher = get(named(IODispatcher)),
            resolver = get(),
        )
    } bind ParkRideService::class

    single<NswParkRideFacilityManager> {
        RealNswParkRideFacilityManager(
            flag = get(),
        )
    }
}
