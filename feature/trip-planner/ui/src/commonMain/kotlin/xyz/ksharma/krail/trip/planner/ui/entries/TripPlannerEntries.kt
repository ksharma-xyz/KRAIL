package xyz.ksharma.krail.trip.planner.ui.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.discover.state.DiscoverEvent
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertScreen
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertsViewModel
import xyz.ksharma.krail.trip.planner.ui.api.DateTimeSelectedResult
import xyz.ksharma.krail.trip.planner.ui.api.DateTimeSelectorRoute
import xyz.ksharma.krail.trip.planner.ui.api.DiscoverRoute
import xyz.ksharma.krail.trip.planner.ui.api.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.api.OurStoryRoute
import xyz.ksharma.krail.trip.planner.ui.api.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.api.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.api.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.api.ServiceAlertRoute
import xyz.ksharma.krail.trip.planner.ui.api.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.api.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.api.ThemeSelectionRoute
import xyz.ksharma.krail.trip.planner.ui.api.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.api.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorScreen
import xyz.ksharma.krail.trip.planner.ui.discover.DiscoverScreen
import xyz.ksharma.krail.trip.planner.ui.discover.DiscoverViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroScreen
import xyz.ksharma.krail.trip.planner.ui.intro.IntroViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopScreen
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.settings.SettingsScreen
import xyz.ksharma.krail.trip.planner.ui.settings.SettingsViewModel
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryScreen
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryViewModel
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.settings.SettingsEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.state.usualride.ThemeSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionScreen
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableScreen
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

// Note: NavigationResult is from app module, accessed via reflection to avoid circular dependency

/**
 * Entry provider for Trip Planner feature.
 * Uses only TripPlannerNavigator interface - no direct Navigator dependency!
 */
@Composable
fun EntryProviderScope<NavKey>.tripPlannerEntries(
    navigator: Any, // Ignored - kept for signature compatibility
    tripPlannerNavigator: TripPlannerNavigator
) {
    savedTripsEntry(tripPlannerNavigator)
    searchStopEntry(tripPlannerNavigator)
    timeTableEntry(tripPlannerNavigator)
    themeSelectionEntry(tripPlannerNavigator)
    alertsEntry(tripPlannerNavigator)
    settingsEntry(tripPlannerNavigator)
    dateTimeSelectorEntry(tripPlannerNavigator)
    ourStoryEntry(tripPlannerNavigator)
    introEntry(tripPlannerNavigator)
    discoverEntry(tripPlannerNavigator)
}

