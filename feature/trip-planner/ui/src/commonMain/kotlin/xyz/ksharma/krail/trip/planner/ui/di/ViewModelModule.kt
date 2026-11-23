package xyz.ksharma.krail.trip.planner.ui.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertsViewModel
import xyz.ksharma.krail.trip.planner.ui.alerts.summary.AlertSummaryViewModel
import xyz.ksharma.krail.trip.planner.ui.alerts.summary.isAlertSummaryEnabled
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorViewModel
import xyz.ksharma.krail.trip.planner.ui.discover.DiscoverViewModel
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroCompletion
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFlowViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroParkRideGateway
import xyz.ksharma.krail.trip.planner.ui.intro.IntroStopsGateway
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.parkride.AddParkRideViewModel
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideStationAdder
import xyz.ksharma.krail.trip.planner.ui.parkride.RealParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.parkride.RealParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.ParkRideSuggestionSources
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.ParkRideSuggestionViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.InviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.savedtrips.RealInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiAttemptReporter
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputViewModel
import xyz.ksharma.krail.trip.planner.ui.search.ai.isAiSearchInputEnabled
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
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.resolveNearbyStopsRadiusKm
import xyz.ksharma.krail.trip.planner.ui.settings.SettingsViewModel
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryViewModel
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel
import kotlin.time.TimeSource

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
            analytics = get(),
            isAlertSummaryEnabled = isAlertSummaryEnabled,
        )
    }

    viewModel { params ->
        val isDebug = get<AppInfoProvider>().getAppInfo().isDebug
        val debugNetworkConfigStore = get<DebugNetworkConfigStore>()
        val flag = get<Flag>()
        // Read live, not once — same reasoning as isAlertSummaryEnabled above.
        val isAiSearchInputEnabled = {
            isAiSearchInputEnabled(isDebug, debugNetworkConfigStore, flag)
        }
        // Composable-supplied — see AiSearchInputViewModel's constructor doc for why this
        // can't be a plain Koin `get()`.
        val resolveCurrentLocation = params.getOrNull<suspend () -> Location?>() ?: { null }
        val sandook = get<Sandook>()
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
            reporter = AiAttemptReporter(
                analytics = get(),
                // The rider's own words for their places. Never wired before, so it defaulted
                // to an empty list: `withLabelWordAsDestination` could never fire, and the
                // rescue for a sentence whose only place is a word like "work" has never run
                // for a rider. Wiring it makes that fallback live and `hadLabelWord` report
                // something other than false.
                riderLabels = {
                    sandook.observeStopLabels().first().map { label -> label.label }
                },
            ),
        )
    }

    // The two step Intro. It takes a SavedStateHandle from Koin's view model parameters.
    viewModel { params ->
        IntroFlowViewModel(
            savedStateHandle = params.get(),
            appDataReadiness = get(),
            completion = IntroCompletion(
                sandook = get(),
                preferences = get(),
                analytics = get(),
                ioDispatcher = get(named(IODispatcher)),
            ),
            timeSource = TimeSource.Monotonic,
            parkRide = IntroParkRideGateway(
                catalogue = RealParkRideCatalogue(
                    nswParkRideFacilityManager = get(),
                    stopResultsManager = get(),
                    sandook = get(),
                    festivalManager = get(),
                ),
                availabilityLoader = RealParkRideAvailabilityLoader(
                    parkRideSandook = get(),
                    parkRideService = get(),
                    flag = get(),
                ),
                parkRideSandook = get(),
                ioDispatcher = get(named(IODispatcher)),
                stopNameLookup = get<StopResultsManager>()::fetchLocalStopName,
                analytics = get(),
            ),
            stops = IntroStopsGateway(
                flag = get(),
                nswStops = get(),
                sandook = get(),
                stopResults = get(),
                ioDispatcher = get(named(IODispatcher)),
            ),
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
            nearbyStopsRepository = get(),
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
            connectivity = get(),
            tripTrackingDebugOverride = tripTrackingDebugOverride,
            clock = get(),
            carriageForecasts = get(),
            disruptions = get(),
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
        MapStopSelectionViewModel(
            nearbyStopsManager = get(),
            searchRadiusKm = resolveNearbyStopsRadiusKm(get()),
        )
    }

    // The home map's pins and its offline notice, built from state the saved trips screen already holds.
    viewModel {
        HomeMapViewModel(
            sandook = get(),
            connectivity = get(),
            flag = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
    }

    // The one-time Park & Ride suggestion on the map home. It reads only local data and adds through the picker's own add.
    viewModel {
        ParkRideSuggestionViewModel(
            preferences = get(),
            sources = ParkRideSuggestionSources(
                catalogue = RealParkRideCatalogue(
                    nswParkRideFacilityManager = get(),
                    stopResultsManager = get(),
                    sandook = get(),
                    festivalManager = get(),
                ),
                sandook = get(),
                parkRideSandook = get(),
            ),
            adder = ParkRideStationAdder(
                parkRideSandook = get(),
                analytics = get(),
                appReviewManager = get(),
            ),
            flag = get(),
            ioDispatcher = get(named(IODispatcher)),
        )
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
            sandook = get(),
            searchSessionStore = get(),
            isAddressSearchEnabled = isAddressSearchEnabled,
            addressSearchMinQueryLength = addressSearchMinQueryLength,
        )
    }
}
