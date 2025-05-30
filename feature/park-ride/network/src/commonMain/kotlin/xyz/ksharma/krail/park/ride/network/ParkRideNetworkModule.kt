package xyz.ksharma.krail.park.ride.network

import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.park.ride.network.service.RealParkRideService

val parkRideNetworkModule = module {
    single {
        RealParkRideService(
            httpClient = parkRideHttpClient(get()),
            ioDispatcher = get(named(IODispatcher)),
        )
    } bind ParkRideService::class
}
