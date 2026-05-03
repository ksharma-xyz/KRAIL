package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_check
import krail.feature.trip_planner.ui.generated.resources.ic_filter
import krail.feature.trip_planner.ui.generated.resources.ic_reverse
import krail.feature.trip_planner.ui.generated.resources.ic_star
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.TransportModeSortOrder
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.departures.ui.DeparturesViewModel
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.AnimatedDots
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.SubtleButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.trip.planner.ui.components.ActionData
import xyz.ksharma.krail.trip.planner.ui.components.DepartureBoardStopCard
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.JourneyCard
import xyz.ksharma.krail.trip.planner.ui.components.JourneyCardState
import xyz.ksharma.krail.trip.planner.ui.components.OriginDestination
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip
import xyz.ksharma.krail.trip.planner.ui.components.loading.LoadingEmojiAnim
import xyz.ksharma.krail.trip.planner.ui.components.map.StopDetailsBottomSheet
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.fromStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.toStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

@Composable
fun TimeTableScreen(
    timeTableState: TimeTableState,
    expandedJourneyId: String?,
    dateTimeSelectionItem: DateTimeSelectionItem?,
    onEvent: (TimeTableUiEvent) -> Unit,
    onAlertClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onJourneyLegClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dateTimeSelectorClicked: () -> Unit = {},
    onModeSelectionChanged: (Set<Int>) -> Unit = {},
    onModeClick: (Boolean) -> Unit = {},
    onMapClick: (String) -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current
    val themeColor = themeColorHex.hexToComposeColor()
    var displayModeSelectionRow by rememberSaveable { mutableStateOf(false) }
    val unselectedModesProductClass: MutableList<Int> = remember(timeTableState.unselectedModes) {
        log("Initial Exclude - : ${timeTableState.unselectedModes}")
        mutableStateListOf(*timeTableState.unselectedModes.toTypedArray())
    }
    var isReverseButtonRotated by rememberSaveable { mutableStateOf(false) }
    // Tracks which stop's details sheet is open. Tap on a stop in the
    // OriginDestination header sets this; the sheet is dismissed back to null.
    var selectedStop by remember { mutableStateOf<StopDisplay?>(null) }

    Box(
        modifier = modifier.fillMaxSize().background(color = KrailTheme.colors.surface),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = dim.spacingXL),
        ) {
            stickyHeader(key = "title-bar") {
                TitleBar(
                    onNavActionClick = onBackClick,
                    title = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Timetable",
                                color = KrailTheme.colors.onSurface,
                            )

                            AnimatedVisibility(
                                visible = timeTableState.silentLoading && !timeTableState.isLoading,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                AnimatedDots(
                                    color = themeColor,
                                    modifier = Modifier.padding(start = dim.spacingXXXL),
                                )
                            }
                        }
                    },
                    actions = {
                        val rotation by animateFloatAsState(
                            targetValue = if (isReverseButtonRotated) 180f else 0f,
                            animationSpec = tween(durationMillis = 300),
                        )
                        ActionButton(
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationZ = rotation
                                },
                            onClick = {
                                isReverseButtonRotated = !isReverseButtonRotated
                                onEvent(TimeTableUiEvent.ReverseTripButtonClicked)
                            },
                            contentDescription = "Reverse Trip Search",
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_reverse),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                                modifier = Modifier.size(dim.iconM),
                            )
                        }
                        ActionButton(
                            onClick = { onEvent(TimeTableUiEvent.SaveTripButtonClicked) },
                            contentDescription = if (timeTableState.isTripSaved) {
                                "Remove Saved Trip"
                            } else {
                                "Save Trip"
                            },
                        ) {
                            Image(
                                painter = if (timeTableState.isTripSaved) {
                                    painterResource(Res.drawable.ic_star_filled)
                                } else {
                                    painterResource(Res.drawable.ic_star)
                                },
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                                modifier = Modifier.size(dim.iconM),
                            )
                        }
                    },
                )
            }
            stickyHeader(key = "Origin-Destination") {
                timeTableState.trip?.let { trip ->
                    OriginDestination(
                        origin = trip.fromStopDisplay(timeTableState.stopLabels),
                        destination = trip.toStopDisplay(timeTableState.stopLabels),
                        timeLineColor = KrailTheme.colors.onSurface,
                        onOriginClick = { display ->
                            selectedStop = display
                            onEvent(
                                TimeTableUiEvent.OriginDestinationStopHeaderClicked(
                                    stopId = display.stopId,
                                    stopName = display.name,
                                    isOrigin = true,
                                ),
                            )
                        },
                        onDestinationClick = { display ->
                            selectedStop = display
                            onEvent(
                                TimeTableUiEvent.OriginDestinationStopHeaderClicked(
                                    stopId = display.stopId,
                                    stopName = display.name,
                                    isOrigin = false,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = dim.spacingL, vertical = dim.spacingM)
                            .background(color = KrailTheme.colors.surface),
                    )
                }
            }

            item(key = "trip-actions-row") {
                FlowRow(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(horizontal = dim.spacingL)
                        .padding(top = dim.spacingL),
                    horizontalArrangement = Arrangement.spacedBy(dim.spacingXL),
                    verticalArrangement = Arrangement.spacedBy(dim.spacingL),
                ) {
                    SubtleButton(
                        onClick = dateTimeSelectorClicked,
                        dimensions = ButtonDefaults.mediumButtonSize(),
                    ) {
                        Text(
                            text = dateTimeSelectionItem?.toDateTimeText() ?: "Plan your trip",
                        )
                    }

                    SubtleButton(
                        onClick = {
                            displayModeSelectionRow = !displayModeSelectionRow
                            onModeClick(displayModeSelectionRow)
                        },
                        dimensions = ButtonDefaults.mediumButtonSize(),
                    ) {
                        Row(
                            // todo -  handle in Button
                            horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_filter),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(ButtonDefaults.subtleButtonColors().contentColor),
                                modifier = Modifier.size(FILTER_ICON_SIZE),
                            )
                            Text(text = "Mode")
                        }
                    }
                }
            }

            if (displayModeSelectionRow) {
                item(key = "transport-mode-selection-row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
                        contentPadding = PaddingValues(horizontal = dim.spacingL),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dim.spacingL, bottom = dim.spacingXS)
                            .animateItem(),
                    ) {
                        items(
                            items = NswTransportConfig.sortedModes(TransportModeSortOrder.PRIORITY),
                            key = { item -> NswTransportConfig.productClassFor(item) },
                        ) {
                            TransportModeChip(
                                transportMode = it,
                                selected = !unselectedModesProductClass.contains(
                                    NswTransportConfig.productClassFor(it),
                                ),
                                onClick = {
                                    // Toggle / Set behavior
                                    val pc = NswTransportConfig.productClassFor(it)
                                    if (unselectedModesProductClass.contains(pc)) {
                                        unselectedModesProductClass.removeAll(listOf(pc))
                                    } else {
                                        unselectedModesProductClass.add(pc)
                                    }
                                    log("After operation Exclude - : $unselectedModesProductClass")
                                },
                            )
                        }
                    }
                }

                item("mode-selection-confirm-button") {
                    Button(
                        dimensions = ButtonDefaults.largeButtonSize(),
                        onClick = {
                            displayModeSelectionRow = false
                            onModeSelectionChanged(unselectedModesProductClass.toSet())
                        },
                        modifier = Modifier
                            .padding(vertical = dim.spacingM, horizontal = dim.spacingL)
                            .animateItem(),
                    ) {
                        // todo -  handle in Button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(color = getForegroundColor(themeColor)),
                                modifier = Modifier.size(dim.iconS),
                            )
                            Text("Done", modifier = Modifier.padding(start = dim.spacingXS))
                        }
                    }
                }
            }

            item(key = "spacer-top") {
                Spacer(modifier = Modifier.height(dim.spacingL))
            }

            if (timeTableState.isError) {
                item("error") {
                    ErrorMessage(
                        title = "Eh! That's not looking right mate!",
                        message = "Let's try again.",
                        actionData = ActionData(
                            actionText = "Retry",
                            onActionClick = { onEvent(TimeTableUiEvent.RetryButtonClicked) },
                        ),
                        modifier = Modifier.fillMaxWidth().animateItem(),
                    )
                }
            } else if (timeTableState.isLoading) {
                timeTableState.loadingEmoji?.let { emoji ->
                    item(key = "loading") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingEmojiAnim(
                                emoji = emoji.emoji,
                                modifier = Modifier.padding(vertical = LOADING_VERTICAL_PADDING).animateItem(),
                            )

                            Text(
                                text = emoji.greeting,
                                style = KrailTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = KrailTheme.colors.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = dim.spacingXL),
                            )
                        }
                    }
                }
            } else if (timeTableState.journeyList.isNotEmpty()) {
                journeyListContent(
                    state = timeTableState,
                    expandedJourneyId = expandedJourneyId,
                    themeColor = themeColor,
                    callbacks = JourneyCallbacks(
                        onEvent = onEvent,
                        onAlertClick = onAlertClick,
                        onLegClick = onJourneyLegClick,
                        onMapClick = onMapClick,
                    ),
                    onPlanTripClick = dateTimeSelectorClicked,
                )
            } else { // Journey list is empty or null
                item(key = "no-results") {
                    ErrorMessage(
                        title = "No route found!",
                        message = "Search for another stop or check back later.",
                        modifier = Modifier.fillMaxWidth().animateItem(),
                    )
                }
            }

            item(key = "spacer-bottom") {
                Spacer(
                    modifier = Modifier.height(BOTTOM_SPACER_HEIGHT).systemBarsPadding(),
                )
            }
        }

        selectedStop?.let { stop ->
            // Stop-details sheet — opened by tapping a stop in the timetable
            // header. Mirrors the SearchStopMap wrapper's contents minus the
            // map-only bits (LatLng, parkAndRide indicator, "Select stop"
            // action button) since this sheet is opened from inside a trip,
            // not from a search picker. transportModes left empty until we
            // have a per-stop modes lookup wired in.
            val departuresViewModel = koinViewModel<DeparturesViewModel>()
            val departuresState by departuresViewModel.uiState
                .collectAsStateWithLifecycle()
            StopDetailsBottomSheet(
                stopId = stop.stopId,
                stopName = stop.name,
                transportModes = persistentListOf(),
                onDismiss = { selectedStop = null },
                additionalInfo = {
                    DepartureBoardStopCard(
                        stopId = stop.stopId,
                        stopName = stop.name,
                        state = departuresState,
                        onEvent = departuresViewModel::onEvent,
                    )
                    Spacer(modifier = Modifier.height(dim.spacingXL))
                },
            )
        }
    }
}

