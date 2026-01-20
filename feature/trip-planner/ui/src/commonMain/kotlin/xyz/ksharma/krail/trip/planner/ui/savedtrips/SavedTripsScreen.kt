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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_settings
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.components.CityCodeText
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideCard
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.components.SearchStopRow
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

@Composable
fun SavedTripsScreen(
    savedTripsState: SavedTripsState,
    modifier: Modifier = Modifier,
    fromButtonClick: () -> Unit = {},
    toButtonClick: () -> Unit = {},
    onReverseButtonClick: () -> Unit = {},
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit = { _, _ -> },
    onSearchButtonClick: () -> Unit = {},
    onSettingsButtonClick: () -> Unit = {},
    onDiscoverButtonClick: () -> Unit = {},
    onEvent: (SavedTripUiEvent) -> Unit = {},
    onInviteFriendsTileDisplay: () -> Unit = {},
) {
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
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            )

            val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 300.dp),
            ) {
                when {
                    savedTripsState.isSavedTripsLoading -> {
                        Unit // display nothing while loading
                    }

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
                                message = "Your saved trips will show up here.",
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
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

                        // Show invite friends tile only if user has 2+ saved trips and it's not in remote config
                        val hasInviteFriendsInRemoteConfig =
                            savedTripsState.infoTiles?.any { tile ->
                                tile.key.startsWith("invite_friends", ignoreCase = true) ||
                                    tile.type == InfoTileData.InfoTileType.INVITE_FRIENDS
                            } ?: false

                        val shouldShowInviteFriends = savedTripsState.savedTrips.size >= 2 &&
                            !hasInviteFriendsInRemoteConfig

                        if (shouldShowInviteFriends) {
                            item(key = "invite_friends_tile_hardcoded") {
                                // Mark tile as seen when displayed (only if not already seen)
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
                                            url = null, // Using nullable url from this version
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
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    ),
                                )
                            }
                        }

                        savedTripsContent(
                            savedTripsState = savedTripsState,
                            onEvent = onEvent,
                            onSavedTripCardClick = onSavedTripCardClick,
                            expandedMap = expandedMap,
                        )
                    }
                }
            }
        }

        SearchStopRow(
            modifier = Modifier.align(Alignment.BottomCenter),
            fromStopItem = savedTripsState.fromStop,
            toStopItem = savedTripsState.toStop,
            fromButtonClick = fromButtonClick,
            toButtonClick = toButtonClick,
            onReverseButtonClick = onReverseButtonClick,
            onSearchButtonClick = { onSearchButtonClick() },
        )
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
                targetOffsetX = { it }, // slide fully to the right
                animationSpec = tween(300),
            ),
        ) {
            InfoTile(
                infoTileData = tileData,
                onCtaClick = onCtaClick,
                onDismissClick = {
                    onDismissClick(tileData)
                },
                onTileExpand = onTileExpand,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

private fun LazyListScope.savedTripsContent(
    savedTripsState: SavedTripsState,
    onEvent: (SavedTripUiEvent) -> Unit,
    onSavedTripCardClick: (StopItem?, StopItem?) -> Unit = { _, _ -> },
    expandedMap: SnapshotStateMap<String, Boolean>,
) {
    stickyHeader(key = "saved_trips_title") {
        SavedTripsTitle {
            Text(text = "Saved Trips")
        }
    }

    items(
        items = savedTripsState.savedTrips,
        key = { trip -> trip.tripId },
    ) { trip ->
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
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
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
            val isExpanded = expandedMap[parkRide.stopId] ?: false

            ParkRideCard(
                isExpanded = isExpanded,
                modifier = Modifier.padding(horizontal = 16.dp),
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

            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "park_ride_spacer_bottom") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SavedTripsTitle(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(color = KrailTheme.colors.surface)
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalTextStyle provides KrailTheme.typography.titleMedium) {
            content()
        }
    }
}

// region Previews

@Composable
private fun SavedTripsScreenPreview() {
    PreviewTheme {
        SavedTripsScreen(savedTripsState = SavedTripsState())
    }
}

// endregion
