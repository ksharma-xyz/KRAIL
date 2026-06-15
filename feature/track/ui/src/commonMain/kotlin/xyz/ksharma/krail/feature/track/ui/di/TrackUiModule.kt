package xyz.ksharma.krail.feature.track.ui.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import xyz.ksharma.krail.feature.track.ui.TrackTripViewModel

val trackUiModule = module {
    viewModel { params ->
        val appInfo = get<AppInfoProvider>().getAppInfo()
        val isTripTrackingEnabled = when {
            appInfo.isDebug -> get<DebugNetworkConfigStore>().state.value.tripTrackingEnabled
            else -> get<Flag>().getFlagValue(FlagKeys.TRIP_TRACKING_ENABLED.key).asBoolean(true)
        }
        TrackTripViewModel(
            encodedData = params.getOrNull<String>(),
            tripPlanningService = get(),
            trackingManager = get(),
            ioDispatcher = get(named(IODispatcher)),
            festivalManager = get(),
            gtfsRealtimeRepository = get(),
            sandook = get(),
            shareManager = get(),
            isTripTrackingEnabled = isTripTrackingEnabled,
        )
    }
}
