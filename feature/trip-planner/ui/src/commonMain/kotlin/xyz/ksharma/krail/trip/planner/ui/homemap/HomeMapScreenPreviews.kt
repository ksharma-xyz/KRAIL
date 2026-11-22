package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputUiState
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

// Previews of the map home. The map is the flat stand-in the Search Stop map draws in a preview, because a
// preview has no GPU, and everything that floats on it is real. `@PreviewScreen` expands each one to the phone,
// the 320 dp narrow window, the wide window, light and dark and the large font scales, so the 320 dp frame at
// 200 percent text is one of the frames each of these draws.

private const val WYNYARD_ID = "200080"
private const val WYNYARD_NAME = "Wynyard Station"

private val SampleTrips = persistentListOf(
    Trip(
        fromStopId = "200060",
        fromStopName = "Central Station",
        toStopId = "200070",
        toStopName = "Town Hall Station",
    ),
    Trip(
        fromStopId = "206710",
        fromStopName = "Parramatta Station",
        toStopId = WYNYARD_ID,
        toStopName = WYNYARD_NAME,
    ),
)

private val SampleLabels = persistentListOf(
    StopLabel(emoji = "H", label = "Home", stopId = WYNYARD_ID, stopName = WYNYARD_NAME),
    StopLabel(emoji = "W", label = "Work"),
)

private fun sampleState(toStop: StopItem? = null) = SavedTripsState(
    isSavedTripsLoading = false,
    isDiscoverAvailable = true,
    displayDiscoverBadge = true,
    savedTrips = SampleTrips,
    stopLabels = SampleLabels,
    toStop = toStop,
)

private fun previewHost(
    notices: List<HomeNotice> = emptyList(),
    pill: LocationPill = LocationPill.Hidden,
) = HomeMapHost(
    map = {
        HomeMapPane(
            mapUiState = MapUiState.Ready(mapDisplay = MapDisplay()),
            pins = HomePins(),
            allowPermissionRequest = false,
            onPermissionStatus = {},
            onMapEvent = {},
            onCardKey = {},
            ornamentTopPadding = it,
        )
    },
    notices = notices.toImmutableList(),
    locationPill = pill,
    onLocationClick = {},
    card = null,
    onCardClose = {},
)

/** The resting home: the map behind the cards, Discover beside Settings, the chips and the pill with its mic. */
@PreviewScreen
@Composable
private fun HomeMapRestingPreview() {
    PreviewTheme {
        HomeMapScreen(
            savedTripsState = sampleState(),
            host = previewHost(),
            aiState = AiSearchInputUiState(isFeatureEnabled = true),
        )
    }
}

/** Both notices under the top row, wrapping whole items at a large font scale. */
@PreviewScreen
@Composable
private fun HomeMapNoticesPreview() {
    PreviewTheme {
        HomeMapScreen(
            savedTripsState = sampleState(),
            host = previewHost(
                notices = listOf(HomeNotice.LocationPermission, HomeNotice.MapOffline),
                pill = LocationPill.OpenSettings,
            ),
        )
    }
}

/** A rider holding a destination in state: home still shows only the one pill, and no second bar. */
@PreviewScreen
@Composable
private fun HomeMapHoldingStopPreview() {
    PreviewTheme {
        HomeMapScreen(
            savedTripsState = sampleState(toStop = StopItem(stopId = WYNYARD_ID, stopName = WYNYARD_NAME)),
            host = previewHost(),
        )
    }
}

/** No saved trips and nothing set: only the map, the chips and the pill, with no empty state card. */
@PreviewScreen
@Composable
private fun HomeMapNoTripsPreview() {
    PreviewTheme {
        HomeMapScreen(
            savedTripsState = SavedTripsState(isSavedTripsLoading = false, stopLabels = SampleLabels),
            host = previewHost(),
        )
    }
}
