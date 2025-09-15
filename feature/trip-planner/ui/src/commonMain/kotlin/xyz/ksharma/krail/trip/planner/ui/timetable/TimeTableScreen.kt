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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.SubtleButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.trip.planner.ui.components.ActionData
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.JourneyCard
import xyz.ksharma.krail.trip.planner.ui.components.JourneyCardState
import xyz.ksharma.krail.trip.planner.ui.components.OriginDestination
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots
import xyz.ksharma.krail.trip.planner.ui.components.loading.LoadingEmojiAnim
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
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
) {
    val themeColorHex by LocalThemeColor.current
    val themeColor = themeColorHex.hexToComposeColor()
    var displayModeSelectionRow by rememberSaveable { mutableStateOf(false) }
    val unselectedModesProductClass: MutableList<Int> = remember(timeTableState.unselectedModes) {
        log("Initial Exclude - : ${timeTableState.unselectedModes}")
        mutableStateListOf(*timeTableState.unselectedModes.toTypedArray())
    }
    var isReverseButtonRotated by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().background(color = KrailTheme.colors.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            TitleBar(
                onNavActionClick = onBackClick,
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        }
                    }
                },
                actions = {
                    val rotation by animateFloatAsState(
                        targetValue = if (isReverseButtonRotated) 180f else 0f,
                        animationSpec = tween(durationMillis = 300)
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
                            modifier = Modifier.size(24.dp),
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
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            )
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            item(key = "Origin-Destination") {
                timeTableState.trip?.let { trip ->
                    OriginDestination(
                        trip = trip,
                        timeLineColor = KrailTheme.colors.onSurface,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(color = KrailTheme.colors.surface),
                    )
                }
            }

            item(key = "trip-actions-row") {
                FlowRow(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_filter),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(ButtonDefaults.subtleButtonColors().contentColor),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(text = "Mode")
                        }
                    }
                }
            }

            if (displayModeSelectionRow) {
                item(key = "transport-mode-selection-row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp)
                            .animateItem(),
                    ) {
                        items(
                            items = TransportMode.values().toList(),
                            key = { item -> item.productClass }
                        ) {
                            TransportModeChip(
                                transportMode = it,
                                selected = !unselectedModesProductClass.contains(it.productClass),
                                onClick = {
                                    // Toggle / Set behavior
                                    if (unselectedModesProductClass.contains(it.productClass)) {
                                        unselectedModesProductClass.removeAll(listOf(it.productClass))
                                    } else {
                                        unselectedModesProductClass.add(it.productClass)
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
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .animateItem()
                    ) {
                        // todo -  handle in Button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(color = getForegroundColor(themeColor)),
                                modifier = Modifier.size(20.dp),
                            )
                            Text("Done", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
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
                                modifier = Modifier.padding(vertical = 60.dp).animateItem(),
                            )

                            Text(
                                text = emoji.greeting,
                                style = KrailTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = KrailTheme.colors.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            } else if (timeTableState.journeyList.isNotEmpty()) {
                items(
                    items = timeTableState.journeyList,
                    key = { it.journeyId },
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
                        onClick = {
                            onEvent(TimeTableUiEvent.JourneyCardClicked(journey.journeyId))
                        },
                        totalUniqueServiceAlerts = journey.totalUniqueServiceAlerts,
                        onAlertClick = {
                            onAlertClick(journey.journeyId)
                        },
                        onLegClick = onJourneyLegClick,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            .animateItem(),
                        departureDeviation = journey.departureDeviation,
                    )
                }
            } else { // Journey list is empty or null
                item(key = "no-results") {
                    ErrorMessage(
                        title = "No route found!",
                        message = "Search for another stop or check back later.",
                        modifier = Modifier.fillMaxWidth().animateItem(),
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(96.dp).systemBarsPadding(),
                )
            }
        }
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
    departureDeviation: TimeTableState.JourneyCardInfo.DepartureDeviation? = null,
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
            modifier = modifier,
            departureDeviation = departureDeviation,
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
            .size(48.dp)
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

@Composable
private fun PreviewTimeTableScreen() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Ferry().colorCode) }
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
                                    transportMode = TransportMode.Bus(),
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

@Composable
private fun PreviewTimeTableScreenError() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Train().colorCode) }
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

@Composable
private fun PreviewTimeTableScreenNoResults() {
    KrailTheme {
        val themeColor = remember { mutableStateOf(TransportMode.Train().colorCode) }
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
