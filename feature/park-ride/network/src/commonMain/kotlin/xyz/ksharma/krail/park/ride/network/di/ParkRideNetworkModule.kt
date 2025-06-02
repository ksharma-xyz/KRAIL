package xyz.ksharma.krail.park.ride.network.di

import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.RealNswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.parkRideHttpClient
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.park.ride.network.service.RealParkRideService

val parkRideNetworkModule = module {
    single {
        RealParkRideService(
            httpClient = parkRideHttpClient(get()),
            ioDispatcher = get(named(IODispatcher)),
        )
    } bind ParkRideService::class

    single<NswParkRideFacilityManager> {
        RealNswParkRideFacilityManager(
            flag = get(),
        )
    }
}
