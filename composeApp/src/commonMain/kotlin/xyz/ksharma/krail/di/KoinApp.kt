
package xyz.ksharma.krail.di

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.dsl.module
import xyz.ksharma.krail.core.analytics.di.analyticsModule
import xyz.ksharma.krail.core.appinfo.di.appInfoModule
import xyz.ksharma.krail.core.appstart.di.appStartModule
import xyz.ksharma.krail.core.appversion.di.appVersionModule
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.di.coroutineDispatchersModule
import xyz.ksharma.krail.core.festival.di.festivalModule
import xyz.ksharma.krail.core.network.coreNetworkModule
import xyz.ksharma.krail.core.remote_config.di.remoteConfigModule
import xyz.ksharma.krail.discover.network.real.di.discoverModule
import xyz.ksharma.krail.info.tile.network.real.di.infoTileModule
import xyz.ksharma.krail.io.gtfs.di.gtfsModule
import xyz.ksharma.krail.park.ride.network.di.parkRideNetworkModule
import xyz.ksharma.krail.platform.ops.di.opsModule
import xyz.ksharma.krail.sandook.di.sandookModule
import xyz.ksharma.krail.theme.di.themeManagerModule
import xyz.ksharma.krail.splash.SplashViewModel
import xyz.ksharma.krail.trip.planner.network.api.di.tripPlannerNetworkModule
import xyz.ksharma.krail.trip.planner.ui.di.viewModelsModule

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        includes(config)
        modules(
            coroutineDispatchersModule,
            coreNetworkModule,
            viewModelsModule,
            sandookModule,
            themeManagerModule,
            splashModule,
            appInfoModule,
            appVersionModule,
            analyticsModule,
            remoteConfigModule,
            gtfsModule,
            appStartModule,
            opsModule,
            tripPlannerNetworkModule,
            parkRideNetworkModule,
            festivalModule,
            discoverModule,
            infoTileModule,
        )
    }
}

val splashModule = module {
    viewModel {
        SplashViewModel(
            sandook = get(),
            analytics = get(),
            appInfoProvider = get(),
            ioDispatcher = get(named(IODispatcher)),
            appStart = get(),
            preferences = get(),
            appVersionManager = get(),
        )
    }
}
