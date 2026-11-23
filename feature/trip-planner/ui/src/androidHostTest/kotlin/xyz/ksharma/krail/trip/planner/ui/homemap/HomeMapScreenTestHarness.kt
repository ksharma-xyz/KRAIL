package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState

/**
 * The map home with an empty map slot, for the layout tests of the pieces only the map home draws (the chip row,
 * the reorder chrome and the one column). The classic home no longer draws them, so these tests read the
 * screen that does.
 */
@Composable
internal fun HomeMapScreenForTest(
    savedTripsState: SavedTripsState,
    actions: HomeMapActions = HomeMapActions(),
) {
    HomeMapScreen(
        savedTripsState = savedTripsState,
        host = HomeMapHost(
            map = { Box(Modifier.fillMaxSize()) },
            notices = persistentListOf(),
            locationPill = LocationPill.Hidden,
            onLocationClick = {},
            card = null,
            onCardClose = {},
        ),
        actions = actions,
    )
}