private data class JourneyCallbacks(
    val onEvent: (TimeTableUiEvent) -> Unit,
    val onAlertClick: (String) -> Unit,
    val onLegClick: (Boolean) -> Unit,
    val onMapClick: (String) -> Unit,
)

private fun LazyListScope.journeyListContent(
    state: TimeTableState,
    expandedJourneyId: String?,
    themeColor: Color,
    callbacks: JourneyCallbacks,
    onPlanTripClick: () -> Unit,
) {
    if (state.paginationEnabled) {
        paginationHeader(
            isLoadingPrevious = state.isLoadingPrevious,
            themeColor = themeColor,
            onEvent = callbacks.onEvent,
        )
        journeyCardItems(
            journeys = state.previousJourneyList,
            keyPrefix = "prev_",
            expandedJourneyId = expandedJourneyId,
            state = state,
            callbacks = callbacks,
        )
    }
    journeyCardItems(
        journeys = state.journeyList,
        keyPrefix = "",
        expandedJourneyId = expandedJourneyId,
        state = state,
        callbacks = callbacks,
    )
    if (state.paginationEnabled) {
        paginationFooter(
            isLoadingMore = state.isLoadingMore,
            canLoadMore = state.canLoadMore,
            themeColor = themeColor,
            onEvent = callbacks.onEvent,
            onPlanTripClick = onPlanTripClick,
        )
    }
}

