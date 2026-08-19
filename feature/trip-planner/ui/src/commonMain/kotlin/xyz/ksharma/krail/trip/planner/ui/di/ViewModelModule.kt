package xyz.ksharma.krail.trip.planner.ui.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import xyz.ksharma.dhruva.location.Location
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
import xyz.ksharma.krail.trip.planner.ui.alerts.summary.AlertSummaryViewModel
import xyz.ksharma.krail.trip.planner.ui.alerts.summary.isAlertSummaryEnabled
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorViewModel
import xyz.ksharma.krail.trip.planner.ui.discover.DiscoverViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroViewModel
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.parkride.AddParkRideViewModel
import xyz.ksharma.krail.trip.planner.ui.parkride.RealParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.parkride.RealParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.savedtrips.InviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.savedtrips.RealInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputViewModel
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.ChainedStopTextResolver
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.LabelWordGuard
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.LabelledStopLocator
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.RiderOriginLocator
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopLabelTextResolver
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.StopSearchTextResolver
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealRemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealSearchSessionStore
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealStopResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.RemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchSessionStore
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
        val isDebug = get<AppInfoProvider>().getAppInfo().isDebug
        val debugNetworkConfigStore = get<DebugNetworkConfigStore>()
        val flag = get<Flag>()
        // Read live, not once, same reasoning as isAddressSearchEnabled above.
        val isAlertSummaryEnabled = {
            if (isDebug) {
                debugNetworkConfigStore.state.value.alertSummaryEnabled
            } else {
                flag.isAlertSummaryEnabled()
            }
        }
        AlertSummaryViewModel(
            aiTextService = get(),
            isAlertSummaryEnabled = isAlertSummaryEnabled,
        )
    }

    viewModel { params ->
        val isDebug = get<AppInfoProvider>().getAppInfo().isDebug
        val debugNetworkConfigStore = get<DebugNetworkConfigStore>()
        val flag = get<Flag>()
        // Read live, not once — same reasoning as isAlertSummaryEnabled above.
        val isAiSearchInputEnabled = {
            if (isDebug) {
                debugNetworkConfigStore.state.value.aiSearchInputEnabled
            } else {
                flag.getFlagValue(FlagKeys.AI_SEARCH_INPUT_ENABLED.key).asBoolean(false)
            }
        }
        // Composable-supplied — see AiSearchInputViewModel's constructor doc for why this
        // can't be a plain Koin `get()`.
        val resolveCurrentLocation = params.getOrNull<suspend () -> Location?>() ?: { null }
        AiSearchInputViewModel(
            aiTextService = get(),
            speechToTextService = get(),
            // Order is precedence. Labels first: "work" is the rider naming a stop
            // themselves, which beats a coincidental match against every stop in the state.
            stopTextResolver = ChainedStopTextResolver(
                listOf(
                    StopLabelTextResolver(sandook = get()),
                    // Guarded: a label word that reached no label must resolve to nothing
                    // rather than being searched for among stop names. "work" with no Work
                    // label set matched Blacktown Workers Club.
                    LabelWordGuard(StopSearchTextResolver(stopResultsManager = get())),
                ),
            ),
            // Same precedence idea as the resolver chain above, applied to where a journey
            // starts: a stop the rider named beats one that happens to be nearby.
            riderOriginLocator = RiderOriginLocator(
                resolveCurrentLocation = resolveCurrentLocation,
                labelledStopLocator = LabelledStopLocator(sandook = get()),
                nearbyStopsRepository = get(),
            ),
            isAiSearchInputEnabled = isAiSearchInputEnabled,
        )
    }

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
            searchSessionStore = get(),
            flag = get(),
            preferences = get(),
            platformOps = get(),
            infoTileManager = get(),
            inviteFriendsTileManager = get(),
            trackingManager = get<TrackingManager>(),
            appReviewManager = get(),
        )
    }

    viewModel {
        AddParkRideViewModel(
            catalogue = RealParkRideCatalogue(
                nswParkRideFacilityManager = get(),
                stopResultsManager = get(),
                sandook = get(),
                festivalManager = get(),
            ),
            parkRideSandook = get(),
            availabilityLoader = RealParkRideAvailabilityLoader(
                parkRideSandook = get(),
                parkRideService = get(),
                flag = get(),
            ),
            platformOps = get(),
            analytics = get(),
            ioDispatcher = get(named(IODispatcher)),
            appReviewManager = get(),
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
            appReviewManager = get(),
            tripTrackingDebugOverride = tripTrackingDebugOverride,
            clock = get(),
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

    single<SearchSessionStore> { RealSearchSessionStore() }

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
            clock = get(),
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
            searchSessionStore = get(),
            isAddressSearchEnabled = isAddressSearchEnabled,
            addressSearchMinQueryLength = addressSearchMinQueryLength,
        )
    }
}
