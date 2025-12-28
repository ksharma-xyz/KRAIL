package xyz.ksharma.krail.trip.planner.ui.entries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.discover.state.DiscoverEvent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertScreen
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
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItemSaver
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.settings.SettingsEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.state.usualride.ThemeSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionScreen
import xyz.ksharma.krail.trip.planner.ui.themeselection.ThemeSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableScreen
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

/**
 * Entry provider for Trip Planner feature.
 * Uses only TripPlannerNavigator interface - no direct Navigator dependency!
 */
@Composable
fun EntryProviderScope<NavKey>.tripPlannerEntries(
    tripPlannerNavigator: TripPlannerNavigator
) {
    savedTripsEntry(tripPlannerNavigator)
    searchStopEntry(tripPlannerNavigator)
    timeTableEntry(tripPlannerNavigator)
    themeSelectionEntry(tripPlannerNavigator)
    // alertsEntry removed - alerts shown as modal within TimeTable
    // dateTimeSelectorEntry removed - date/time selector shown as modal within TimeTable
    settingsEntry(tripPlannerNavigator)
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

        // Listen for StopSelected results using ResultEffect
        ResultEffect<StopSelectedResult> { result ->
            log("SavedTrips: ===== STOP SELECTED RESULT RECEIVED =====")
            log("SavedTrips: fieldType=${result.fieldType}, stopId=${result.stopId}, stopName=${result.stopName}")

            val stopItem = StopItem(
                stopId = result.stopId,
                stopName = result.stopName
            )

            when (result.fieldType) {
                SearchStopFieldType.FROM -> {
                    log("SavedTrips: Setting FROM stop")
                    viewModel.onEvent(SavedTripUiEvent.FromStopChanged(stopItem.toJsonString()))
                }
                SearchStopFieldType.TO -> {
                    log("SavedTrips: Setting TO stop")
                    viewModel.onEvent(SavedTripUiEvent.ToStopChanged(stopItem.toJsonString()))
                }
            }
            log("SavedTrips: ✅ Stop selection processed")
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
        // ViewModel is scoped to this NavEntry and will be recreated each time
        // This ensures recent stops are refreshed when the screen is opened
        val viewModel: SearchStopViewModel = koinViewModel()
        val searchStopState by viewModel.uiState.collectAsStateWithLifecycle()

        // Refresh recent stops every time screen opens.
        // Note: Cannot rely on StateFlow's onStart because WhileSubscribed(5000) keeps the
        // flow alive for 5 seconds after screen closes. If user returns within that window,
        // onStart won't run again and recent stops won't refresh.
        LaunchedEffect(Unit) {
            viewModel.onEvent(SearchStopUiEvent.RefreshRecentStopsList)
        }

        // Capture ResultEventBus in composable scope for use in callbacks
        val resultEventBus = LocalResultEventBus.current

        SearchStopScreen(
            searchStopState = searchStopState,
            onStopSelect = { stopItem ->
                log("SearchStop: onStopSelected: fieldType=${key.fieldType}, stopItem: ${stopItem.stopName} (${stopItem.stopId})")

                // Send result using captured bus reference
                val result = StopSelectedResult(
                    fieldType = key.fieldType,
                    stopId = stopItem.stopId,
                    stopName = stopItem.stopName
                )
                resultEventBus.sendResult(result = result)
                log("SearchStop: ✅ Result sent via ResultEventBus")

                // Navigate back
                tripPlannerNavigator.goBack()
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
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
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

        // State for showing service alerts modal
        var showAlertsModal by rememberSaveable { mutableStateOf(false) }
        var alertsToDisplay by remember { mutableStateOf(persistentSetOf<xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert>()) }

        // State for showing date/time selector modal
        var showDateTimeSelectorModal by rememberSaveable { mutableStateOf(false) }

        // State for date/time selection - survives configuration changes using custom Saver
        var dateTimeSelectionItem by rememberSaveable(
            key,
            stateSaver = DateTimeSelectionItemSaver,
        ) { mutableStateOf<DateTimeSelectionItem?>(null) }

        // Clear date/time selection when route changes (new trip selected)
        LaunchedEffect(key) {
            log("TimeTable: ===== ROUTE CHANGE EFFECT =====")
            log("TimeTable: Route changed to ${key.fromStopId}->${key.toStopId}")
            log("TimeTable: Clearing stale date/time selection")
            dateTimeSelectionItem = null
            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(null))
        }

        // Reload data whenever the route changes (different trip selected)
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

        Box(modifier = Modifier.fillMaxSize()) {
            // Main TimeTable Screen
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
                            alertsToDisplay = alerts.toPersistentSet()
                            showAlertsModal = true
                        }
                    }
                },
                dateTimeSelectionItem = dateTimeSelectionItem,
                dateTimeSelectorClicked = {
                    // Show modal instead of navigating
                    showDateTimeSelectorModal = true
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

            // Service Alerts Modal - using ModalBottomSheet for better UX
            if (showAlertsModal) {
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showAlertsModal = false },
                    containerColor = KrailTheme.colors.surface,
                ) {
                    ServiceAlertScreen(
                        serviceAlerts = alertsToDisplay,
                        onBackClick = { showAlertsModal = false }
                    )
                }
            }

            // Date/Time Selector Modal - using ModalBottomSheet for better UX
            if (showDateTimeSelectorModal) {
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showDateTimeSelectorModal = false },
                    containerColor = KrailTheme.colors.surface,
                ) {
                    DateTimeSelectorScreen(
                        dateTimeSelection = dateTimeSelectionItem,
                        onBackClick = {
                            showDateTimeSelectorModal = false
                        },
                        onDateTimeSelected = { selection ->
                            // Update state immediately for real-time feedback
                            dateTimeSelectionItem = selection
                            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(selection))
                            showDateTimeSelectorModal = false
                        },
                        onResetClick = {
                            // Clear selection
                            dateTimeSelectionItem = null
                            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(null))
                            showDateTimeSelectorModal = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * ThemeSelection Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryProviderScope<NavKey>.themeSelectionEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<ThemeSelectionRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane()
    ) { key ->
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
 * Settings Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryProviderScope<NavKey>.settingsEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<SettingsRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane()
    ) { key ->
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
 * OurStory Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryProviderScope<NavKey>.ourStoryEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<OurStoryRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane()
    ) { key ->
        val viewModel: OurStoryViewModel = koinViewModel()
        val ourStoryState by viewModel.models.collectAsStateWithLifecycle()

        OurStoryScreen(
            state = ourStoryState,
            onBackClick = { tripPlannerNavigator.goBack() }
        )
    }
}

/**
 * Intro Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryProviderScope<NavKey>.introEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<IntroRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane()
    ) { key ->
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
 * Discover Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryProviderScope<NavKey>.discoverEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<DiscoverRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane()
    ) { key ->
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