private fun LazyListScope.paginationHeader(
    isLoadingPrevious: Boolean,
    themeColor: Color,
    onEvent: (TimeTableUiEvent) -> Unit,
) {
    item(key = "show-previous") {
        if (isLoadingPrevious) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = KrailTheme.dimensions.spacingL),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedDots(color = themeColor, modifier = Modifier.padding(vertical = KrailTheme.dimensions.spacingM))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = KrailTheme.dimensions.spacingXS, horizontal = KrailTheme.dimensions.spacingXXS),
            ) {
                TextButton(onClick = { onEvent(TimeTableUiEvent.LoadPreviousTrips) }) {
                    Text(text = "Show previous departures")
                }
            }
        }
    }
}

private fun LazyListScope.paginationFooter(
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    themeColor: Color,
    onEvent: (TimeTableUiEvent) -> Unit,
    onPlanTripClick: () -> Unit,
) {
    if (isLoadingMore) {
        item(key = "loading-more") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = KrailTheme.dimensions.spacingL),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedDots(color = themeColor, modifier = Modifier.padding(vertical = KrailTheme.dimensions.spacingM))
            }
        }
    } else if (canLoadMore) {
        item(key = "load-more-button") {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = KrailTheme.dimensions.spacingXS, horizontal = KrailTheme.dimensions.spacingXXS),
            ) {
                TextButton(onClick = { onEvent(TimeTableUiEvent.LoadMoreTrips) }) {
                    Text(text = "Load more departures")
                }
            }
        }
    } else {
        item(key = "plan-trip-button") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = KrailTheme.dimensions.spacingXS),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = onPlanTripClick) {
                    Text(text = "Plan your trip")
                }
            }
        }
    }
}

