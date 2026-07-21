package xyz.ksharma.krail.trip.planner.ui.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.DefaultDispatcher
import xyz.ksharma.krail.core.di.DispatchersComponent.Companion.IODispatcher
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.io.gtfs.GtfsQualifiers
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertsViewModel
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorViewModel
import xyz.ksharma.krail.trip.planner.ui.discover.DiscoverViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroViewModel
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.parkride.AddParkRideViewModel
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.savedtrips.InviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.savedtrips.RealInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealRemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealStopResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.RemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.resolveAddressSearchMinQueryLength
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.DefaultFuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.FuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.NearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.createNearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.settings.SettingsViewModel
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryViewModel
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

val viewModelsModule = module {
    viewModelOf(::ServiceAlertsViewModel)
    viewModelOf(::DateTimeSelectorViewModel)
    viewModelOf(::OurStoryViewModel)

    viewModel {
        IntroViewModel(
            analytics = get(),
            platformOps = get(),
            preferences = get(),
            nswStopsManager = get(named(GtfsQualifiers.NSW_STOPS_MANAGER)),
            nswBusRoutesManager = get(named(GtfsQualifiers.NSW_BUS_ROUTES_MANAGER)),
        )
    }

    viewModel {
        SettingsViewModel(
            appInfoProvider = get(),
            analytics = get(),
            platformOps = get(),
        )
    }

    viewModel {
        SavedTripsViewModel(
            sandook = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
            nswParkRideFacilityManager = get(),
            parkRideService = get(),
            parkRideSandook = get(),
            stopResultsManager = get(),
            flag = get(),
            preferences = get(),
            platformOps = get(),
            infoTileManager = get(),
            inviteFriendsTileManager = get(),
            trackingManager = get<TrackingManager>(),
        )
    }

    viewModel {
        AddParkRideViewModel(
            catalogue = ParkRideCatalogue(
                nswParkRideFacilityManager = get(),
                stopResultsManager = get(),
                sandook = get(),
                festivalManager = get(),
            ),
            parkRideSandook = get(),
            availabilityLoader = ParkRideAvailabilityLoader(
                parkRideSandook = get(),
                parkRideService = get(),
                flag = get(),
            ),
            platformOps = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }

    viewModel {
        val isDebug = get<AppInfoProvider>().getAppInfo().isDebug
        val tripTrackingDebugOverride = when {
            isDebug -> get<DebugNetworkConfigStore>().state.value.tripTrackingEnabled
            else -> get<Flag>().getFlagValue(FlagKeys.TRIP_TRACKING_ENABLED.key).asBoolean(true)
        }
        TimeTableViewModel(
            tripPlanningService = get(),
            rateLimiter = get(),
            sandook = get(),
            preferences = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
            festivalManager = get(),
            flag = get(),
            shareManager = get(),
            tripTrackingDebugOverride = tripTrackingDebugOverride,
        )
    }

    viewModel {
        ThemeSelectionViewModel(
            sandook = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
            preferences = get(),
        )
    }

    viewModel {
        DiscoverViewModel(
            discoverSydneyManager = get(),
            ioDispatcher = get(named(IODispatcher)),
            analytics = get(),
            platformOps = get(),
            appInfoProvider = get(),
            appCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }

    single<FuzzyStopRanker> { DefaultFuzzyStopRanker() }
    single<StopResultsManager> {
        RealStopResultsManager(
            sandook = get(),
            nswBusRoutesSandook = get(),
            flag = get(),
            fuzzyStopRanker = get(),
            defaultDispatcher = get(named(DefaultDispatcher)),
        )
    }

    single<RemoteAddressResultsManager> {
        RealRemoteAddressResultsManager(
            tripPlanningService = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }

    single<NearbyStopsManager> {
        createNearbyStopsManager(
            repository = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }

    // Per-entry map ViewModel — each nav entry (SavedTrips, SearchStop) gets its own
    // instance so screens never share state or compete for NearbyStopsManager queries.
    // NearbyStopsManager itself is a single so network/cache work is still deduplicated.
    viewModel {
        MapStopSelectionViewModel(nearbyStopsManager = get())
    }

    single<InviteFriendsTileManager> { RealInviteFriendsTileManager(get()) }

    viewModel {
        val isDebug = get<AppInfoProvider>().getAppInfo().isDebug
        val debugNetworkConfigStore = get<DebugNetworkConfigStore>()
        val flag = get<Flag>()
        // Read live, not once: the debug toggle can flip while this ViewModel is
        // already alive (Debug Settings -> back, same nav entry), so a snapshot
        // Boolean captured at construction time would silently stay stale forever.
        val isAddressSearchEnabled = {
            if (isDebug) {
                debugNetworkConfigStore.state.value.addressSearchEnabled
            } else {
                flag.getFlagValue(FlagKeys.SEARCH_STOP_ADDRESS_SEARCH_ENABLED.key).asBoolean(false)
            }
        }
        // Read live, not once, same reasoning as isAddressSearchEnabled above - Remote
        // Config can push a new threshold while this ViewModel is already alive.
        val addressSearchMinQueryLength = { resolveAddressSearchMinQueryLength(flag) }
        SearchStopViewModel(
            analytics = get(),
            stopResultsManager = get(),
            remoteAddressResultsManager = get(),
            nearbyStopsManager = get(),
            flag = flag,
            ioDispatcher = get(named(IODispatcher)),
            preferences = get(),
            sandook = get(),
            isAddressSearchEnabled = isAddressSearchEnabled,
            addressSearchMinQueryLength = addressSearchMinQueryLength,
        )
    }
}
