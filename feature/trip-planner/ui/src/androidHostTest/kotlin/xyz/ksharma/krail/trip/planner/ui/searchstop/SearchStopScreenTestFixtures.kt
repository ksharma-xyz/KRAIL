package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * Shared fixtures for SearchStopScreen Compose UI tests. Keep stops + labels here so
 * each test can build its specific scenario via copy() rather than hand-rolling
 * SearchStopState every time.
 */
internal object SearchStopFixtures {

    val centralStop = SearchStopState.StopResult(
        "Central Station",
        "stop_central",
        persistentListOf(TransportMode.Train),
    )

    val townHallStop = SearchStopState.StopResult(
        "Town Hall",
        "stop_town_hall",
        persistentListOf(TransportMode.Train, TransportMode.LightRail),
    )

    val homeUnset = StopLabel(emoji = "🏠", label = "Home")
    val homeSet = StopLabel(
        emoji = "🏠",
        label = "Home",
        stopId = "stop_central",
        stopName = "Central Station",
    )
    val workUnset = StopLabel(emoji = "💼", label = "Work")
    val gymUnset = StopLabel(emoji = "🏋", label = "Gym")

    /** Idle Recent state with one recent stop and the seeded Home/Work labels. */
    fun recentWithDefaults(): SearchStopState = SearchStopState(
        listState = ListState.Recent,
        recentStops = persistentListOf(centralStop, townHallStop),
        stopLabels = persistentListOf(homeUnset, workUnset),
    )

    /** Recent state with Home already set — used for "tap set pill" navigation tests. */
    fun recentWithHomeSet(): SearchStopState = SearchStopState(
        listState = ListState.Recent,
        recentStops = persistentListOf(centralStop, townHallStop),
        stopLabels = persistentListOf(homeSet, workUnset),
    )

    /** Fresh-install state — no recents, defaults seeded. Pill row should be hidden. */
    fun freshInstall(): SearchStopState = SearchStopState(
        listState = ListState.Recent,
        recentStops = persistentListOf(),
        stopLabels = persistentListOf(homeUnset, workUnset),
    )
}