private fun LazyListScope.journeyCardItems(
    journeys: ImmutableList<TimeTableState.JourneyCardInfo>,
    keyPrefix: String,
    expandedJourneyId: String?,
    state: TimeTableState,
    callbacks: JourneyCallbacks,
) {
    items(
        items = journeys,
        key = { "${keyPrefix}${it.journeyId}" },
    ) { journey ->
        JourneyCardItem(
            timeToDeparture = journey.timeText,
            platformNumber = journey.platformNumber,
            platformText = journey.platformText,
            originTime = journey.originTime,
            destinationTime = journey.destinationTime,
            durationText = journey.travelTime,
            totalWalkTime = journey.totalWalkTime,
            transportModeLineList = journey.transportModeLines,
            legList = journey.legs.toImmutableList(),
            cardState = if (expandedJourneyId == journey.journeyId) {
                JourneyCardState.EXPANDED
            } else {
                JourneyCardState.DEFAULT
            },
            onClick = { callbacks.onEvent(TimeTableUiEvent.JourneyCardClicked(journey.journeyId)) },
            totalUniqueServiceAlerts = journey.totalUniqueServiceAlerts,
            onAlertClick = { callbacks.onAlertClick(journey.journeyId) },
            onLegClick = callbacks.onLegClick,
            onMapClick = { callbacks.onMapClick(journey.journeyId) },
            isMapsAvailable = state.isMapsAvailable,
            onShareJourney = { bitmap, shareText, isPastDeparture ->
                callbacks.onEvent(
                    TimeTableUiEvent.ShareJourneyClicked(
                        bitmap = bitmap,
                        shareText = shareText,
                        journeyId = journey.journeyId,
                        isPastDeparture = isPastDeparture,
                    ),
                )
            },
            modifier = Modifier.padding(vertical = KrailTheme.dimensions.spacingM).animateItem(),
            departureDeviation = journey.departureDeviation,
            scheduledOriginTime = journey.scheduledOriginTime,
            deepLinkUrl = state.deepLinkUrls[journey.journeyId],
        )
    }
}

