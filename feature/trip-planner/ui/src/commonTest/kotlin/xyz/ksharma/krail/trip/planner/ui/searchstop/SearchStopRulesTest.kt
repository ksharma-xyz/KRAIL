package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchStopRulesTest {

    // region shouldShowPillRow

    private val centralRecent = SearchStopState.StopResult(
        "Central",
        "stop_central",
        persistentListOf(TransportMode.Train),
    )
    private val centralResult = SearchStopState.SearchResult.Stop(
        stopName = "Central",
        stopId = "stop_central",
        transportModeType = persistentListOf(TransportMode.Train),
    )
    private val homeUnset = StopLabel(emoji = "🏠", label = "Home")
    private val homeSet = StopLabel(
        emoji = "🏠",
        label = "Home",
        stopId = "stop_central",
        stopName = "Central Station",
    )

    @Test
    fun `pill row hidden on fresh Recent state with no recents`() {
        assertFalse(shouldShowPillRow(ListState.Recent, recentStops = emptyList(), stopLabels = listOf(homeSet)))
    }

    @Test
    fun `pill row visible in Recent state once at least one recent exists and a label is set`() {
        assertTrue(
            shouldShowPillRow(ListState.Recent, recentStops = listOf(centralRecent), stopLabels = listOf(homeSet)),
        )
    }

    @Test
    fun `pill row hidden during Results loading with no results yet`() {
        val state = ListState.Results(results = persistentListOf(), isLoading = true)
        assertFalse(shouldShowPillRow(state, recentStops = listOf(centralRecent), stopLabels = listOf(homeSet)))
    }

    @Test
    fun `pill row visible when Results state has at least one result and a label is set`() {
        val state = ListState.Results(results = persistentListOf(centralResult), isLoading = false)
        assertTrue(shouldShowPillRow(state, recentStops = emptyList(), stopLabels = listOf(homeSet)))
    }

    @Test
    fun `pill row hidden in NoMatch state`() {
        assertFalse(shouldShowPillRow(ListState.NoMatch, recentStops = listOf(centralRecent), stopLabels = listOf(homeSet)))
    }

    @Test
    fun `pill row hidden in Error state`() {
        assertFalse(shouldShowPillRow(ListState.Error, recentStops = listOf(centralRecent), stopLabels = listOf(homeSet)))
    }

    @Test
    fun `pill row hidden when no label is set yet, even with recents`() {
        // Fresh install: Home/Work are seeded but unset. No pills to show, so no
        // point showing the trailing Manage button either.
        assertFalse(
            shouldShowPillRow(ListState.Recent, recentStops = listOf(centralRecent), stopLabels = listOf(homeUnset)),
        )
    }

    @Test
    fun `pill row hidden when stopLabels is empty`() {
        assertFalse(
            shouldShowPillRow(ListState.Recent, recentStops = listOf(centralRecent), stopLabels = emptyList()),
        )
    }

    // endregion

    // region shouldShowEmptyStateStops

    @Test
    fun `empty-state stops shown on fresh Recent state with no recents`() {
        assertTrue(shouldShowEmptyStateStops(ListState.Recent, recentStops = emptyList()))
    }

    @Test
    fun `empty-state stops hidden once at least one recent exists`() {
        assertFalse(shouldShowEmptyStateStops(ListState.Recent, recentStops = listOf(centralRecent)))
    }

    @Test
    fun `empty-state stops hidden in Results state`() {
        val state = ListState.Results(results = persistentListOf(centralResult), isLoading = false)
        assertFalse(shouldShowEmptyStateStops(state, recentStops = emptyList()))
    }

    @Test
    fun `empty-state stops hidden in NoMatch and Error states`() {
        assertFalse(shouldShowEmptyStateStops(ListState.NoMatch, recentStops = emptyList()))
        assertFalse(shouldShowEmptyStateStops(ListState.Error, recentStops = emptyList()))
    }

    // endregion

    // region EMPTY_STATE_STOPS contents

    @Test
    fun `empty-state stops are the four curated interchanges in order`() {
        assertEquals(
            listOf("Town Hall Station", "Central Station", "Parramatta Station", "Wynyard Station"),
            EMPTY_STATE_STOPS.map { it.stopName },
        )
        assertEquals(
            listOf("200070", "200060", "215020", "200080"),
            EMPTY_STATE_STOPS.map { it.stopId },
        )
    }

    @Test
    fun `empty-state stops all carry at least one transport mode`() {
        assertTrue(EMPTY_STATE_STOPS.all { it.transportModeType.isNotEmpty() })
    }

    // endregion
}
