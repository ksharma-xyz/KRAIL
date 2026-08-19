package xyz.ksharma.krail.trip.planner.ui.flow

import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeInfoTileManager
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideFacilityManager
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputViewModel
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.ChainedStopTextResolver
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.LabelWordGuard
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.LabelledStopLocator
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.RiderOriginLocator
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopLabelTextResolver
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopSearchTextResolver
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealSearchSessionStore
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAiTextService
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsRepository
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeSpeechToTextService
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager

/**
 * Every collaborator the home entry reaches for, faked at the module edge and nowhere else:
 * the database, the network services, the on-device AI, the recogniser. Everything between
 * those edges and the semantics tree is the real thing — the real ViewModels, the real
 * resolver chain (in the same order [xyz.ksharma.krail.trip.planner.ui.di.viewModelsModule]
 * builds it), the real screen.
 *
 * The three ViewModels are built here rather than by Koin so a flow test can drive them with
 * the same events the surface sends. Koin hands out these instances, so the ones under test
 * and the ones on screen are the same objects. Reading state off them is not how a flow test
 * asserts — that is what the semantics tree is for — but sending them an event is how a rider
 * gets in.
 */
internal class HomeFlowFakes {

    val sandook = FakeSandook()
    val stopResultsManager = FakeStopResultsManager()
    val aiTextService = FakeAiTextService()
    val speechToTextService = FakeSpeechToTextService()
    val nearbyStopsRepository = FakeNearbyStopsRepository()
    val flag = FakeFlag()

    val aiSearchInputViewModel = AiSearchInputViewModel(
        aiTextService = aiTextService,
        speechToTextService = speechToTextService,
        stopTextResolver = ChainedStopTextResolver(
            listOf(
                StopLabelTextResolver(sandook = sandook),
                LabelWordGuard(StopSearchTextResolver(stopResultsManager = stopResultsManager)),
            ),
        ),
        riderOriginLocator = RiderOriginLocator(
            // No GPS in the harness. UserLocationManager only exists as a @Composable
            // factory, and the real entry supplies this lambda through
            // koinViewModel(parametersOf(...)); the harness's Koin definition ignores those
            // params, so a flow that needs a GPS-derived origin has to fake the nearby
            // repository instead.
            resolveCurrentLocation = { null },
            labelledStopLocator = LabelledStopLocator(sandook = sandook),
            nearbyStopsRepository = nearbyStopsRepository,
        ),
        isAiSearchInputEnabled = { true },
    )

    val savedTripsViewModel = SavedTripsViewModel(
        sandook = sandook,
        analytics = FakeAnalytics(),
        // Unconfined rather than a StandardTestDispatcher: a flow test has no TestScope to
        // advance, so anything the ViewModel moves off the main dispatcher has to run where
        // it is launched or it never runs at all.
        ioDispatcher = Dispatchers.Unconfined,
        nswParkRideFacilityManager = FakeParkRideFacilityManager(),
        parkRideService = FakeParkRideService(),
        parkRideSandook = FakeNswParkRideSandook(),
        stopResultsManager = stopResultsManager,
        searchSessionStore = RealSearchSessionStore(),
        flag = flag,
        preferences = FakeSandookPreferences(),
        infoTileManager = FakeInfoTileManager(),
        platformOps = FakePlatformOps(),
        inviteFriendsTileManager = FakeInviteFriendsTileManager(),
        trackingManager = TrackingManager(),
        appReviewManager = FakeAppReviewManager(),
    )

    val mapStopSelectionViewModel = MapStopSelectionViewModel(
        nearbyStopsManager = FakeNearbyStopsManagerForMap(),
    )

    /**
     * What the entry resolves through `koinViewModel()`. Only the three ViewModels the home
     * entry asks for — everything else it needs is already inside them.
     */
    val koinModule: Module = module {
        viewModel<SavedTripsViewModel> { savedTripsViewModel }
        viewModel<AiSearchInputViewModel> { aiSearchInputViewModel }
        viewModel<MapStopSelectionViewModel> { mapStopSelectionViewModel }
    }
}