/**
 * SavedTrips Entry - List Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryProviderScope<NavKey>.savedTripsEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<SavedTripsRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.listPane()
    ) { key ->
        // Scoped ViewModel that survives navigation
        val viewModel: SavedTripsViewModel = koinViewModel(key = "SavedTripsNav")
        val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

        // Listen for results from SearchStopScreen using SharedFlow
        LaunchedEffect(tripPlannerNavigator) {
            log("SavedTrips: Starting to collect results")
            tripPlannerNavigator.navigationResults.collect { result ->
                log("SavedTrips: Received result: $result (type: ${result::class.simpleName})")
                when (result) {
                    is StopSelectedResult -> {
                        log("SavedTrips received StopSelected result: ${result.stopId}")

                        val stopItem = StopItem(
                            stopId = result.stopId,
                            stopName = result.stopName
                        )

                        when (result.fieldType) {
                            SearchStopFieldType.FROM -> {
                                log("SavedTrips: Updating FROM stop")
                                viewModel.onEvent(SavedTripUiEvent.FromStopChanged(stopItem.toJsonString()))
                            }
                            SearchStopFieldType.TO -> {
                                log("SavedTrips: Updating TO stop")
                                viewModel.onEvent(SavedTripUiEvent.ToStopChanged(stopItem.toJsonString()))
                            }
                        }
                    }
                    else -> {
                        log("SavedTrips: Ignoring result type: ${result::class.simpleName}")
                    }
                }
            }
        }

        SavedTripsScreen(
            savedTripsState = savedTripState,
            fromButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsFromButtonClick)
                tripPlannerNavigator.navigateToSearchStop(SearchStopFieldType.FROM)
            },
            toButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsToButtonClick)
                tripPlannerNavigator.navigateToSearchStop(SearchStopFieldType.TO)
            },
            onReverseButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.ReverseStopClick)
            },
            onSavedTripCardClick = { fromStop, toStop ->
                if (fromStop?.stopId != null && toStop?.stopId != null) {
                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsSavedTripCardClick(
                            fromStop.stopId,
                            toStop.stopId
                        )
                    )
                    tripPlannerNavigator.navigateToTimeTable(
                        fromStopId = fromStop.stopId,
                        fromStopName = fromStop.stopName,
                        toStopId = toStop.stopId,
                        toStopName = toStop.stopName
                    )
                }
            },
            onSearchButtonClick = {
                val fromStopItem = savedTripState.fromStop
                val toStopItem = savedTripState.toStop

                if (fromStopItem != null && toStopItem != null &&
                    fromStopItem.stopId != toStopItem.stopId) {
                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsLoadTimeTableClick(
                            fromStopId = fromStopItem.stopId,
                            toStopId = toStopItem.stopId
                        )
                    )
                    tripPlannerNavigator.navigateToTimeTable(
                        fromStopId = fromStopItem.stopId,
                        fromStopName = fromStopItem.stopName,
                        toStopId = toStopItem.stopId,
                        toStopName = toStopItem.stopName
                    )
                }
            },
            onSettingsButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsSettingsButtonClick)
                tripPlannerNavigator.navigateToSettings()
            },
            onDiscoverButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsDiscoverButtonClick)
                tripPlannerNavigator.navigateToDiscover()
            },
            onEvent = { event -> viewModel.onEvent(event) },
            onInviteFriendsTileDisplay = { viewModel.markInviteFriendsTileAsSeen() }
        )
    }
}

/**
 * SearchStop Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryProviderScope<NavKey>.searchStopEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<SearchStopRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane()
    ) { key ->
        val viewModel: SearchStopViewModel = koinViewModel()
        val searchStopState by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        SearchStopScreen(
            searchStopState = searchStopState,
            onStopSelect = { stopItem ->
                log("SearchStop: onStopSelected: fieldType=${key.fieldType}, stopItem: $stopItem")

                // Emit result using the typed method
                scope.launch {
                    tripPlannerNavigator.emitStopSelected(
                        fieldType = key.fieldType,
                        stopId = stopItem.stopId,
                        stopName = stopItem.stopName
                    )
                }
            },
            goBack = {
                tripPlannerNavigator.goBack()
            },
            onEvent = { event -> viewModel.onEvent(event) }
        )
    }
}

/**
 * TimeTable Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryProviderScope<NavKey>.timeTableEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<TimeTableRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane()
    ) { key ->
        // ViewModel is scoped to this NavEntry - no need for vmKey
        // When entry is removed from backstack, ViewModel is destroyed
        val viewModel: TimeTableViewModel = koinViewModel()
        val timeTableState by viewModel.uiState.collectAsStateWithLifecycle()
        val expandedJourneyId by viewModel.expandedJourneyId.collectAsStateWithLifecycle()

        // CRITICAL: Must collect isLoading to trigger the onStart block that calls fetchTrip()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

        // State for date/time selection - keyed to route so it resets when route changes
        var dateTimeSelectionItem by remember(key) { mutableStateOf<DateTimeSelectionItem?>(null) }

        // Listen for DateTimeSelector results using SharedFlow
        // Key this to 'key' so it restarts (and clears) when navigating to a different trip
        LaunchedEffect(key, tripPlannerNavigator) {
            tripPlannerNavigator.navigationResults.collect { result ->
                when (result) {
                    is DateTimeSelectedResult -> {
                        log("TimeTable received DateTimeSelected result for route: ${key.fromStopId}->${key.toStopId}")
                        if (result.dateTimeJson.isNotEmpty()) {
                            dateTimeSelectionItem = DateTimeSelectionItem.fromJsonString(result.dateTimeJson)
                            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(dateTimeSelectionItem))
                        } else {
                            // Reset was clicked
                            dateTimeSelectionItem = null
                            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(null))
                        }
                    }
                    else -> { /* Ignore other result types */ }
                }
            }
        }

        // Reload data whenever the route changes (different trip selected)
        // key is the TimeTableRoute object - it changes when from/to stops change
        // Note: dateTimeSelectionItem is already keyed to 'key' via remember(key) on line 243,
        // so it automatically resets to null when navigating to a different trip
        LaunchedEffect(key) {
            log("=== TimeTable LaunchedEffect TRIGGERED ===")
            log("TimeTable: Route = ${key.fromStopId} -> ${key.toStopId}")
            log("TimeTable: fromStopName = ${key.fromStopName}")
            log("TimeTable: toStopName = ${key.toStopName}")

            // Load fresh timetable data for the new trip
            val trip = Trip(
                fromStopId = key.fromStopId,
                fromStopName = key.fromStopName,
                toStopId = key.toStopId,
                toStopName = key.toStopName
            )
            log("TimeTable: Created Trip object: $trip")
            log("TimeTable: Sending LoadTimeTable event to ViewModel")

            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip = trip))

            log("TimeTable: LoadTimeTable event sent")
            log("=== TimeTable LaunchedEffect END ===")
        }


        TimeTableScreen(
            timeTableState = timeTableState,
            expandedJourneyId = expandedJourneyId,
            onEvent = { viewModel.onEvent(it) },
            onBackClick = {
                tripPlannerNavigator.goBack()
            },
            onAlertClick = { journeyId ->
                log("AlertClicked for journeyId: $journeyId")
                viewModel.fetchAlertsForJourney(journeyId) { alerts ->
                    if (alerts.isNotEmpty()) {
                        tripPlannerNavigator.navigateToAlerts(journeyId)
                    }
                }
            },
            dateTimeSelectionItem = dateTimeSelectionItem,
            dateTimeSelectorClicked = {
                // Just navigate - result will come back via SharedFlow
                tripPlannerNavigator.navigateToDateTimeSelector(
                    dateTimeSelectionItem?.toJsonString()
                )
            },
            onJourneyLegClick = { isExpanded ->
                // Journey leg click is handled internally by the TimeTableScreen
                // No additional action needed here
            },
            onModeSelectionChanged = { selectedModes ->
                // Handle mode filter selection changes
                viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(selectedModes))
            },
            onModeClick = { isVisible ->
                // Handle mode filter visibility toggle
                viewModel.onEvent(TimeTableUiEvent.ModeClicked(isVisible))
            }
        )
    }
}

