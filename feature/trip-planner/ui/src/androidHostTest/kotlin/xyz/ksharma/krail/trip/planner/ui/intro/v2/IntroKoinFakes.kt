package xyz.ksharma.krail.trip.planner.ui.intro.v2

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeAppDataReadiness
import xyz.ksharma.krail.core.testing.fakes.FakeFestivalManager
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideFacilityManager
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager

/**
 * Everything the real registration of [IntroFlowViewModel] asks Koin for, as fakes at the edges. Loaded
 * next to `viewModelsModule`, so the view model is made by the real registration.
 */
internal fun introKoinFakes(sandook: Sandook = FakeSandook()): Module = module {
    single<AppDataReadiness> { FakeAppDataReadiness(initial = StopsReadiness.Ready(stopCount = 100)) }
    single<Sandook> { sandook }
    single<SandookPreferences> { FakeSandookPreferences() }
    single<Analytics> { FakeAnalytics() }
    single<Flag> { FakeFlag() }
    single<CoroutineDispatcher>(named(IODispatcher)) { Dispatchers.Unconfined }
    single<NswParkRideFacilityManager> { FakeParkRideFacilityManager() }
    single<StopResultsManager> { FakeStopResultsManager() }
    single<FestivalManager> { FakeFestivalManager() }
    single<NswParkRideSandook> { FakeNswParkRideSandook() }
    single<ParkRideService> { FakeParkRideService() }
}
