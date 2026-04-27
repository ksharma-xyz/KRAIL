@file:Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")

package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_settings
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.feature.track.ui.components.TrackingCard
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.components.AddLabelBottomSheet
import xyz.ksharma.krail.trip.planner.ui.components.CityCodeText
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.components.SearchStopRow
import xyz.ksharma.krail.trip.planner.ui.components.StopLabelPillRow
import xyz.ksharma.krail.trip.planner.ui.departureboard.departureBoardAccordionSection
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.StopDetailsBottomSheet
import xyz.ksharma.krail.trip.planner.ui.state.departureboard.DepartureBoardUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

private const val LAZY_COLUMN_BOTTOM_PADDING = 300

@Composable
fun SavedTripsScreen(
    savedTripsState: SavedTripsState,
    modifier: Modifier = Modifier,
    trackedJourney: TrackedJourney? = null,
    fromButtonClick: () -> Unit = {},
    toButtonClick: () -> Unit = {},
    onUnsetLabelTap: (StopLabel) -> Unit = {},
    onAddLabelNavigate: (labelKey: String) -> Unit = {},
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit = { _, _ -> },
    onSearchButtonClick: () -> Unit = {},
    onSettingsButtonClick: () -> Unit = {},
    onDiscoverButtonClick: () -> Unit = {},
    onEvent: (SavedTripUiEvent) -> Unit = {},
    onInviteFriendsTileDisplay: () -> Unit = {},
    onTrackingCardClick: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    departureBoardEntries: ImmutableList<StopDepartureBoardEntry> = persistentListOf(),
    expandedDepartureBoardStopId: String? = null,
    onDepartureBoardEvent: (DepartureBoardUiEvent) -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    val emptyStateTip = remember {
        buildList {
            add("Tap ★ on a trip to save it here.")
            add("Saved trips also show Park & Ride.")
            add("Find nearby stops on the map.")
        }.random()
    }

    // Search row expand / from-highlight state — rememberSaveable survives rotation.
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var isFromHighlighted by rememberSaveable { mutableStateOf(false) }

    // Bottom sheet state for set-label "use as" sheet and add-label sheet.
    var useAsSheetStop by remember { mutableStateOf<StopItem?>(null) }
    var showAddLabelSheet by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = KrailTheme.colors.surface),
    ) {
        Column {
            TitleBar(
                title = {
                    Text(text = "KRAIL", color = themeColor())
                },
                actions = {
                    if (savedTripsState.isDiscoverAvailable) {
                        RoundIconButton(
                            showBadge = savedTripsState.displayDiscoverBadge,
                            onClick = onDiscoverButtonClick,
                        ) {
                            CityCodeText("SYD")
                        }
                    }

                    RoundIconButton(
                        onClick = onSettingsButtonClick,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_settings),
                            contentDescription = "Settings",
                            colorFilter = ColorFilter.tint(LocalContentColor.current),
                            modifier = Modifier.size(dim.spacingXXXL),
                        )
                    }
                },
            )

            val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

            LazyColumn(
                contentPadding = PaddingValues(bottom = LAZY_COLUMN_BOTTOM_PADDING.dp),
            ) {
                // Label pills — always shown when labels exist
                if (savedTripsState.stopLabels.isNotEmpty()) {
                    item(key = "stop-label-pills") {
                        StopLabelPillRow(
                            stopLabels = savedTripsState.stopLabels,
                            onLabelClick = { stopItem ->
                                useAsSheetStop = stopItem
                            },
                            onUnsetLabelClick = { label ->
                                onUnsetLabelTap(label)
                            },
                            onAddLabelClick = {
                                showAddLabelSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = dim.spacingM),
                        )
                    }
                }

                when {
                    savedTripsState.isSavedTripsLoading -> Unit

                    savedTripsState.savedTrips.isEmpty() -> {
                        savedTripsState.infoTiles?.let { infoTiles ->
                            infoTiles(
                                infoTiles = infoTiles,
                                onCtaClick = { tileData ->
                                    onEvent(SavedTripUiEvent.InfoTileCtaClick(tileData))
                                },
                                onDismissClick = { tileData ->
                                    onEvent(SavedTripUiEvent.DismissInfoTile(tileData))
                                },
                                onTileExpand = {
                                    onEvent(SavedTripUiEvent.InfoTileExpand(it.key))
                                },
                            )
                        }

                        item(key = "empty_state") {
                            ErrorMessage(
                                emoji = "🌟",
                                title = "Let's Go! Sydney",
                                message = emptyStateTip,
                                modifier = Modifier
                                    .padding(horizontal = dim.pageHorizontalPadding)
                                    .animateItem(),
                            )
                        }
                    }

                    savedTripsState.savedTrips.isNotEmpty() -> {
                        savedTripsState.infoTiles?.let { infoTiles ->
                            infoTiles(
                                infoTiles = infoTiles,
                                onCtaClick = { tileData ->
                                    onEvent(SavedTripUiEvent.InfoTileCtaClick(tileData))
                                },
                                onDismissClick = { tileData ->
                                    onEvent(SavedTripUiEvent.DismissInfoTile(tileData))
                                },
                                onTileExpand = {
                                    onEvent(SavedTripUiEvent.InfoTileExpand(it.key))
                                },
                            )
                        }

                        val hasInviteFriendsInRemoteConfig =
                            savedTripsState.infoTiles?.any { tile ->
                                tile.key.startsWith("invite_friends", ignoreCase = true) ||
                                    tile.type == InfoTileData.InfoTileType.INVITE_FRIENDS
                            } ?: false

                        val shouldShowInviteFriends = savedTripsState.savedTrips.size >= 2 &&
                            !hasInviteFriendsInRemoteConfig

                        if (shouldShowInviteFriends) {
                            item(key = "invite_friends_tile_hardcoded") {
                                LaunchedEffect(!savedTripsState.hasSeenInviteFriendsTile) {
                                    if (!savedTripsState.hasSeenInviteFriendsTile) {
                                        onInviteFriendsTileDisplay()
                                    }
                                }

                                InfoTile(
                                    infoTileData = InfoTileData(
                                        key = "invite_friends_hardcoded",
                                        title = "Invite Your Friends",
                                        description = "You're the reason KRAIL is ad-free. Share it " +
                                            "with friends and help Sydney ride better.",
                                        primaryCta = InfoTileCta(
                                            text = "Invite",
                                            url = null,
                                        ),
                                        type = InfoTileData.InfoTileType.INVITE_FRIENDS,
                                        showDismissButton = false,
                                    ),
                                    initialState = if (savedTripsState.hasSeenInviteFriendsTile) {
                                        InfoTileState.COLLAPSED
                                    } else {
                                        InfoTileState.EXPANDED
                                    },
                                    onCtaClick = { tileData ->
                                        onEvent(SavedTripUiEvent.InfoTileCtaClick(tileData))
                                    },
                                    onDismissClick = { },
                                    onTileExpand = { tileData ->
                                        onEvent(SavedTripUiEvent.InfoTileExpand(tileData.key))
                                    },
                                    modifier = Modifier.padding(
                                        horizontal = dim.pageHorizontalPadding,
                                        vertical = dim.spacingM,
                                    ),
                                )
                            }
                        }

                        savedTripsContent(
                            savedTripsState = savedTripsState,
                            trackedJourney = trackedJourney,
                            onEvent = onEvent,
                            onSavedTripCardClick = onSavedTripCardClick,
                            onTrackingCardClick = onTrackingCardClick,
                            onStopTracking = onStopTracking,
                            expandedMap = expandedMap,
                            departureBoardEntries = departureBoardEntries,
                            expandedDepartureBoardStopId = expandedDepartureBoardStopId,
                            onDepartureBoardEvent = onDepartureBoardEvent,
                        )
                    }
                }
            }
        }

        SearchStopRow(
            modifier = Modifier.align(Alignment.BottomCenter),
            fromStopItem = savedTripsState.fromStop,
            toStopItem = savedTripsState.toStop,
            isExpanded = isSearchExpanded,
            isFromHighlighted = isFromHighlighted,
            onExpandRequest = {
                isSearchExpanded = true
                isFromHighlighted = false
            },
            fromButtonClick = {
                isFromHighlighted = false
                fromButtonClick()
            },
            toButtonClick = toButtonClick,
            onReverseButtonClick = {
                onEvent(SavedTripUiEvent.ReverseStopClick)
            },
            onSearchButtonClick = { onSearchButtonClick() },
        )

        useAsSheetStop?.let { stop ->
            val dim = KrailTheme.dimensions
            StopDetailsBottomSheet(
                stop = NearbyStopFeature(
                    stopId = stop.stopId,
                    stopName = stop.stopName,
                    position = LatLng(0.0, 0.0),
                    transportModes = persistentListOf(),
                ),
                onDismiss = { useAsSheetStop = null },
                actionContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dim.pageHorizontalPadding)
                            .padding(bottom = dim.spacingM),
                        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
                    ) {
                        Button(
                            onClick = {
                                onEvent(SavedTripUiEvent.StopLabelPillTapped(stop))
                                isSearchExpanded = true
                                isFromHighlighted = false
                                useAsSheetStop = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Going here")
                        }
                        TextButton(
                            onClick = {
                                onEvent(SavedTripUiEvent.StopLabelUsedAsFrom(stop))
                                isSearchExpanded = true
                                useAsSheetStop = null
                            },
                            dimensions = ButtonDefaults.largeButtonSize(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Starting from here",
                                color = KrailTheme.colors.onSurface,
                            )
                        }
                    }
                },
            )
        }

        if (showAddLabelSheet) {
            AddLabelBottomSheet(
                onDismiss = { showAddLabelSheet = false },
                onPickStop = { emoji, name ->
                    onEvent(SavedTripUiEvent.SetPendingNewLabel(emoji, name))
                    onAddLabelNavigate(name)
                    showAddLabelSheet = false
                },
            )
        }
    }
}