/**
 * ThemeSelection Entry
 */
@Composable
private fun EntryProviderScope<NavKey>.themeSelectionEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<ThemeSelectionRoute> { key ->
        val viewModel: ThemeSelectionViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        ThemeSelectionScreen(
            selectedThemeStyle = state.selectedThemeStyle ?: KrailThemeStyle.Train,
            onThemeSelected = { themeId ->
                val hexColorCode = KrailThemeStyle.entries
                    .firstOrNull { it.id == themeId }?.hexColorCode

                check(hexColorCode != null) { "hexColorCode for themeId $themeId not found" }

                // Update global theme via interface method
                tripPlannerNavigator.updateTheme(hexColorCode)

                // Save to database
                viewModel.onEvent(ThemeSelectionEvent.ThemeSelected(themeId))
            },
            onBackClick = { tripPlannerNavigator.goBack() },
            onThemeModeSelect = { code ->
                viewModel.onEvent(ThemeSelectionEvent.ThemeModeSelected(code))
            }
        )
    }
}

/**
 * Alerts Entry
 */
@Composable
private fun EntryProviderScope<NavKey>.alertsEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<ServiceAlertRoute> { key ->
        val viewModel: ServiceAlertsViewModel = koinViewModel()
        val alertState by viewModel.uiState.collectAsStateWithLifecycle()

        ServiceAlertScreen(
            serviceAlerts = alertState.serviceAlerts.toImmutableSet(),
            onBackClick = {
                tripPlannerNavigator.goBack()
            }
        )
    }
}

/**
 * Settings Entry
 */
@Composable
private fun EntryProviderScope<NavKey>.settingsEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<SettingsRoute> { key ->
        val viewModel: SettingsViewModel = koinViewModel()
        val settingsState by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        SettingsScreen(
            appVersion = settingsState.appVersion,
            onChangeThemeClick = {
                tripPlannerNavigator.navigateToThemeSelection()
            },
            onBackClick = {
                tripPlannerNavigator.goBack()
            },
            onReferFriendClick = {
                viewModel.onReferFriendClick()
            },
            onIntroClick = {
                scope.launch {
                    viewModel.onIntroClick()
                    tripPlannerNavigator.navigateToIntro()
                }
            },
            onAboutUsClick = {
                scope.launch {
                    viewModel.onOurStoryClick()
                    tripPlannerNavigator.navigateToOurStory()
                }
            },
            onSocialLinkClick = { socialType ->
                viewModel.onEvent(SettingsEvent.SocialLinkClick(socialType))
            }
        )
    }
}