@Composable // todo - probably don't need this
private fun JourneyCardItem(
    timeToDeparture: String,
    platformNumber: String?,
    platformText: String?,
    originTime: String,
    durationText: String,
    totalWalkTime: String?,
    destinationTime: String,
    onClick: () -> Unit,
    cardState: JourneyCardState,
    legList: ImmutableList<TimeTableState.JourneyCardInfo.Leg>,
    onAlertClick: () -> Unit,
    totalUniqueServiceAlerts: Int,
    modifier: Modifier = Modifier,
    transportModeLineList: ImmutableList<TransportModeLine>? = null,
    onLegClick: (Boolean) -> Unit,
    onMapClick: () -> Unit = {},
    onShareJourney: (ImageBitmap, String, Boolean) -> Unit = { _, _, _ -> },
    isMapsAvailable: Boolean = false,
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation? = null,
    scheduledOriginTime: String? = null,
    deepLinkUrl: String? = null,
) {
    if (!transportModeLineList.isNullOrEmpty() && legList.isNotEmpty()) {
        JourneyCard(
            timeToDeparture = timeToDeparture,
            originTime = originTime,
            destinationTime = destinationTime,
            totalTravelTime = durationText,
            platformNumber = platformNumber,
            platformText = platformText,
            cardState = cardState,
            transportModeLineList = transportModeLineList,
            legList = legList,
            totalWalkTime = totalWalkTime,
            onClick = onClick,
            onAlertClick = onAlertClick,
            totalUniqueServiceAlerts = totalUniqueServiceAlerts,
            onLegClick = onLegClick,
            onMapClick = onMapClick,
            onShareJourney = onShareJourney,
            isMapsAvailable = isMapsAvailable,
            modifier = modifier,
            departureDeviation = departureDeviation,
            scheduledOriginTime = scheduledOriginTime,
            deepLinkUrl = deepLinkUrl,
        )
    }
}

@Composable
fun ActionButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(KrailTheme.dimensions.buttonRoundSize)
            .clip(CircleShape)
            .klickable { onClick() }
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// region Preview

@PreviewScreen
@Composable
private fun PreviewTimeTableScreen() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(NswTransportMode.Ferry.colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            TimeTableScreen(
                timeTableState = TimeTableState(
                    trip = Trip(
                        fromStopName = "From Stop",
                        toStopName = "To Stop",
                        fromStopId = "123",
                        toStopId = "456",
                    ),
                    journeyList = listOf(
                        TimeTableState.JourneyCardInfo(
                            timeText = "12:00",
                            platformText = "Stand A",
                            platformNumber = "A",
                            originTime = "12:00",
                            destinationTime = "12:30",
                            travelTime = "30 mins",
                            transportModeLines = persistentListOf(
                                TransportModeLine(
                                    transportMode = TransportMode.Bus,
                                    lineName = "123",
                                ),
                            ),
                            legs = persistentListOf(),
                            totalUniqueServiceAlerts = 3,
                            originUtcDateTime = "2024-11-01T12:00:00Z",
                            destinationUtcDateTime = "2024-11-01T12:30:00Z",
                        ),
                    ).toImmutableList(),
                ),
                expandedJourneyId = null,
                onEvent = {},
                onAlertClick = {},
                onBackClick = {},
                dateTimeSelectionItem = null,
                onJourneyLegClick = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewTimeTableScreenError() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(NswTransportMode.Train.colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            TimeTableScreen(
                timeTableState = TimeTableState(
                    trip = Trip(
                        fromStopName = "From Stop",
                        toStopName = "To Stop",
                        fromStopId = "123",
                        toStopId = "456",
                    ),
                    isError = true,
                    isLoading = false,
                ),
                expandedJourneyId = null,
                onEvent = {},
                onAlertClick = {},
                onBackClick = {},
                dateTimeSelectionItem = null,
                onJourneyLegClick = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewTimeTableScreenNoResults() {
    PreviewTheme {
        val themeColor = remember { mutableStateOf(NswTransportMode.Train.colorCode) }
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
            TimeTableScreen(
                timeTableState = TimeTableState(
                    trip = Trip(
                        fromStopName = "From Stop",
                        toStopName = "To Stop",
                        fromStopId = "123",
                        toStopId = "456",
                    ),
                    isError = false,
                    isLoading = false,
                ),
                dateTimeSelectionItem = null,
                expandedJourneyId = null,
                onEvent = {},
                onAlertClick = {},
                onBackClick = {},
                onJourneyLegClick = {},
            )
        }
    }
}

// endregion

private val FILTER_ICON_SIZE = 18.dp
private val LOADING_VERTICAL_PADDING = 60.dp
private val BOTTOM_SPACER_HEIGHT = 96.dp
