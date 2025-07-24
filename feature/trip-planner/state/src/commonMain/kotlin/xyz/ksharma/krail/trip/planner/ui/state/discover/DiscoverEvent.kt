package xyz.ksharma.krail.trip.planner.ui.state.discover

sealed interface DiscoverEvent {
    data object ButtonClicked: DiscoverEvent
}