/**
 * DateTimeSelector Entry
 */
@Composable
private fun EntryProviderScope<NavKey>.dateTimeSelectorEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<DateTimeSelectorRoute> { key ->
        val scope = rememberCoroutineScope()

        // Parse the JSON to get the current selection
        val currentSelection = remember(key.dateTimeSelectionItemJson) {
            key.dateTimeSelectionItemJson?.let { DateTimeSelectionItem.fromJsonString(it) }
        }

        DateTimeSelectorScreen(
            dateTimeSelection = currentSelection,
            onBackClick = {
                tripPlannerNavigator.goBack()
            },
            onDateTimeSelected = { dateTimeSelection ->
                // Emit result using the typed method
                scope.launch {
                    dateTimeSelection?.let {
                        tripPlannerNavigator.emitDateTimeSelected(it.toJsonString())
                    }
                }
            },
            onResetClick = {
                // Emit empty result
                scope.launch {
                    tripPlannerNavigator.emitDateTimeSelected("")
                }
            }
        )
    }
}

/**
 * OurStory Entry
 */
@Composable
private fun EntryProviderScope<NavKey>.ourStoryEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<OurStoryRoute> { key ->
        val viewModel: OurStoryViewModel = koinViewModel()
        val ourStoryState by viewModel.models.collectAsStateWithLifecycle()

        OurStoryScreen(
            state = ourStoryState,
            onBackClick = { tripPlannerNavigator.goBack() }
        )
    }
}

/**
 * Intro Entry
 */
@Composable
private fun EntryProviderScope<NavKey>.introEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<IntroRoute> { key ->
        val viewModel = koinViewModel<IntroViewModel>()
        val introState by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        IntroScreen(
            state = introState,
            onIntroComplete = { pageType, pageNumber ->
                scope.launch {
                    viewModel.onEvent(IntroUiEvent.Complete(pageType, pageNumber))
                    // Clear entire back stack - user shouldn't go back to intro
                    tripPlannerNavigator.clearBackStackAndNavigate(SavedTripsRoute)
                }
            }
        ) { event -> viewModel.onEvent(event) }
    }
}

/**
 * Discover Entry
 */
@Composable
private fun EntryProviderScope<NavKey>.discoverEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<DiscoverRoute> { key ->
        val viewModel: DiscoverViewModel = koinViewModel()
        val discoverState by viewModel.uiState.collectAsStateWithLifecycle()

        DiscoverScreen(
            state = discoverState,
            onBackClick = {
                tripPlannerNavigator.goBack()
            },
            onAppSocialLinkClicked = { krailSocialType ->
                viewModel.onEvent(
                    event = DiscoverEvent.AppSocialLinkClicked(krailSocialType = krailSocialType)
                )
            },
            onPartnerSocialLinkClicked = { partnerSocialLink, cardId, cardType ->
                viewModel.onEvent(
                    event = DiscoverEvent.PartnerSocialLinkClicked(
                        partnerSocialLink = partnerSocialLink,
                        cardId = cardId,
                        cardType = cardType
                    )
                )
            },
            onCtaClicked = { url, cardId, cardType ->
                viewModel.onEvent(
                    event = DiscoverEvent.CtaButtonClicked(
                        url = url,
                        cardId = cardId,
                        cardType = cardType
                    )
                )
            },
            onShareClick = { cardId, cardType ->
                viewModel.onEvent(
                    event = DiscoverEvent.ShareButtonClicked(
                        cardId = cardId,
                        cardType = cardType
                    )
                )
            },
            onCardSeen = { cardId ->
                viewModel.onEvent(
                    event = DiscoverEvent.CardSeen(cardId = cardId)
                )
            },
            resetAllSeenCards = {
                viewModel.onEvent(event = DiscoverEvent.ResetAllSeenCards)
            },
            onChipSelected = { cardType ->
                viewModel.onEvent(event = DiscoverEvent.FilterChipClicked(cardType))
            }
        )
    }
}