private fun LazyListScope.infoTiles(
    infoTiles: ImmutableList<InfoTileData>,
    onCtaClick: (InfoTileData) -> Unit,
    onDismissClick: (InfoTileData) -> Unit,
    onTileExpand: (InfoTileData) -> Unit,
) {
    items(
        items = infoTiles,
        key = { item -> item.key },
    ) { tileData ->
        var visible by remember { mutableStateOf(true) }
        AnimatedVisibility(
            visible = visible,
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300),
            ),
        ) {
            val dim = KrailTheme.dimensions
            InfoTile(
                infoTileData = tileData,
                onCtaClick = onCtaClick,
                onDismissClick = {
                    onDismissClick(tileData)
                },
                onTileExpand = onTileExpand,
                modifier = Modifier
                    .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingM),
            )
        }
    }
}

private fun LazyListScope.savedTripsContent(
    savedTripsState: SavedTripsState,
    trackedJourney: TrackedJourney?,
    onEvent: (SavedTripUiEvent) -> Unit,
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit = { _, _ -> },
    onTrackingCardClick: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    expandedMap: SnapshotStateMap<String, Boolean>,
    departureBoardEntries: ImmutableList<StopDepartureBoardEntry>,
    expandedDepartureBoardStopId: String?,
    onDepartureBoardEvent: (DepartureBoardUiEvent) -> Unit = {},
) {
    if (trackedJourney != null) {
        stickyHeader(key = "tracking_title") {
            SavedTripsTitle {
                Text(text = "Tracking Real Time")
            }
        }

        item(key = "tracking_card") {
            TrackingCard(
                tracked = trackedJourney,
                onCardClick = onTrackingCardClick,
                onStopTracking = onStopTracking,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    stickyHeader(key = "saved_trips_title") {
        SavedTripsTitle {
            Text(text = "Saved Trips")
        }
    }

    items(
        items = savedTripsState.savedTrips,
        key = { trip -> trip.tripId },
    ) { trip ->
        val dim = KrailTheme.dimensions
        SavedTripCard(
            trip = trip,
            onStarClick = { onEvent(SavedTripUiEvent.DeleteSavedTrip(trip)) },
            onCardClick = {
                onSavedTripCardClick(
                    StopItem(
                        stopId = trip.fromStopId,
                        stopName = trip.fromStopName,
                    ),
                    StopItem(
                        stopId = trip.toStopId,
                        stopName = trip.toStopName,
                    ),
                )
            },
            primaryTransportMode = null, // TODO
            modifier = Modifier
                .padding(horizontal = dim.pageHorizontalPadding),
        )

        Spacer(modifier = Modifier.height(dim.spacingXL))
    }

    if (savedTripsState.parkRideUiState.isNotEmpty()) {
        stickyHeader(key = "park_ride_title") {
            SavedTripsTitle {
                Text(text = "Park & Ride")
            }
        }

        items(
            items = savedTripsState.parkRideUiState,
            key = { parkRide -> parkRide.stopId },
        ) { parkRide ->
            val dim = KrailTheme.dimensions
            val isExpanded = expandedMap[parkRide.stopId] ?: false

            ParkRideCard(
                isExpanded = isExpanded,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
                onClick = {
                    val newExpanded = !isExpanded
                    expandedMap[parkRide.stopId] = newExpanded
                    onEvent(
                        SavedTripUiEvent.ParkRideCardClick(
                            parkRideState = parkRide,
                            isExpanded = newExpanded,
                        ),
                    )
                },
                parkRideUiState = parkRide,
            )

            Spacer(modifier = Modifier.height(dim.spacingXL))
        }
    }

    if (departureBoardEntries.isNotEmpty()) {
        stickyHeader(key = "departure_board_title") {
            SavedTripsTitle {
                Text(text = "Departure Board")
            }
        }

        departureBoardEntries.forEach { entry ->
            departureBoardAccordionSection(
                entry = entry,
                isExpanded = expandedDepartureBoardStopId == entry.stopId,
                onEvent = onDepartureBoardEvent,
            )
        }

        item(key = "departure_board_bottom_spacer") {
            val dim = KrailTheme.dimensions
            Spacer(modifier = Modifier.height(dim.pageSectionGap))
        }
    }
}

@Composable
private fun SavedTripsTitle(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier.fillMaxWidth()
            .background(color = KrailTheme.colors.surface)
            .padding(vertical = dim.spacingXL, horizontal = dim.spacingXL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        CompositionLocalProvider(LocalTextStyle provides KrailTheme.typography.titleMedium) {
            content()
        }
    }
}

// region Previews

@Preview(name = "1. Empty — no saved trips")
@Composable
private fun PreviewSavedTripsScreen_Empty() {
    PreviewTheme {
        SavedTripsScreen(
            savedTripsState = SavedTripsState(
                isSavedTripsLoading = false,
                isDiscoverAvailable = true,
                stopLabels = StopLabel.defaults,
            ),
        )
    }
}

@Preview(name = "2. With saved trips + unset labels")
@Composable
private fun PreviewSavedTripsScreen_WithTrips() {
    PreviewTheme {
        SavedTripsScreen(
            savedTripsState = SavedTripsState(
                isSavedTripsLoading = false,
                isDiscoverAvailable = false,
                stopLabels = StopLabel.defaults,
            ),
        )
    }
}

@Preview(name = "3. Search row expanded")
@Composable
private fun PreviewSavedTripsScreen_SearchExpanded() {
    PreviewTheme {
        SavedTripsScreen(
            savedTripsState = SavedTripsState(
                isSavedTripsLoading = false,
                stopLabels = persistentListOf(
                    StopLabel(
                        emoji = "🏠",
                        label = "Home",
                        stopId = "2000001",
                        stopName = "Central Station",
                    ),
                    StopLabel(emoji = "💼", label = "Work"),
                ),
                toStop = StopItem(stopId = "2000001", stopName = "Central Station"),
            ),
        )
    }
}

// endregion
