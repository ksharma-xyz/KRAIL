@file:Suppress("LongMethod", "TooManyFunctions", "CyclomaticComplexMethod", "StringLiteralDuplication")

package xyz.ksharma.krail.trip.planner.ui.tracktrip

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.share.withBrandingHeader
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.track.DepartureDeviation
import xyz.ksharma.krail.feature.track.LiveTrackingOverlay
import xyz.ksharma.krail.feature.track.TrackTripState
import xyz.ksharma.krail.feature.track.TrackedJourneyDisplay
import xyz.ksharma.krail.feature.track.TrackedLeg
import xyz.ksharma.krail.feature.track.TrackedStop
import xyz.ksharma.krail.feature.track.TrackingConfig
import xyz.ksharma.krail.feature.track.TripDeepLink
import xyz.ksharma.krail.feature.track.ui.TrackTripViewModel
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.SHADOW_ALPHA
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shadowRadius
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shadowSpread
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shape
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Divider
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.components.TitleBar
import xyz.ksharma.krail.taj.icons.rememberShareIconPainter
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.components.ActionData
import xyz.ksharma.krail.trip.planner.ui.components.ErrorMessage
import xyz.ksharma.krail.trip.planner.ui.components.OriginDestination
import xyz.ksharma.krail.trip.planner.ui.components.TrackedLegView
import xyz.ksharma.krail.trip.planner.ui.components.WalkingLeg
import xyz.ksharma.krail.trip.planner.ui.components.loading.AnimatedDots
import xyz.ksharma.krail.trip.planner.ui.components.loading.LoadingEmojiAnim
import xyz.ksharma.krail.trip.planner.ui.journeymap.JourneyMap
import xyz.ksharma.krail.trip.planner.ui.journeymap.LiveVehicleLayer
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.TrackedJourneyMapMapper.toJourneyMapState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.timetable.ActionButton
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun TrackTripScreen(
    encodedData: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val viewModel: TrackTripViewModel = koinViewModel(parameters = { parametersOf(encodedData) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val countdownDisplay by viewModel.countdownDisplay.collectAsStateWithLifecycle()
    val now by viewModel.clock.collectAsStateWithLifecycle()
    val stopCoordinates by viewModel.stopCoordinates.collectAsStateWithLifecycle()
    val liveOverlay by viewModel.liveOverlay.collectAsStateWithLifecycle()
    val isRefreshing = (state as? TrackTripState.Tracking)?.isRefreshing == true

    // Trip expired or finished — clean up happened in ViewModel, navigate back immediately.
    LaunchedEffect(state) {
        if (state is TrackTripState.ArrivedAndFinished) onBack()
    }

    var mapExpanded by rememberSaveable { mutableStateOf(false) }
    var triggerShare by remember { mutableStateOf(false) }
    val isJourneyActive = state is TrackTripState.Tracking || state is TrackTripState.Arrived

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KrailTheme.colors.surface),
    ) {
        Column {
            TitleBar(
                title = { Text(text = "Track Trip") },
                onNavActionClick = onBack,
                actions = {
                    AnimatedVisibility(
                        visible = isRefreshing,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        AnimatedDots(modifier = Modifier.size(48.dp, 16.dp))
                    }
                    if (isJourneyActive) {
                        TextButton(onClick = { mapExpanded = !mapExpanded }) {
                            Text(text = if (mapExpanded) "Hide Map" else "Map")
                        }
                        ActionButton(
                            onClick = { triggerShare = true },
                            contentDescription = "Share journey",
                        ) {
                            Image(
                                painter = rememberShareIconPainter(),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                },
            )

            when (val s = state) {
                TrackTripState.Initial -> Unit
                TrackTripState.ArrivedAndFinished -> Unit // LaunchedEffect above handles navigate back

                is TrackTripState.Prompt -> PromptContent(
                    deepLink = s.deepLink,
                    onStartTracking = viewModel::onStartTracking,
                )

                is TrackTripState.Loading -> LoadingContent(deepLink = s.deepLink, emoji = s.emoji)

                is TrackTripState.Tracking -> JourneyContent(
                    journey = s.journey,
                    countdownDisplay = countdownDisplay,
                    now = now,
                    mapExpanded = mapExpanded,
                    stopCoordinates = stopCoordinates,
                    liveOverlay = liveOverlay,
                    triggerShare = triggerShare,
                    onShareCapture = { bitmap ->
                        triggerShare = false
                        val text = "${s.journey.fromStopName} to ${s.journey.toStopName}," +
                            " departs ${s.journey.originTime}"
                        viewModel.shareTrip(bitmap = bitmap, text = text)
                    },
                )

                is TrackTripState.Arrived -> JourneyContent(
                    journey = s.journey,
                    countdownDisplay = countdownDisplay,
                    now = now,
                    isArrived = true,
                    mapExpanded = mapExpanded,
                    stopCoordinates = stopCoordinates,
                    liveOverlay = liveOverlay,
                    triggerShare = triggerShare,
                    onShareCapture = { bitmap ->
                        triggerShare = false
                        val text = "${s.journey.fromStopName} to ${s.journey.toStopName}," +
                            " departs ${s.journey.originTime}"
                        viewModel.shareTrip(bitmap = bitmap, text = text)
                    },
                )

                TrackTripState.NotFound -> Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ErrorMessage(
                        title = "Service not found",
                        message = "This service wasn't found. It may have been cancelled or\u00A0changed.",
                        emoji = "\uD83D\uDEAB",
                        actionData = ActionData("Retry") { viewModel.retry() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    Button(
                        onClick = {
                            viewModel.onStopTracking()
                            onBack()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp),
                    ) {
                        Text("Stop Tracking")
                    }
                }

                TrackTripState.Error -> Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ErrorMessage(
                        title = "Something went wrong",
                        message = "Couldn't reach transport services. Check your connection and try\u00A0again.",
                        actionData = ActionData("Retry") { viewModel.retry() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    Button(
                        onClick = {
                            viewModel.onStopTracking()
                            onBack()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp),
                    ) {
                        Text("Stop Tracking")
                    }
                }

                is TrackTripState.AlreadyTracking -> AlreadyTrackingContent(
                    currentDeepLink = s.currentDeepLink,
                    requestedDeepLink = s.requestedDeepLink,
                    onStopCurrentAndTrackNew = {
                        viewModel.onStopTracking()
                        viewModel.onStartTracking(s.requestedDeepLink)
                    },
                )
            }
        }
    }
}

@Composable
private fun PromptContent(
    deepLink: TripDeepLink,
    onStartTracking: (TripDeepLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OriginDestination(
            trip = Trip(
                fromStopId = deepLink.fromStopId,
                fromStopName = deepLink.fromStopName,
                toStopId = deepLink.toStopId,
                toStopName = deepLink.toStopName,
            ),
            timeLineColor = KrailTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(color = KrailTheme.colors.surface),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Track this trip for live real-time\u00A0updates.",
                style = KrailTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onStartTracking(deepLink) }) {
                Text("Start Tracking")
            }
        }
    }
}

@Composable
private fun AlreadyTrackingContent(
    currentDeepLink: TripDeepLink,
    requestedDeepLink: TripDeepLink,
    onStopCurrentAndTrackNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OriginDestination(
            trip = Trip(
                fromStopId = requestedDeepLink.fromStopId,
                fromStopName = requestedDeepLink.fromStopName,
                toStopId = requestedDeepLink.toStopId,
                toStopName = requestedDeepLink.toStopName,
            ),
            timeLineColor = KrailTheme.colors.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(color = KrailTheme.colors.surface),
        )

        ErrorMessage(
            emoji = "\uD83D\uDEA6",
            title = "Already on another trip",
            message = "Currently tracking ${
                currentDeepLink.fromStopName
                    .split(" ")
                    .joinToString("\u00A0")
            } to\u00A0${
                currentDeepLink.toStopName
                    .split(" ")
                    .joinToString("\u00A0")
            }.",
            actionData = ActionData("Stop & Track New", onStopCurrentAndTrackNew),
            filledButton = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
    deepLink: TripDeepLink? = null,
    emoji: String = "\uD83D\uDE86",
) {
    Column(modifier = modifier.fillMaxSize()) {
        deepLink?.let { dl ->
            OriginDestination(
                trip = Trip(
                    fromStopId = dl.fromStopId,
                    fromStopName = dl.fromStopName,
                    toStopId = dl.toStopId,
                    toStopName = dl.toStopName,
                ),
                timeLineColor = KrailTheme.colors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(color = KrailTheme.colors.surface),
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingEmojiAnim(
                emoji = emoji,
                modifier = Modifier.padding(vertical = 40.dp),
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun JourneyContent(
    journey: TrackedJourneyDisplay,
    countdownDisplay: Pair<String, String>,
    now: Instant,
    modifier: Modifier = Modifier,
    isArrived: Boolean = false,
    mapExpanded: Boolean = false,
    stopCoordinates: Map<String, LatLng> = emptyMap(),
    liveOverlay: LiveTrackingOverlay? = null,
    triggerShare: Boolean = false,
    onShareCapture: (ImageBitmap) -> Unit = {},
) {
    val journeyMapState = remember(stopCoordinates) {
        if (stopCoordinates.isNotEmpty()) {
            journey.toJourneyMapState(stopCoordinates)
        } else {
            JourneyMapUiState.Loading
        }
    }

    val graphicsLayer = rememberGraphicsLayer()
    val cardBackground = KrailTheme.colors.surface
    val onSurfaceColor = KrailTheme.colors.onSurface
    val screenDensity = LocalDensity.current.density

    LaunchedEffect(triggerShare) {
        if (triggerShare) {
            val raw = graphicsLayer.toImageBitmap()
            val branded = withContext(Dispatchers.Default) {
                raw.withBrandingHeader(
                    backgroundColor = cardBackground,
                    textColor = onSurfaceColor,
                    density = screenDensity,
                )
            }
            onShareCapture(branded)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .drawWithContent {
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            },
    ) {
        AnimatedVisibility(
            visible = mapExpanded,
            enter = expandVertically(tween(400)),
            exit = shrinkVertically(tween(400)),
        ) {
            Box(modifier = Modifier.fillMaxHeight(0.45f)) {
                JourneyMap(
                    journeyMapState = journeyMapState,
                    showFreshnessBadge = false,
                    modifier = Modifier.fillMaxSize(),
                    extraMapContent = {
                        liveOverlay?.let {
                            LiveVehicleLayer(overlay = it, legs = journey.legs)
                        }
                    },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "origin-destination") {
                OriginDestination(
                    trip = Trip(
                        fromStopId = journey.fromStopId,
                        fromStopName = journey.fromStopName,
                        toStopId = journey.toStopId,
                        toStopName = journey.toStopName,
                    ),
                    timeLineColor = KrailTheme.colors.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(color = KrailTheme.colors.surface),
                )
            }

            stickyHeader(key = "arrival-info") {
                CountdownCard(
                    label = countdownDisplay.first,
                    value = countdownDisplay.second,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item(key = "divider") {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 12.dp),
                )
            }

            itemsIndexed(journey.legs, key = { index, _ -> "leg_$index" }) { _, leg ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    when (leg) {
                        is TrackedLeg.Transport -> TrackedLegView(
                            leg = leg,
                            now = now,
                            isArrived = isArrived,
                            stopDelays = liveOverlay?.stopDelays ?: emptyMap(),
                        )

                        is TrackedLeg.Walk -> WalkingLeg(
                            duration = leg.durationText,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CountdownCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    if (TrackingConfig.USE_FLIP_COUNTDOWN) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .dropShadow(
                    shape = shape,
                    shadow = Shadow(
                        radius = shadowRadius,
                        color = themeBackgroundColor(),
                        spread = shadowSpread,
                        alpha = SHADOW_ALPHA,
                    ),
                )
                // clip shape added before clickable so that ripple indication is bounded within shape
                .clip(shape)
                .border(
                    width = InfoTileDefaults.borderWidth,
                    color = themeBackgroundColor(),
                    shape = shape,
                )
                .background(color = KrailTheme.colors.surface)
                .padding(
                    vertical = InfoTileDefaults.verticalPadding,
                    horizontal = InfoTileDefaults.horizontalPadding,
                ),
        ) {
            if (label.isNotEmpty()) {
                Text(
                    text = "$label ",
                    style = KrailTheme.typography.titleMedium,
                )
            }
            FlipText(
                text = value,
                style = KrailTheme.typography.titleMedium,
            )
        }
    } else {
        InfoTile(
            infoTileData = InfoTileData(
                key = "track_arrival_info",
                title = if (label.isNotEmpty()) "$label $value" else value,
                description = "",
                type = InfoTileData.InfoTileType.INFO,
                showDismissButton = false,
            ),
            onCtaClick = {},
            onDismissClick = {},
            modifier = modifier,
        )
    }
}

/**
 * Renders text where each digit animates independently with a vertical flip (solari-board style).
 * Non-digit characters (m, s, space) are static. Digits are keyed by their position from the
 * right so only the digit(s) that actually change trigger an animation each tick.
 */
@Composable
private fun FlipText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.animateContentSize(tween(200)),
    ) {
        text.forEachIndexed { leftIndex, char ->
            val rightKey = text.length - 1 - leftIndex
            if (char.isDigit()) {
                key(rightKey) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            slideInVertically(
                                animationSpec = tween(250, easing = FastOutSlowInEasing),
                                initialOffsetY = { it },
                            ) + fadeIn(tween(200)) togetherWith
                                slideOutVertically(
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    targetOffsetY = { -it },
                                ) + fadeOut(tween(150))
                        },
                        label = "digit_r$rightKey",
                    ) { c ->
                        Text(text = c.toString(), style = style)
                    }
                }
            } else {
                Text(text = char.toString(), style = style)
            }
        }
    }
}

// region Previews

@OptIn(ExperimentalTime::class)
@Composable
private fun TrackTripScreenPreview(state: TrackTripState) {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        Column(modifier = Modifier.background(KrailTheme.colors.surface)) {
            TitleBar(
                title = { Text(text = "Track Trip") },
                onNavActionClick = {},
                actions = {
                    if (state is TrackTripState.Tracking && state.isRefreshing) {
                        AnimatedDots(modifier = Modifier.size(48.dp, 16.dp))
                    }
                },
            )

            when (state) {
                is TrackTripState.Prompt -> PromptContent(
                    deepLink = state.deepLink,
                    onStartTracking = {},
                )

                is TrackTripState.Loading -> LoadingContent(deepLink = state.deepLink)

                is TrackTripState.Tracking -> JourneyContent(
                    journey = state.journey,
                    countdownDisplay = "Arriving in" to "12m 34s",
                    now = Instant.parse("2024-01-01T12:15:00Z"),
                )

                is TrackTripState.Arrived -> JourneyContent(
                    journey = state.journey,
                    countdownDisplay = "Arrived" to "just now",
                    now = Instant.parse("2024-01-01T12:35:00Z"),
                    isArrived = true,
                )

                TrackTripState.NotFound -> Column(modifier = Modifier.fillMaxWidth()) {
                    ErrorMessage(
                        title = "Service not found",
                        message = "This service wasn't found. It may have been cancelled or\u00A0changed.",
                        emoji = "\uD83D\uDEAB",
                        actionData = ActionData("Retry") { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp),
                    ) {
                        Text("Stop Tracking")
                    }
                }

                TrackTripState.Error -> Column(modifier = Modifier.fillMaxWidth()) {
                    ErrorMessage(
                        title = "Something went wrong",
                        message = "Couldn't reach transport services. Check your connection and try\u00A0again.",
                        actionData = ActionData("Retry") { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp),
                    ) {
                        Text("Stop Tracking")
                    }
                }

                is TrackTripState.AlreadyTracking -> AlreadyTrackingContent(
                    currentDeepLink = state.currentDeepLink,
                    requestedDeepLink = state.requestedDeepLink,
                    onStopCurrentAndTrackNew = {},
                )

                TrackTripState.Initial -> LoadingContent(deepLink = null)
                TrackTripState.ArrivedAndFinished -> Unit
            }
        }
    }
}

private val sampleDeepLink = TripDeepLink(
    fromStopId = "200060",
    toStopId = "215020",
    fromStopName = "Central Station",
    toStopName = "Parramatta Station",
    departureUtcDateTime = "2024-01-01T12:00:00Z",
    legs = emptyList(),
)

// Base journey: on-time train, vehicle mid-route (between Redfern and Parramatta)
private val sampleJourneyDisplay = TrackedJourneyDisplay(
    fromStopId = "200060",
    toStopId = "215020",
    fromStopName = "Central Station",
    toStopName = "Parramatta Station",
    originTime = "12:00",
    scheduledOriginTime = null,
    destinationTime = "12:30",
    originUtcDateTime = "2024-01-01T01:00:00Z",
    destinationUtcDateTime = "2024-01-01T01:30:00Z",
    travelTime = "30m",
    departureDeviation = DepartureDeviation.OnTime,
    legs = persistentListOf(
        TrackedLeg.Transport(
            transportMode = TransportMode.Train,
            lineName = "T1",
            lineColorCode = "#F6891F",
            headsign = "Emu Plains",
            stops = persistentListOf(
                TrackedStop(
                    stopId = "200060",
                    name = "Central Station",
                    scheduledTime = "12:00",
                    estimatedTime = null,
                    utcTime = "2024-01-01T01:00:00Z",
                    scheduledUtcTime = "2024-01-01T01:00:00Z",
                ),
                TrackedStop(
                    stopId = "200070",
                    name = "Redfern Station",
                    scheduledTime = "12:02",
                    estimatedTime = "12:03",
                    utcTime = "2024-01-01T01:03:00Z",
                    scheduledUtcTime = "2024-01-01T01:02:00Z",
                ),
                TrackedStop(
                    stopId = "215020",
                    name = "Parramatta Station",
                    scheduledTime = "12:30",
                    estimatedTime = null,
                    utcTime = "2024-01-01T01:30:00Z",
                    scheduledUtcTime = "2024-01-01T01:30:00Z",
                ),
            ),
        ),
    ),
)

// Delayed journey: train running 5 min late
private val sampleDelayedJourney = sampleJourneyDisplay.copy(
    originTime = "12:05",
    scheduledOriginTime = "12:00",
    departureDeviation = DepartureDeviation.Late("5 mins late"),
    legs = persistentListOf(
        (sampleJourneyDisplay.legs.first() as TrackedLeg.Transport).copy(
            stops = persistentListOf(
                TrackedStop(
                    stopId = "200060",
                    name = "Central Station",
                    scheduledTime = "12:00",
                    estimatedTime = "12:05",
                    utcTime = "2024-01-01T01:05:00Z",
                    scheduledUtcTime = "2024-01-01T01:00:00Z",
                ),
                TrackedStop(
                    stopId = "200070",
                    name = "Redfern Station",
                    scheduledTime = "12:02",
                    estimatedTime = "12:07",
                    utcTime = "2024-01-01T01:07:00Z",
                    scheduledUtcTime = "2024-01-01T01:02:00Z",
                ),
                TrackedStop(
                    stopId = "215020",
                    name = "Parramatta Station",
                    scheduledTime = "12:30",
                    estimatedTime = "12:35",
                    utcTime = "2024-01-01T01:35:00Z",
                    scheduledUtcTime = "2024-01-01T01:30:00Z",
                ),
            ),
        ),
    ),
)

@PreviewComponent
@Composable
private fun TrackTripScreenPromptPreview() {
    TrackTripScreenPreview(state = TrackTripState.Prompt(sampleDeepLink))
}

@PreviewComponent
@Composable
private fun TrackTripScreenLoadingPreview() {
    TrackTripScreenPreview(state = TrackTripState.Initial)
}

@PreviewComponent
@Composable
private fun TrackTripScreenTrackingPreview() {
    TrackTripScreenPreview(
        state = TrackTripState.Tracking(
            journey = sampleJourneyDisplay,
            isRefreshing = false,
        ),
    )
}

@PreviewComponent
@Composable
private fun TrackTripScreenTrackingRefreshingPreview() {
    TrackTripScreenPreview(
        state = TrackTripState.Tracking(
            journey = sampleJourneyDisplay,
            isRefreshing = true,
        ),
    )
}

@PreviewComponent
@Composable
private fun TrackTripScreenTrackingDelayedPreview() {
    TrackTripScreenPreview(
        state = TrackTripState.Tracking(
            journey = sampleDelayedJourney,
            isRefreshing = false,
        ),
    )
}

@PreviewComponent
@Composable
private fun TrackTripScreenNotFoundPreview() {
    TrackTripScreenPreview(state = TrackTripState.NotFound)
}

@PreviewComponent
@Composable
private fun TrackTripScreenErrorPreview() {
    TrackTripScreenPreview(state = TrackTripState.Error)
}

@PreviewComponent
@Composable
private fun TrackTripScreenArrivedPreview() {
    TrackTripScreenPreview(state = TrackTripState.Arrived(sampleJourneyDisplay))
}

@PreviewComponent
@Composable
private fun TrackTripScreenArrivedDelayedPreview() {
    TrackTripScreenPreview(state = TrackTripState.Arrived(sampleDelayedJourney))
}

@PreviewComponent
@Composable
private fun TrackTripScreenAlreadyTrackingPreview() {
    TrackTripScreenPreview(
        state = TrackTripState.AlreadyTracking(
            currentDeepLink = sampleDeepLink,
            requestedDeepLink = sampleDeepLink.copy(fromStopName = "Strathfield"),
        ),
    )
}

@OptIn(ExperimentalTime::class)
@PreviewComponent
@Composable
private fun TrackTripScreenTrackingMapButtonPreview() {
    // Shows the Map/Hide Map button in the actions row.
    // The map itself cannot render in Compose previews (requires ComponentActivity for permissions).
    TrackTripScreenPreview(
        state = TrackTripState.Tracking(
            journey = sampleJourneyDisplay,
            isRefreshing = false,
        ),
    )
}

// endregion
